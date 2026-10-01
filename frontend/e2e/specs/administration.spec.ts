import { mkdirSync } from 'node:fs'
import { join } from 'node:path'
import type { Page } from '@playwright/test'
import { ADRESSES, demo, expect, test } from '../fixtures'

// Fonctionnalité : administration des utilisateurs visible (D-62). En tête du tableau de bord de la
// console, le super administrateur et le RSSI trouvent la gestion des utilisateurs (actions directes,
// indicateurs cliquables vers la liste filtrée) ; la fiche d'un compte a ses quatre onglets. Dans
// l'application du département, le RSSI est mené à la console par la navigation et son tableau de bord.

const CAPTURES = process.env.GIMAC_CAPTURES
async function capturer(page: Page, nom: string) {
  if (!CAPTURES) return
  mkdirSync(CAPTURES, { recursive: true })
  await page.screenshot({ path: join(CAPTURES, `${nom}.png`) })
}

for (const [profil, titre] of [['ROLE_SUPER_ADMIN', 'Tableau de bord du super administrateur'], ['ROLE_RSSI', 'Tableau de bord de la sécurité des SI']] as const) {
  test(`Scénario : ${profil} gère les utilisateurs depuis le tableau de bord de la console`, async ({ page }) => {
    await demo(page, { profil })
    await page.goto(`${ADRESSES.console}/console/tableau-de-bord`)
    await expect(page.getByRole('heading', { level: 1 })).toHaveText(titre)
    const gestion = page.getByRole('region', { name: 'Gestion des utilisateurs' })
    await expect(gestion).toBeVisible()
    const actions = gestion.getByRole('group', { name: 'Actions de gestion des utilisateurs' })
    for (const n of ['Créer un compte', 'Tous les comptes', 'Comptes sans second facteur', 'Premiers accès en attente', 'Profils et permissions', 'Journal d’administration', 'Politique de sécurité']) {
      await expect(actions.getByRole('button', { name: n })).toBeVisible()
    }
    await capturer(page, `administration-console-${profil}`)
    await test.step('Quand il ouvre l’indicateur des comptes sans second facteur, la liste est filtrée', async () => {
      await gestion.locator('.gm-stat[role="button"]').filter({ hasText: 'Sans second facteur' }).click()
      await expect(page).toHaveURL(/\/console\/comptes\?etat=SANS_2FA$/)
      await expect(page.getByRole('combobox', { name: 'État' })).toHaveValue('SANS_2FA')
    })
    await test.step('Et la fiche d’un compte a ses onglets Fiche, Sécurité, Sessions et Journal', async () => {
      await page.goto(`${ADRESSES.console}/console/comptes/u-dg`)
      for (const o of ['Fiche', 'Sécurité', 'Sessions', 'Journal']) await expect(page.getByRole('tab', { name: o })).toBeVisible()
      await page.getByRole('tab', { name: 'Sécurité' }).click()
      await expect(page.getByRole('group', { name: 'Actes de sécurité du compte' })).toBeVisible()
      await page.getByRole('tab', { name: 'Sessions' }).click()
      await expect(page.getByRole('button', { name: 'Fermer les sessions' })).toBeVisible()
      await capturer(page, `administration-fiche-sessions-${profil}`)
    })
  })
}

test('Scénario : le RSSI de l’application du département est mené aux écrans de la console', async ({ page }) => {
  await demo(page, { profil: 'ROLE_RSSI' })
  await page.goto(`${ADRESSES.departement}/des/tableau-de-bord`)
  const nav = page.getByRole('navigation', { name: 'Navigation principale' })
  await expect(nav.getByText('Administration', { exact: true })).toBeVisible()
  await expect(nav.getByRole('link', { name: 'Comptes utilisateurs' })).toHaveAttribute('href', `${ADRESSES.consoleLiee}/console/comptes`)
  await expect(nav.getByRole('link', { name: 'Console d’administration' })).toHaveAttribute('href', `${ADRESSES.consoleLiee}/console/tableau-de-bord`)
  const section = page.getByRole('region', { name: 'Administration et sécurité des comptes' })
  await expect(section.getByRole('link', { name: 'Créer un compte' })).toHaveAttribute('href', `${ADRESSES.consoleLiee}/console/comptes/nouveau`)
  await expect(section.getByRole('link', { name: 'Comptes sans second facteur' })).toHaveAttribute('href', `${ADRESSES.consoleLiee}/console/comptes?etat=SANS_2FA`)
  await capturer(page, 'administration-departement-rssi')
  // Un profil sans IAM_ACCOUNT_READ n'a ni groupe ni section d'administration.
  await demo(page, { profil: 'ROLE_SNA' })
  await page.goto(`${ADRESSES.departement}/des/tableau-de-bord?profil=ROLE_SNA`)
  await expect(page.getByRole('heading', { level: 1 })).toHaveText('Tableau de bord')
  await expect(page.getByRole('navigation', { name: 'Navigation principale' }).getByText('Administration', { exact: true })).toHaveCount(0)
})
