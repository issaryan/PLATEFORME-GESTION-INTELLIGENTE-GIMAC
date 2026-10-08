import { mkdirSync } from 'node:fs'
import { dirname, join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'
import type { Page } from '@playwright/test'
import { ADRESSES, demo, expect, test } from '../fixtures'

// Fonctionnalité : destinataire d'un rapport (docs/lot1/mon-activite.md §2.4, « Destinataire du rapport »), services
// simulés de l'application du département. Avant de soumettre, l'auteur voit à qui le rapport sera remis et pourquoi
// (n+1, chef de l'unité, chef d'une unité au-dessus) et peut choisir une autre personne de sa chaîne hiérarchique ; au
// sommet de l'organisation, le rapport est déposé au lieu d'être refusé. Captures en thème clair et sombre dans
// e2e/captures/d95 (ou $GIMAC_CAPTURES/d95).

const ICI = dirname(fileURLToPath(import.meta.url))
const CAPTURES = process.env.GIMAC_CAPTURES ? join(process.env.GIMAC_CAPTURES, 'd95') : resolve(ICI, '..', 'captures', 'd95')
async function capturer(page: Page, nom: string) {
  mkdirSync(CAPTURES, { recursive: true })
  await page.evaluate(() => Promise.all(document.getAnimations().map((a) => a.finished.catch(() => undefined))))
  await page.screenshot({ path: join(CAPTURES, `${nom}.png`) })
}
const toast = (page: Page, texte: string | RegExp) => page.getByRole('status').filter({ hasText: texte })

/** Brouillon rédigé du jour, enregistré : la consultation s'ouvre ensuite sur lui. */
async function brouillon(page: Page, texte: string) {
  await page.goto(`${ADRESSES.departement}/des/rapports`)
  await page.getByRole('button', { name: 'Nouveau rapport' }).click()
  const d = page.getByRole('dialog', { name: 'Nouveau rapport' })
  await d.getByLabel('Rédiger').check()
  await d.getByLabel(/Contenu du rapport/).fill(texte)
  await d.getByRole('button', { name: 'Enregistrer le brouillon' }).click()
  await expect(toast(page, 'Brouillon enregistré.')).toBeVisible()
}

for (const theme of ['light', 'dark'] as const) {
  const suffixe = theme === 'light' ? 'clair' : 'sombre'
  test.describe(`Fonctionnalité : destinataire d’un rapport, services simulés (${suffixe})`, () => {
    test('Scénario : une collaboratrice voit à qui son rapport sera remis et choisit un responsable plus haut', async ({ page }) => {
      await demo(page, { profil: 'ROLE_COLLABORATEUR', theme })
      await test.step('Étant donné son brouillon du jour', async () => {
        await brouillon(page, '# Bilan\n\n- Supervision des liens du participant')
      })
      const c = page.getByRole('dialog', { name: /Rapport quotidien de Nadège Mvondo Assiga/ })
      await test.step('Quand elle ouvre le brouillon : destinataire trouvé d’office et motif annoncés', async () => {
        await expect(c.getByText('Sera remis à : Gaston Ndzana Ebode (votre n+1)')).toBeVisible()
        await expect(c.getByLabel(/^Remettre à/)).toHaveValue('u-resp-info')
        await capturer(page, `rapport-destinataire-${suffixe}`)
      })
      await test.step('Alors elle choisit le chef du département et le rapport lui est remis', async () => {
        await c.getByLabel(/^Remettre à/).selectOption('U-001')
        await expect(c.getByText('Sera remis à : Paul Mbarga Atangana (choisi par vous)')).toBeVisible()
        await c.getByRole('button', { name: 'Enregistrer et soumettre' }).click()
        await expect(toast(page, 'Rapport remis à Paul Mbarga Atangana (choisi par vous).')).toBeVisible()
      })
    })

    test('Scénario : au sommet de l’organisation, le rapport est déposé au lieu d’être refusé', async ({ page }) => {
      await demo(page, { profil: 'ROLE_DG', theme })
      await brouillon(page, 'Comité de direction : arbitrages budgétaires')
      const c = page.getByRole('dialog', { name: /Rapport quotidien de Jean-Baptiste Ella Mintsa/ })
      await expect(c.getByText(/Personne n’est au-dessus de vous dans l’organisation/)).toBeVisible()
      await c.getByRole('button', { name: 'Enregistrer et soumettre' }).click()
      await expect(toast(page, 'Rapport déposé : personne n’est au-dessus de vous, il reste consultable.')).toBeVisible()
      await expect(page.getByRole('table').getByText('Déposé')).toBeVisible()
      await capturer(page, `rapport-depose-${suffixe}`)
    })
  })
}
