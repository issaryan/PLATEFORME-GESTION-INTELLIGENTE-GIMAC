import { createRequire } from 'node:module'
import type { Page } from '@playwright/test'
import { ADRESSES, demo, expect, test } from '../fixtures'

// Fonctionnalité : les écrans de la console, de Mon compte et du premier accès respectent WCAG 2.2 AA
// (audit axe-core dans le navigateur, contrastes compris). Seule exclusion : les étiquettes de FilterBar
// (texte atténué sur fond gm-color-surface-sunken, 4,39:1), écart du système de conception publié
// signalé dans son README et à trancher au référentiel.
const axe = createRequire(import.meta.url).resolve('axe-core/axe.min.js')
const EXCLUS = ['.gm-filter-label']

/** Audit de la page entière, ou du seul dialogue ouvert (le contenu recouvert par le voile est hors sujet). */
async function auditer(page: Page, portee?: string) {
  await page.addScriptTag({ path: axe })
  const violations = await page.evaluate(async ({ exclus, portee }) => {
    const w = window as unknown as { axe: { run: (c: unknown, o: unknown) => Promise<{ violations: { id: string; nodes: { html: string }[] }[] }> } }
    const r = await w.axe.run({ include: portee ? [[portee]] : [document], exclude: exclus.map((s: string) => [s]) }, {
      runOnly: { type: 'tag', values: ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa'] },
    })
    return r.violations.map((v) => `${v.id} : ${v.nodes.slice(0, 3).map((n) => n.html.slice(0, 140)).join(' | ')}`)
  }, { exclus: EXCLUS, portee })
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

  // Application du département (D-57, D-58, D-63, D-64) : tableaux de bord par acteur, registre et dossier
  // des institutions, ouverture d'une liaison au cadrage, liaisons et désignation du conducteur, liste et
  // dossier numérique du parcours (onglets Décision et Étapes), messagerie.
  for (const theme of ['light', 'dark'] as const) {
    test(`Scénario : écrans de l’application du département (thème ${theme})`, async ({ page }) => {
      await demo(page, { profil: 'ROLE_CHEF_DDRM', theme })
      for (const [chemin, titre] of [
        ['/des/tableau-de-bord', 'Tableau de bord'],
        ['/des/institutions', 'Institutions membres'],
        ['/des/institutions/INST-0105', 'Mobile Money Sanaga'],
        ['/des/institutions/INST-0042?onglet=transitions', 'Établissement de paiement A'],
        ['/des/institutions/INST-0105?formulaire=liaison', 'Ouvrir une liaison (cadrage) pour Mobile Money Sanaga'],
        ['/des/liaisons', 'Liaisons et parcours'],
        ['/des/liaisons/LIA-2026-030', 'Établissement de microfinance C'],
        ['/des/liaisons/LIA-2026-030?formulaire=chef', 'Désigner le conducteur de projet de la liaison de Établissement de microfinance C'],
        ['/des/parcours', 'Liaisons'],
        ['/des/parcours/LIA-2026-030', 'Établissement de paiement A'],
        ['/des/parcours/LIA-2026-019?onglet=decision', 'Banque G'],
        ['/des/parcours/LIA-2026-036?onglet=etapes&etape=ATELIERS', 'Établissement de microfinance C'],
        ['/des/messagerie?conversation=CONV-0005', 'Messagerie'],
      ]) {
        await test.step(chemin, async () => {
          await page.goto(`${ADRESSES.departement}${chemin}`)
          await expect(page.getByRole('heading', { level: 1 })).toHaveText(titre)
          await page.waitForLoadState('networkidle')
          await auditer(page)
        })
      }
    })
  }

  test('Scénario : tableau de bord du conducteur de projet (thème sombre)', async ({ page }) => {
    await demo(page, { profil: 'ROLE_CONDUCTEUR_PROJET', theme: 'dark' })
    await page.goto(`${ADRESSES.departement}/des/tableau-de-bord`)
    await expect(page.getByRole('region', { name: 'Mon travail' }).getByRole('heading', { name: /^Mes projets \(\d+\)$/ })).toBeVisible()
    await expect(page.getByRole('link', { name: 'LIA-2026-041' }).first()).toBeVisible()
    await page.waitForLoadState('networkidle')
    await auditer(page)
  })

  test('Scénario : dialogue de confirmation motivée du département', async ({ page }) => {
    await demo(page, { profil: 'ROLE_CHEF_DDRM', theme: 'light' })
    await page.goto(`${ADRESSES.departement}/des/institutions/INST-0105`)
    await page.getByRole('button', { name: 'Proposer la clôture sans suite' }).click()
    await expect(page.getByRole('alertdialog')).toBeVisible()
    await auditer(page, '[role="alertdialog"]')
  })

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
