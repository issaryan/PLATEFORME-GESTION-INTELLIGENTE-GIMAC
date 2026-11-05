import { mkdirSync } from 'node:fs'
import { dirname, join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'
import type { Page } from '@playwright/test'
import { ADRESSES, demo, expect, test } from '../fixtures'

// D-133 : le gestionnaire du projet (chef de la DDRM) définit puis modifie le périmètre du projet de certification,
// services groupés par interface et par famille, noms usuels en clair ; un agent sans droit voit le périmètre en
// lecture seule. Services simulés, clair et sombre.
const ICI = dirname(fileURLToPath(import.meta.url))
const CAPTURES = process.env.GIMAC_CAPTURES ? join(process.env.GIMAC_CAPTURES, 'perimetre-projet') : resolve(ICI, '..', 'captures', 'perimetre-projet')
async function capturer(page: Page, nom: string) {
  mkdirSync(CAPTURES, { recursive: true })
  await page.evaluate(() => Promise.all(document.getAnimations().map((a) => a.finished.catch(() => undefined))))
  await page.screenshot({ path: join(CAPTURES, `${nom}.png`), fullPage: true })
}

for (const theme of ['light', 'dark'] as const) {
  const suffixe = theme === 'light' ? 'clair' : 'sombre'
  test.describe(`Fonctionnalité : périmètre du projet de certification (${suffixe})`, () => {
    test('Scénario : le gestionnaire modifie le périmètre, un autre agent le lit', async ({ page }) => {
      await test.step('Étant donné le chef de la DDRM, gestionnaire du projet, sur l’onglet Périmètre', async () => {
        await demo(page, { profil: 'ROLE_CHEF_DDRM', theme })
        await page.goto(`${ADRESSES.departement}/des/liaisons/LIA-2026-030?onglet=perimetre`)
        await expect(page.getByText('Périmètre du projet de certification')).toBeVisible()
        await expect(page.getByText('Mobile, protocole propriétaire · Transferts et paiement marchand')).toBeVisible()
        await capturer(page, `1-perimetre-initial-${suffixe}`)
      })
      await test.step('Quand il ouvre la modification : groupes, compteur, recherche', async () => {
        await page.getByRole('button', { name: 'Modifier le périmètre' }).click()
        const d = page.getByRole('dialog')
        await expect(d.getByText('Transferts et paiement marchand (2 sur 2)')).toBeVisible()
        await capturer(page, `2-modification-groupes-${suffixe}`)
        await d.getByLabel('Tout le groupe « Autres services »').check()
        await d.getByLabel('Transfert', { exact: true }).uncheck()
        await expect(d.getByText('Ajoutés : Cash-in agent, Paiement de facture')).toBeVisible()
        await expect(d.getByText('Retirés : Transfert')).toBeVisible()
        await d.getByLabel(/Motif/).fill('Lot 1 : paiements et factures ; le transfert passe au lot 2')
        await capturer(page, `3-resume-des-changements-${suffixe}`)
        await d.getByRole('button', { name: 'Enregistrer le périmètre' }).click()
        await expect(page.getByRole('dialog')).toHaveCount(0)
      })
      await test.step('Alors la version 2 est tracée dans l’historique avec le droit utilisé', async () => {
        await expect(page.getByText('Lot 1 : paiements et factures ; le transfert passe au lot 2')).toBeVisible()
        await expect(page.getByText('Transfert (hors périmètre)')).toBeVisible()
        await capturer(page, `4-historique-${suffixe}`)
      })
      await test.step('Et un agent de la SNA le voit en lecture seule', async () => {
        await demo(page, { profil: 'ROLE_SNA', theme })
        await page.goto(`${ADRESSES.departement}/des/liaisons/LIA-2026-030?onglet=perimetre`)
        await expect(page.getByText(/Seuls le gestionnaire du projet/)).toBeVisible()
        await expect(page.getByRole('button', { name: 'Modifier le périmètre' })).toHaveCount(0)
        await capturer(page, `5-lecture-seule-${suffixe}`)
      })
    })
  })
}
