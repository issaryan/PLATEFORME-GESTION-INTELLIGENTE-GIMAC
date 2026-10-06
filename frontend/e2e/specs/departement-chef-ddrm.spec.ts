import { ADRESSES, demo, expect, test } from '../fixtures'
import { confirmer, monTravail, naviguer, ongletSelectionne } from './outils'

// Fonctionnalité : le chef de la DDRM tranche les propositions (étape 2) puis ouvre la liaison d'un
// adhérent au cadrage et désigne son conducteur de projet (étape 3, D-57, D-58). Services simulés ; la
// réauthentification par navigator.credentials est couverte par departement-passerelle.spec.ts.

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
    await test.step('Quand l’adhésion d’un dossier complet lui est proposée, il l’approuve avec sa clé de sécurité', async () => {
      await page.getByRole('region', { name: 'Relations avec les membres (DDRM)' }).getByRole('button', { name: /^Candidats \d+$/ }).click()
      await page.getByRole('link', { name: 'Banque de Libreville' }).click()
      await expect(page.getByRole('heading', { level: 1 })).toHaveText('Banque de Libreville')
      await page.getByRole('button', { name: 'Proposer l\'adhésion' }).click()
      await confirmer(page, 'Proposer l\'adhésion', 'Dossier complet')
      await expect(page.getByRole('alertdialog')).toHaveCount(0)
      await ongletSelectionne(page, 'Propositions de statut')
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
      await page.getByRole('button', { name: 'Proposer la clôture sans suite' }).click()
      await confirmer(page, 'Proposer la clôture sans suite', 'Le candidat ne répond plus')
      await expect(page.getByRole('alertdialog')).toHaveCount(0)
      await ongletSelectionne(page, 'Propositions de statut')
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
      await page.getByLabel('Paiement mobile').check()
      await page.locator('select[name="chefDeProjetId"]').selectOption('U-202')
      await page.locator('select[name="banquePartenaireId"]').selectOption({ label: 'Banque B' })
      await page.getByRole('dialog').getByRole('button', { name: 'Ouvrir une liaison (cadrage)' }).click()
    })
    await test.step('Alors la liaison s’ouvre en phase de cadrage, avec son périmètre et ses intervenants', async () => {
      await expect(page.getByText(/ouverte en phase de cadrage\. Le statut de Mobile Money Sanaga passera à « En homologation »/)).toBeVisible()
      await expect(page.getByRole('heading', { level: 1 })).toHaveText('Mobile Money Sanaga')
      const resume = page.locator('section').filter({ has: page.getByRole('heading', { name: 'Cadrage de la liaison' }) })
      for (const texte of ['Cadrage', 'Transverse', 'Transfert', 'Paiement', 'Brice Ondo Mba', 'Banque B']) await expect(resume.getByText(texte, { exact: true })).toBeVisible()
    })
    await test.step('Quand il désigne un autre conducteur, motif à l’appui', async () => {
      await page.getByRole('button', { name: 'Désigner le conducteur' }).click()
      await page.locator('select[name="chefDeProjetId"]').selectOption('U-203')
      await page.locator('textarea[name="motif"]').fill('Réorganisation du portefeuille des conducteurs')
      await page.getByRole('dialog').getByRole('button', { name: 'Désigner le conducteur' }).click()
    })
    await test.step('Alors le conducteur est à jour dans les intervenants', async () => {
      await expect(page.locator('section').filter({ has: page.getByRole('heading', { name: 'Cadrage de la liaison' }) }).getByText('Estelle Ngo Bassong')).toBeVisible()
    })
    await test.step('Et l’institution est passée d’elle-même en homologation', async () => {
      await naviguer(page, 'Institutions membres')
      await page.getByRole('link', { name: 'Mobile Money Sanaga' }).click()
      await expect(page.getByRole('heading', { level: 1 })).toHaveText('Mobile Money Sanaga')
      await expect(page.getByText('En homologation').first()).toBeVisible()
      await page.getByRole('tab', { name: 'Propositions de statut' }).click()
      await expect(page.getByRole('cell', { name: 'automatique' })).toBeVisible()
      await page.getByRole('tab', { name: 'Liaisons' }).click()
      await expect(page.getByRole('link', { name: /Paiement mobile \(LIA-2026-\d+\)/ })).toBeVisible()
    })
  })

  test('Scénario : un projet déjà ouvert pour l’institution est refusé par le service', async ({ page }) => {
    await page.goto(`${ADRESSES.departement}/des/institutions/INST-0042?formulaire=liaison`)
    await expect(page.getByText(/est déjà en homologation : la nouvelle liaison s'ajoute/)).toBeVisible()
    await page.getByLabel('Carte interbancaire').check()
    await page.locator('select[name="chefDeProjetId"]').selectOption('U-201')
    const d = page.getByRole('dialog')
    await d.getByRole('button', { name: 'Ouvrir une liaison (cadrage)' }).click()
    await expect(d.getByText('Une liaison est déjà ouverte pour cette institution membre et ce type de projet.')).toBeVisible()
    await d.getByRole('button', { name: 'Annuler' }).click()
    await d.getByRole('button', { name: 'Abandonner les modifications' }).click()
    await expect(page.getByRole('dialog')).toHaveCount(0)
    await expect(page.getByRole('heading', { level: 1 })).toHaveText('Établissement de paiement A')
  })
})
