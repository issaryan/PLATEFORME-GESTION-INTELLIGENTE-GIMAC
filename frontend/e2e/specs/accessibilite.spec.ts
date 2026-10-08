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
  // Animations d'apparition (fondu des modales) terminées : sinon le contraste est mesuré en cours de fondu.
  await page.evaluate(() => Promise.all(document.getAnimations().filter((a) => a.effect?.getComputedTiming().iterations !== Infinity).map((a) => a.finished.catch(() => undefined))))
  // axe-core injecté une fois par document : la navigation dans l'application (ecran) garde le document.
  if (!(await page.evaluate(() => 'axe' in window))) await page.addScriptTag({ path: axe })
  const violations = await page.evaluate(async ({ exclus, portee }) => {
    const w = window as unknown as { axe: { run: (c: unknown, o: unknown) => Promise<{ violations: { id: string; nodes: { html: string }[] }[] }> } }
    const r = await w.axe.run({ include: portee ? [[portee]] : [document], exclude: exclus.map((s: string) => [s]) }, {
      runOnly: { type: 'tag', values: ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa'] },
    })
    return r.violations.map((v) => `${v.id} : ${v.nodes.slice(0, 3).map((n) => n.html.slice(0, 140)).join(' | ')}`)
  }, { exclus: EXCLUS, portee })
  expect(violations).toEqual([])
}

/**
 * Ouvre un écran : chargement complet la première fois (ou depuis une autre application), puis navigation dans
 * l'application (routeur de l'historique), comme un utilisateur qui passe d'un écran à l'autre. Une suite d'écrans
 * audités ne recharge plus à chaque écran la construction instrumentée ni axe-core : ces rechargements successifs
 * (une douzaine par scénario) faisaient dépasser le délai d'un scénario sous charge parallèle.
 */
async function ecran(page: Page, base: string, chemin: string) {
  if (new URL(page.url()).origin !== new URL(base).origin || !(await page.locator('#root > *').count())) {
    await page.goto(`${base}${chemin}`)
    return
  }
  await page.evaluate((c) => {
    window.history.pushState({}, '', c)
    window.dispatchEvent(new PopStateEvent('popstate', { state: {} }))
  }, chemin)
  await page.waitForURL(`${base}${chemin}`)
}

