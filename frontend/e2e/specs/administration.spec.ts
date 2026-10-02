import { mkdirSync } from 'node:fs'
import { join } from 'node:path'
import type { Page } from '@playwright/test'
import { ADRESSES, demo, expect, test } from '../fixtures'

// Fonctionnalité : administration des utilisateurs visible (D-62). En tête du tableau de bord de la
// console, le super administrateur et le RSSI trouvent la gestion des utilisateurs (actions directes,
// indicateurs cliquables vers la liste filtrée) ; la fiche d'un compte a ses quatre onglets. Dans
// l'application du département (D-69), le RSSI et le super administrateur gèrent les comptes, le journal,
// les profils et la politique sans quitter l'application ni changer de thème ; seule la console de la
// plateforme (supervision, configuration) reste un lien externe.

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

test('Scénario : le RSSI gère les comptes dans l’application du département, sans changer d’application ni de thème', async ({ page }) => {
  await demo(page, { profil: 'ROLE_RSSI', theme: 'dark' })
  await page.goto(`${ADRESSES.departement}/des/tableau-de-bord`)
  const nav = page.getByRole('navigation', { name: 'Navigation principale' })
  await test.step('Étant donné le groupe « Administration » de sa navigation', async () => {
    await expect(nav.getByText('Administration', { exact: true })).toBeVisible()
    await expect(nav.getByRole('link', { name: 'Comptes utilisateurs' })).toHaveAttribute('href', '/des/administration/comptes')
    await expect(nav.getByRole('link', { name: 'Console de la plateforme' })).toHaveAttribute('href', `${ADRESSES.consoleLiee}/console/tableau-de-bord`)
    const section = page.getByRole('region', { name: 'Administration et sécurité des comptes' })
    await expect(section.getByRole('link', { name: 'Créer un compte' })).toHaveAttribute('href', '/des/administration/comptes/nouveau')
    await expect(section.getByRole('link', { name: 'Comptes sans second facteur' })).toHaveAttribute('href', '/des/administration/comptes?etat=SANS_2FA')
  })
  await test.step('Quand il ouvre « Comptes utilisateurs », la liste s’affiche dans le département, en mode sombre', async () => {
    await nav.getByRole('link', { name: 'Comptes utilisateurs' }).click()
    await expect(page).toHaveURL(`${ADRESSES.departement}/des/administration/comptes`)
    await expect(page.getByRole('heading', { level: 1 })).toHaveText('Comptes')
    await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark')
    await expect(nav.getByRole('link', { name: 'Liaisons et parcours' })).toBeVisible()
    await capturer(page, 'administration-departement-comptes-dark')
  })
  await test.step('Et il ouvre une fiche de compte, puis le journal, les profils et la politique', async () => {
    await page.getByRole('row').nth(1).getByRole('link').first().click()
    await expect(page).toHaveURL(/\/des\/administration\/comptes\/[^/?]+/)
    await expect(page.getByRole('tab').first()).toBeVisible()
    await nav.getByRole('link', { name: 'Journal d’administration' }).click()
    await expect(page.getByRole('heading', { level: 1 })).toHaveText('Journal d’administration')
    await nav.getByRole('link', { name: 'Profils et permissions' }).click()
    await expect(page.getByRole('heading', { level: 1 })).toHaveText('Profils et permissions')
    await nav.getByRole('link', { name: 'Politique de sécurité' }).click()
    await expect(page.getByRole('heading', { level: 1 })).toHaveText('Politique de sécurité')
    await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark')
  })
  await test.step('Alors un profil sans IAM_ACCOUNT_READ n’a ni groupe ni accès', async () => {
    await demo(page, { profil: 'ROLE_SNA' })
    await page.goto(`${ADRESSES.departement}/des/tableau-de-bord?profil=ROLE_SNA`)
    await expect(page.getByRole('heading', { level: 1 })).toHaveText('Tableau de bord')
    await expect(page.getByRole('navigation', { name: 'Navigation principale' }).getByText('Administration', { exact: true })).toHaveCount(0)
    await page.goto(`${ADRESSES.departement}/des/administration/comptes?profil=ROLE_SNA`)
    await expect(page.getByText('Accès refusé')).toBeVisible()
  })
})

test('Scénario : le super administrateur crée un compte depuis le département', async ({ page }) => {
  await demo(page, { profil: 'ROLE_SUPER_ADMIN', theme: 'light' })
  await page.goto(`${ADRESSES.departement}/des/administration/comptes/nouveau`)
  await expect(page.getByRole('heading', { level: 1 })).toHaveText('Comptes')
  await expect(page.getByRole('dialog')).toBeVisible()
  await capturer(page, 'administration-departement-creation-light')
})
