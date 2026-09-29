import { createRequire } from 'node:module'
import type { Page } from '@playwright/test'
import { ADRESSES, demo, expect, test } from '../fixtures'

// Fonctionnalité : les écrans de la console, de Mon compte et du premier accès respectent WCAG 2.2 AA
// (audit axe-core dans le navigateur, contrastes compris). Seule exclusion : les étiquettes de FilterBar
// (texte atténué sur fond gm-color-surface-sunken, 4,39:1), écart du système de conception publié
// signalé dans son README et à trancher au référentiel.
const axe = createRequire(import.meta.url).resolve('axe-core/axe.min.js')
const EXCLUS = ['.gm-filter-label']

async function auditer(page: Page) {
  await page.addScriptTag({ path: axe })
  const violations = await page.evaluate(async (exclus) => {
    const w = window as unknown as { axe: { run: (c: unknown, o: unknown) => Promise<{ violations: { id: string; nodes: { html: string }[] }[] }> } }
    const r = await w.axe.run({ include: [document], exclude: exclus.map((s: string) => [s]) }, {
      runOnly: { type: 'tag', values: ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa'] },
    })
    return r.violations.map((v) => `${v.id} : ${v.nodes.slice(0, 3).map((n) => n.html.slice(0, 140)).join(' | ')}`)
  }, EXCLUS)
  expect(violations).toEqual([])
}

test.describe('Fonctionnalité : accessibilité des écrans D-62', () => {
  for (const theme of ['light', 'dark'] as const) {
    test(`Scénario : écrans de la console du super administrateur (thème ${theme})`, async ({ page }) => {
      await demo(page, { profil: 'ROLE_SUPER_ADMIN', theme })
      for (const [chemin, titre] of [
        ['/console/tableau-de-bord', 'Tableau de bord du super administrateur'],
        ['/console/comptes', 'Comptes'],
        ['/console/comptes/nouveau', 'Créer un compte'],
        ['/console/comptes/u-sna', 'Rachel Biyong Mengue'],
        ['/console/journal', 'Journal d’administration'],
        ['/console/politique-securite', 'Politique de sécurité'],
        ['/console/messagerie', 'Messagerie de la plateforme'],
        ['/console/profils', 'Profils et permissions'],
        ['/console/plateforme/configuration', 'Configuration de la plateforme'],
        ['/console/plateforme/journalisation?service=iam', 'Journalisation des services'],
        ['/console/mon-compte', 'Mon compte'],
      ]) {
        await test.step(chemin, async () => {
          await page.goto(`${ADRESSES.console}${chemin}`)
          await expect(page.getByRole('heading', { level: 1 })).toHaveText(titre)
          await page.waitForLoadState('networkidle')
          await auditer(page)
        })
      }
    })
  }

  test('Scénario : dialogues de signature et de confirmation', async ({ page }) => {
    await demo(page, { profil: 'ROLE_SUPER_ADMIN' })
    await page.goto(`${ADRESSES.console}/console/comptes/u-sna`)
    await page.getByRole('button', { name: 'Modifier les profils' }).click()
    await auditer(page)
    await page.getByRole('button', { name: 'Continuer vers la signature' }).click()
    await expect(page.getByRole('dialog')).toBeVisible()
    await auditer(page)
  })

  test('Scénario : assistant de premier accès', async ({ page }) => {
    await demo(page, { premierAcces: true })
    await page.goto(`${ADRESSES.departement}/des/tableau-de-bord`)
    await expect(page.getByRole('heading', { level: 1 })).toHaveText('Premier accès à la plateforme')
    await auditer(page)
  })
})
