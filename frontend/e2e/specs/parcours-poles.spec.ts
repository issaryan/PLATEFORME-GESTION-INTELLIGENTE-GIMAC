import { ADRESSES, demo, expect, test } from '../fixtures'
import { cheminDossier, confirmerAction, joindre, monTravail, tiroir } from './outils'

// Fonctionnalité : les pôles réseau et système réalisent leurs opérations (D-58, D-59) : le pôle réseau
// contrôle l'atteignabilité du port d'essai (rang 13) et voit l'opération suspendue par un blocage de
// LIA-2026-030 ; le pôle système met à jour le script de dépôt (rang 28, oubli sans symptôme). Leurs
// preuves attendent la validation du conducteur de projet. Services simulés.

test.describe('Fonctionnalité : opérations des pôles réseau et système', () => {
  test('Scénario : le pôle réseau contrôle l’atteignabilité du port, une autre opération étant bloquée', async ({ page }) => {
    await demo(page, { profil: 'ROLE_POLE_RESEAU', theme: 'light' })
    await test.step('Étant donné le pôle réseau et ses tâches', async () => {
      await page.goto(`${ADRESSES.departement}/des/tableau-de-bord`)
      await expect(page.locator('.gm-pagehead-nom', { hasText: 'Idriss Mahamat Saleh' })).toBeVisible()
      const compteurs = monTravail(page).locator('[aria-label="Mes compteurs"]')
      await expect(compteurs.getByText('Opérations réseau', { exact: true })).toBeVisible()
      await expect(monTravail(page).getByRole('link', { name: 'Contrôle d\'atteignabilité du port' })).toBeVisible()
      await expect(monTravail(page).getByRole('heading', { name: 'Blocage en cours (1)' })).toBeVisible()
      await compteurs.getByRole('link', { name: 'Liens d\'essai à établir' }).click()
      await expect(page).toHaveURL(/\/des\/parcours\?etape=INTEGRATION/)
      await expect(page.getByRole('link', { name: 'LIA-2026-040' })).toBeVisible()
    })
    await test.step('Quand il ouvre l’opération bloquée de LIA-2026-030, aucun dépôt n’est offert', async () => {
      await page.goto(`${ADRESSES.departement}${cheminDossier('LIA-2026-030', 'operations', '14')}`)
      const t = tiroir(page, '14')
      await expect(t.getByText('Opération suspendue par un blocage')).toBeVisible()
      await expect(t.getByText(/Élément de sécurité du participant à renouveler/)).toBeVisible()
      await expect(t.getByRole('button', { name: 'Déposer la preuve' })).toHaveCount(0)
      await expect(page.getByRole('tab', { name: 'Blocages (1)' })).toBeVisible()
      await expect(page.getByRole('button', { name: 'Déclarer un blocage' })).toHaveCount(0)
    })
    await test.step('Et les coordonnées communiquées par le participant (rang 12) se relisent', async () => {
      await page.goto(`${ADRESSES.departement}${cheminDossier('LIA-2026-040', 'operations', '12')}`)
      const t = tiroir(page, '12')
      await expect(t.getByText('Adresse et port d\'essai')).toBeVisible()
      await expect(t.getByText('participant-040.essai:8443')).toBeVisible()
    })
    await test.step('Quand il dépose la preuve du contrôle d’atteignabilité de LIA-2026-040', async () => {
      await page.goto(`${ADRESSES.departement}${cheminDossier('LIA-2026-040', 'operations', '13')}`)
      const t = tiroir(page, '13')
      await joindre(t, 'atteignabilite-port.png', 'capture', 'image/png')
      await t.getByRole('button', { name: 'Déposer la preuve' }).click()
      await confirmerAction(page, 'Déposer la preuve')
    })
    await test.step('Alors elle attend la validation du conducteur de projet', async () => {
      await expect(tiroir(page, '13').getByText(/Preuve déposée, en attente de la validation de Conducteur de projet 2/)).toBeVisible()
    })
  })

  test('Scénario : le pôle système met à jour le script de dépôt, opération facile à omettre', async ({ page }) => {
    await demo(page, { profil: 'ROLE_POLE_SYSTEME', theme: 'dark' })
    await test.step('Étant donné le pôle système et le script de dépôt à mettre à jour', async () => {
      await page.goto(`${ADRESSES.departement}/des/tableau-de-bord`)
      await expect(page.locator('.gm-pagehead-nom', { hasText: 'Chantal Ngo Bassong' })).toBeVisible()
      await expect(monTravail(page).getByText(/Opération facile à omettre|facile à omettre/).first()).toBeVisible()
      await monTravail(page).locator('[aria-label="Mes compteurs"]').getByRole('link', { name: 'Scripts de dépôt à mettre à jour' }).click()
      await expect(page).toHaveURL(/LIA-2026-005\?onglet=operations&operation=LIA-2026-005%3A28/)
    })
    await test.step('Quand il dépose la trace de la mise à jour', async () => {
      const t = tiroir(page, '28')
      await expect(t.getByText('Mise à jour du script de dépôt : à ne pas omettre')).toBeVisible()
      await joindre(t, 'trace-script.png', 'trace', 'image/png')
      await t.getByRole('button', { name: 'Déposer la preuve' }).click()
      await expect(page.getByRole('alertdialog').getByText('trace-script.png')).toBeVisible()
      await confirmerAction(page, 'Déposer la preuve')
    })
    await test.step('Alors la preuve attend la validation et l’opération 29 (secret) ne demande aucun code', async () => {
      await expect(tiroir(page, '28').getByText(/Preuve déposée, en attente de la validation de Conducteur de projet 2/)).toBeVisible()
      await tiroir(page, '28').getByRole('button', { name: 'Fermer' }).click()
      await page.getByRole('button', { name: /^Ouvrir l'opération 29 :/ }).click()
      await expect(tiroir(page, '29').getByText('Aucun secret dans le dossier')).toBeVisible()
      await expect(tiroir(page, '29').getByLabel(/Code de contrôle/)).toHaveCount(0)
    })
  })
})
