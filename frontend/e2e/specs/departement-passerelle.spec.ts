import { ADRESSES, expect, test } from '../fixtures'
import { PasserelleDes, PERMISSIONS_CHEF_DDRM } from './passerelle-des'

// Fonctionnalité : l'application du département sur la passerelle (VITE_SERVICES=http), dont Playwright
// simule les réponses : approbation d'une adhésion signée par la clé de sécurité (navigator.credentials,
// en-tête X-Reauth-Assertion), ouverture d'une liaison au cadrage (POST /liaisons) avec refus du service
// puis succès, désignation motivée du conducteur (PUT /liaisons/{id}/chef-de-projet), canal poussé.

test.describe('Fonctionnalité : application du département sur la passerelle', () => {
  test('Scénario : le chef de la DDRM approuve une adhésion avec sa clé de sécurité', async ({ page }) => {
    const gw = new PasserelleDes()
    await gw.brancher(page)
    await test.step('Étant donné une proposition d’adhésion en attente', async () => {
      await page.goto(`${ADRESSES.departementHttp}/des/tableau-de-bord`)
      await expect(page.getByRole('region', { name: 'Relations avec les membres (DDRM)' }).getByText('Propositions à trancher')).toBeVisible()
      await page.goto(`${ADRESSES.departementHttp}/des/institutions/i-bdl?onglet=transitions`)
      await expect(page.getByRole('cell', { name: 'Dossier complet et conditions définies' })).toBeVisible()
    })
    await test.step('Quand il l’approuve en présentant sa clé de sécurité', async () => {
      await page.getByRole('button', { name: 'Approuver' }).click()
      const d = page.getByRole('dialog')
      await d.getByRole('button', { name: 'Utiliser ma clé de sécurité' }).click()
      await expect(d.getByText('Signature confirmée')).toBeVisible()
      await d.getByRole('button', { name: 'Continuer' }).click()
    })
    await test.step('Alors la décision part signée et l’institution est adhérente', async () => {
      await expect(page.getByRole('table').getByText('Approuvée')).toBeVisible()
      const defi = gw.appels.find((a) => a.chemin === '/bff/des/iam/reauthentification/defi')!
      expect(defi.corps).toEqual({ purpose: 'STATUS_APPROVE' })
      const appel = gw.appels.find((a) => a.chemin.endsWith('/transitions/t-1/approbation'))!
      expect(JSON.parse(Buffer.from(appel.entetes['x-reauth-assertion'], 'base64').toString())).toMatchObject({ challengeId: 'defi-des', id: 'cle-des', clientDataJSON: 'AQI' })
      await page.getByRole('tab', { name: 'Détail' }).click()
      await expect(page.getByText('Adhérent').first()).toBeVisible()
    })
  })

  test('Scénario : ouverture refusée par MS-HOMOLOGATION puis acceptée, désignation motivée', async ({ page }) => {
    const gw = new PasserelleDes({ refusOuverture: { status: 409, code: 'HOM_LIAISON_INSTITUTION_NOT_ELIGIBLE', details: { statut: 'CANDIDAT' } } })
    await gw.brancher(page)
    await page.goto(`${ADRESSES.departementHttp}/des/institutions/i-mms`)
    await page.getByRole('button', { name: 'Ouvrir une liaison (cadrage)' }).click()
    await test.step('Quand il soumet l’ouverture et que le service juge l’institution inéligible', async () => {
      await page.getByLabel('Paiement mobile').check()
      await page.locator('select[name="chefDeProjetId"]').selectOption('U-201')
      await page.locator('select[name="banquePartenaireId"]').selectOption({ label: 'Banque B' })
      await page.getByRole('dialog').getByRole('button', { name: 'Ouvrir une liaison (cadrage)' }).click()
      await expect(page.getByText(/son statut est « Candidat »/)).toBeVisible()
      await expect(page.getByText('e2e-HOM_LIAISON_INSTITUTION_NOT_ELIGIBLE')).toBeVisible()
    })
    await test.step('Alors une nouvelle soumission ouvre la liaison au cadrage', async () => {
      await page.getByRole('dialog').getByRole('button', { name: 'Ouvrir une liaison (cadrage)' }).click()
      // D-98 : la liaison ouverte mène à son dossier (parcours).
      await expect(page).toHaveURL(/\/des\/parcours\/LIA-2026-101\?nouvelle=1/)
      const ouverture = gw.appels.filter((a) => a.methode === 'POST' && a.chemin === '/bff/des/homologation/liaisons').at(-1)!
      expect(ouverture.corps).toEqual({ institutionId: 'i-mms', typeProjet: 'PAIEMENT_MOBILE', chefDeProjetId: 'U-201', banquePartenaireId: 'i-bqb' })
    })
    await test.step('Quand il désigne un autre conducteur avec un motif', async () => {
      await page.goto(`${ADRESSES.departementHttp}/des/liaisons/LIA-2026-101`)
      await page.getByRole('button', { name: 'Désigner le conducteur' }).click()
      await page.locator('select[name="chefDeProjetId"]').selectOption('U-202')
      await page.locator('textarea[name="motif"]').fill('Charge du premier conducteur')
      await page.getByRole('dialog').getByRole('button', { name: 'Désigner le conducteur' }).click()
      await expect(page.locator('section').filter({ has: page.getByRole('heading', { name: 'Cadrage de la liaison' }) }).getByText('Brice Ondo Mba')).toBeVisible()
      const designation = gw.appels.find((a) => a.methode === 'PUT' && a.chemin === '/bff/des/homologation/liaisons/LIA-2026-101/chef-de-projet')!
      expect(designation.corps).toEqual({ chefDeProjetId: 'U-202', motif: 'Charge du premier conducteur' })
    })
    await test.step('Et la fiche de l’institution montre la bascule automatique en homologation', async () => {
      await page.goto(`${ADRESSES.departementHttp}/des/institutions/i-mms?onglet=transitions`)
      await expect(page.getByRole('cell', { name: 'automatique' })).toBeVisible()
      await page.getByRole('tab', { name: 'Liaisons' }).click()
      await expect(page.getByRole('link', { name: 'Paiement mobile (LIA-2026-101)' })).toBeVisible()
    })
  })

  test('Scénario : sans droit de désignation, l’ouverture est refusée (403)', async ({ page }) => {
    const gw = new PasserelleDes({ permissions: PERMISSIONS_CHEF_DDRM, refusOuverture: { status: 403, code: 'HOM_PROJECT_DESIGNATION_FORBIDDEN' } })
    await gw.brancher(page)
    await page.goto(`${ADRESSES.departementHttp}/des/institutions/i-mms?formulaire=liaison`)
    await page.getByLabel('Carte interbancaire').check()
    await page.locator('select[name="chefDeProjetId"]').selectOption('U-202')
    await page.getByRole('dialog').getByRole('button', { name: 'Ouvrir une liaison (cadrage)' }).click()
    await expect(page.getByText(/droit de désigner un conducteur de projet/)).toBeVisible()
    await test.step('Et une liaison antérieure au cadrage se lit sans périmètre ni conducteur', async () => {
      await page.goto(`${ADRESSES.departementHttp}/des/liaisons/LIA-2025-090`)
      const resume = page.locator('section').filter({ has: page.getByRole('heading', { name: 'Cadrage de la liaison' }) })
      await expect(resume.getByText('aucun conducteur désigné')).toBeVisible()
      await expect(resume.getByText('Certification')).toBeVisible()
      await page.getByRole('tab', { name: 'Périmètre' }).click()
      await expect(page.getByText(/aucun service souscrit/).first()).toBeVisible()
      await expect(page.getByRole('heading', { name: 'Aucun service souscrit' })).toBeVisible()
    })
  })

  test('Scénario : prise de contact contrôlée avant envoi, refus du service sous le champ, puis enregistrement', async ({ page }) => {
    const gw = new PasserelleDes({ refusEnregistrement: { codeIdentification: 'must match "^[A-Z]{4}[A-Z]{2}[A-Z0-9]{2}([A-Z0-9]{3})?$"' } })
    await gw.brancher(page)
    const enregistrements = () => gw.appels.filter((a) => a.methode === 'POST' && a.chemin === '/bff/des/participant/institutions')
    await page.goto(`${ADRESSES.departementHttp}/des/institutions`)
    await page.getByRole('button', { name: 'Enregistrer une prise de contact' }).click()
    const d = page.getByRole('dialog')
    await test.step('Quand il saisit un code BEAC avec un tiret, le champ est signalé sans appel au service', async () => {
      await page.locator('input[name="nom"]').fill('Banque des Lagunes')
      await page.locator('select[name="categorie"]').selectOption('BANQUE')
      await page.locator('select[name="pays"]').selectOption('GA')
      await page.locator('input[name="codeIdentification"]').fill('bdlg ga li')
      await page.locator('input[name="codeBanqueBeac"]').fill('10005-001')
      await page.locator('input[name="datePriseDeContact"]').fill('12/01/2026')
      await d.getByRole('button', { name: 'Enregistrer une prise de contact' }).click()
      await expect(d.getByText(/Code banque BEAC attendu : lettres et chiffres seulement/).first()).toBeVisible()
      await expect(page.locator('input[name="codeBanqueBeac"]')).toHaveAttribute('aria-invalid', 'true')
      expect(enregistrements()).toHaveLength(0)
    })
    await test.step('Alors, une fois corrigé, le refus du service (fieldErrors) s’affiche sous le champ concerné', async () => {
      await page.locator('input[name="codeBanqueBeac"]').fill('10005 001')
      await d.getByRole('button', { name: 'Enregistrer une prise de contact' }).click()
      await expect(d.getByText(/Code BIC refusé par le service/).first()).toBeVisible()
      await expect(d.getByText('Le service a refusé la saisie : corrigez les champs signalés.')).toBeVisible()
      await expect(page.locator('input[name="codeIdentification"]')).toHaveAttribute('aria-invalid', 'true')
      await expect(d.getByText('error.validation')).toHaveCount(0)
      expect(enregistrements()[0].corps).toEqual({
        nom: 'Banque des Lagunes', sigle: null, pays: 'GA', categorie: 'BANQUE', codeIdentification: 'BDLGGALI', codeBanqueBeac: '10005001',
        datePriseDeContact: '2026-01-12', originePriseDeContact: null, projetEnvisage: null,
      })
    })
    await test.step('Et une nouvelle soumission enregistre la prise de contact et ouvre la fiche', async () => {
      await d.getByRole('button', { name: 'Enregistrer une prise de contact' }).click()
      await expect(page.getByRole('heading', { level: 1 })).toHaveText('Banque des Lagunes')
      expect(enregistrements()).toHaveLength(2)
    })
  })

  test('Scénario : liste des liaisons indisponible, erreur expliquée avec son identifiant', async ({ page }) => {
    const gw = new PasserelleDes({ listeEnErreur: true })
    await gw.brancher(page)
    await page.goto(`${ADRESSES.departementHttp}/des/liaisons`)
    await expect(page.getByText('e2e-HOM_INDISPONIBLE')).toBeVisible()
    await page.goto(`${ADRESSES.departementHttp}/des/institutions/i-mms?onglet=liaisons`)
    await expect(page.getByRole('heading', { level: 1 })).toHaveText('Mobile Money Sanaga')
  })
})
