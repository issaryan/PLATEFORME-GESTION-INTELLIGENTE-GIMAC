import { ADRESSES, demo, expect, test } from '../fixtures'
import { confirmer, monTravail, naviguer } from './outils'

// Fonctionnalité : le chef de la DDRM tranche les propositions (étape 2) puis ouvre la liaison d'un
// adhérent au cadrage et désigne son conducteur de projet (étape 3, D-57, D-58). Services simulés ; la
// réauthentification par navigator.credentials est couverte par departement-passerelle.spec.ts.

/**
 * DDR-01 (maker-checker) : la proposition vient d'un autre acteur que le chef qui la tranche. Démonstration continue (D-98) :
 * l'acteur change par la session de démonstration, sur le même registre simulé.
 */
async function acteur(page: import('@playwright/test').Page, role: string) {
  await page.evaluate((r) => {
    localStorage.setItem('gm.demoContinue', '1')
    sessionStorage.setItem('gm.sessionDemo', JSON.stringify({ identifiant: '', role: r }))
  }, role)
}

test.describe('Fonctionnalité : décisions du chef de la DDRM et cadrage des liaisons', () => {
  test.beforeEach(async ({ page }) => {
    await demo(page, { profil: 'ROLE_CHEF_DDRM', theme: 'light' })
  })

  test('Scénario : trancher les propositions de statut', async ({ page }) => {
    await test.step('Étant donné le chef de la DDRM sur son tableau de bord', async () => {
      await page.goto(`${ADRESSES.departement}/des/tableau-de-bord`)
      await expect(page.locator('.gm-pagehead-user', { hasText: 'Chef de la DDRM' })).toBeVisible()
      await expect(page.getByRole('region', { name: 'Relations avec les membres (DDRM)' }).getByText('Propositions à trancher')).toBeVisible()
      await expect(monTravail(page)).toBeVisible()
      await expect(page.getByRole('region', { name: 'Pilotage des projets de certification' })).toBeVisible()
    })
    await test.step('Quand l’adhésion d’un dossier complet lui est proposée par la chargée, il l’approuve avec sa clé de sécurité', async () => {
      await page.getByRole('region', { name: 'Relations avec les membres (DDRM)' }).getByRole('button', { name: /^Candidats \d+$/ }).click()
      await page.getByRole('link', { name: 'Banque de Libreville' }).click()
      await expect(page.getByRole('heading', { level: 1 })).toHaveText('Banque de Libreville')
      const fiche = page.url().split('?')[0]
      await acteur(page, 'ROLE_CHARGE_DDRM')
      await page.goto(fiche)
      await page.getByRole('button', { name: 'Proposer l\'adhésion' }).click()
      await confirmer(page, 'Proposer l\'adhésion', 'Dossier complet')
      await expect(page.getByRole('alertdialog')).toHaveCount(0)
      await acteur(page, 'ROLE_CHEF_DDRM')
      await page.goto(`${fiche}?onglet=transitions`)
      await page.getByRole('button', { name: 'Approuver' }).click()
      const d = page.getByRole('dialog')
      await expect(d.getByText(/L'institution passera à l'état « Adhérent »/)).toBeVisible()
      await d.getByRole('button', { name: 'Utiliser ma clé de sécurité' }).click()
      await expect(d.getByText('Signature confirmée')).toBeVisible()
      await d.getByRole('button', { name: 'Continuer' }).click()
    })
    await test.step('Alors l’institution devient adhérente', async () => {
      await expect(page.getByRole('table').getByText('Approuvée')).toBeVisible()
      await page.getByRole('tab', { name: 'Détail' }).click()
      await expect(page.getByRole('button', { name: 'Ouvrir une liaison (cadrage)' })).toBeVisible()
    })
    await test.step('Quand une clôture sans suite est proposée, il la rejette en motivant', async () => {
      await naviguer(page, 'Institutions membres')
      await page.getByRole('link', { name: 'Fintech Kribi Pay' }).click()
      await expect(page.getByRole('heading', { level: 1 })).toHaveText('Fintech Kribi Pay')
      const fiche = page.url().split('?')[0]
      await acteur(page, 'ROLE_CHARGE_DDRM')
      await page.goto(fiche)
      await page.getByRole('button', { name: 'Proposer la clôture sans suite' }).click()
      await confirmer(page, 'Proposer la clôture sans suite', 'Le candidat ne répond plus')
      await expect(page.getByRole('alertdialog')).toHaveCount(0)
      await acteur(page, 'ROLE_CHEF_DDRM')
      await page.goto(`${fiche}?onglet=transitions`)
      await page.getByRole('row', { name: /Le candidat ne répond plus/ }).getByRole('button', { name: 'Plus d\'actions' }).click()
      await page.getByRole('dialog').getByRole('button', { name: 'Rejeter' }).click()
      await confirmer(page, 'Rejeter', 'Relance prévue le mois prochain')
    })
    await test.step('Alors la proposition est rejetée', async () => {
      await expect(page.getByText('Rejetée').first()).toBeVisible()
    })
  })

  test('Scénario : ouvrir la liaison d’un adhérent au cadrage et désigner son conducteur', async ({ page }) => {
    await page.goto(`${ADRESSES.departement}/des/tableau-de-bord`)
    await test.step('Quand il part du registre des adhérents', async () => {
      await page.getByRole('region', { name: 'Relations avec les membres (DDRM)' }).getByRole('button', { name: /^Adhérents sans liaison \d+$/ }).click()
      await expect(page.getByRole('link', { name: 'Mobile Money Sanaga' })).toBeVisible()
      await page.getByRole('row', { name: /Mobile Money Sanaga/ }).getByRole('button', { name: 'Ouvrir une liaison (cadrage)' }).click()
    })
    await test.step('Et qu’il choisit le type de projet, le conducteur et la banque partenaire', async () => {
      await expect(page.getByRole('dialog', { name: 'Ouvrir une liaison (cadrage) pour Mobile Money Sanaga' })).toBeVisible()
      await expect(page.getByRole('heading', { level: 1 })).toHaveText('Mobile Money Sanaga')
      await expect(page.getByText(/fera ensuite passer Mobile Money Sanaga du statut « Adhérent » au statut « En homologation »/)).toBeVisible()
      await page.getByLabel('Carte interbancaire').check()
      await page.locator('select[name="chefDeProjetId"]').selectOption('U-COND-2')
      await page.locator('select[name="banquePartenaireId"]').selectOption({ label: 'Banque B' })
      await page.getByRole('dialog').getByRole('button', { name: 'Ouvrir une liaison (cadrage)' }).click()
    })
    // D-98 : l'ouverture mène au dossier de la liaison, où chaque acteur suit et réalise ses opérations.
    await test.step('Alors le dossier de la liaison s’ouvre au cadrage, avec la banque partenaire et le conducteur', async () => {
      await expect(page.getByText(/Liaison LIA-\d{4}-\d{3} ouverte au cadrage/)).toBeVisible()
      await expect(page.getByRole('heading', { level: 1 })).toHaveText('Mobile Money Sanaga')
      for (const texte of ['Banque B', 'Brice Ondo Mba']) await expect(page.getByText(texte, { exact: true }).first()).toBeVisible()
    })
    await test.step('Quand il désigne un autre conducteur, motif à l’appui', async () => {
      await page.getByRole('button', { name: 'Redésigner le conducteur' }).click()
      const d = page.getByRole('dialog')
      await d.getByLabel(/Conducteur désigné/).selectOption('U-COND-1')
      await d.getByLabel(/Motif de la redésignation/).fill('Réorganisation du portefeuille des conducteurs')
      await d.getByRole('button', { name: 'Redésigner le conducteur' }).click()
    })
    await test.step('Alors le conducteur est à jour dans le dossier', async () => {
      await expect(page.getByText('Aïcha Mahamat Nour', { exact: true }).first()).toBeVisible()
    })
    await test.step('Et l’institution est passée d’elle-même en homologation', async () => {
      await naviguer(page, 'Institutions membres')
      await page.getByRole('link', { name: 'Mobile Money Sanaga' }).click()
      await expect(page.getByRole('heading', { level: 1 })).toHaveText('Mobile Money Sanaga')
      await expect(page.getByText('En homologation').first()).toBeVisible()
      await page.getByRole('tab', { name: 'Propositions de statut' }).click()
      await expect(page.getByRole('cell', { name: 'automatique' })).toBeVisible()
      await naviguer(page, 'Liaisons et parcours')
      await page.getByRole('searchbox', { name: 'Rechercher' }).fill('Mobile Money')
      await expect(page.getByRole('link', { name: /^LIA-2026-\d+$/ }).first()).toBeVisible()
    })
  })

  test('Scénario : un projet déjà ouvert pour l’institution est refusé par le service', async ({ page }) => {
    await page.goto(`${ADRESSES.departement}/des/institutions/INST-0042?formulaire=liaison`)
    await expect(page.getByText(/est déjà en homologation : la nouvelle liaison s'ajoute/)).toBeVisible()
    await page.getByLabel('Carte interbancaire').check()
    await page.locator('select[name="chefDeProjetId"]').selectOption('U-COND-1')
    const d = page.getByRole('dialog')
    await d.getByRole('button', { name: 'Ouvrir une liaison (cadrage)' }).click()
    await expect(d.getByText('Une liaison est déjà ouverte pour cette institution membre et ce type de projet.')).toBeVisible()
    await d.getByRole('button', { name: 'Annuler' }).click()
    await d.getByRole('button', { name: 'Abandonner les modifications' }).click()
    await expect(page.getByRole('dialog')).toHaveCount(0)
    await expect(page.getByRole('heading', { level: 1 })).toHaveText('Établissement de paiement A')
  })
})
