import type { Locator, Page } from '@playwright/test'
import { ADRESSES, demo, expect, test, type ProfilDemo } from '../fixtures'
import { naviguer, ongletSelectionne } from './outils'
import { capturerGuide, choisirMultiple, fermerToasts, relierCartes, zoomerSur, type Theme } from './guide-referentiels-outils'

// Guide visuel pas à pas « Mettre à jour les référentiels métier » (captures numérotées, thèmes clair et sombre),
// services simulés de l'application du département. Partie A : processus d'une cellule ou d'un service (D-87,
// docs/lot1/processus-unite.md). Partie B : référentiel d'homologation (D-77, D-89, docs/lot1/modelisateur-processus.md
// et docs/lot1/referentiel-droits-etapes.md). Captures dans e2e/captures/guide-referentiels (ou $GIMAC_CAPTURES/
// guide-referentiels), décrites dans e2e/captures/guide-referentiels/LISEZMOI.md.
//
// Guide, et non scénario de recette : il ne s'exécute que sur demande (GIMAC_GUIDE=1), jamais dans `npm run e2e` ni
// `npm run e2e:couverture`. Régénération :
//   GIMAC_GUIDE=1 npx playwright test --config e2e/playwright.config.ts guide-referentiels
// L'état des services simulés vit dans la page : chaque parcours reste dans un seul chargement (navigation interne).

test.skip(!process.env.GIMAC_GUIDE, 'Guide visuel : GIMAC_GUIDE=1 pour produire les captures.')
test.describe.configure({ mode: 'parallel' })

const outilsProcessus = (page: Page) => page.getByRole('toolbar', { name: 'Outils du modélisateur' })
const modaleEtape = (page: Page, titre: string | RegExp = /^(Nouvelle étape|Étape [A-Z_]+)$/) => page.getByRole('dialog', { name: titre })
const champ = (portee: Locator, nom: string) => portee.locator(`[name="${nom}"]`)

/** Ouvre la modale « Nouvelle étape » et remplit l'onglet Général. */
async function nouvelleEtape(page: Page, code: string, libelle: string, action: string) {
  await outilsProcessus(page).getByRole('button', { name: 'Ajouter une étape' }).click()
  const d = modaleEtape(page, 'Nouvelle étape')
  await expect(d).toBeVisible()
  await champ(d, 'code').fill(code)
  await champ(d, 'libelle').fill(libelle)
  await champ(d, 'action').fill(action)
  return d
}
async function validerEtape(page: Page, d: Locator, bouton = 'Ajouter une étape') {
  await d.getByRole('button', { name: bouton, exact: true }).click()
  await expect(d).toHaveCount(0)
}

