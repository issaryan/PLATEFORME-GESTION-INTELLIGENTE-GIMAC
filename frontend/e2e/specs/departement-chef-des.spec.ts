import { ADRESSES, demo, expect, test } from '../fixtures'
import { naviguer } from './outils'

// Fonctionnalité : le chef du DES suit le cadrage des liaisons, désigne les conducteurs de projet et
// ouvre les liaisons des adhérents (D-57, D-58) ; il parcourt la fiche d'une liaison. Services simulés.

test.describe('Fonctionnalité : cadrage des liaisons par le chef du DES', () => {
  test.beforeEach(async ({ page }) => {
    await demo(page, { profil: 'ROLE_CHEF_DES', theme: 'light' })
  })

  test('Scénario : suivre les liaisons en cadrage et désigner un conducteur depuis la liste', async ({ page }) => {
    await test.step('Étant donné le chef du DES sur son tableau de bord', async () => {
      await page.goto(`${ADRESSES.departement}/des/tableau-de-bord`)
      await expect(page.getByText('Chef du DES', { exact: true })).toBeVisible()
      const cadrage = page.getByRole('region', { name: 'Cadrage des liaisons' })
      await expect(cadrage.getByText('Adhérents à engager')).toBeVisible()
      await expect(cadrage.getByRole('link', { name: 'Établissement de microfinance C (Paiement mobile)' })).toBeVisible()
      await expect(page.getByRole('region', { name: 'Relations avec les membres (DDRM)' })).toHaveCount(0)
    })
    await test.step('Quand il ouvre la liste des liaisons en cadrage', async () => {
      await page.getByRole('region', { name: 'Cadrage des liaisons' }).getByRole('button').filter({ hasText: 'Liaisons en cadrage' }).click()
      await expect(page.getByRole('heading', { level: 1 })).toHaveText('Liaisons et parcours')
      await expect(page.getByRole('link', { name: 'Établissement de microfinance C' })).toBeVisible()
      await expect(page.getByRole('link', { name: 'Banque D' })).toHaveCount(0)
    })
    await test.step('Et qu’il désigne un autre conducteur pour la liaison', async () => {
      await page.getByRole('row', { name: /Établissement de microfinance C/ }).getByRole('button', { name: 'Désigner le conducteur' }).click()
      await expect(page.getByRole('heading', { level: 1 })).toHaveText('Désigner le conducteur de projet de la liaison de Établissement de microfinance C')
      await page.locator('select[name="chefDeProjetId"]').selectOption('U-202')
      await page.getByRole('button', { name: 'Désigner le conducteur' }).click()
    })
    await test.step('Alors la liste affiche le nouveau conducteur', async () => {
      await expect(page.getByRole('row', { name: /Établissement de microfinance C/ }).getByText('Brice Ondo Mba')).toBeVisible()
    })
    await test.step('Et le filtre « Mes projets » et la recherche restent à sa main', async () => {
      await page.getByRole('switch', { name: 'Mes projets' }).click()
      await expect(page.getByRole('switch', { name: 'Mes projets' })).toBeChecked()
      await page.getByRole('switch', { name: 'Mes projets' }).click()
      await expect(page.getByRole('switch', { name: 'Mes projets' })).not.toBeChecked()
      await page.getByRole('searchbox', { name: 'Rechercher', exact: true }).fill('Banque')
      await expect(page.getByRole('link', { name: 'Banque B' })).toBeVisible()
      await page.getByRole('tab', { name: 'Pilotage' }).click()
      await expect(page.getByRole('link', { name: 'Établissement de paiement A' })).toBeVisible()
      await page.getByRole('tab', { name: 'Liste des liaisons' }).click()
    })
    await test.step('Et « Ouvrir une liaison » le renvoie au registre des adhérents', async () => {
      await page.getByRole('button', { name: 'Ouvrir une liaison' }).click()
      await expect(page.getByRole('heading', { level: 1 })).toHaveText('Institutions membres')
      await expect(page.getByRole('link', { name: 'Crédit rural du Woleu' })).toBeVisible()
    })
  })

  test('Scénario : ouvrir une liaison, refus d’un type sans référentiel publié puis succès', async ({ page }) => {
    await page.goto(`${ADRESSES.departement}/des/institutions?statut=ADHERENT`)
    await page.getByRole('link', { name: 'Crédit rural du Woleu' }).click()
    await expect(page.getByRole('heading', { level: 1 })).toHaveText('Crédit rural du Woleu')
    await page.getByRole('button', { name: 'Ouvrir une liaison (cadrage)' }).click()
    await test.step('Quand il soumet sans type de projet', async () => {
      await page.locator('select[name="chefDeProjetId"]').selectOption('U-201')
      await page.getByRole('button', { name: 'Ouvrir une liaison (cadrage)' }).click()
      await expect(page.getByText('Choisissez le type de projet.').first()).toBeVisible()
    })
    await test.step('Quand il choisit un projet délégataire, sans référentiel publié', async () => {
      await page.getByLabel('Délégataire').check()
      await page.getByRole('button', { name: 'Ouvrir une liaison (cadrage)' }).click()
      await expect(page.getByText(/Aucun référentiel d'opérations n'est publié pour ce type de projet/)).toBeVisible()
    })
    await test.step('Puis un projet de carte interbancaire', async () => {
      await page.getByLabel('Carte interbancaire').check()
      await page.getByRole('button', { name: 'Ouvrir une liaison (cadrage)' }).click()
      await expect(page.getByText(/ouverte en phase de cadrage/)).toBeVisible()
      await expect(page.getByRole('heading', { level: 1 })).toHaveText('Crédit rural du Woleu')
    })
  })

  test('Scénario : parcourir la fiche d’une liaison en homologation', async ({ page }) => {
    await page.goto(`${ADRESSES.departement}/des/liaisons`)
    await page.getByRole('link', { name: 'Établissement de paiement A' }).click()
    await expect(page.getByRole('heading', { level: 1 })).toHaveText('Établissement de paiement A')
    for (const [onglet, attendu] of [
      ['Graphe', 'OP-E01'],
      ['Périmètre', 'Périmètre en lecture seule'],
      ['Opérations', 'Rapprochement à blanc'],
      ['Résultats du simulateur', 'SC-XYZ-99'],
      ['Preuves', 'Aucun dossier de preuves exporté'],
      ['Chronologie', 'Liaison ouverte'],
    ] as const) {
      await test.step(`Onglet ${onglet}`, async () => {
        await page.getByRole('tab', { name: onglet }).click()
        await expect(page.getByText(attendu).first()).toBeVisible()
      })
    }
    await test.step('Désignation du responsable d’une opération', async () => {
      await page.getByRole('tab', { name: 'Opérations' }).click()
      await page.getByRole('button', { name: 'Attribuer l\'opération' }).first().click()
      await page.locator('select[name="responsableId"]').selectOption('U-102')
      await page.locator('textarea[name="motif"]').fill('Charge rééquilibrée')
      await page.locator('form').getByRole('button', { name: 'Attribuer l\'opération' }).click()
      await expect(page.locator('form')).toHaveCount(0)
    })
    await test.step('Export scellé du dossier de preuves, après confirmation', async () => {
      await page.getByRole('tab', { name: 'Détail' }).click()
      await page.getByRole('button', { name: 'Exporter le dossier de preuves' }).click()
      await page.getByRole('alertdialog').getByRole('button', { name: 'Exporter le dossier de preuves' }).click()
      await expect(page.getByText('Opérations et leurs preuves')).toBeVisible()
    })
    await test.step('Dégel motivé d’une liaison gelée', async () => {
      await naviguer(page, 'Liaisons et parcours')
      await page.getByRole('link', { name: 'Banque D' }).click()
      await expect(page.getByRole('heading', { level: 1 })).toHaveText('Banque D')
      await page.getByRole('button', { name: 'Dégeler la liaison' }).click()
      const d = page.getByRole('alertdialog')
      await expect(d.getByText(/Institution suspendue par la production monétique/)).toBeVisible()
      await d.getByLabel(/Motif/).fill('Suspension levée par la production monétique')
      await d.getByRole('button', { name: 'Dégeler la liaison' }).click()
      await expect(page.getByText('Dégeler la liaison : effectué.')).toBeVisible()
    })
    await test.step('Dossier de mise en service d’une liaison apte', async () => {
      await naviguer(page, 'Liaisons et parcours')
      await page.getByRole('link', { name: 'Banque B' }).click()
      await expect(page.getByRole('heading', { level: 1 })).toHaveText('Banque B')
      await page.getByRole('button', { name: 'Ouvrir le dossier de mise en service' }).click()
      await expect(page.getByRole('tab', { name: 'Circuit des quitus' })).toBeVisible()
    })
    await test.step('Retour au tableau de bord', async () => {
      await naviguer(page, 'Tableau de bord')
      await expect(page.getByRole('heading', { level: 1 })).toHaveText('Tableau de bord')
    })
  })
})
