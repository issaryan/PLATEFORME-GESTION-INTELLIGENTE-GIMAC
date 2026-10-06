import { ADRESSES, expect, test } from '../fixtures'
import { joindre } from './outils'
import { PasserelleProcessus } from './passerelle-processus'

// Fonctionnalité : processus d'unité sur la passerelle (D-87, VITE_SERVICES=http). Chaque geste atteint la route de
// MS-PILOTAGE avec son corps : modification d'un brouillon (PUT), contrôle, publication refusée puis levée motivée,
// lancement (désignations et champs), signature avec l'assertion FIDO2 (X-Reauth-Assertion, finalité
// SIGNATURE_PROCESSUS), réaffectation, suspension et reprise ; l'exécutant prend son étape, dépose le livrable par le
// dépôt fragmenté de MS-DOCUMENT (finalité PROCESSUS_UNITE, référence de l'instance) et la termine. Sans contexte
// sécurisé (http par adresse IP), la signature est refusée avec un message clair.

test.describe('Fonctionnalité : processus d’unité par la passerelle', () => {
  test('Scénario : le chef du service modélise, publie, lance, signe et pilote une instance', async ({ page }) => {
    const gw = new PasserelleProcessus('resp')
    await gw.brancher(page)
    await page.goto(`${ADRESSES.departementHttp}/des/processus`)

    await test.step('Brouillon : l’étape modifiée part entière dans le PUT de la définition', async () => {
      await page.getByRole('button', { name: 'Accès VPN d’un participant' }).first().click()
      await page.getByRole('button', { name: 'Liste' }).click()
      await page.getByRole('button', { name: 'Modifier l’étape CONFIGURATION' }).click()
      const d = page.getByRole('dialog', { name: 'Étape CONFIGURATION' })
      await d.getByRole('tab', { name: 'Intervenant et validateur' }).click()
      await d.getByLabel(/^Profil de remplacement/).first().selectOption('ROLE_POLE_RESEAU')
      await d.getByRole('button', { name: 'Enregistrer' }).click()
      await expect(d).toHaveCount(0)
      const put = gw.dernier('PUT', '/bff/des/pilotage/processus/PRC-0002')!
      const etape = (put.corps as { etapes: { code: string; intervenant: unknown }[] }).etapes.find((e) => e.code === 'CONFIGURATION')!
      expect(etape.intervenant).toEqual({ mode: 'PERSONNE', personneId: 'u-res', remplacement: 'ROLE_POLE_RESEAU' })
      expect(put.corps).toMatchObject({ code: 'ACCES_VPN_PARTICIPANT', unite: 'POLE_RESEAU' })
    })

    await test.step('Contrôle, publication refusée (avertissements) puis levée motivée', async () => {
      await page.getByRole('button', { name: 'Contrôler' }).click()
      await expect(page.getByText('Unité sans chef').first()).toBeVisible()
      expect(gw.dernier('POST', '/bff/des/pilotage/processus/PRC-0002/controle')).toBeTruthy()
      await page.getByRole('button', { name: 'Publier' }).click()
      await page.getByRole('alertdialog', { name: 'Publier le processus' }).getByRole('button', { name: 'Publier' }).click()
      const lever = page.getByRole('alertdialog', { name: 'Lever les avertissements' })
      await lever.getByLabel(/Motif/).fill('Validation par le chef du service en attendant')
      await lever.getByRole('button', { name: 'Lever et publier' }).click()
      await expect(page.getByText('Version publiée ou retirée : lecture seule ; créez une nouvelle version pour la modifier.')).toBeVisible()
      const publications = gw.appels.filter((a) => a.methode === 'POST' && a.chemin === '/bff/des/pilotage/processus/PRC-0002/publication')
      expect(publications.map((a) => a.corps)).toEqual([{}, { motif: 'Validation par le chef du service en attendant', leverAvertissements: true }])
    })

    await test.step('Lancement : intitulé, champs typés et désignation au lancement', async () => {
      await page.goto(`${ADRESSES.departementHttp}/des/processus`)
      await page.getByRole('button', { name: 'Lancer le processus Ouverture d’un flux réseau' }).click()
      const d = page.getByRole('dialog', { name: 'Lancer le processus' })
      await d.getByLabel(/^Intitulé de l’instance/).fill('Flux SNMP vers la supervision')
      await d.getByLabel(/^Adresse source/).fill('10.20.4.21')
      await d.getByLabel(/^Adresse de destination/).fill('172.16.8.12')
      await d.getByLabel(/^Port/).fill('161')
      await d.getByLabel(/^Protocole/).selectOption('UDP')
      await d.getByLabel(/^Date souhaitée/).fill('15/10/2026')
      await d.getByLabel(/^Intervenant de « Mise en œuvre/).selectOption('u-collab')
      await d.getByRole('button', { name: 'Lancer l’instance' }).click()
      await expect(page.getByRole('heading', { level: 1, name: 'Flux SNMP vers la supervision' })).toBeVisible()
      expect(gw.dernier('POST', '/bff/des/pilotage/processus/PRC-0001/instances')!.corps).toEqual({
        intitule: 'Flux SNMP vers la supervision', champs: { SOURCE: '10.20.4.21', DESTINATION: '172.16.8.12', PORT: 161, PROTOCOLE: 'UDP', DATE_SOUHAITEE: '2026-10-15' },
        designations: { MISE_EN_OEUVRE: 'u-collab' },
      })
    })

    await test.step('Signature de la recette : défi SIGNATURE_PROCESSUS puis assertion dans l’en-tête', async () => {
      await page.goto(`${ADRESSES.departementHttp}/des/processus/instances/INS-0003`)
      const panneau = page.getByRole('complementary', { name: /Recette du flux/ })
      await panneau.getByRole('button', { name: 'Signer' }).click()
      const d = page.getByRole('dialog', { name: 'Signer : Signature du procès-verbal' })
      await d.getByRole('button', { name: 'Utiliser ma clé de sécurité' }).click()
      await expect(d.getByText('Signature confirmée')).toBeVisible()
      await d.getByRole('button', { name: 'Continuer' }).click()
      expect(gw.dernier('POST', '/bff/des/iam/reauthentification/defi')!.corps).toEqual({ purpose: 'SIGNATURE_PROCESSUS' })
      const s = gw.dernier('POST', '/bff/des/pilotage/processus/instances/INS-0003/etapes/RECETTE/signature')!
      expect(s.corps).toEqual({ signature: 'SIGNATURE_RECETTE' })
      expect(s.entetes['x-reauth-assertion']).toBeTruthy()
      await expect(page.getByText('Close', { exact: true }).first()).toBeVisible()
    })

    await test.step('Réaffectation motivée, suspension et reprise de l’autre instance', async () => {
      await page.goto(`${ADRESSES.departementHttp}/des/processus/instances/INS-0007?etape=ANALYSE`)
      await page.getByRole('button', { name: 'Réaffecter l’étape' }).click()
      const d = page.getByRole('dialog', { name: 'Réaffecter l’étape' })
      await d.getByLabel(/^Nouvel exécutant/).selectOption('u-res')
      await d.getByLabel(/Motif/).fill('Idriss assure l’analyse cette semaine')
      await d.getByRole('button', { name: 'Réaffecter l’étape' }).click()
      await expect(page.getByText(/Étape réaffectée/).first()).toBeVisible()
      expect(gw.dernier('POST', '/bff/des/pilotage/processus/instances/INS-0007/etapes/ANALYSE/reaffectation')!.corps).toEqual({ personneId: 'u-res', motif: 'Idriss assure l’analyse cette semaine' })
      await page.getByRole('button', { name: 'Suspendre' }).click()
      const s = page.getByRole('alertdialog', { name: 'Suspendre' })
      await s.getByLabel(/Motif/).fill('Gel des changements de fin de mois')
      await s.getByRole('button', { name: 'Suspendre' }).click()
      await expect(page.getByText(/Instance suspendue : aucun acte/)).toBeVisible()
      await page.getByRole('button', { name: 'Reprendre' }).click()
      await page.getByRole('alertdialog', { name: 'Reprendre' }).getByRole('button', { name: 'Reprendre' }).click()
      await expect(page.getByText(/Instance suspendue : aucun acte/)).toHaveCount(0)
      expect(gw.dernier('POST', '/bff/des/pilotage/processus/instances/INS-0007/suspension')!.corps).toEqual({ motif: 'Gel des changements de fin de mois' })
      expect(gw.dernier('POST', '/bff/des/pilotage/processus/instances/INS-0007/reprise')!.corps).toEqual({})
    })
  })

  test('Scénario : un membre du pôle prend l’étape, dépose son livrable par morceaux et la termine', async ({ page }) => {
    const gw = new PasserelleProcessus('membre')
    await gw.brancher(page)
    await page.goto(`${ADRESSES.departementHttp}/des/processus/instances/INS-0007?etape=ANALYSE`)
    await page.getByRole('button', { name: 'Prendre l’étape' }).click()
    await expect(page.getByRole('button', { name: 'Terminer l’étape' })).toBeVisible()
    expect(gw.appels.some((a) => a.methode === 'POST' && /^\/bff\/des\/pilotage\/taches\/[^/]+\/prise$/.test(a.chemin))).toBe(true)
    const panneau = page.getByRole('complementary', { name: /Analyse de sécurité du flux/ })
    await joindre(panneau, 'note-analyse.pdf')
    await panneau.getByRole('button', { name: 'Envoyer NOTE_ANALYSE' }).click()
    await expect(page.getByRole('status').filter({ hasText: 'Livrable note-analyse.pdf déposé.' })).toBeVisible()
    expect(gw.dernier('POST', '/bff/des/document/depots')!.corps).toMatchObject({ nomFichier: 'note-analyse.pdf', typeMime: 'application/pdf', finalite: 'PROCESSUS_UNITE', reference: 'INS-0007' })
    expect(gw.appels.some((a) => a.methode === 'PUT' && a.chemin === '/bff/des/document/depots/DEP-1/parties/1')).toBe(true)
    expect(gw.dernier('POST', '/bff/des/pilotage/processus/instances/INS-0007/etapes/ANALYSE/livrables')!.corps).toEqual({ livrable: 'NOTE_ANALYSE', documentId: 'DOC-E2E-1' })
    await page.getByRole('button', { name: 'Terminer l’étape' }).click()
    await page.getByRole('alertdialog', { name: 'Terminer l’étape' }).getByRole('button', { name: 'Terminer l’étape' }).click()
    await expect(panneau.getByText('À valider', { exact: true }).first()).toBeVisible()
    expect(gw.dernier('POST', '/bff/des/pilotage/processus/instances/INS-0007/etapes/ANALYSE/fin')).toBeTruthy()
  })

  test('Scénario : sans connexion sécurisée, la signature est impossible et l’écran le dit', async ({ page }) => {
    const gw = new PasserelleProcessus('resp')
    await gw.brancher(page)
    await page.addInitScript(() => Object.defineProperty(window, 'isSecureContext', { configurable: true, get: () => false }))
    await page.goto(`${ADRESSES.departementHttp}/des/processus/instances/INS-0003`)
    const panneau = page.getByRole('complementary', { name: /Recette du flux/ })
    await expect(panneau.getByText('La signature exige une connexion sécurisée (HTTPS)')).toBeVisible()
    await expect(panneau.getByRole('button', { name: 'Signer' })).toBeDisabled()
    expect(gw.appels.some((a) => a.chemin.endsWith('/signature'))).toBe(false)
  })
})
