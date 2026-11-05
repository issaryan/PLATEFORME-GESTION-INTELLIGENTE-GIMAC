import { mkdirSync } from 'node:fs'
import { join } from 'node:path'
import type { Page } from '@playwright/test'
import { ADRESSES, demo, expect, test } from '../fixtures'

// D-121 : graphes des tests effectués par les participants dans la sandbox, services simulés au contrat exact
// (jeu de démonstration : cinq participants 99001 à 99005, trois interfaces, treize semaines jusqu'au 04/11/2026).
// Super administrateur (demo.admin.gimac, SBX_SUP_CONSULTATION par D-135) ; horloge figée au 04/11/2026.
// Captures en thème clair et sombre, en largeur de bureau et à 360 px (GIMAC_CAPTURES).

const CAPTURES = process.env.GIMAC_CAPTURES
async function capturer(page: Page, nom: string) {
  if (!CAPTURES) return
  mkdirSync(CAPTURES, { recursive: true })
  await page.screenshot({ path: join(CAPTURES, `${nom}.png`), fullPage: true })
}
const suffixe = (theme: 'light' | 'dark') => (theme === 'light' ? 'clair' : 'sombre')

for (const theme of ['light', 'dark'] as const) {
  test(`Scénario : le super administrateur lit l'activité des tests et la vue graphique d'un dossier (thème ${theme})`, async ({ page }) => {
    await page.clock.setFixedTime(new Date('2026-11-04T10:00:00Z'))
    await demo(page, { profil: 'ROLE_SUPER_ADMIN', theme })
    await test.step('Étant donné l’activité des tests des quatre-vingt-dix derniers jours', async () => {
      await page.goto(`${ADRESSES.departement}/des/sandbox/activite?periode=90`)
      await expect(page.getByRole('heading', { level: 1, name: 'Activité des tests' })).toBeVisible()
      await expect(page.getByRole('group', { name: 'Indicateurs de la période' })).toBeVisible()
      await expect(page.getByText('Verdicts par statut')).toBeVisible()
      await expect(page.getByText('Avancement de la certification par participant')).toBeVisible()
      await expect(page.locator('.gm-state-done').first()).toBeVisible()
      await capturer(page, `01-activite-des-tests-${suffixe(theme)}`)
    })
    await test.step('Quand il restreint la vue au participant 99004 et à la carte', async () => {
      await page.goto(`${ADRESSES.departement}/des/sandbox/activite?periode=90&participant=99004&interface=CARTE`)
      await expect(page.getByText('Participant : 99004')).toBeVisible()
      await capturer(page, `02-activite-99004-carte-${suffixe(theme)}`)
    })
    await test.step('Alors l’écran reste lisible à 360 px', async () => {
      await page.setViewportSize({ width: 360, height: 800 })
      await page.goto(`${ADRESSES.departement}/des/sandbox/activite?periode=30`)
      await expect(page.getByRole('heading', { level: 1, name: 'Activité des tests' })).toBeVisible()
      await capturer(page, `03-activite-360px-${suffixe(theme)}`)
      await page.setViewportSize({ width: 1440, height: 900 })
    })
    await test.step('Et l’onglet sandbox du dossier montre jauges, verdicts par cas et chronologie', async () => {
      await page.goto(`${ADRESSES.departement}/des/parcours/LIA-2026-022?onglet=sandbox`)
      await expect(page.getByLabel('Vue graphique de la campagne')).toBeVisible()
      await expect(page.getByText('Verdicts par cas')).toBeVisible()
      await expect(page.getByText('Chronologie des exécutions')).toBeVisible()
      await capturer(page, `04-dossier-onglet-sandbox-${suffixe(theme)}`)
    })
  })
}
