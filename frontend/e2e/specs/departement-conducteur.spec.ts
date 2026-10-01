import { ADRESSES, demo, expect, test } from '../fixtures'
import { carte, monTravail } from './outils'

// Fonctionnalité : le conducteur de projet suit « Mes projets » (D-58) : tableau de bord « Mon travail »,
// liste filtrée d'emblée sur ses liaisons, dossier en lecture, sans désignation ni ouverture. Services simulés.

test.describe('Fonctionnalité : « Mes projets » du conducteur de projet', () => {
  test.beforeEach(async ({ page }) => {
    await demo(page, { profil: 'ROLE_CONDUCTEUR_PROJET', theme: 'light' })
  })

  test('Scénario : suivre ses projets depuis le tableau de bord', async ({ page }) => {
    await test.step('Étant donné le conducteur de projet sur son tableau de bord', async () => {
      await page.goto(`${ADRESSES.departement}/des/tableau-de-bord`)
      await expect(page.locator('.gm-pagehead-nom', { hasText: 'Aïcha Mahamat Nour' })).toBeVisible()
      const projets = carte(page, /^Mes projets \(\d+\)$/)
      await expect(projets.getByRole('link', { name: 'LIA-2026-041' })).toBeVisible()
      await expect(projets.getByRole('link', { name: 'LIA-2026-027' })).toBeVisible()
      await expect(monTravail(page).getByRole('button', { name: 'Ouvrir une liaison' })).toHaveCount(0)
      await expect(page.getByRole('region', { name: 'Pilotage des projets de certification' })).toHaveCount(0)
    })
    await test.step('Quand il ouvre la liste de ses projets', async () => {
      await carte(page, /^Mes projets \(\d+\)$/).getByRole('link', { name: 'Voir tout' }).click()
    })
    await test.step('Alors « Mes projets » est sélectionné et seules ses liaisons figurent', async () => {
      await expect(page.getByRole('heading', { level: 1 })).toHaveText('Mes projets')
      await expect(page.getByRole('tab', { name: 'Mes projets' })).toHaveAttribute('aria-selected', 'true')
      await expect(page.getByRole('link', { name: 'LIA-2026-041' })).toBeVisible()
      await expect(page.getByRole('link', { name: 'LIA-2026-012' })).toHaveCount(0)
      await expect(page.getByRole('button', { name: 'Ouvrir une liaison' })).toHaveCount(0)
    })
    await test.step('Quand il affiche toutes les liaisons, celles des autres conducteurs apparaissent', async () => {
      await page.getByRole('tab', { name: 'Toutes les liaisons' }).click()
      await expect(page.getByRole('heading', { level: 1 })).toHaveText('Liaisons')
      await expect(page.getByRole('link', { name: 'LIA-2026-012' })).toBeVisible()
      await page.getByRole('tab', { name: 'Mes projets' }).click()
      await expect(page.getByRole('link', { name: 'LIA-2026-012' })).toHaveCount(0)
    })
    await test.step('Et le dossier de sa liaison en cadrage se lit sans désignation possible', async () => {
      await page.getByRole('link', { name: 'LIA-2026-041' }).click()
      await expect(page.getByRole('heading', { level: 1 })).toHaveText('Fintech F')
      await expect(page.getByText('Conducteur de projet 1', { exact: true })).toBeVisible()
      await expect(page.getByRole('button', { name: /conducteur/ })).toHaveCount(0)
      await expect(page.getByRole('button', { name: 'Déclarer un blocage' })).toBeVisible()
    })
  })
})
