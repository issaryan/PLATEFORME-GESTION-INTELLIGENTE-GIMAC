import { ADRESSES, demo, expect, recueillirCouverture, test } from '../fixtures'
import { naviguer } from './outils'
import type { Page } from '@playwright/test'

// Fonctionnalité : à son premier accès, chaque utilisateur complète le compte créé par l'administration
// avant tout tableau de bord, dans les trois couches (D-62) ; « Mon compte » reste ensuite disponible.

async function completerPremierAcces(page: Page, o: { cle: boolean }) {
  await expect(page.getByRole('heading', { level: 1 })).toHaveText('Premier accès à la plateforme')
  await expect(page.getByRole('navigation', { name: 'Navigation principale' }).getByRole('link', { name: 'Mon compte' })).toBeVisible()
  await test.step('Quand il valide sans téléphone ni charte, les erreurs sont signalées', async () => {
    await page.getByRole('button', { name: 'Valider et continuer' }).click()
    await expect(page.getByText('Le téléphone professionnel est obligatoire.')).toBeVisible()
    await expect(page.getByText('Vous devez accepter la charte d’usage pour continuer.')).toBeVisible()
  })
  await test.step('Quand il renseigne ses coordonnées et accepte la charte', async () => {
    await page.locator('input[name="telephone"]').fill('+237 677 21 43 65')
    await page.locator('input[name="fonction"]').fill('Analyste')
    await page.getByLabel('J’ai lu la charte d’usage et je l’accepte').check()
    await page.getByRole('button', { name: 'Valider et continuer' }).click()
  })
  await test.step('Et il enrôle une clé de sécurité ou choisit de le faire plus tard', async () => {
    await expect(page.getByRole('heading', { name: 'Enrôler une clé de sécurité' })).toBeVisible()
    if (o.cle) await page.getByRole('button', { name: 'Enrôler une clé' }).click()
    else await page.getByRole('button', { name: 'Plus tard' }).click()
  })
  await test.step('Alors les actions du fournisseur d’identité sont expliquées puis exécutées à la reconnexion', async () => {
    await expect(page.getByRole('heading', { name: 'Sécuriser votre authentification' })).toBeVisible()
    await expect(page.getByText('Mot de passe', { exact: true })).toBeVisible()
    await page.getByRole('button', { name: 'Continuer' }).click()
  })
}

test.describe('Fonctionnalité : premier accès et Mon compte dans les trois couches', () => {
  test('Scénario : premier accès dans la console', async ({ page }) => {
    await demo(page, { profil: 'ROLE_RSSI', premierAcces: true })
    await page.goto(`${ADRESSES.console}/console/comptes`)
    await completerPremierAcces(page, { cle: true })
    await expect(page.getByRole('heading', { level: 1 })).toHaveText('Comptes')
  })

  test('Scénario : premier accès dans l’application du département, puis Mon compte', async ({ page }) => {
    await demo(page, { premierAcces: true })
    await page.goto(`${ADRESSES.departement}/des/tableau-de-bord`)
    await completerPremierAcces(page, { cle: false })
    await expect(page.getByRole('heading', { level: 1 })).toHaveText('Tableau de bord')
    await naviguer(page, 'Mon compte')
    await expect(page.getByRole('heading', { level: 1, name: 'Mon compte' })).toBeVisible()
    await expect(page.getByText('Aucune clé de sécurité enrôlée')).toBeVisible()
    await page.locator('select[name="langue"]').selectOption('en')
    await page.getByRole('button', { name: 'Enregistrer ma fiche' }).click()
    await expect(page.getByText('Votre fiche a été enregistrée.')).toBeVisible()
  })

  test('Scénario : premier accès dans le portail des participants, puis déconnexion', async ({ page }) => {
    await demo(page, { premierAcces: true })
    await page.route('**/api/logout', (r) => r.fulfill({ json: { logoutUrl: '/connexion' } }))
    await page.goto(`${ADRESSES.portail}/portail/liaisons`)
    await completerPremierAcces(page, { cle: true })
    await naviguer(page, 'Mon compte')
    await page.getByRole('button', { name: 'Reconfigurer mon second facteur' }).click()
    await page.getByRole('alertdialog').getByRole('button', { name: 'Continuer vers la reconnexion' }).click()
    await expect(page.getByText('Reconnexion en cours vers les pages d’authentification du GIMAC.')).toBeVisible()
    await recueillirCouverture(page)
    await naviguer(page, 'Se déconnecter')
    await expect(page.getByRole('button', { name: 'Se connecter avec le fournisseur d\'identité' })).toBeVisible()
  })

  test('Scénario : sans premier accès en attente, les pages sont servies directement', async ({ page }) => {
    await page.goto(`${ADRESSES.departement}/des/mon-compte`)
    await expect(page.getByRole('heading', { level: 1, name: 'Mon compte' })).toBeVisible()
    await expect(page.getByText('YubiKey 5 NFC (bureau)')).toBeVisible()
    await page.goto(`${ADRESSES.portail}/portail/mon-compte`)
    await expect(page.getByText('correspondant-a')).toBeVisible()
  })
})
