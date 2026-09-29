import { ADRESSES, demo, expect, test } from '../fixtures'

// Fonctionnalité : le conducteur de projet suit « Mes projets » (D-58) : tableau de bord, liste filtrée
// d'emblée sur ses liaisons, fiche en lecture, sans désignation ni ouverture. Services simulés.

test.describe('Fonctionnalité : « Mes projets » du conducteur de projet', () => {
  test.beforeEach(async ({ page }) => {
    await demo(page, { profil: 'ROLE_CONDUCTEUR_PROJET', theme: 'light' })
  })

  test('Scénario : suivre ses projets depuis le tableau de bord', async ({ page }) => {
    await test.step('Étant donné le conducteur de projet sur son tableau de bord', async () => {
      await page.goto(`${ADRESSES.departement}/des/tableau-de-bord`)
      await expect(page.getByText('Bienvenue, Aïcha Mahamat Nour')).toBeVisible()
      const projets = page.getByRole('region', { name: 'Mes projets' })
      await expect(projets.getByRole('link', { name: 'Établissement de paiement A (Carte interbancaire)' })).toBeVisible()
      await expect(projets.getByRole('link', { name: 'Établissement de microfinance C (Paiement mobile)' })).toBeVisible()
      await expect(page.getByRole('region', { name: 'Cadrage des liaisons' })).toHaveCount(0)
    })
    await test.step('Quand il ouvre la liste de ses projets', async () => {
      await page.getByRole('region', { name: 'Mes projets' }).getByRole('link', { name: 'Voir tout' }).click()
    })
    await test.step('Alors « Mes projets » est activé et seules ses liaisons figurent', async () => {
      await expect(page.getByRole('switch', { name: 'Mes projets' })).toBeChecked()
      await expect(page.getByRole('link', { name: 'Établissement de microfinance C' })).toBeVisible()
      await expect(page.getByRole('link', { name: 'Banque D' })).toHaveCount(0)
      await expect(page.getByRole('button', { name: 'Ouvrir une liaison' })).toHaveCount(0)
      await expect(page.getByRole('button', { name: 'Désigner le conducteur' })).toHaveCount(0)
    })
    await test.step('Quand il désactive le filtre, toutes les liaisons apparaissent', async () => {
      await page.getByRole('switch', { name: 'Mes projets' }).click()
      await expect(page.getByRole('link', { name: 'Banque D' })).toBeVisible()
      await page.getByRole('switch', { name: 'Mes projets' }).click()
      await expect(page.getByRole('link', { name: 'Banque D' })).toHaveCount(0)
    })
    await test.step('Et la fiche de sa liaison en cadrage se lit sans désignation possible', async () => {
      await page.getByRole('link', { name: 'Établissement de microfinance C' }).click()
      await expect(page.getByRole('heading', { level: 1 })).toHaveText('Établissement de microfinance C')
      const resume = page.locator('section').filter({ has: page.getByRole('heading', { name: 'Cadrage de la liaison' }) })
      await expect(resume.getByText('Aïcha Mahamat Nour')).toBeVisible()
      await expect(resume.getByText('Transverse')).toBeVisible()
      await expect(page.getByRole('button', { name: 'Désigner le conducteur' })).toHaveCount(0)
    })
  })
})
