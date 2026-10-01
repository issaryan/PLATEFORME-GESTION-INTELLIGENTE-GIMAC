import type { Page } from '@playwright/test'
import { ADRESSES, expect, recueillirCouverture, test } from '../fixtures'
import { CONNEXION_E2E, Passerelle, PERMISSIONS_SUPER_ADMIN } from './passerelle'

// Fonctionnalité : connexion et sessions entièrement sur la plateforme (D-65, docs/lot1/connexion.md).
// Identifiant et mot de passe, puis code à six chiffres ; messages par code d'erreur ; mot de passe oublié
// et nouveau mot de passe ; premier accès avec code TOTP ; fermeture après 30 minutes d'inactivité.
// Services simulés : mot de passe de démonstration, tout code de six chiffres accepté ; passerelle simulée
// par Playwright pour les états propres à MS-GATEWAY. Aucune page du fournisseur d'identité.
test.use({ connecte: false })

const DEMO = 'Gimac-Demo-2026!'

async function saisirIdentifiants(page: Page, identifiant: string, motDePasse: string) {
  await page.locator('input[name="identifiant"]').fill(identifiant)
  await page.locator('input[name="motDePasse"]').fill(motDePasse)
  await page.getByRole('button', { name: 'Se connecter' }).click()
}

test.describe('Fonctionnalité : connexion sur la plateforme', () => {
  test('Scénario : connexion réussie avec code, retour à la page demandée', async ({ page }) => {
    await test.step('Étant donné un utilisateur sans session qui ouvre une page de la console', async () => {
      await page.goto(`${ADRESSES.console}/console/comptes`)
      await expect(page).toHaveURL(/\/connexion\?retour=%2Fconsole%2Fcomptes$/)
      await expect(page.getByRole('heading', { level: 1 })).toContainText('GIMAC')
      await expect(page.getByText(/fournisseur d.identité/)).toHaveCount(0)
    })
    await test.step('Quand il saisit son identifiant et son mot de passe, un code est demandé', async () => {
      await saisirIdentifiants(page, 'rssi', DEMO)
      await expect(page.getByText('Mot de passe vérifié pour rssi.', { exact: false })).toBeVisible()
      await expect(page.locator('input[name="motDePasse"]')).toHaveCount(0)
      await page.locator('input[name="code"]').fill('12')
      await page.getByRole('button', { name: 'Vérifier le code' }).click()
      await expect(page.getByText('Le code compte exactement six chiffres.')).toBeVisible()
    })
    await test.step('Alors, avec un code de six chiffres, la page demandée s’ouvre sous son profil', async () => {
      await recueillirCouverture(page)
      await page.locator('input[name="code"]').fill('000000')
      await page.getByRole('button', { name: 'Vérifier le code' }).click()
      await expect(page).toHaveURL(/\/console\/comptes$/)
      await expect(page.getByRole('heading', { level: 1 })).toHaveText('Comptes')
      await expect(page.getByText('Carine Ebongue Ndjock').first()).toBeVisible()
    })
  })

  test('Scénario : refus par code d’erreur (identifiants, compte désactivé, couche refusée, tentatives)', async ({ page }) => {
    await page.goto(`${ADRESSES.console}/connexion`)
    await page.getByRole('button', { name: 'Se connecter' }).click()
    await expect(page.getByText('Saisissez votre identifiant.')).toBeVisible()
    await saisirIdentifiants(page, 'compte.desactive', DEMO)
    await expect(page.getByText(/Ce compte est désactivé/)).toBeVisible()
    await saisirIdentifiants(page, 'correspondant-a', DEMO)
    await expect(page.getByText(/Ce compte n'ouvre pas cette application/)).toBeVisible()
    for (let i = 0; i < 5; i++) {
      await saisirIdentifiants(page, 'super.admin', `faux-${i}`)
      await expect(page.getByText('Identifiant ou mot de passe incorrect.')).toBeVisible()
      await expect(page.locator('input[name="motDePasse"]')).toHaveValue('')
    }
    await saisirIdentifiants(page, 'super.admin', DEMO)
    await expect(page.getByText(/Trop de tentatives/)).toBeVisible()
  })

  test('Scénario : sur la passerelle, code refusé puis accepté ; corps du contrat et jeton anti-CSRF', async ({ page, context }) => {
    const gw = new Passerelle({ session: null })
    await gw.brancher(page)
    await context.addCookies([{ name: 'XSRF-TOKEN', value: 'csrf-e2e', url: ADRESSES.consoleHttp }])
    await page.goto(`${ADRESSES.consoleHttp}/connexion`)
    await saisirIdentifiants(page, 'super.admin', CONNEXION_E2E.motDePasse)
    await page.locator('input[name="code"]').fill('999999')
    await page.getByRole('button', { name: 'Vérifier le code' }).click()
    await expect(page.getByText(/Code incorrect ou expiré/)).toBeVisible()
    await expect(page.getByText('e2e-CNX_IDENTIFIANTS_INVALIDES')).toBeVisible()
    await page.getByRole('button', { name: "Changer d'identifiant" }).click()
    await saisirIdentifiants(page, 'super.admin', CONNEXION_E2E.motDePasse)
    await page.locator('input[name="code"]').fill(CONNEXION_E2E.code)
    await page.getByRole('button', { name: 'Vérifier le code' }).click()
    await expect(page.getByRole('heading', { name: 'Tableau de bord du super administrateur' })).toBeVisible()
    const appels = gw.appels.filter((a) => a.chemin === '/api/connexion')
    expect(appels.map((a) => a.corps)).toEqual([
      { identifiant: 'super.admin', motDePasse: CONNEXION_E2E.motDePasse, couche: 'console' },
      { identifiant: 'super.admin', motDePasse: CONNEXION_E2E.motDePasse, couche: 'console', code: '999999' },
      { identifiant: 'super.admin', motDePasse: CONNEXION_E2E.motDePasse, couche: 'console' },
      { identifiant: 'super.admin', motDePasse: CONNEXION_E2E.motDePasse, couche: 'console', code: CONNEXION_E2E.code },
    ])
    expect(appels.every((a) => a.entetes['x-xsrf-token'] === 'csrf-e2e')).toBe(true)
    expect(gw.appels.some((a) => a.chemin.startsWith('/oauth2'))).toBe(false)
  })
})

test.describe('Fonctionnalité : mot de passe oublié', () => {
  test('Scénario : demande de lien, toujours la même réponse', async ({ page }) => {
    await page.goto(`${ADRESSES.departement}/connexion`)
    await page.getByRole('link', { name: 'Mot de passe oublié ?' }).click()
    await expect(page).toHaveURL(/\/connexion\/mot-de-passe-oublie$/)
    await page.getByRole('button', { name: 'Recevoir un lien de réinitialisation' }).click()
    await expect(page.getByText('Saisissez votre identifiant ou votre adresse électronique.')).toBeVisible()
    await page.locator('input[name="identifiant"]').fill('personne@nulle-part.org')
    await page.getByRole('button', { name: 'Recevoir un lien de réinitialisation' }).click()
    await expect(page.getByText(/Si un compte actif correspond/)).toBeVisible()
    await page.getByRole('link', { name: 'Retour à la connexion' }).click()
    await expect(page.locator('input[name="motDePasse"]')).toBeVisible()
  })

  test('Scénario : nouveau mot de passe depuis le lien (jeton invalide, politique, succès)', async ({ page }) => {
    const gw = new Passerelle({ session: null })
    await gw.brancher(page)
    await page.goto(`${ADRESSES.consoleHttp}/connexion/nouveau-mot-de-passe?jeton=perime`)
    const saisir = async (mdp: string) => {
      await page.locator('input[name="nouveauMotDePasse"]').fill(mdp)
      await page.locator('input[name="confirmationMotDePasse"]').fill(mdp)
      await page.getByRole('button', { name: 'Enregistrer le mot de passe' }).click()
    }
    await expect(page.getByText(/Au moins 12 caractères/)).toBeVisible()
    await saisir('Nouveau-Mdp-2026!')
    await expect(page.getByText(/Ce lien est inconnu, expiré ou déjà utilisé/)).toBeVisible()
    await expect(page.getByRole('link', { name: 'Demander un nouveau lien' })).toBeVisible()
    await page.goto(`${ADRESSES.consoleHttp}/connexion/nouveau-mot-de-passe?jeton=jeton-valide-e2e-0123456789`)
    await saisir('Court!')
    await expect(page.getByText('Ce mot de passe ne respecte pas la politique de sécurité.')).toBeVisible()
    await expect(page.getByText('au moins 12 caractères', { exact: true })).toBeVisible()
    await expect(page.getByText('au moins un chiffre')).toBeVisible()
    await saisir('Nouveau-Mdp-2026!')
    await expect(page.getByText(/Votre mot de passe a été enregistré/)).toBeVisible()
    expect(gw.appels.filter((a) => a.chemin === '/api/connexion/nouveau-mot-de-passe').at(-1)!.corps).toEqual({ jeton: 'jeton-valide-e2e-0123456789', motDePasse: 'Nouveau-Mdp-2026!' })
    await page.getByRole('link', { name: 'Se connecter' }).click()
    await expect(page.locator('input[name="identifiant"]')).toBeVisible()
  })
})

test.describe('Fonctionnalité : premier accès depuis l’écran de connexion', () => {
  test('Scénario : compte provisoire, mot de passe, code TOTP, fiche, charte, puis connexion', async ({ page }) => {
    const NOUVEAU = 'Agent-Nouveau-2026!'
    await page.goto(`${ADRESSES.departement}/des/tableau-de-bord`)
    await saisirIdentifiants(page, 'nouvel.agent', DEMO)
    await expect(page.getByRole('heading', { level: 1 })).toHaveText('Premier accès à la plateforme')
    await expect(page.locator('.gm-step')).toHaveCount(5)
    await page.locator('input[name="motDePasseActuel"]').fill(DEMO)
    await page.locator('input[name="nouveauMotDePasse"]').fill(NOUVEAU)
    await page.locator('input[name="confirmationMotDePasse"]').fill(NOUVEAU)
    await page.getByRole('button', { name: 'Enregistrer le mot de passe' }).click()
    await expect(page.getByRole('img', { name: /Code QR de configuration/ })).toBeVisible()
    await page.locator('input[name="codeTotp"]').fill('314159')
    await page.locator('input[name="libelleTotp"]').fill('Téléphone professionnel')
    await page.getByRole('button', { name: 'Vérifier et activer' }).click()
    await page.locator('input[name="telephone"]').fill('+237 699 31 41 59')
    await page.getByRole('button', { name: 'Continuer' }).click()
    await page.getByLabel('J’ai lu la charte d’usage et je l’accepte').check()
    await page.getByRole('button', { name: 'Valider et continuer' }).click()
    await expect(page.getByRole('heading', { name: 'Vous connecter' })).toBeVisible()
    await expect(page.locator('input[name="motDePasse"]')).toHaveCount(0)
    await recueillirCouverture(page)
    await page.locator('input[name="code"]').fill('271828')
    await page.getByRole('button', { name: 'Ouvrir ma session' }).click()
    await expect(page.getByRole('heading', { level: 1 })).toHaveText('Tableau de bord')
    expect(await page.evaluate(() => JSON.parse(sessionStorage.getItem('gm.sessionDemo')!))).toMatchObject({ identifiant: 'nouvel.agent', premierAcces: false })
  })
})

test.describe('Fonctionnalité : fermeture de la session après 30 minutes d’inactivité', () => {
  test.use({ connecte: true })

  test('Scénario : avertissement à 28 minutes, « Rester connecté », puis fermeture à 30 minutes', async ({ page }) => {
    await page.clock.install()
    await page.goto(`${ADRESSES.console}/console/tableau-de-bord`)
    await expect(page.getByRole('heading', { name: 'Tableau de bord du super administrateur' })).toBeVisible()
    await test.step('Quand l’utilisateur reste inactif 28 minutes, la fenêtre d’avertissement s’ouvre', async () => {
      await page.clock.fastForward('28:01')
      const d = page.getByRole('alertdialog', { name: 'Votre session va expirer' })
      await expect(d).toBeVisible()
      await expect(page.getByTestId('compte-a-rebours')).toHaveText(/^01:5\d$/)
      await d.getByRole('button', { name: 'Rester connecté' }).click()
      await expect(d).toHaveCount(0)
    })
    await test.step('Et l’activité (clavier, souris) repousse l’échéance', async () => {
      await page.clock.fastForward('20:00')
      await page.mouse.move(200, 200)
      await page.keyboard.press('Shift')
      await page.clock.fastForward('20:00')
      await expect(page.getByRole('alertdialog')).toHaveCount(0)
    })
    await test.step('Alors, sans activité, la session est fermée à 30 minutes avec le message dédié', async () => {
      await recueillirCouverture(page)
      await page.clock.fastForward('30:05')
      await expect(page).toHaveURL(/\/connexion\?motif=inactivite&minutes=30&retour=%2Fconsole%2Ftableau-de-bord/)
      await expect(page.getByText('Session fermée après 30 minutes d\'inactivité.')).toBeVisible()
    })
  })

  test('Scénario : sur la passerelle, 401 CNX_SESSION_EXPIREE ramène à la connexion ; prolongation et appels de fond', async ({ page }) => {
    const gw = new Passerelle({ session: { permissions: PERMISSIONS_SUPER_ADMIN } })
    await gw.brancher(page)
    await page.clock.install()
    await page.goto(`${ADRESSES.consoleHttp}/console/mon-compte`)
    await expect(page.getByRole('heading', { level: 1, name: 'Mon compte' })).toBeVisible()
    await page.clock.fastForward('28:30')
    await page.getByRole('alertdialog').getByRole('button', { name: 'Rester connecté' }).click()
    await expect(page.getByRole('alertdialog')).toHaveCount(0)
    expect(gw.appels.filter((a) => a.chemin === '/api/session/prolongation' && a.methode === 'POST')).toHaveLength(1)
    gw.o.sessionExpiree = true
    await page.getByRole('button', { name: 'Modifier ma fiche' }).click()
    await page.getByRole('dialog').getByRole('button', { name: 'Enregistrer ma fiche' }).click()
    await expect(page).toHaveURL(/\/connexion\?motif=expiree&retour=%2Fconsole%2Fmon-compte/)
    await expect(page.getByText('Votre session a expiré. Reconnectez-vous pour continuer.')).toBeVisible()
  })
})
