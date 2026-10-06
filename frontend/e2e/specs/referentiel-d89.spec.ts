import { mkdirSync } from 'node:fs'
import { dirname, join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'
import type { Page } from '@playwright/test'
import { ADRESSES, demo, expect, test } from '../fixtures'
import { PasserelleReferentiels } from './passerelle-referentiels'

// Fonctionnalité D-89 : référentiel des opérations, droits et étapes configurables (docs/lot1/referentiel-droits-
// etapes.md). L'ingénieur d'homologation ajoute une étape colorée (modale « Étapes »), y range une opération et voit
// son couloir ; un pôle ne voit que les opérations qui le concernent (bandeau, fantômes grisés) ; le super
// administrateur édite le référentiel dans l'application interne. Services simulés (thèmes clair et sombre, captures
// dans e2e/captures/d88-d89 ou $GIMAC_CAPTURES/d88-d89), puis passerelle : routes et corps exacts.

const ICI = dirname(fileURLToPath(import.meta.url))
const CAPTURES = process.env.GIMAC_CAPTURES ? join(process.env.GIMAC_CAPTURES, 'd88-d89') : resolve(ICI, '..', 'captures', 'd88-d89')
async function capturer(page: Page, nom: string) {
  mkdirSync(CAPTURES, { recursive: true })
  await page.evaluate(() => Promise.all(document.getAnimations().filter((a) => a.effect?.getComputedTiming().iterations !== Infinity).map((a) => a.finished.catch(() => undefined))))
  await page.screenshot({ path: join(CAPTURES, `${nom}.png`) })
}
const PAGE = '/des/referentiels/operations'
const outils = (page: Page) => page.getByRole('toolbar', { name: 'Outils du modélisateur' })

for (const theme of ['light', 'dark'] as const) {
  const suffixe = theme === 'light' ? 'clair' : 'sombre'

  test.describe(`Fonctionnalité : étapes configurables et lecture filtrée (D-89, ${suffixe})`, () => {
    test('Scénario : l’ingénieur ajoute une étape colorée, y range une opération ; un pôle voit ses opérations', async ({ page }) => {
      test.slow()
      await demo(page, { profil: 'ROLE_INGENIEUR_HOMOLOGATION', theme })
      await page.goto(`${ADRESSES.departement}${PAGE}?version=REF-CI-4`)
      await expect(page.getByTestId('modelisateur-canevas')).toBeVisible()

      await test.step('Quand il ouvre « Étapes » et ajoute « Recette interne », couleur Accent, avant Certification', async () => {
        await outils(page).getByRole('button', { name: 'Étapes' }).click()
        const d = page.getByRole('dialog', { name: 'Étapes de la version' })
        await expect(d.getByRole('list', { name: 'Étapes, dans leur ordre' }).getByRole('listitem')).toHaveCount(11)
        await d.getByRole('button', { name: 'Ajouter une étape' }).click()
        const form = d.getByRole('region', { name: 'Nouvelle étape' })
        await form.getByLabel(/^Code/).fill('RECETTE_INTERNE')
        await form.getByLabel(/^Libellé/).fill('Recette interne')
        await form.getByLabel(/^Étape canonique/).selectOption('TESTS')
        await form.getByRole('radio', { name: 'Accent' }).check()
        await form.getByRole('button', { name: 'Enregistrer l\'étape' }).click()
        const liste = d.getByRole('list', { name: 'Étapes, dans leur ordre' })
        await expect(liste.getByRole('listitem')).toHaveCount(12)
        // De la fin à avant Certification : quatre crans vers le haut (Suivi, Mise en production, Approbation, Certification).
        for (let i = 0; i < 4; i++) {
          await d.getByRole('button', { name: 'Monter Recette interne' }).click()
          await expect(page.locator('.gm-mod-sr')).toHaveText(`Recette interne : position ${11 - i} sur 12.`)
        }
        await capturer(page, `modale-etapes-${suffixe}`)
        await d.getByRole('button', { name: 'Fermer' }).last().click()
      })

      await test.step('Alors une opération rangée dans l’étape a son couloir, teinté de la couleur choisie', async () => {
        await page.getByRole('button', { name: 'Modifier l\'opération C10' }).dispatchEvent('click')
        const op = page.getByRole('dialog', { name: 'Opération C10' })
        await op.getByLabel(/^Étape/).selectOption({ label: 'Recette interne' })
        await op.getByRole('button', { name: 'Enregistrer' }).click()
        await expect(op).toHaveCount(0)
        await outils(page).getByRole('button', { name: 'Disposition automatique' }).click()
        const couloir = page.locator('.gm-mod-couloir-etape', { hasText: 'Recette interne' })
        await expect(couloir).toBeVisible()
        await expect(couloir).toHaveClass(/gm-mod-couloir-colore/)
        expect(await couloir.evaluate((e) => (e as HTMLElement).style.getPropertyValue('--gm-mod-couloir-couleur'))).toBe('var(--gm-color-brand-accent)')
        await page.waitForTimeout(500)
        // Zoom vers la carte rangée dans la nouvelle étape : couloirs et libellés lisibles sur la capture.
        await page.getByTestId('modelisateur-canevas').scrollIntoViewIfNeeded()
        const noeud = page.locator('.react-flow__node[data-id="C10"]')
        for (let i = 0; i < 8; i++) {
          const b = await noeud.boundingBox()
          if (!b || b.width >= 130) break
          await page.mouse.move(b.x + b.width / 2, b.y + b.height / 2)
          await page.mouse.wheel(0, -300)
          await page.waitForTimeout(250)
        }
        // Panoramique : le titre du couloir « Recette interne » amené en haut, au centre du canevas.
        const zone = (await page.getByTestId('modelisateur-canevas').boundingBox())!
        const titre = (await couloir.locator('.gm-mod-couloir-titre').boundingBox())!
        await page.mouse.move(zone.x + 12, zone.y + 12)
        await page.mouse.down()
        await page.mouse.move(zone.x + 12 + (zone.x + zone.width / 2 - titre.x - titre.width / 2), zone.y + 12 + (zone.y + 24 - titre.y), { steps: 10 })
        await page.mouse.up()
        await page.waitForTimeout(300)
        await capturer(page, `modelisateur-etapes-${suffixe}`)
      })

      await test.step('Et un pôle réseau ne voit que ses opérations : bandeau, fantômes grisés et non ouvrables', async () => {
        await demo(page, { profil: 'ROLE_POLE_RESEAU', theme })
        await page.goto(`${ADRESSES.departement}${PAGE}?version=REF-CI-4`)
        await expect(page.getByText('Vous voyez les opérations qui vous concernent')).toBeVisible()
        const fantome = page.locator('.gm-mod-carte-fantome').first()
        await expect(fantome).toBeVisible()
        await expect(outils(page).getByRole('button', { name: 'Ajouter une opération' })).toHaveCount(0)
        // Cadrage : les opérations visibles et leurs fantômes sont dans la fenêtre du canevas dès l'ouverture.
        const zone = page.getByTestId('modelisateur-canevas')
        await zone.evaluate((e) => e.scrollIntoView({ block: 'end' }))
        const dansLeCanevas = (selecteur: string) => async () => {
          const z = (await zone.boundingBox())!
          return page.locator(selecteur).evaluateAll((l, b) => l.filter((n) => {
            const r = n.getBoundingClientRect()
            return r.width > 20 && r.left >= b.x && r.right <= b.x + b.width && r.top >= b.y && r.bottom <= b.y + b.height
          }).length, z)
        }
        await expect.poll(dansLeCanevas('.react-flow__node-operation')).toBeGreaterThanOrEqual(1)
        await expect.poll(dansLeCanevas('.react-flow__node-fantome')).toBeGreaterThanOrEqual(1)
        await capturer(page, `vue-filtree-${suffixe}`)
        // Un fantôme ne s'ouvre pas au double clic.
        await page.locator('.react-flow__node-fantome').first().dispatchEvent('dblclick')
        await expect(page.getByRole('dialog')).toHaveCount(0)
        const telechargement = page.waitForEvent('download')
        await outils(page).getByRole('button', { name: 'Exporter (CSV)' }).click()
        expect((await telechargement).suggestedFilename()).toBe('referentiel-carte_interbancaire-brouillon.csv')
      })
    })
  })
}

test.describe('Fonctionnalité : le super administrateur édite le référentiel dans l’application interne (D-89)', () => {
  test('Scénario : entrée « Référentiels » de la barre latérale, modélisateur en édition, étapes modifiables', async ({ page }) => {
    await demo(page, { profil: 'ROLE_SUPER_ADMIN', theme: 'light' })
    await page.goto(`${ADRESSES.departement}/des/tableau-de-bord`)
    await page.getByRole('navigation', { name: 'Navigation principale' }).getByRole('link', { name: 'Référentiels d\'opérations', exact: true }).click()
    await expect(page).toHaveURL(/\/des\/referentiels\/operations/)
    await page.goto(`${ADRESSES.departement}${PAGE}?version=REF-CI-4`)
    await expect(outils(page).getByRole('button', { name: 'Ajouter une opération' })).toBeVisible()
    await expect(page.getByText('Version figée : lecture seule')).toHaveCount(0)
    await outils(page).getByRole('button', { name: 'Étapes' }).click()
    await expect(page.getByRole('dialog', { name: 'Étapes de la version' }).getByRole('button', { name: 'Ajouter une étape' })).toBeVisible()
  })
})

test.describe('Fonctionnalité : étapes et lecture filtrée par la passerelle (D-89)', () => {
  test('Scénario : routes des étapes, catalogue de la version, export ; lecture concernée', async ({ page }) => {
    const gw = new PasserelleReferentiels({ d89: true })
    await gw.brancher(page)
    const V = '7d0c2a2e-4a51-4c1e-9a55-5f1f0d2c0a01'
    const R = `/bff/des/homologation/referentiels/${V}`
    await page.goto(`${ADRESSES.departementHttp}${PAGE}?version=${V}`)
    await expect(page.locator('.gm-mod-couloir-etape', { hasText: 'Tests' })).toHaveClass(/gm-mod-couloir-colore/)

    await test.step('Catalogue lu avec ?version= ; ajout, modification, ordre, retrait : corps exacts', async () => {
      expect(gw.appels.some((a) => a.chemin === '/bff/des/homologation/referentiels/catalogue' && a.search === `?version=${V}`)).toBe(true)
      await outils(page).getByRole('button', { name: 'Étapes' }).click()
      const d = page.getByRole('dialog', { name: 'Étapes de la version' })
      await d.getByRole('button', { name: 'Ajouter une étape' }).click()
      const form = d.getByRole('region', { name: 'Nouvelle étape' })
      await form.getByLabel(/^Code/).fill('RECETTE')
      await form.getByLabel(/^Libellé/).fill('Recette')
      await form.getByRole('radio', { name: 'Accent' }).check()
      await form.getByRole('button', { name: 'Enregistrer l\'étape' }).click()
      await expect(d.getByRole('list', { name: 'Étapes, dans leur ordre' }).getByRole('listitem')).toHaveCount(5)
      await d.getByRole('button', { name: 'Monter Recette' }).click()
      await expect(page.locator('.gm-mod-sr')).toHaveText('Recette : position 4 sur 5.')
      await d.getByRole('button', { name: 'Modifier Recette' }).click()
      const edition = d.getByRole('region', { name: 'Modifier l\'étape Recette' })
      await edition.getByLabel(/^Libellé/).fill('Recette interne')
      await edition.getByLabel(/^Étape canonique/).selectOption('TESTS')
      await edition.getByRole('button', { name: 'Enregistrer l\'étape' }).click()
      await expect(page.locator('.gm-mod-sr')).toHaveText('Étape Recette interne enregistrée.')
      await d.getByRole('button', { name: 'Retirer Recette interne' }).click()
      await page.getByRole('alertdialog').getByRole('button', { name: 'Retirer Recette interne' }).click()
      await expect(page.locator('.gm-mod-sr')).toHaveText('Étape Recette interne retirée.')
      const ecritures = gw.appels.filter((a) => a.chemin.startsWith(`${R}/etapes`))
      expect(ecritures.map((a) => `${a.methode} ${a.chemin.slice(R.length)}`)).toEqual(['POST /etapes', 'PUT /etapes/ordre', 'PUT /etapes/RECETTE', 'DELETE /etapes/RECETTE'])
      expect(ecritures[0].corps).toEqual({ code: 'RECETTE', libelle: 'Recette', couleur: 'ACCENT', canonique: null })
      expect(ecritures[1].corps).toEqual({ codes: ['CADRAGE', 'LANCEMENT', 'TESTS', 'RECETTE', 'CERTIFICATION'] })
      expect(ecritures[2].corps).toEqual({ libelle: 'Recette interne', couleur: 'ACCENT', canonique: 'TESTS' })
      await d.getByRole('button', { name: 'Fermer' }).last().click()
    })

    await test.step('Export CSV : GET …/export', async () => {
      const telechargement = page.waitForEvent('download')
      await outils(page).getByRole('button', { name: 'Exporter (CSV)' }).click()
      await telechargement
      expect(gw.appels.some((a) => a.methode === 'GET' && a.chemin === `${R}/export`)).toBe(true)
    })
  })

  test('Scénario : lecteur concerné, graphe filtré (fantôme), liste avec X-Gimac-Filtre', async ({ page }) => {
    const gw = new PasserelleReferentiels({ d89: true, concerne: true })
    await gw.brancher(page)
    const V = '7d0c2a2e-4a51-4c1e-9a55-5f1f0d2c0a01'
    await page.goto(`${ADRESSES.departementHttp}${PAGE}?version=${V}`)
    await expect(page.getByText('Vous voyez les opérations qui vous concernent')).toBeVisible()
    await expect(page.locator('.gm-mod-carte-fantome[data-code="2"]')).toBeVisible()
    await expect(page.locator('.gm-mod-carte[data-code="3"]').getByRole('button', { name: 'Consulter l\'opération 3' })).toBeVisible()
    await page.goto(`${ADRESSES.departementHttp}${PAGE}?version=${V}&vue=liste`)
    await expect(page.getByTestId('modelisateur-liste').getByText('Hors périmètre')).toBeVisible()
    await page.goto(`${ADRESSES.departementHttp}${PAGE}?version=${V}&onglet=operations`)
    await expect(page.getByText('Vous voyez les opérations qui vous concernent')).toBeVisible()
  })
})
