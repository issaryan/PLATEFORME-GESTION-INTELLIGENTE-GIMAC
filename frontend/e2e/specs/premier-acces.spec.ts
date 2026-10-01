import { ADRESSES, demo, expect, recueillirCouverture, test } from '../fixtures'
import { naviguer } from './outils'
import type { Page } from '@playwright/test'

// Fonctionnalité : à son premier accès, chaque utilisateur sécurise et complète le compte créé par
// l'administration avant tout tableau de bord, dans les trois couches, entièrement sur la plateforme
// (D-62, D-65) : mot de passe, code TOTP, fiche, charte ; « Mon compte » reste ensuite disponible.

const PROVISOIRE = 'Gimac-Demo-2026!'
const NOUVEAU = 'Mon-Secret-Gimac-2026!'

async function completerPremierAcces(page: Page) {
  await expect(page.getByRole('heading', { level: 1 })).toHaveText('Premier accès à la plateforme')
  await expect(page.getByRole('navigation', { name: 'Navigation principale' }).getByRole('link', { name: 'Mon compte' })).toBeVisible()
  await test.step('Quand il remplace son mot de passe provisoire (politique rappelée)', async () => {
    await expect(page.getByText(/Au moins 12 caractères/)).toBeVisible()
    await page.getByRole('button', { name: 'Enregistrer le mot de passe' }).click()
    await expect(page.getByText('Saisissez votre mot de passe actuel.')).toBeVisible()
    await page.locator('input[name="motDePasseActuel"]').fill(PROVISOIRE)
    await page.locator('input[name="nouveauMotDePasse"]').fill('simple')
    await page.locator('input[name="confirmationMotDePasse"]').fill('simple')
    await page.getByRole('button', { name: 'Enregistrer le mot de passe' }).click()
    await expect(page.getByText(/au moins une majuscule/)).toBeVisible()
    await page.locator('input[name="motDePasseActuel"]').fill(PROVISOIRE)
    await page.locator('input[name="nouveauMotDePasse"]').fill(NOUVEAU)
    await page.locator('input[name="confirmationMotDePasse"]').fill(NOUVEAU)
    await page.getByRole('button', { name: 'Enregistrer le mot de passe' }).click()
  })
  await test.step('Et il associe son application d’authentification (code QR, clé sur demande)', async () => {
    await expect(page.getByRole('img', { name: /Code QR de configuration/ })).toBeVisible()
    await page.getByRole('button', { name: 'Afficher la clé en clair' }).click()
    await expect(page.getByTestId('totp-secret')).toHaveText(/^([A-Z2-7]{4} ){7}[A-Z2-7]{4}$/)
    await page.locator('input[name="codeTotp"]').fill('000000')
    await page.getByRole('button', { name: 'Vérifier et activer' }).click()
  })
  await test.step('Et il renseigne ses coordonnées, puis accepte la charte', async () => {
    await page.getByRole('button', { name: 'Continuer' }).click()
    await expect(page.getByText('Le téléphone professionnel est obligatoire.')).toBeVisible()
    await page.locator('input[name="telephone"]').fill('+237 677 21 43 65')
    await page.locator('input[name="fonction"]').fill('Analyste')
    await page.getByRole('button', { name: 'Continuer' }).click()
    await page.getByRole('button', { name: 'Valider et continuer' }).click()
    await expect(page.getByText('Vous devez accepter la charte d’usage pour continuer.')).toBeVisible()
    await page.getByLabel('J’ai lu la charte d’usage et je l’accepte').check()
    await page.getByRole('button', { name: 'Valider et continuer' }).click()
  })
}

test.describe('Fonctionnalité : premier accès et Mon compte dans les trois couches', () => {
  test('Scénario : premier accès dans la console', async ({ page }) => {
    await demo(page, { profil: 'ROLE_RSSI', premierAcces: true })
    await page.goto(`${ADRESSES.console}/console/comptes`)
    await completerPremierAcces(page)
    await expect(page.getByRole('heading', { level: 1 })).toHaveText('Comptes')
  })

  test('Scénario : premier accès dans l’application du département, puis Mon compte', async ({ page }) => {
    await demo(page, { premierAcces: true })
    await page.goto(`${ADRESSES.departement}/des/tableau-de-bord`)
    await completerPremierAcces(page)
    await expect(page.getByRole('heading', { level: 1 })).toHaveText('Tableau de bord')
    await naviguer(page, 'Mon compte')
    await expect(page.getByRole('heading', { level: 1, name: 'Mon compte' })).toBeVisible()
    await expect(page.getByText('Aucune clé de sécurité enrôlée')).toBeVisible()
    await page.getByRole('button', { name: 'Modifier ma fiche' }).click()
    await page.getByRole('dialog').locator('select[name="langue"]').selectOption('en')
    await page.getByRole('dialog').getByRole('button', { name: 'Enregistrer ma fiche' }).click()
    await expect(page.getByText('Votre fiche a été enregistrée.')).toBeVisible()
  })

  test('Scénario : premier accès dans le portail des participants, Mon compte, puis déconnexion', async ({ page }) => {
    await demo(page, { premierAcces: true })
    await page.goto(`${ADRESSES.portail}/portail/liaisons`)
    await completerPremierAcces(page)
    await naviguer(page, 'Mon compte')
    await page.getByRole('button', { name: 'Reconfigurer mon code TOTP' }).click()
    const d = page.getByRole('dialog')
    await expect(d.getByText(/sera remplacé/)).toBeVisible()
    await d.locator('input[name="codeTotp"]').fill('112233')
    // Saisie en cours : l'abandon est confirmé avant de fermer.
    await d.getByRole('button', { name: 'Annuler' }).click()
    await expect(d.getByText('Abandonner la saisie ?')).toBeVisible()
    await d.getByRole('button', { name: 'Reprendre la saisie' }).click()
    await d.getByRole('button', { name: 'Vérifier et activer' }).click()
    await expect(page.getByText(/Votre application d’authentification est associée/)).toBeVisible()
    await page.getByRole('button', { name: 'Changer mon mot de passe' }).click()
    await page.getByRole('dialog').locator('input[name="motDePasseActuel"]').fill('faux')
    await page.getByRole('dialog').locator('input[name="nouveauMotDePasse"]').fill(NOUVEAU)
    await page.getByRole('dialog').locator('input[name="confirmationMotDePasse"]').fill(NOUVEAU)
    await page.getByRole('dialog').getByRole('button', { name: 'Enregistrer le mot de passe' }).click()
    await expect(page.getByText('Mot de passe actuel incorrect.')).toBeVisible()
    await page.getByRole('dialog').getByRole('button', { name: 'Fermer' }).click()
    await recueillirCouverture(page)
    await naviguer(page, 'Se déconnecter')
    await expect(page.getByText('Vous êtes déconnecté.')).toBeVisible()
    await expect(page.getByRole('button', { name: 'Se connecter' })).toBeVisible()
  })

  test('Scénario : sans premier accès en attente, les pages sont servies directement', async ({ page }) => {
    await page.goto(`${ADRESSES.departement}/des/mon-compte`)
    await expect(page.getByRole('heading', { level: 1, name: 'Mon compte' })).toBeVisible()
    await expect(page.getByText('YubiKey 5 NFC (bureau)')).toBeVisible()
    await page.goto(`${ADRESSES.portail}/portail/mon-compte`)
    await expect(page.getByText('correspondant-a')).toBeVisible()
  })
})
