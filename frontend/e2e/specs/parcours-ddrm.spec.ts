import { ADRESSES, demo, expect, test } from '../fixtures'
import { carte, joindre, monTravail, tiroir } from './outils'

// Fonctionnalité : la DDRM conduit la phase amont du parcours (étapes 3 à 5, D-57 à D-59) : le chef de la
// DDRM suit les liaisons en phase amont et les conducteurs à désigner ; la chargée de la DDRM réalise ses
// tâches (compte rendu de la réunion de lancement, comptes rendus d'ateliers). Services simulés.

test.describe('Fonctionnalité : phase amont conduite par la DDRM', () => {
  test('Scénario : le chef de la DDRM suit les liaisons en phase amont et dépose un compte rendu d’atelier', async ({ page }) => {
    await demo(page, { profil: 'ROLE_CHEF_DDRM', theme: 'light' })
    await test.step('Étant donné le chef de la DDRM sur son tableau de bord', async () => {
      await page.goto(`${ADRESSES.departement}/des/tableau-de-bord`)
      await expect(page.locator('.gm-pagehead-nom', { hasText: 'Bertrand Nguema Obiang' })).toBeVisible()
      const compteurs = monTravail(page).locator('[aria-label="Mes compteurs"]')
      for (const c of ['Conducteurs à désigner', 'Liaisons en phase amont', 'Ateliers à tenir', 'Périmètres à arrêter']) await expect(compteurs.getByText(c, { exact: true })).toBeVisible()
      await expect(carte(page, 'Liaison sans conducteur (1)')).toBeVisible()
      await expect(carte(page, /^Liaisons en phase amont \(\d\)$/).getByRole('link', { name: 'LIA-2026-041' })).toBeVisible()
      // Le chef tient aussi la fonction des chefs de département (D-93 §6.5) : affectation des ressources à mener.
      await expect(carte(page, /^Mes tâches \(\d+\)$/).getByText('Affectation des ressources du projet').first()).toBeVisible()
      await expect(page.getByRole('region', { name: 'Pilotage des projets de certification' })).toBeVisible()
    })
    await test.step('Quand il ouvre toutes les liaisons en phase amont', async () => {
      await carte(page, /^Liaisons en phase amont \(\d\)$/).getByRole('link', { name: 'Voir tout' }).click()
    })
    await test.step('Alors la liste est filtrée sur le cadrage, le lancement et les ateliers', async () => {
      await expect(page).toHaveURL(/etape=CADRAGE%2CLANCEMENT%2CATELIERS|etape=CADRAGE,LANCEMENT,ATELIERS/)
      const puce = page.getByText('Étape courante : Cadrage, Lancement, Ateliers')
      await expect(puce).toBeVisible()
      await expect(page.getByRole('link', { name: 'LIA-2026-036' })).toBeVisible()
      await expect(page.getByRole('link', { name: 'LIA-2026-030' })).toHaveCount(0)
      await page.locator('.gm-tag').filter({ hasText: 'Étape courante : Cadrage, Lancement, Ateliers' }).getByRole('button').click()
      await expect(page.getByRole('link', { name: 'LIA-2026-030' })).toBeVisible()
    })
    await test.step('Quand il dépose le compte rendu d’un atelier de la liaison LIA-2026-036', async () => {
      await page.getByRole('link', { name: 'LIA-2026-036' }).click()
      await page.getByRole('tab', { name: 'Étapes' }).click()
      await expect(carte(page, '5. Ateliers')).toBeVisible()
      await expect(carte(page, 'Ateliers : comptes rendus')).toBeVisible()
      await carte(page, 'Ateliers : comptes rendus').getByRole('button', { name: 'Déposer le compte rendu' }).first().click()
      const d = page.getByRole('dialog', { name: 'Déposer le compte rendu' })
      await d.getByRole('button', { name: 'Déposer le compte rendu' }).click()
      await expect(d.getByText('Joignez le compte rendu.')).toBeVisible()
      await joindre(d, 'cr-atelier-interfaces.pdf')
      await d.getByLabel('Commentaire').fill('Contrat d\'interface validé par le participant')
      await d.getByRole('button', { name: 'Déposer le compte rendu' }).click()
    })
    await test.step('Alors le compte rendu figure au livrable de l’opération', async () => {
      await expect(page.getByText(/^Compte rendu déposé : /)).toBeVisible()
      await expect(carte(page, 'Ateliers : comptes rendus').getByText('cr-atelier-interfaces.pdf')).toBeVisible()
    })
    await test.step('Et l’étape d’adhésion, close, réunit ses six éléments', async () => {
      await page.getByRole('link', { name: '2. Adhésion' }).click()
      await expect(carte(page, '2. Adhésion').getByText('Réuni', { exact: true })).toHaveCount(6)
      await expect(carte(page, '2. Adhésion').getByText('Dossier d\'adhésion')).toBeVisible()
    })
  })

  test('Scénario : la chargée de la DDRM réalise sa tâche de lancement', async ({ page }) => {
    await demo(page, { profil: 'ROLE_CHARGE_DDRM', theme: 'light' })
    await test.step('Étant donné la chargée de la DDRM et ses tâches de phase amont', async () => {
      await page.goto(`${ADRESSES.departement}/des/tableau-de-bord`)
      await expect(page.locator('.gm-pagehead-nom', { hasText: 'Fatimé Abakar Hissein' })).toBeVisible()
      await expect(page.getByRole('region', { name: 'Pilotage des projets de certification' })).toHaveCount(0)
      await expect(carte(page, 'Liaison sans conducteur (1)')).toHaveCount(0)
      const taches = carte(page, /^Mes tâches \(\d+\)$/)
      await expect(taches.getByRole('row', { name: /LIA-2026-041/ }).getByRole('link', { name: 'Réunion de lancement' })).toBeVisible()
      await expect(taches.getByText('R · Responsable').first()).toBeVisible()
      await expect(carte(page, /^Liaisons en phase amont \(\d\)$/)).toBeVisible()
    })
    await test.step('Quand elle ouvre la tâche de la réunion de lancement', async () => {
      await carte(page, /^Mes tâches \(\d+\)$/).getByRole('row', { name: /LIA-2026-041/ }).getByRole('link', { name: 'Réunion de lancement' }).click()
    })
    await test.step('Alors le tiroir de l’opération la présente, avec ses intervenants', async () => {
      const t = tiroir(page, '7')
      await expect(t.getByText('Réunion de lancement', { exact: true })).toBeVisible()
      await expect(t.getByText('Chargé DDRM 1 · Développement du Réseau et du Marché')).toBeVisible()
      await t.getByRole('tab', { name: 'Intervenants' }).click()
      await expect(t.getByText(/validateur \(V\) est toujours distinct du responsable/)).toBeVisible()
      await t.getByRole('tab', { name: 'Détail' }).click()
      await expect(t.getByText('Plateforme d\'exécution')).toBeVisible()
      await t.getByRole('button', { name: 'Fermer' }).click()
      await expect(tiroir(page, '7')).toHaveCount(0)
    })
    await test.step('Et elle dépose le compte rendu de lancement depuis l’étape de lancement', async () => {
      await page.getByRole('tab', { name: 'Étapes' }).click()
      await page.getByRole('link', { name: '4. Lancement' }).click()
      const conduite = carte(page, 'Lancement : ressources et réunion de lancement')
      await conduite.getByRole('button', { name: 'Déposer le compte rendu' }).click()
      const d = page.getByRole('dialog', { name: 'Déposer le compte rendu' })
      await joindre(d, 'cr-lancement-LIA-2026-041.pdf')
      await d.getByRole('button', { name: 'Déposer le compte rendu' }).click()
      await expect(page.getByText('Compte rendu déposé : Réunion de lancement.')).toBeVisible()
      await expect(conduite.getByText('cr-lancement-LIA-2026-041.pdf')).toBeVisible()
    })
  })
})
