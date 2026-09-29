import { ADRESSES, expect, recueillirCouverture, test } from '../fixtures'
import { naviguer, signer, signerEtTerminer } from './outils'
import { Passerelle, PERMISSIONS_SUPER_ADMIN } from './passerelle'

// Fonctionnalité : la console sur la passerelle réelle (VITE_SERVICES=http), dont Playwright simule les
// réponses : session à deux facteurs, flux SSE de supervision avec reconnexion, réauthentification FIDO2
// par navigator.credentials (428, 403, 409), enrôlement d'une clé, déconnexion.

test.describe('Fonctionnalité : console sur la passerelle', () => {
  test('Scénario : supervision en temps réel par le flux SSE, avec reconnexion', async ({ page }) => {
    const gw = new Passerelle({ session: { permissions: PERMISSIONS_SUPER_ADMIN } })
    await gw.brancher(page)
    await page.goto(`${ADRESSES.consoleHttp}/console/tableau-de-bord`)
    await expect(page.getByRole('heading', { level: 1 })).toHaveText('Tableau de bord du super administrateur')
    await test.step('Alors l’impulsion du flux remplace l’instantané initial', async () => {
      await expect(page.getByRole('region', { name: 'État en temps réel' }).getByText('Opérationnelle', { exact: true }).first()).toBeVisible()
    })
    await test.step('Et la fermeture du flux est signalée puis suivie d’une reconnexion', async () => {
      await expect.poll(() => gw.fluxOuverts, { timeout: 15_000 }).toBeGreaterThanOrEqual(3)
      await expect(page.getByRole('cell', { name: 'gimac-app-01' })).toBeVisible()
    })
  })

  test('Scénario : changer les profils d’un compte avec la clé de sécurité (428 puis succès)', async ({ page }) => {
    const gw = new Passerelle({ session: { permissions: PERMISSIONS_SUPER_ADMIN } })
    await gw.brancher(page)
    await page.goto(`${ADRESSES.consoleHttp}/console/comptes`)
    await page.getByRole('searchbox', { name: 'Rechercher', exact: true }).fill('biyong')
    await page.getByRole('link', { name: 'r.biyong' }).click()
    await page.getByRole('button', { name: 'Modifier les profils' }).click()
    await page.getByRole('dialog').getByLabel('Normes et agréments').uncheck()
    await page.getByRole('dialog').getByLabel('Normes et agréments').check()
    await page.getByRole('button', { name: 'Continuer vers la signature' }).click()
    const d = await signer(page, 'Revue annuelle des habilitations')
    await expect(d.getByText('Cet acte exige une réauthentification avec votre clé de sécurité.')).toBeVisible()
    await d.getByRole('button', { name: 'Réessayer' }).click()
    await expect(d.getByText('Signature confirmée')).toBeVisible()
    await d.getByRole('button', { name: 'Continuer' }).click()
    const appel = gw.appels.filter((a) => a.chemin.endsWith('/profils')).at(-1)!
    expect(JSON.parse(Buffer.from(appel.entetes['x-reauth-assertion'], 'base64').toString())).toMatchObject({ challengeId: 'defi-1', id: 'cle-e2e', clientDataJSON: 'AQI' })
    expect(appel.corps).toEqual({ profils: ['ROLE_SNA'], motif: 'Revue annuelle des habilitations' })
    expect(gw.appels.filter((a) => a.chemin === '/bff/console/iam/reauthentification/defi').map((a) => a.corps)).toContainEqual({ purpose: 'ADMIN_SENSITIVE' })
    await page.getByRole('button', { name: 'Supprimer le compte' }).click()
    await page.getByRole('alertdialog').getByLabel('Je confirme : cette action est irréversible.').check()
    await page.getByRole('button', { name: 'Continuer vers la signature' }).click()
    await signer(page, 'Départ définitif de la collaboratrice')
    await expect(page.getByRole('heading', { level: 1 })).toHaveText('Comptes')
    expect(gw.appels.some((a) => a.methode === 'DELETE' && a.chemin === '/bff/console/iam/comptes/u-sna')).toBe(true)
  })

  test('Scénario : politique refusée champ par champ par MS-IAM puis appliquée', async ({ page }) => {
    const gw = new Passerelle({ session: { permissions: PERMISSIONS_SUPER_ADMIN } })
    await gw.brancher(page)
    await page.goto(`${ADRESSES.consoleHttp}/console/politique-securite`)
    await page.getByRole('button', { name: 'Appliquer la politique' }).click()
    await signer(page, 'Réduction de la durée d’inactivité')
    await expect(page.getByText('Valeur refusée par MS-IAM : 5 à 120.')).toBeVisible()
    await expect(page.getByText('La somme des classes de caractères dépasse la longueur minimale.')).toBeVisible()
    await page.getByRole('button', { name: 'Appliquer la politique' }).click()
    await signerEtTerminer(page, 'Réduction de la durée d’inactivité')
    await expect(page.getByText('La politique de sécurité a été appliquée.')).toBeVisible()
    await naviguer(page, 'Profils et permissions')
    await expect(page.getByRole('cell', { name: /ROLE_SNA/ })).toBeVisible()
  })

  test('Scénario : aucune clé enrôlée, enrôlement depuis Mon compte, puis déconnexion', async ({ page }) => {
    const gw = new Passerelle({ session: { permissions: PERMISSIONS_SUPER_ADMIN }, sansCle: true })
    await gw.brancher(page)
    await page.goto(`${ADRESSES.consoleHttp}/console/comptes/u-rssi`)
    await page.getByRole('button', { name: 'Modifier les profils' }).click()
    await page.getByRole('button', { name: 'Continuer vers la signature' }).click()
    const d = await signer(page, 'Revue des habilitations du RSSI')
    await expect(d.getByText(/Aucune clé de sécurité n'est enrôlée/)).toBeVisible()
    await d.getByRole('link', { name: 'Enrôler une clé dans « Mon compte »' }).click()
    await expect(page.getByRole('heading', { level: 1, name: 'Mon compte' })).toBeVisible()
    await page.locator('input[name="nomNouvelleCle"]').fill('Clé FIDO2 du poste')
    await page.getByRole('button', { name: 'Enrôler une clé' }).click()
    await expect(page.getByText('La clé « Clé FIDO2 du poste » est enrôlée.')).toBeVisible()
    const enrolement = gw.appels.find((a) => a.methode === 'POST' && a.chemin === '/bff/console/iam/moi/cles-securite')!
    expect(enrolement.corps).toEqual({ challengeId: 'enr-1', nom: 'Clé FIDO2 du poste', clientDataJSON: 'Bg', attestationObject: 'Bwg' })
    await page.getByRole('row', { name: /YubiKey \(bureau\)/ }).getByRole('button', { name: 'Supprimer la clé' }).click()
    await page.getByRole('alertdialog').getByRole('button', { name: 'Supprimer la clé' }).click()
    await expect(page.getByText('La clé « YubiKey (bureau) » a été supprimée.')).toBeVisible()
    await page.getByRole('button', { name: 'Fermer', exact: true }).click()
    await expect(page.getByText('La session a été fermée.')).toBeVisible()
    await page.locator('input[name="telephone"]').fill('+237 699 99 88 77')
    await page.getByRole('button', { name: 'Enregistrer ma fiche' }).click()
    await expect(page.getByText('Votre fiche a été enregistrée.')).toBeVisible()
    await recueillirCouverture(page)
    await naviguer(page, 'Se déconnecter')
    await expect(page.getByRole('button', { name: 'Se connecter avec le fournisseur d\'identité' })).toBeVisible()
    expect(gw.appels.some((a) => a.chemin === '/api/logout')).toBe(true)
  })

  test('Scénario : premier accès sur la passerelle, reconnexion pour les actions requises', async ({ page }) => {
    const gw = new Passerelle({ session: { permissions: PERMISSIONS_SUPER_ADMIN }, premierAcces: true })
    await gw.brancher(page)
    await page.goto(`${ADRESSES.consoleHttp}/console/tableau-de-bord`)
    await expect(page.getByRole('heading', { level: 1 })).toHaveText('Premier accès à la plateforme')
    await page.locator('input[name="telephone"]').fill('+237 699 12 12 12')
    await page.getByLabel('J’ai lu la charte d’usage et je l’accepte').check()
    await page.getByRole('button', { name: 'Valider et continuer' }).click()
    await expect(page.getByRole('heading', { name: 'Sécuriser votre authentification' })).toBeVisible()
    expect(gw.appels.find((a) => a.chemin.endsWith('/premiere-configuration'))!.corps).toEqual({ telephone: '+237 699 12 12 12', langue: 'fr', fonction: 'Agent', charteAcceptee: true })
    await recueillirCouverture(page)
    await page.getByRole('button', { name: 'Continuer' }).click()
    await expect(page.getByRole('heading', { name: 'Authentification GIMAC' })).toBeVisible()
    expect(gw.appels.some((a) => a.chemin === '/api/logout')).toBe(true)
  })

  test('Scénario : sans session ou sans second facteur, aucun écran n’est servi', async ({ page }) => {
    const sans = new Passerelle({ session: null })
    await sans.brancher(page)
    await page.goto(`${ADRESSES.consoleHttp}/console/comptes`)
    // Session absente (401) : retour immédiat à l'authentification par la passerelle.
    await expect(page.getByRole('heading', { name: 'Authentification GIMAC' })).toBeVisible()
    await page.unrouteAll()
    const unFacteur = new Passerelle({ session: { permissions: PERMISSIONS_SUPER_ADMIN, doubleFacteur: false } })
    await unFacteur.brancher(page)
    await page.goto(`${ADRESSES.consoleHttp}/console/comptes`)
    await expect(page.getByText(/exige une authentification à deux facteurs/)).toBeVisible()
    await recueillirCouverture(page)
    await page.getByRole('button', { name: 'Se connecter' }).click()
    await expect(page.getByRole('heading', { name: 'Authentification GIMAC' })).toBeVisible()
  })
})
