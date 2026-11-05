import { mkdirSync } from 'node:fs'
import { join } from 'node:path'
import type { Page } from '@playwright/test'
import { ADRESSES, demo, expect, test, type ProfilDemo } from '../fixtures'

// Barre latérale de l'application du DES en sections repliables (demande de l'utilisateur du 05/11/2026), services
// simulés : super administrateur (toutes les sections) et chef du DES, en largeur de bureau et à 360 px (tiroir),
// thèmes clair et sombre. GIMAC_CAPTURES reçoit les captures ; GIMAC_CAPTURES_ETAT les préfixe (avant, apres).

const CAPTURES = process.env.GIMAC_CAPTURES
const ETAT = process.env.GIMAC_CAPTURES_ETAT ?? 'apres'
async function capturer(page: Page, nom: string) {
  if (!CAPTURES) return
  mkdirSync(CAPTURES, { recursive: true })
  await page.screenshot({ path: join(CAPTURES, `${ETAT}-${nom}.png`), fullPage: true })
}
const suffixe = (theme: 'light' | 'dark') => (theme === 'light' ? 'clair' : 'sombre')
const nav = (page: Page) => page.getByRole('navigation', { name: 'Navigation principale' })

for (const theme of ['light', 'dark'] as const) {
  for (const [profil, nom] of [['ROLE_SUPER_ADMIN', 'super-admin'], ['ROLE_CHEF_DES', 'chef-des']] as [ProfilDemo, string][]) {
    test(`Barre latérale du DES : ${nom}, sections repliables (thème ${theme})`, async ({ page }) => {
      await demo(page, { profil, theme })
      await page.goto(`${ADRESSES.departement}/des/sandbox/activite`)
      await expect(nav(page)).toBeVisible()
      await capturer(page, `01-barre-${nom}-${suffixe(theme)}`)
      if (ETAT !== 'apres') return
      // La section de l'écran courant est ouverte ; une autre se replie et se déplie au clavier, état mémorisé.
      const sandbox = nav(page).getByRole('button', { name: /Sandbox/ })
      await expect(sandbox).toHaveAttribute('aria-expanded', 'true')
      const accueil = nav(page).getByRole('link', { name: 'Tableau de bord' })
      await expect(accueil).toBeVisible()
      const activite = nav(page).getByRole('button', { name: /Mon activité/ })
      await activite.focus()
      await page.keyboard.press('Enter')
      await expect(activite).toHaveAttribute('aria-expanded', 'true')
      await page.reload()
      await expect(nav(page).getByRole('button', { name: /Mon activité/ })).toHaveAttribute('aria-expanded', 'true')
      await capturer(page, `02-barre-${nom}-section-ouverte-${suffixe(theme)}`)
    })
  }

  test(`Barre latérale du DES à 360 px, tiroir ouvert (thème ${theme})`, async ({ page }) => {
    await page.setViewportSize({ width: 360, height: 800 })
    await demo(page, { profil: 'ROLE_SUPER_ADMIN', theme })
    await page.goto(`${ADRESSES.departement}/des/sandbox/activite`)
    await page.getByRole('button', { name: 'Ouvrir la navigation' }).click()
    await expect(nav(page)).toBeVisible()
    await capturer(page, `03-barre-360px-${suffixe(theme)}`)
  })
}
