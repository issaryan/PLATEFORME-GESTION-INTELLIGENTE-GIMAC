import { mkdirSync } from 'node:fs'
import { join } from 'node:path'
import type { Page } from '@playwright/test'
import { ADRESSES, demo, expect, test } from '../fixtures'

// Fonctionnalité : modale CRUD standard (CrudDialog, D-63). Toute création, modification, suppression ou
// action sensible déclenchée depuis une fiche ou une liste s'ouvre dans une fenêtre modale (aria-modal,
// focus piégé, Échap), adaptée à son contexte, la fiche restant affichée dessous ; un formulaire
// modifié demande confirmation avant l'abandon. GIMAC_CAPTURES=<dossier> enregistre les captures.

const CAPTURES = process.env.GIMAC_CAPTURES
async function capturer(page: Page, nom: string) {
  if (!CAPTURES) return
  mkdirSync(CAPTURES, { recursive: true })
  await page.screenshot({ path: join(CAPTURES, `${nom}.png`) })
}

test.describe('Fonctionnalité : modales de la console', () => {
  test.beforeEach(async ({ page }) => {
    await demo(page, { profil: 'ROLE_SUPER_ADMIN' })
  })

  test('Scénario : modifier la fiche d’un compte dans une modale, abandon confirmé', async ({ page }) => {
    await page.goto(`${ADRESSES.console}/console/comptes/u-dg`)
    const titre = await page.getByRole('heading', { level: 1 }).textContent()
    await page.getByRole('button', { name: 'Modifier la fiche' }).click()
    const d = page.getByRole('dialog', { name: 'Modifier la fiche' })
    await expect(d).toHaveAttribute('aria-modal', 'true')
    await expect(d.locator('.gm-dialog-sujet')).toContainText('·')
    await expect(page.getByRole('heading', { level: 1 })).toHaveText(titre!)
    await capturer(page, 'modale-modifier-fiche')
    await d.locator('input[name="fonction"]').fill('Directrice générale adjointe')
    await page.keyboard.press('Escape')
    await expect(d.getByText('Abandonner la saisie ?')).toBeVisible()
    await capturer(page, 'modale-abandon-saisie')
    await d.getByRole('button', { name: 'Reprendre la saisie' }).click()
    await d.getByRole('button', { name: 'Enregistrer la fiche' }).click()
    await expect(page.getByRole('dialog')).toHaveCount(0)
    await expect(page.getByText('La fiche du compte a été enregistrée.')).toBeVisible()
  })

  test('Scénario : créer un compte dans une modale ouverte sur la liste', async ({ page }) => {
    await page.goto(`${ADRESSES.console}/console/comptes`)
    await page.getByRole('button', { name: 'Créer un compte' }).click()
    const d = page.getByRole('dialog', { name: 'Créer un compte' })
    await expect(d).toBeVisible()
    await expect(page).toHaveURL(/\/console\/comptes$/)
    // Focus placé dans la modale (premier élément focalisable) et piégé.
    await expect.poll(() => d.evaluate((n) => n.contains(document.activeElement))).toBe(true)
    await capturer(page, 'modale-creation-compte')
    await page.keyboard.press('Escape')
    await expect(page.getByRole('dialog')).toHaveCount(0)
  })

  test('Scénario : suppression et actes de sécurité en modale adaptée au contexte', async ({ page }) => {
    await page.goto(`${ADRESSES.console}/console/comptes/u-sna`)
    await page.getByRole('button', { name: 'Supprimer le compte' }).click()
    const s = page.getByRole('alertdialog', { name: 'Supprimer le compte' })
    await expect(s.getByText('Action définitive : elle ne pourra pas être annulée.')).toBeVisible()
    await expect(s.getByRole('button', { name: 'Continuer vers la signature' })).toBeDisabled()
    await capturer(page, 'modale-suppression')
    await s.getByRole('button', { name: 'Annuler' }).click()
    await page.getByRole('tab', { name: 'Sécurité' }).click()
    await page.getByRole('button', { name: 'Exiger des actions' }).click()
    const a = page.getByRole('alertdialog', { name: 'Exiger des actions' })
    await expect(a.getByLabel(/Motif/)).toBeVisible()
    await capturer(page, 'modale-action-sensible')
    await a.getByRole('button', { name: 'Annuler' }).click()
    await expect(page.getByRole('alertdialog')).toHaveCount(0)
  })

  test('Scénario : politique de sécurité et messagerie modifiées dans des modales', async ({ page }) => {
    await page.goto(`${ADRESSES.console}/console/politique-securite`)
    await page.getByRole('button', { name: 'Modifier la politique' }).click()
    const p = page.getByRole('dialog', { name: 'Modifier la politique' })
    await expect(p.getByText('Votre clé de sécurité vous sera demandée pour confirmer ce geste.')).toBeVisible()
    await capturer(page, 'modale-politique')
    await p.getByRole('button', { name: 'Annuler' }).click()
    await page.goto(`${ADRESSES.console}/console/messagerie`)
    await page.getByRole('button', { name: 'Modifier la messagerie' }).click()
    await expect(page.getByRole('dialog', { name: 'Modifier la messagerie' }).locator('input[name="hote"]')).toBeVisible()
    await capturer(page, 'modale-messagerie')
  })
})

test.describe('Fonctionnalité : modales de l’application du département', () => {
  test('Scénario : déclarer un blocage depuis le dossier de liaison, fiche visible dessous', async ({ page }) => {
    await demo(page, { profil: 'ROLE_CHEF_DES' })
    await page.goto(`${ADRESSES.departement}/des/parcours/LIA-2026-046`)
    await expect(page.getByRole('heading', { level: 1 })).toHaveText('Établissement de paiement A')
    await capturer(page, 'dossier-liaison-entete')
    await page.getByRole('button', { name: 'Déclarer un blocage' }).first().click()
    const d = page.getByRole('dialog', { name: 'Déclarer un blocage' })
    await expect(d).toHaveAttribute('aria-modal', 'true')
    await expect(d.locator('.gm-dialog-sujet')).toContainText('LIA-2026-046')
    await capturer(page, 'modale-blocage')
    await page.keyboard.press('Escape')
    await expect(page.getByRole('dialog')).toHaveCount(0)
  })

  test('Scénario : modifier la fiche d’une institution dans une modale', async ({ page }) => {
    await demo(page, { profil: 'ROLE_CHARGE_DDRM' })
    await page.goto(`${ADRESSES.departement}/des/institutions/INST-0105`)
    const titre = await page.getByRole('heading', { level: 1 }).textContent()
    await page.getByRole('button', { name: 'Modifier la fiche' }).click()
    const d = page.getByRole('dialog')
    await expect(d.locator('input[name="nom"]')).toBeVisible()
    await expect(page.getByRole('heading', { level: 1 })).toHaveText(titre!)
    await capturer(page, 'modale-institution')
    await d.getByRole('button', { name: 'Annuler' }).click()
    await expect(page.getByRole('dialog')).toHaveCount(0)
  })
})
