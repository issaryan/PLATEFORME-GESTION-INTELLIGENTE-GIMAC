import { mkdirSync } from 'node:fs'
import { join } from 'node:path'
import type { Page } from '@playwright/test'
import { ADRESSES, demo, expect, test } from '../fixtures'

// Fonctionnalité : recherche globale de la barre supérieure (Ctrl+K, DS-GIMAC-01 §7.4). Le bouton
// « Rechercher » ou Ctrl+K ouvre la palette : écrans permis de la navigation, puis objets cherchés dans les
// services selon les droits (liaisons, institutions, conversations, comptes) ; flèches, Entrée ou clic
// ouvrent l'objet, Échap ferme. Services simulés ; captures en clair et en sombre (GIMAC_CAPTURES).

const CAPTURES = process.env.GIMAC_CAPTURES
async function capturer(page: Page, nom: string) {
  if (!CAPTURES) return
  mkdirSync(CAPTURES, { recursive: true })
  await page.screenshot({ path: join(CAPTURES, `${nom}.png`) })
}
const palette = (page: Page) => page.getByRole('dialog', { name: 'Recherche globale' })

for (const theme of ['light', 'dark'] as const) {
  test(`Scénario : le chef du DES retrouve une liaison et une conversation (thème ${theme})`, async ({ page }) => {
    await demo(page, { profil: 'ROLE_CHEF_DES', theme })
    await page.goto(`${ADRESSES.departement}/des/tableau-de-bord`)
    await expect(page.getByRole('region', { name: 'Mon travail' })).toBeVisible()
    await test.step('Quand il ouvre la recherche par le bouton de la barre supérieure', async () => {
      await page.getByRole('button', { name: 'Recherche globale (Ctrl+K)' }).click()
      // Écrans permis, dans l'ordre de la navigation (« Mon activité » d'abord, D-76) : un terme les filtre.
      await expect(palette(page).getByRole('option', { name: 'Ma journée' })).toBeVisible()
      await palette(page).getByRole('combobox').fill('liaisons')
      await expect(palette(page).getByRole('option', { name: 'Liaisons et parcours' })).toBeVisible()
    })
    await test.step('Et qu’il tape une référence de liaison', async () => {
      await palette(page).getByRole('combobox').fill('LIA-2026-030')
      await expect(palette(page).getByRole('option', { name: /LIA-2026-030/ }).first()).toBeVisible()
      await expect(palette(page).getByRole('group', { name: 'Objets' })).toBeVisible()
      await capturer(page, `recherche-liaison-${theme}`)
      await page.keyboard.press('Enter')
    })
    await test.step('Alors le dossier de la liaison s’ouvre et la palette se ferme', async () => {
      await expect(page).toHaveURL(/\/des\/parcours\/[^/]+$/)
      await expect(palette(page)).toHaveCount(0)
    })
    await test.step('Quand il cherche une conversation au clavier (Ctrl+K)', async () => {
      await page.keyboard.press('Control+k')
      await palette(page).getByRole('combobox').fill('Essais')
      await palette(page).getByRole('option', { name: /Essais de LIA-2026-030/ }).click()
      await expect(page).toHaveURL(/\/des\/messagerie\?conversation=/)
      await expect(page.getByRole('region', { name: 'Essais de LIA-2026-030' })).toBeVisible()
    })
    await test.step('Et Échap ferme la palette sans rien ouvrir', async () => {
      await page.keyboard.press('Control+k')
      await palette(page).getByRole('combobox').fill('zzzz')
      await expect(palette(page).getByText('Aucun résultat pour « zzzz »')).toBeVisible()
      await page.keyboard.press('Escape')
      await expect(palette(page)).toHaveCount(0)
    })
  })
}

test('Scénario : le RSSI retrouve un compte depuis le département, le super administrateur depuis la console', async ({ page }) => {
  await demo(page, { profil: 'ROLE_RSSI' })
  await page.goto(`${ADRESSES.departement}/des/tableau-de-bord`)
  await page.getByRole('button', { name: 'Recherche globale (Ctrl+K)' }).click()
  await palette(page).getByRole('combobox').fill('biyong')
  await palette(page).getByRole('option', { name: /Biyong/ }).first().click()
  await expect(page).toHaveURL(/\/des\/administration\/comptes\/[^/?]+/)
  await demo(page, { profil: 'ROLE_SUPER_ADMIN' })
  await page.goto(`${ADRESSES.console}/console/tableau-de-bord`)
  await expect(page.getByRole('heading', { level: 1, name: 'Tableau de bord du super administrateur' })).toBeVisible()
  await page.keyboard.press('Control+k')
  await palette(page).getByRole('combobox').fill('biyong')
  await palette(page).getByRole('option', { name: /Biyong/ }).first().click()
  await expect(page).toHaveURL(/\/console\/comptes\/[^/?]+/)
})

test('Scénario : le portail du participant n’a pas de recherche globale', async ({ page }) => {
  await demo(page, { profil: 'ROLE_CORRESPONDANT' })
  await page.goto(`${ADRESSES.portail}/portail/tableau-de-bord`)
  await expect(page.getByRole('heading', { level: 1 })).toBeVisible()
  await expect(page.getByRole('button', { name: 'Recherche globale (Ctrl+K)' })).toHaveCount(0)
})