for (const theme of ['light', 'dark'] as const satisfies readonly Theme[]) {
  const suffixe = theme === 'light' ? 'clair' : 'sombre'
  const capturer = (page: Page, nom: string, o: Parameters<typeof capturerGuide>[3] = {}) => capturerGuide(page, nom, theme, o)

  test.describe(`Guide A : processus d’une cellule ou d’un service (${suffixe}) @guide`, () => {
    test('Le chef du service Production informatique crée, publie, met à jour et lance un processus du pôle réseau', async ({ page }) => {
      test.setTimeout(240_000)
      // Chef du service Production informatique (Gaston Ndzana Ebode) : PROCESSUS_DEFINIR de portée UNITES sur les unités
      // qu'il dirige, dont le pôle réseau et le pôle système (le pôle n'a pas de chef propre dans l'organisation simulée).
      await demo(page, { profil: 'ROLE_RESPONSABLE_SERVICE', theme })
      await page.goto(`${ADRESSES.departement}/des/tableau-de-bord`)

      await test.step('01 : menu « Processus de l’unité », catalogue filtré sur le pôle réseau', async () => {
        await naviguer(page, 'Processus de l’unité')
        await expect(page.getByRole('heading', { level: 1, name: 'Processus de l’unité' })).toBeVisible()
        await page.locator('#proc-filtre-unite').selectOption('POLE_RESEAU')
        await expect(page.getByRole('heading', { level: 2, name: /^Pôle réseau/ })).toBeVisible()
        await capturer(page, '01-catalogue-unite', {
          cibles: [page.getByRole('navigation', { name: 'Navigation principale' }).getByRole('link', { name: 'Processus de l’unité', exact: true }), page.locator('#proc-filtre-unite')],
        })
      })

      await test.step('02 : « Nouveau processus », modale remplie', async () => {
        await page.getByRole('button', { name: 'Nouveau processus' }).click()
        const d = page.getByRole('dialog', { name: 'Nouveau processus' })
        await champ(d, 'code').fill('CHANGEMENT_ROUTEUR')
        await champ(d, 'unite').selectOption('POLE_RESEAU')
        await champ(d, 'libelle').fill('Changement de configuration d’un routeur')
        await champ(d, 'categorie').fill('Réseau')
        await champ(d, 'delaiCibleJours').fill('7')
        await champ(d, 'description').fill('Demande, analyse d’impact, mise en œuvre et vérification d’un changement de configuration sur un routeur de la plateforme.')
        await capturer(page, '02-nouveau-processus')
        await d.getByRole('button', { name: 'Créer le brouillon' }).click()
      })

      await test.step('03 : le brouillon s’ouvre dans le modélisateur, vide', async () => {
        await expect(page.getByRole('heading', { level: 1, name: 'Changement de configuration d’un routeur' })).toBeVisible()
        await expect(page.getByText('Aucune étape : ajoutez la première.')).toBeVisible()
        await capturer(page, '03-modelisateur-vide', { cibles: [outilsProcessus(page).getByRole('button', { name: 'Ajouter une étape' })] })
      })

      await test.step('04 : première étape, onglet Général (code, libellé, action)', async () => {
        const d = await nouvelleEtape(page, 'DEMANDE', 'Demande de changement', 'Décrire le changement demandé : routeur concerné, configuration visée, motif et créneau souhaité.')
        await capturer(page, '04-etape-general')
        // Intervenant par défaut : le lanceur de l'instance.
        await validerEtape(page, d)
      })

      await test.step('05 à 11 : deuxième étape, un onglet par aspect', async () => {
        const d = await nouvelleEtape(page, 'ANALYSE', 'Analyse d’impact du changement', 'Évaluer l’impact du changement sur les flux des participants et rédiger le plan de changement.')
        await ongletSelectionne(d, 'Intervenant et validateur')
        await champ(d, 'intervenant.mode').selectOption('PROFIL')
        await champ(d, 'intervenant.profil').selectOption('ROLE_POLE_RESEAU')
        await capturer(page, '05-intervenant-profil', { cibles: [d.getByRole('group', { name: 'Intervenant', exact: true })] })

        await champ(d, 'intervenant.mode').selectOption('PERSONNE')
        await expect(champ(d, 'intervenant.personneId')).toBeEnabled()
        await champ(d, 'intervenant.personneId').selectOption('u-res')
        await champ(d, 'intervenant.remplacement').selectOption('ROLE_POLE_RESEAU')
        await capturer(page, '06-intervenant-personne', { cibles: [d.getByRole('group', { name: 'Intervenant', exact: true })] })

        await d.getByLabel('L’étape est validée par un validateur').check()
        await champ(d, 'validateur.mode').selectOption('CHEF_UNITE')
        await champ(d, 'validateur.unite').selectOption('PROD_INFO')
        await d.getByRole('group', { name: 'Validateur', exact: true }).scrollIntoViewIfNeeded()
        await capturer(page, '07-validateur', { cibles: [d.getByRole('group', { name: 'Validateur', exact: true })] })

        await ongletSelectionne(d, 'Livrables')
        await d.getByRole('button', { name: 'Ajouter un livrable' }).click()
        await champ(d, 'livrables.0.code').fill('PLAN_CHANGEMENT')
        await champ(d, 'livrables.0.libelle').fill('Plan de changement')
        await expect(d.getByLabel('obligatoire')).toBeChecked()
        await capturer(page, '08-livrables', { cibles: [d.getByRole('group', { name: 'Livrable 1' })] })

        await ongletSelectionne(d, 'Signatures')
        await d.getByRole('button', { name: 'Ajouter une signature' }).click()
        await champ(d, 'signatures.0.code').fill('VISA_PLAN')
        await champ(d, 'signatures.0.libelle').fill('Visa du chef de service sur le plan')
        await champ(d, 'signatures.0.signataire.unite').selectOption('PROD_INFO')
        await d.getByLabel('PLAN_CHANGEMENT · Plan de changement').check()
        await d.getByLabel('PLAN_CHANGEMENT · Plan de changement').scrollIntoViewIfNeeded()
        await capturer(page, '09-signatures', { cibles: [d.getByRole('group', { name: 'Signature 1' })] })

        await ongletSelectionne(d, 'Champs')
        await d.getByRole('button', { name: 'Ajouter un champ' }).click()
        await champ(d, 'champs.0.code').fill('FENETRE')
        await champ(d, 'champs.0.libelle').fill('Fenêtre de maintenance retenue')
        await champ(d, 'champs.0.type').selectOption('DATE')
        await d.getByLabel('Obligatoire').check()
        await capturer(page, '10-champs', { cibles: [d.getByRole('group', { name: 'Champ 1' })] })

        await ongletSelectionne(d, 'Chronologie')
        await d.getByLabel('DEMANDE · Demande de changement').check()
        await champ(d, 'delaiJours').fill('2')
        await capturer(page, '11-chronologie', { cibles: [d.getByRole('tabpanel', { name: 'Chronologie' })] })
        await validerEtape(page, d)
      })

      await test.step('12 : deux étapes de plus, la dernière reliée sur le canevas', async () => {
        let d = await nouvelleEtape(page, 'MISE_EN_OEUVRE', 'Mise en œuvre sur le routeur', 'Appliquer la configuration pendant la fenêtre retenue et joindre la configuration appliquée.')
        await ongletSelectionne(d, 'Intervenant et validateur')
        await champ(d, 'intervenant.mode').selectOption('AU_LANCEMENT')
        // Profil de remplacement choisi par erreur : aucun membre du pôle réseau ne le porte (relevé au contrôle).
        await champ(d, 'intervenant.remplacement').selectOption('ROLE_POLE_SYSTEME')
        await ongletSelectionne(d, 'Livrables')
        await d.getByRole('button', { name: 'Ajouter un livrable' }).click()
        await champ(d, 'livrables.0.code').fill('CONFIG_APPLIQUEE')
        await champ(d, 'livrables.0.libelle').fill('Configuration appliquée')
        await ongletSelectionne(d, 'Chronologie')
        await d.getByLabel('ANALYSE · Analyse d’impact du changement').check()
        await champ(d, 'delaiJours').fill('1')
        await validerEtape(page, d)

        d = await nouvelleEtape(page, 'VERIFICATION', 'Vérification et clôture', 'Vérifier le fonctionnement des flux après le changement et clôturer la demande.')
        await ongletSelectionne(d, 'Intervenant et validateur')
        await champ(d, 'intervenant.mode').selectOption('PROFIL')
        await champ(d, 'intervenant.profil').selectOption('ROLE_POLE_RESEAU')
        // Validateur « chef de l'unité » de l'unité propriétaire : le pôle réseau n'a pas de chef (relevé au contrôle).
        await d.getByLabel('L’étape est validée par un validateur').check()
        await validerEtape(page, d)

        // Liaison au canevas : tirer de la sortie de MISE_EN_OEUVRE vers l'entrée de VERIFICATION.
        await relierCartes(page, 'processus-canevas', 'MISE_EN_OEUVRE', 'VERIFICATION')
        await expect(page.getByTestId('rf__edge-MISE_EN_OEUVRE>VERIFICATION')).toBeAttached()
        await page.getByRole('button', { name: 'Ajuster à la vue' }).click()
        await page.waitForTimeout(400)
        await capturer(page, '12-graphe-relie', { cibles: [page.getByTestId('processus-canevas')] })
      })

      await test.step('13 : « Contrôler » liste les anomalies', async () => {
        await outilsProcessus(page).getByRole('button', { name: 'Contrôler' }).click()
        const controle = page.getByRole('region', { name: 'Contrôle de publication' })
        await expect(controle.getByRole('listitem')).toHaveCount(2)
        await capturer(page, '13-controle-anomalies', { cibles: [controle] })
      })

      await test.step('14 et 15 : corrections, puis contrôle sans anomalie', async () => {
        const controle = page.getByRole('region', { name: 'Contrôle de publication' })
        await controle.getByRole('button', { name: /MISE_EN_OEUVRE/ }).click()
        let d = modaleEtape(page, 'Étape MISE_EN_OEUVRE')
        await ongletSelectionne(d, 'Intervenant et validateur')
        await champ(d, 'intervenant.remplacement').selectOption('ROLE_POLE_RESEAU')
        await validerEtape(page, d, 'Enregistrer')
        await controle.getByRole('button', { name: /VERIFICATION/ }).click()
        d = modaleEtape(page, 'Étape VERIFICATION')
        await ongletSelectionne(d, 'Intervenant et validateur')
        await champ(d, 'validateur.unite').selectOption('PROD_INFO')
        await capturer(page, '14-correction-anomalie', { cibles: [d.getByRole('group', { name: 'Validateur', exact: true })] })
        await validerEtape(page, d, 'Enregistrer')
        await outilsProcessus(page).getByRole('button', { name: 'Contrôler' }).click()
        await expect(controle.getByText('Aucune anomalie : le processus peut être publié.')).toBeVisible()
        await capturer(page, '15-controle-conforme', { cibles: [controle] })
      })

      await test.step('16 et 17 : « Publier », puis la version publiée dans le catalogue', async () => {
        await outilsProcessus(page).getByRole('button', { name: 'Publier' }).click()
        const c = page.getByRole('alertdialog', { name: 'Publier le processus' })
        await capturer(page, '16-publier')
        await c.getByRole('button', { name: 'Publier' }).click()
        await expect(page.getByText('Version publiée ou retirée : lecture seule ; créez une nouvelle version pour la modifier.')).toBeVisible()
        await page.getByRole('button', { name: 'Catalogue', exact: true }).click()
        await page.locator('#proc-filtre-unite').selectOption('POLE_RESEAU')
        const ligne = page.getByRole('row').filter({ hasText: 'CHANGEMENT_ROUTEUR' })
        await expect(ligne.getByText('Publiée')).toBeVisible()
        await capturer(page, '17-catalogue-publie', { cibles: [ligne] })
      })

      await test.step('18 : mise à jour d’un processus publié, « Nouvelle version » (brouillon v2)', async () => {
        await page.getByRole('button', { name: 'Changement de configuration d’un routeur', exact: true }).click()
        await outilsProcessus(page).getByRole('button', { name: 'Nouvelle version' }).click()
        await expect(page.getByText('CHANGEMENT_ROUTEUR · v2 · Pôle réseau')).toBeVisible()
        await capturer(page, '18-nouvelle-version', { cibles: [page.getByText('CHANGEMENT_ROUTEUR · v2 · Pôle réseau'), outilsProcessus(page).locator('[name="version"]')] })
      })

      await test.step('19 : modification d’une étape dans la v2', async () => {
        await page.getByRole('button', { name: 'Modifier MISE_EN_OEUVRE' }).click()
        const d = modaleEtape(page, 'Étape MISE_EN_OEUVRE')
        await ongletSelectionne(d, 'Signatures')
        await d.getByRole('button', { name: 'Ajouter une signature' }).click()
        await champ(d, 'signatures.0.code').fill('VISA_CONFIG')
        await champ(d, 'signatures.0.libelle').fill('Visa du chef du pôle sur la configuration')
        await d.getByLabel('CONFIG_APPLIQUEE · Configuration appliquée').check()
        await d.getByLabel('CONFIG_APPLIQUEE · Configuration appliquée').scrollIntoViewIfNeeded()
        await capturer(page, '19-modification-v2', { cibles: [d.getByRole('group', { name: 'Signature 1' })] })
        await validerEtape(page, d, 'Enregistrer')
      })

      await test.step('20 et 21 : publication de la v2 (avertissement levé par motif), versions au catalogue', async () => {
        await outilsProcessus(page).getByRole('button', { name: 'Publier' }).click()
        await page.getByRole('alertdialog', { name: 'Publier le processus' }).getByRole('button', { name: 'Publier' }).click()
        const lever = page.getByRole('alertdialog', { name: 'Lever les avertissements' })
        await lever.getByLabel(/Motif/).fill('Le chef du service Production informatique vise la configuration tant que le pôle réseau n’a pas de chef.')
        await capturer(page, '20-publier-v2-motif')
        await lever.getByRole('button', { name: 'Lever et publier' }).click()
        await expect(page.getByText('Version publiée ou retirée : lecture seule ; créez une nouvelle version pour la modifier.')).toBeVisible()
        await page.getByRole('button', { name: 'Catalogue', exact: true }).click()
        await page.locator('#proc-filtre-unite').selectOption('POLE_RESEAU')
        const lignes = page.getByRole('row').filter({ hasText: 'CHANGEMENT_ROUTEUR' })
        await expect(lignes).toHaveCount(2)
        await expect(lignes.filter({ hasText: 'v1' }).getByText('Retirée')).toBeVisible()
        await capturer(page, '21-versions-catalogue', { cibles: [lignes.first(), lignes.last()] })
      })

      await test.step('22 et 23 : lancement d’une instance (désignation) et suivi', async () => {
        await page.getByRole('button', { name: 'Lancer le processus Changement de configuration d’un routeur' }).click()
        const d = page.getByRole('dialog', { name: 'Lancer le processus' })
        await d.getByLabel(/^Intitulé de l’instance/).fill('Mise à jour des listes d’accès du routeur RTR-CORE-02')
        await d.getByLabel(/^Intervenant de « Mise en œuvre/).selectOption('u-collab')
        await capturer(page, '22-lancement-instance', { cibles: [d.getByLabel(/^Intervenant de « Mise en œuvre/)] })
        await d.getByRole('button', { name: 'Lancer l’instance' }).click()
        await expect(page.getByRole('heading', { level: 1, name: 'Mise à jour des listes d’accès du routeur RTR-CORE-02' })).toBeVisible()
        await page.waitForTimeout(500)
        await capturer(page, '23-suivi-instance', { pleinePage: true })
      })
    })

    const autreUnite = async (page: Page, profil: ProfilDemo) => {
      await demo(page, { profil, theme })
      await page.goto(`${ADRESSES.departement}/des/tableau-de-bord`)
      await naviguer(page, 'Processus de l’unité')
      await expect(page.getByRole('heading', { level: 1, name: 'Processus de l’unité' })).toBeVisible()
      await expect(page.getByText('Aucun processus', { exact: true })).toBeVisible()
    }

    test('Chaque cellule ou service a ses propres processus : vue du chef de la DDRM et d’un membre du pôle système', async ({ page }) => {
      await autreUnite(page, 'ROLE_CHEF_DDRM')
      await capturer(page, '24-autre-service-catalogue', { cibles: [page.getByRole('button', { name: 'Nouveau processus' })] })
      await page.getByRole('button', { name: 'Nouveau processus' }).click()
      const d = page.getByRole('dialog', { name: 'Nouveau processus' })
      await champ(d, 'code').fill('REVUE_PARTENARIAT')
      await champ(d, 'libelle').fill('Revue annuelle d’un partenariat bancaire')
      await champ(d, 'categorie').fill('Marché')
      await d.getByRole('button', { name: 'Créer le brouillon' }).click()
      const sousTitre = page.getByText('REVUE_PARTENARIAT · v1 · Département Développement du Réseau et du Marché')
      await expect(sousTitre).toBeVisible()
      await page.getByRole('button', { name: 'Catalogue', exact: true }).click()
      const groupe = page.getByRole('heading', { level: 2, name: /^Département Développement du Réseau et du Marché/ })
      await expect(groupe).toBeVisible()
      await capturer(page, '25-autre-service-processus', { cibles: [groupe, page.getByRole('row').filter({ hasText: 'REVUE_PARTENARIAT' })] })

      await autreUnite(page, 'ROLE_POLE_SYSTEME')
      await expect(page.getByRole('button', { name: 'Nouveau processus' })).toHaveCount(0)
      await capturer(page, '26-membre-autre-pole', { cibles: [page.getByText('Aucun processus', { exact: true })] })
    })
  })

  test.describe(`Guide B : référentiel d’homologation (${suffixe}) @guide`, () => {
    const outils = (page: Page) => page.getByRole('toolbar', { name: 'Outils du modélisateur' })
    const arbre = (page: Page) => page.getByRole('tree')
    /** Version d'un type de projet dans l'arbre : rang dans le groupe « Paiement mobile ». */
    const noeudPaiementMobile = (page: Page, nom: string) => arbre(page).getByRole('treeitem', { name: /Paiement mobile/ }).getByRole('treeitem', { name: new RegExp(`^${nom}`) })

    test('L’ingénieur d’homologation dérive, modélise, contrôle, compare, publie et exporte une version', async ({ page }) => {
      test.setTimeout(300_000)
      await demo(page, { profil: 'ROLE_INGENIEUR_HOMOLOGATION', theme })
      await page.goto(`${ADRESSES.departement}/des/tableau-de-bord`)

      await test.step('30 : menu « Référentiels d’opérations », versions par type de projet', async () => {
        await naviguer(page, 'Référentiels d\'opérations')
        await expect(page.getByRole('heading', { level: 1, name: 'Référentiels d\'opérations' })).toBeVisible()
        await capturer(page, '30-referentiels-versions', {
          cibles: [page.getByRole('navigation', { name: 'Navigation principale' }).getByRole('link', { name: 'Référentiels d\'opérations', exact: true }), arbre(page)],
        })
      })

      await test.step('31 : version publiée en lecture seule', async () => {
        await noeudPaiementMobile(page, 'Version 2').click()
        await expect(page.getByRole('heading', { name: 'Paiement mobile, version 2' })).toBeVisible()
        await expect(page.getByTestId('modelisateur-canevas')).toBeVisible()
        await capturer(page, '31-version-publiee-lecture', { cibles: [page.getByText('Version figée : lecture seule. Dérivez un brouillon pour la modifier.'), page.getByRole('button', { name: 'Dériver un brouillon' }).first()] })
      })

      await test.step('32 et 33 : « Dériver un brouillon », puis le brouillon ouvert dans le modélisateur', async () => {
        await page.getByRole('button', { name: 'Dériver un brouillon' }).first().click()
        const c = page.getByRole('alertdialog')
        await expect(c.getByText('Paiement mobile, version 2')).toBeVisible()
        await capturer(page, '32-deriver-brouillon')
        await c.getByRole('button', { name: 'Dériver un brouillon' }).click()
        await expect(page.getByRole('heading', { name: 'Paiement mobile, version 2, brouillon' })).toBeVisible()
        await expect(outils(page).getByRole('button', { name: 'Ajouter une opération' })).toBeVisible()
        await fermerToasts(page)
        await capturer(page, '33-brouillon-modelisateur', { cibles: [outils(page)] })
      })

      const etapes = page.getByRole('dialog', { name: 'Étapes de la version' })
      const liste = etapes.getByRole('list', { name: 'Étapes, dans leur ordre' })
      await test.step('34 : bouton « Étapes », la modale des étapes de la version', async () => {
        await outils(page).getByRole('button', { name: 'Étapes' }).click()
        await expect(liste.getByRole('listitem')).toHaveCount(11)
        await capturer(page, '34-modale-etapes')
      })

      await test.step('35 : ajout d’une étape personnalisée (code, libellé, rattachement canonique, couleur)', async () => {
        await etapes.getByRole('button', { name: 'Ajouter une étape' }).click()
        const form = etapes.getByRole('region', { name: 'Nouvelle étape' })
        await form.getByLabel(/^Code/).fill('RECETTE_INTERNE')
        await form.getByLabel(/^Libellé/).fill('Recette interne')
        await form.getByLabel(/^Étape canonique/).selectOption('TESTS')
        await form.getByRole('radio', { name: 'Accent' }).check()
        await form.scrollIntoViewIfNeeded()
        await capturer(page, '35-nouvelle-etape', { cibles: [form] })
        await form.getByRole('button', { name: 'Enregistrer l\'étape' }).click()
        await expect(liste.getByRole('listitem')).toHaveCount(12)
      })

      await test.step('36 : réordonnancement (Monter jusqu’avant la certification)', async () => {
        for (let i = 0; i < 4; i++) {
          await etapes.getByRole('button', { name: 'Monter Recette interne' }).click()
          await expect(page.locator('.gm-mod-sr')).toHaveText(`Recette interne : position ${11 - i} sur 12.`)
        }
        const item = liste.getByRole('listitem').filter({ hasText: 'RECETTE_INTERNE' })
        await item.scrollIntoViewIfNeeded()
        await capturer(page, '36-reordonner-etape', { cibles: [item, etapes.getByRole('button', { name: 'Monter Recette interne' })] })
      })

      await test.step('37 : une étape protégée ne se supprime pas (pas de bouton de retrait, rattachement figé)', async () => {
        await etapes.getByRole('button', { name: 'Modifier Certification' }).click()
        const form = etapes.getByRole('region', { name: 'Modifier l\'étape Certification' })
        await expect(form.getByLabel(/^Étape canonique/)).toBeDisabled()
        const item = liste.getByRole('listitem').filter({ hasText: 'CERTIFICATION' })
        // Défilement de la modale : l'étape protégée (étiquette « Protégée », sans bouton de retrait) en haut, le formulaire dessous.
        await item.evaluate((e) => e.scrollIntoView({ block: 'start' }))
        await capturer(page, '37-etape-protegee', { cibles: [item, form.getByLabel(/^Étape canonique/)] })
        await form.getByRole('button', { name: 'Annuler' }).click()
        await etapes.getByRole('button', { name: 'Fermer' }).last().click()
        await expect(etapes).toHaveCount(0)
      })

      const op = page.getByRole('dialog', { name: 'Nouvelle opération' })
      await test.step('38 à 42 : « Ajouter une opération », un onglet par aspect', async () => {
        await outils(page).getByRole('button', { name: 'Ajouter une opération' }).click()
        await op.getByLabel(/^Code/).fill('R1')
        await op.getByLabel(/^Libellé/).fill('Recette interne du parcours de paiement')
        await op.getByLabel(/^Description/).fill('Rejouer les scénarios de paiement sur la plateforme de recette avant le constat de couverture.')
        await op.getByLabel(/^Étape/).selectOption({ label: 'Recette interne' })
        // Segment choisi par erreur : la branche du type de projet est postérieure au tronc commun amont (relevé au contrôle).
        await op.getByLabel(/^Segment/).selectOption('BRANCHE')
        await capturer(page, '38-operation-general')

        await ongletSelectionne(op, 'Rôles')
        await op.getByLabel(/Responsable \(R\)/).selectOption('SNA')
        await op.getByLabel(/Validateur \(V\)/).selectOption('CONDUCTEUR')
        await choisirMultiple(op.getByRole('combobox', { name: 'Consultés (C)' }), 'Pôle réseau')
        await choisirMultiple(op.getByRole('combobox', { name: 'Informés (I)' }), 'Production monétique')
        await choisirMultiple(op.getByRole('combobox', { name: 'Externes (E)' }), 'Participant')
        await op.getByRole('heading', { name: 'Nouvelle opération' }).click()
        await capturer(page, '39-operation-roles')

        await ongletSelectionne(op, 'Livrables et preuves')
        await op.getByRole('button', { name: 'Ajouter un livrable ou une preuve' }).click()
        const l = op.getByRole('group', { name: 'Livrable 1' })
        await l.getByLabel(/^Libellé/).fill('Procès-verbal de recette interne')
        await l.getByLabel(/^Nature/).selectOption('LIVRABLE')
        await l.getByLabel(/^Déposant/).selectOption('SNA')
        await capturer(page, '40-operation-livrables', { cibles: [l] })

        await ongletSelectionne(op, 'Exécution')
        await op.getByLabel(/^Geste/).selectOption('DEPOSER_LIVRABLE')
        await op.getByLabel(/^Plateforme/).selectOption('PLATEFORME_MOBILE')
        await capturer(page, '41-operation-execution', { cibles: [op.getByLabel(/^Geste/), op.getByLabel(/^Plateforme/)] })

        await ongletSelectionne(op, 'Chronologie')
        await choisirMultiple(op.getByRole('combobox', { name: /Prédécesseurs/ }), '16', /^16\b/)
        await op.getByLabel(/^Délai cible/).fill('2')
        await op.getByRole('heading', { name: 'Nouvelle opération' }).click()
        await capturer(page, '42-operation-chronologie', { cibles: [op.getByRole('tabpanel', { name: 'Chronologie' })] })
        await op.getByRole('button', { name: 'Créer l\'opération' }).click()
        await expect(op).toHaveCount(0)
      })

      await test.step('43 : relier deux opérations sur le canevas (R1 vers 17)', async () => {
        await outils(page).getByRole('button', { name: 'Disposition automatique' }).click()
        await expect(page.getByText('Disposition automatique enregistrée.')).toBeAttached()
        await page.waitForTimeout(600)
        await fermerToasts(page)
        await relierCartes(page, 'modelisateur-canevas', 'R1', '17')
        await expect(page.getByTestId('rf__edge-R1>17')).toBeAttached()
        await page.waitForTimeout(300)
        await capturer(page, '43-relier-operations', { cibles: [page.locator('.react-flow__node[data-id="R1"]'), page.locator('.react-flow__node[data-id="17"]')] })
      })

      await test.step('44 : vue Liste accessible', async () => {
        await outils(page).getByRole('button', { name: 'Liste', exact: true }).click()
        const ligne = page.getByTestId('modelisateur-liste').getByRole('row').filter({ hasText: 'Recette interne du parcours de paiement' })
        await expect(ligne).toBeVisible()
        await ligne.scrollIntoViewIfNeeded()
        await capturer(page, '44-vue-liste', { cibles: [ligne] })
      })

      await test.step('45 et 46 : « Contrôler » relève l’anomalie, puis correction', async () => {
        await outils(page).getByRole('button', { name: 'Contrôler', exact: true }).click()
        const controle = page.getByRole('region', { name: /^Anomalies du graphe/ })
        await expect(controle.getByText('Segment incohérent')).toBeVisible()
        await controle.scrollIntoViewIfNeeded()
        await capturer(page, '45-controle-anomalies', { cibles: [controle] })
        await page.getByRole('button', { name: 'Modifier l\'opération R1' }).click()
        const d = page.getByRole('dialog', { name: 'Opération R1' })
        await d.getByLabel(/^Segment/).selectOption('AMONT')
        await d.getByRole('button', { name: 'Enregistrer' }).click()
        await expect(d).toHaveCount(0)
        await expect(controle.getByText('Aucune anomalie : le graphe est publiable.')).toBeVisible()
        await controle.evaluate((e) => e.scrollIntoView({ block: 'center' }))
        await capturer(page, '46-controle-conforme', { cibles: [controle] })
      })

      await test.step('47 : « Comparer » avec la version active', async () => {
        await outils(page).getByRole('button', { name: 'Comparer' }).click()
        await expect(page.getByRole('tab', { name: 'Comparaison avec la version active', selected: true })).toBeVisible()
        // Présentation unifiée : lignes ajoutées (+) et modifiées (~) lisibles d'un coup d'œil.
        await page.getByText('Unifié', { exact: true }).click()
        await page.waitForTimeout(500)
        await capturer(page, '47-comparer-version-active', { pleinePage: true })
        await page.getByRole('tab', { name: 'Modélisateur' }).click()
      })

      await test.step('48 et 49 : « Publier la version » (impact), puis la nouvelle version active', async () => {
        await outils(page).getByRole('button', { name: 'Publier la version' }).click()
        const c = page.getByRole('alertdialog')
        await expect(c).toBeVisible()
        await page.waitForTimeout(500)
        await capturer(page, '48-publier-version')
        await c.getByRole('button', { name: 'Publier la version' }).click()
        await expect(page.getByText(/lecture seule/).first()).toBeVisible()
        await fermerToasts(page)
        const v3 = noeudPaiementMobile(page, 'Version 3')
        await expect(v3).toBeVisible()
        await capturer(page, '49-nouvelle-version-active', { cibles: [v3, noeudPaiementMobile(page, 'Version 2')] })
      })

      await test.step('50 : export CSV', async () => {
        const telechargement = page.waitForEvent('download')
        await outils(page).getByRole('button', { name: 'Exporter (CSV)' }).click()
        const f = await telechargement
        expect(f.suggestedFilename()).toMatch(/\.csv$/)
        await page.waitForTimeout(300)
        await capturer(page, '50-export-csv', { cibles: [outils(page).getByRole('button', { name: 'Exporter (CSV)' })] })
      })
    })

    test('Une personne non éditrice (pôle réseau) ne voit que les opérations qui la concernent', async ({ page }) => {
      await demo(page, { profil: 'ROLE_POLE_RESEAU', theme })
      await page.goto(`${ADRESSES.departement}/des/tableau-de-bord`)
      await naviguer(page, 'Référentiels d\'opérations')
      await noeudPaiementMobile(page, 'Version 2').click()
      const bandeau = page.getByText('Vous voyez les opérations qui vous concernent')
      await expect(bandeau).toBeVisible()
      await expect(page.locator('.gm-mod-carte-fantome').first()).toBeAttached()
      await expect(outils(page).getByRole('button', { name: 'Ajouter une opération' })).toHaveCount(0)
      await page.waitForTimeout(500)
      await capturer(page, '51-vue-filtree-non-editeur', { pleinePage: true, cibles: [bandeau.locator('xpath=ancestor::*[contains(@class,"gm-alert") or @role="status" or @role="note"][1]')] })
      // Zoom sur une voisine hors périmètre (7) : carte grisée, réduite au code et au libellé, non ouvrable.
      await zoomerSur(page, 'modelisateur-canevas', '7', 140, [0.7, 0.45])
      await page.waitForTimeout(300)
      await capturer(page, '52-fantomes-grises', { cibles: [page.locator('.react-flow__node[data-id="7"]')] })
      await outils(page).getByRole('button', { name: 'Liste', exact: true }).click()
      await expect(page.getByTestId('modelisateur-liste').getByText('Hors périmètre').first()).toBeVisible()
      await capturer(page, '53-liste-filtree-non-editeur', { cibles: [page.getByTestId('modelisateur-liste').getByRole('row').filter({ hasText: 'Hors périmètre' }).first()] })
    })
  })
}
