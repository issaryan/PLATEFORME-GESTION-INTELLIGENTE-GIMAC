import { ADRESSES, demo, expect, test } from '../fixtures'
import { carte, cheminDossier, confirmerAction, monTravail, ongletSelectionne, tiroir } from './outils'

// Fonctionnalité : Normes et Agréments constate la couverture du périmètre (rang 17) qui ouvre le
// procès-verbal, suit les essais du simulateur (rang 16), remet la fiche de suivi des services au
// participant et constate la mise en service (étape 10, rangs 32 et 33). Services simulés.

test.describe('Fonctionnalité : constats de Normes et Agréments', () => {
  test.beforeEach(async ({ page }) => {
    await demo(page, { profil: 'ROLE_SNA', theme: 'light' })
  })

  test('Scénario : constater la couverture du périmètre, incomplète, qui bloque le procès-verbal', async ({ page }) => {
    await test.step('Étant donné Normes et Agréments et la couverture à constater de LIA-2026-022', async () => {
      await page.goto(`${ADRESSES.departement}/des/tableau-de-bord`)
      await expect(page.getByText('Normes et Agréments', { exact: true })).toBeVisible()
      const compteurs = monTravail(page).locator('[aria-label="Mes compteurs"]')
      for (const c of ['Essais en cours', 'Documents de paramétrage attendus', 'Mises en service à constater']) await expect(compteurs.getByText(c, { exact: true })).toBeVisible()
      await compteurs.getByRole('link', { name: 'Couvertures à constater' }).click()
    })
    await test.step('Quand il ouvre l’opération 17 et lit la couverture calculée par le service', async () => {
      await expect(page).toHaveURL(/LIA-2026-022\?onglet=operations&operation=LIA-2026-022%3A17/)
      const t = tiroir(page, '17')
      await expect(t.getByText('Couverture de certification')).toBeVisible()
      await expect(t.getByText('75 %')).toBeVisible()
      await expect(t.getByText(/le procès-verbal ne pourra pas être établi/)).toBeVisible()
      await t.getByRole('tab', { name: 'Détail' }).click()
      await expect(t.getByText('3 sur 4 services du périmètre')).toBeVisible()
      await t.getByRole('tab', { name: 'Exécution' }).click()
    })
    await test.step('Et qu’il constate la couverture', async () => {
      await tiroir(page, '17').getByRole('button', { name: 'Constater la couverture' }).click()
      await confirmerAction(page, 'Constater la couverture')
      await expect(tiroir(page, '17').getByText(/Opération close le/)).toBeVisible()
      await tiroir(page, '17').getByRole('button', { name: 'Fermer' }).click()
    })
    await test.step('Alors la décision montre le procès-verbal bloqué par la couverture de 75 %', async () => {
      await ongletSelectionne(page, 'Décision')
      await expect(page.getByText('Procès-verbal bloqué par la couverture')).toBeVisible()
      await expect(page.getByText(/La couverture de certification est de 75 % \(3 services certifiés sur 4\)/)).toBeVisible()
      await expect(page.getByRole('button', { name: /Établir le procès-verbal/ })).toHaveCount(0)
    })
  })

  test('Scénario : lire les essais repris du simulateur d’un projet mobile', async ({ page }) => {
    await page.goto(`${ADRESSES.departement}${cheminDossier('LIA-2026-038', 'operations', '16')}`)
    const t = tiroir(page, '16')
    await test.step('Alors le taux du simulateur est repris tel quel, service par service', async () => {
      await expect(t.getByText('Taux repris du simulateur, sans recalcul.')).toBeVisible()
      await expect(t.getByText('60 %').first()).toBeVisible()
    })
    await test.step('Et le périmètre de la liaison indique l’avancement repris du simulateur', async () => {
      await t.getByRole('button', { name: 'Fermer' }).click()
      await ongletSelectionne(page, 'Périmètre')
      await expect(page.getByText('repris du simulateur').first()).toBeVisible()
    })
  })

  test('Scénario : remettre la fiche de suivi au participant, puis constater une mise en service', async ({ page }) => {
    await test.step('Étant donné la liaison en production LIA-2025-071 dont la fiche est à remettre', async () => {
      await page.goto(`${ADRESSES.departement}${cheminDossier('LIA-2025-071', 'mise-en-service')}`)
      await expect(page.getByText('À remettre au participant')).toBeVisible()
    })
    await test.step('Quand il remet la fiche au participant', async () => {
      await page.getByRole('button', { name: 'Remettre la fiche au participant' }).click()
      await expect(page.getByRole('alertdialog').getByText(/service par service/)).toBeVisible()
      await confirmerAction(page, 'Remettre la fiche au participant')
    })
    await test.step('Alors la fiche attend le participant, sans autre geste', async () => {
      await expect(page.getByText('Fiche remise : le participant la renseigne sur son portail.')).toBeVisible()
      await expect(page.getByTestId('actions-fiche')).toHaveCount(0)
      await expect(page.getByRole('button', { name: 'Ouvrir une nouvelle liaison' })).toHaveCount(0)
    })
    await test.step('Quand il ouvre la mise en service à constater depuis son tableau de bord', async () => {
      await page.getByRole('link', { name: 'Tableau de bord', exact: true }).click()
      await monTravail(page).locator('[aria-label="Mes compteurs"]').getByRole('link', { name: 'Mises en service à constater' }).click()
      await expect(page).toHaveURL(/LIA-2025-064\?onglet=mise-en-service/)
      await expect(page.getByText(/est à constater/)).toBeVisible()
      await expect(page.getByText('Ouverture reportée à la prochaine fenêtre de maintenance')).toBeVisible()
      await expect(page.getByText('2 services ouverts sur 3')).toBeVisible()
    })
    await test.step('Et qu’il constate les services effectivement ouverts', async () => {
      await page.getByRole('button', { name: 'Constater la mise en service' }).click()
      await expect(page.getByRole('alertdialog').getByText('2 service(s) ouvert(s) sur 3', { exact: false })).toBeVisible()
      await confirmerAction(page, 'Constater la mise en service')
    })
    await test.step('Alors la mise en service est constatée et le suivi de l’exploitation s’ouvre', async () => {
      await expect(page.getByText(/Mise en service constatée le/)).toBeVisible()
      await expect(carte(page, 'Suivi de l\'exploitation').getByRole('cell', { name: '33' })).toBeVisible()
    })
  })
})