test.describe('Fonctionnalité : accessibilité des écrans D-62', () => {
  for (const theme of ['light', 'dark'] as const) {
    test(`Scénario : écrans de la console du super administrateur (thème ${theme})`, async ({ page }) => {
      await demo(page, { profil: 'ROLE_SUPER_ADMIN', theme })
      for (const [chemin, titre] of [
        ['/console/tableau-de-bord', 'Tableau de bord du super administrateur'],
        ['/console/comptes', 'Comptes'],
        ['/console/comptes/nouveau', 'Comptes'], // création en modale sur la liste
        ['/console/comptes/u-sna', 'Rachel Biyong Mengue'],
        ['/console/journal', 'Journal d’administration'],
        ['/console/politique-securite', 'Politique de sécurité'],
        ['/console/plateforme/courriels', 'Courriels de la plateforme'],
        ['/console/messagerie', 'Messagerie'],
        ['/console/profils', 'Profils et permissions'],
        ['/console/plateforme/configuration', 'Configuration de la plateforme'],
        ['/console/plateforme/journalisation?service=iam', 'Journalisation des services'],
        ['/console/mon-compte', 'Mon compte'],
      ]) {
        await test.step(chemin, async () => {
          await ecran(page, ADRESSES.console, chemin)
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
      // Treize écrans audités l'un après l'autre : délai porté au double.
      test.slow()
      await demo(page, { profil: 'ROLE_CHEF_DDRM', theme })
      for (const [chemin, titre] of [
        ['/des/tableau-de-bord', 'Tableau de bord'],
        ['/des/institutions', 'Institutions membres'],
        ['/des/institutions/INST-0105', 'Mobile Money Sanaga'],
        ['/des/institutions/INST-0042?onglet=transitions', 'Établissement de paiement A'],
        ['/des/institutions/INST-0105?formulaire=liaison', 'Mobile Money Sanaga'], // ouverture au cadrage en modale sur la fiche
        ['/des/liaisons', 'Liaisons et parcours'],
        ['/des/liaisons/LIA-2026-030', 'Établissement de microfinance C'],
        ['/des/liaisons/LIA-2026-030?formulaire=chef', 'Établissement de microfinance C'], // désignation en modale sur la fiche
        ['/des/parcours', 'Liaisons'],
        ['/des/parcours/LIA-2026-030', 'Établissement de paiement A'],
        ['/des/parcours/LIA-2026-019?onglet=decision', 'Banque G'],
        ['/des/parcours/LIA-2026-036?onglet=etapes&etape=ATELIERS', 'Établissement de microfinance C'],
        ['/des/messagerie?conversation=CONV-0005', 'Messagerie'],
      ]) {
        await test.step(chemin, async () => {
          await ecran(page, ADRESSES.departement, chemin)
          await expect(page.getByRole('heading', { level: 1 })).toHaveText(titre)
          await page.waitForLoadState('networkidle')
          await auditer(page)
        })
      }
    })
  }

  // Profils, organigramme, rôles par fonctionnalité et processus d'unité (D-86, D-87, D-90).
  for (const theme of ['light', 'dark'] as const) {
    test(`Scénario : profils, organigramme et processus d’unité (thème ${theme})`, async ({ page }) => {
      test.slow()
      await demo(page, { profil: 'ROLE_SUPER_ADMIN', theme })
      await page.goto(`${ADRESSES.console}/console/organigramme`)
      await expect(page.getByRole('heading', { level: 1 })).toHaveText('Organigramme')
      await page.locator('[data-unite="SNA"]').click()
      await expect(page.getByRole('group', { name: 'Membres de Service Normes et Agréments' })).toBeVisible()
      await auditer(page)
      await page.getByRole('button', { name: 'Liste' }).click()
      await auditer(page)
      await page.goto(`${ADRESSES.console}/console/matrice-pouvoirs`)
      await expect(page.getByRole('columnheader', { name: /Processus : lancer/ })).toBeVisible()
      await auditer(page)
      await page.goto(`${ADRESSES.console}/console/profils`)
      await page.getByRole('row').filter({ hasText: 'ROLE_SNA' }).getByRole('button', { name: 'Modifier la composition' }).click()
      await expect(page.getByRole('complementary', { name: 'Aperçu de la barre latérale' })).toBeVisible()
      await auditer(page, '[role="dialog"]')
      await page.getByRole('dialog').getByRole('button', { name: 'Annuler' }).click()
      await page.getByRole('button', { name: 'Comparer deux profils' }).click()
      await page.getByRole('dialog').getByLabel('Second profil').selectOption('ROLE_CHEF_DES')
      await auditer(page, '[role="dialog"]')

      await demo(page, { profil: 'ROLE_RESPONSABLE_SERVICE', theme })
      await page.goto(`${ADRESSES.departement}/des/processus`)
      await expect(page.getByRole('heading', { level: 1 })).toHaveText('Processus de l’unité')
      await page.waitForLoadState('networkidle')
      await auditer(page)
      await page.getByRole('button', { name: 'Ouverture d’un flux réseau', exact: true }).first().click()
      await expect(page.getByTestId('processus-canevas')).toBeVisible()
      await auditer(page)
      await page.getByRole('button', { name: 'Consulter RECETTE' }).click()
      await auditer(page, '[role="dialog"]')
      await page.getByRole('dialog').getByRole('button', { name: 'Fermer' }).last().click()
      await page.goto(`${ADRESSES.departement}/des/processus?vue=instances`)
      await page.getByRole('combobox', { name: 'Instances' }).selectOption('unite')
      await page.getByRole('button', { name: 'Flux SFTP vers la Banque de Libreville' }).click()
      await expect(page.getByRole('complementary', { name: /Recette du flux/ })).toBeVisible()
      await auditer(page)
    })
  }

  // Accusés de lecture (D-88) et référentiel aux étapes configurables (D-89) : coches et détail « Lu par »,
  // modélisateur (couloirs colorés, poignées décoratives), modale « Étapes », vue filtrée d'un pôle (fantômes).
  for (const theme of ['light', 'dark'] as const) {
    test(`Scénario : accusés de lecture et référentiel D-89 (thème ${theme})`, async ({ page }) => {
      test.slow()
      await demo(page, { profil: 'ROLE_CHEF_DES', theme })
      await page.goto(`${ADRESSES.departement}/des/messagerie`)
      await page.getByRole('list', { name: 'Conversations' }).getByRole('button', { name: /Essais de LIA-2026-030/ }).click()
      const lu = page.getByRole('button', { name: /^Lu par 2 sur 3/ })
      await lu.click()
      await expect(page.getByRole('dialog', { name: 'Lu par' }).getByText('heure inconnue')).toBeVisible()
      await auditer(page)
      await page.getByRole('list', { name: 'Conversations' }).getByRole('button', { name: /Aïcha Mahamat Nour/ }).click()
      await expect(page.getByRole('region', { name: 'Aïcha Mahamat Nour' }).getByRole('img', { name: 'Lu' }).first()).toBeVisible()
      await auditer(page)

      await demo(page, { profil: 'ROLE_INGENIEUR_HOMOLOGATION', theme })
      await page.goto(`${ADRESSES.departement}/des/referentiels/operations?version=REF-CI-4`)
      await expect(page.getByTestId('modelisateur-canevas')).toBeVisible()
      await page.waitForLoadState('networkidle')
      await auditer(page)
      await page.getByRole('toolbar', { name: 'Outils du modélisateur' }).getByRole('button', { name: 'Étapes' }).click()
      const d = page.getByRole('dialog', { name: 'Étapes de la version' })
      await d.getByRole('button', { name: 'Ajouter une étape' }).click()
      await expect(d.getByRole('region', { name: 'Nouvelle étape' })).toBeVisible()
      await auditer(page, '[role="dialog"]')
      await d.getByRole('button', { name: 'Fermer' }).last().click()

      await demo(page, { profil: 'ROLE_POLE_RESEAU', theme })
      await page.goto(`${ADRESSES.departement}/des/referentiels/operations?version=REF-CI-4`)
      await expect(page.getByText('Vous voyez les opérations qui vous concernent')).toBeVisible()
      await expect(page.locator('.gm-mod-carte-fantome').first()).toBeVisible()
      await auditer(page)
      await page.goto(`${ADRESSES.departement}/des/referentiels/operations?version=REF-CI-4&onglet=operations`)
      await expect(page.getByRole('heading', { name: 'Opérations voisines' })).toBeVisible()
      await auditer(page)
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

  // Connexion sur la plateforme (D-65) : formulaire, code, mot de passe oublié, nouveau mot de passe.
  for (const theme of ['light', 'dark'] as const) {
    test(`Scénario : écrans de connexion (thème ${theme})`, async ({ page }) => {
      await demo(page, { theme })
      await page.goto(`${ADRESSES.console}/connexion?motif=inactivite&minutes=30`)
      await expect(page.locator('input[name="identifiant"]')).toBeVisible()
      await auditer(page)
      await page.locator('input[name="identifiant"]').fill('rssi')
      await page.locator('input[name="motDePasse"]').fill('Gimac-Demo-2026!')
      await page.getByRole('button', { name: 'Se connecter' }).click()
      await expect(page.locator('input[name="code"]')).toBeVisible()
      await auditer(page)
      await page.goto(`${ADRESSES.console}/connexion/mot-de-passe-oublie`)
      await auditer(page)
      await page.goto(`${ADRESSES.console}/connexion/nouveau-mot-de-passe?jeton=jeton-de-demonstration-e2e`)
      await auditer(page)
    })
  }

  test('Scénario : avertissement d’inactivité et modales de Mon compte', async ({ page }) => {
    await page.clock.install()
    await page.goto(`${ADRESSES.departement}/des/mon-compte`)
    await expect(page.getByRole('heading', { level: 1, name: 'Mon compte' })).toBeVisible()
    await page.getByRole('button', { name: 'Reconfigurer mon code TOTP' }).click()
    await expect(page.getByRole('dialog').getByRole('img', { name: /Code QR/ })).toBeVisible()
    await auditer(page, '[role="dialog"]')
    await page.getByRole('dialog').getByRole('button', { name: 'Annuler' }).click()
    await page.getByRole('button', { name: 'Changer mon mot de passe' }).click()
    await auditer(page, '[role="dialog"]')
    await page.getByRole('dialog').getByRole('button', { name: 'Annuler' }).click()
    await page.clock.fastForward('28:10')
    await expect(page.getByRole('alertdialog', { name: 'Votre session va expirer' })).toBeVisible()
    await auditer(page, '[role="alertdialog"]')
  })

  test('Scénario : assistant de premier accès', async ({ page }) => {
    await demo(page, { premierAcces: true })
    await page.goto(`${ADRESSES.departement}/des/tableau-de-bord`)
    await expect(page.getByRole('heading', { level: 1 })).toHaveText('Premier accès à la plateforme')
    await auditer(page)
  })
})
