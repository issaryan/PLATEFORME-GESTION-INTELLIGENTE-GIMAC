import { ADRESSES, demo, expect, test } from '../fixtures'
import { carte, monTravail, naviguer, ongletSelectionne } from './outils'

// Fonctionnalité : le chef du DES suit son travail (« Mon travail »), désigne les conducteurs de projet et
// ouvre les liaisons des adhérents (D-57, D-58) ; il parcourt la fiche historique d'une liaison (écrans
// DES-08 et DES-09, joignables depuis le dossier). Services simulés.

test.describe('Fonctionnalité : cadrage des liaisons par le chef du DES', () => {
  test.beforeEach(async ({ page }) => {
    await demo(page, { profil: 'ROLE_CHEF_DES', theme: 'light' })
  })

  test('Scénario : suivre son travail, désigner le conducteur d’une liaison qui n’en a pas, puis la liste historique', async ({ page }) => {
    await test.step('Étant donné le chef du DES sur son tableau de bord', async () => {
      await page.goto(`${ADRESSES.departement}/des/tableau-de-bord`)
      await expect(page.locator('.gm-pagehead-user', { hasText: 'Chef du DES' })).toBeVisible()
      await expect(monTravail(page).getByRole('heading', { name: 'Liaison sans conducteur (1)' })).toBeVisible()
      await expect(page.getByRole('region', { name: 'Pilotage des projets de certification' })).toBeVisible()
      await expect(page.getByRole('region', { name: 'Relations avec les membres (DDRM)' })).toHaveCount(0)
    })
    await test.step('Quand il désigne le conducteur de la liaison sans conducteur', async () => {
      await carte(page, 'Liaison sans conducteur (1)').getByRole('row', { name: /LIA-2026-044/ }).getByRole('button', { name: 'Désigner le conducteur' }).click()
      await expect(page).toHaveURL(/\/des\/parcours\/LIA-2026-044\?action=designer/)
      const d = page.getByRole('dialog', { name: 'Désigner le conducteur' })
      await expect(d.getByLabel(/Conducteur désigné/).locator('option')).toHaveCount(3)
      await d.getByLabel(/Conducteur désigné/).selectOption('U-COND-1')
      await d.getByRole('button', { name: 'Désigner le conducteur' }).click()
    })
    await test.step('Alors le dossier de la liaison porte son conducteur', async () => {
      await expect(page.getByText('Conducteur de projet 1 conduit désormais cette liaison.')).toBeVisible()
      await expect(page.getByRole('button', { name: 'Redésigner le conducteur' })).toBeVisible()
    })
    await test.step('Et la liste historique des liaisons lui permet encore de désigner un conducteur', async () => {
      await page.goto(`${ADRESSES.departement}/des/liaisons`)
      await expect(page.getByRole('heading', { level: 1 })).toHaveText('Liaisons et parcours')
      await page.getByRole('row', { name: /Établissement de microfinance C/ }).getByRole('button', { name: 'Désigner le conducteur' }).click()
      await expect(page.getByRole('dialog', { name: 'Désigner le conducteur de projet de la liaison de Établissement de microfinance C' })).toBeVisible()
      await page.locator('select[name="chefDeProjetId"]').selectOption('U-202')
      await page.getByRole('dialog').getByRole('button', { name: 'Désigner le conducteur' }).click()
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
    const d = page.getByRole('dialog')
    await test.step('Quand il soumet sans type de projet', async () => {
      await page.locator('select[name="chefDeProjetId"]').selectOption('U-201')
      await d.getByRole('button', { name: 'Ouvrir une liaison (cadrage)' }).click()
      await expect(page.getByText('Choisissez le type de projet.').first()).toBeVisible()
    })
    await test.step('Quand il choisit un projet délégataire, sans référentiel publié', async () => {
      await page.getByLabel('Délégataire').check()
      await d.getByRole('button', { name: 'Ouvrir une liaison (cadrage)' }).click()
      await expect(page.getByText(/Aucun référentiel d'opérations n'est publié pour ce type de projet/)).toBeVisible()
    })
    await test.step('Puis un projet de carte interbancaire', async () => {
      await page.getByLabel('Carte interbancaire').check()
      await d.getByRole('button', { name: 'Ouvrir une liaison (cadrage)' }).click()
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
      await page.getByRole('dialog').getByRole('button', { name: 'Attribuer l\'opération' }).click()
      await expect(page.getByRole('dialog')).toHaveCount(0)
    })
    await test.step('Export scellé du dossier de preuves, après confirmation', async () => {
      await page.getByRole('tab', { name: 'Détail' }).click()
      await page.getByRole('button', { name: 'Exporter le dossier de preuves' }).click()
      await page.getByRole('alertdialog').getByRole('button', { name: 'Exporter le dossier de preuves' }).click()
      await expect(page.getByText('Opérations et leurs preuves')).toBeVisible()
    })
    await test.step('Dégel motivé d’une liaison gelée', async () => {
      await page.goto(`${ADRESSES.departement}/des/liaisons`)
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
      await page.goto(`${ADRESSES.departement}/des/liaisons`)
      await page.getByRole('link', { name: 'Banque B' }).click()
      await expect(page.getByRole('heading', { level: 1 })).toHaveText('Banque B')
      await page.getByRole('button', { name: 'Ouvrir le dossier de mise en service' }).click()
      await ongletSelectionne(page, 'Décision')
      await expect(page.getByText('Décision de mise en production : modèle hybride')).toBeVisible()
      // La liaison historique LIA-2025-102 n'a pas d'état de décision au parcours simulé : le résumé le dit.
      await expect(page.getByTestId('resume-decision').getByText('L\'état de la décision n\'est pas disponible pour cette liaison.')).toBeVisible()
      await page.getByRole('button', { name: 'Ouvrir la décision de la liaison' }).click()
      await expect(page).toHaveURL(/\/des\/parcours\/LIA-2025-102\?onglet=decision/)
    })
    await test.step('Retour au tableau de bord', async () => {
      await naviguer(page, 'Tableau de bord')
      await expect(page.getByRole('heading', { level: 1 })).toHaveText('Tableau de bord')
    })
  })
})
