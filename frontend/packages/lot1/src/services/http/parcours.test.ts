import { afterEach, describe, expect, it, vi } from 'vitest'
import { decisionHttp, parcoursHttp, portailParcoursHttp, versResume, type LiaisonVue } from './parcours'

// Contrat HTTP du parcours : routes réelles de l'étape 3 (LiaisonsCadrageResource), routes prévues des
// étapes 4 à 10 (docs/lot1/correspondance.md), pagination, dépôt documentaire et réauthentification.
const reponses: unknown[] = []
const appels = () => (globalThis.fetch as unknown as { mock: { calls: [URL, RequestInit][] } }).mock.calls
afterEach(() => {
  vi.restoreAllMocks()
  reponses.length = 0
})
const brancher = (...r: unknown[]) => {
  reponses.push(...r)
  vi.spyOn(globalThis, 'fetch').mockImplementation(async () => new Response(JSON.stringify(reponses.shift() ?? {}), { status: 200, headers: { 'Content-Type': 'application/json' } }))
}
const url = (i: number) => new URL(String(appels()[i][0]))
const route = (i: number) => `${appels()[i][1].method ?? 'GET'} ${url(i).pathname}${url(i).search}`
const corps = (i: number) => JSON.parse(String(appels()[i][1].body ?? 'null'))
const entete = (i: number, nom: string) => new Headers(appels()[i][1].headers).get(nom)
const fichier = new File(['x'], 'preuve.pdf', { type: 'application/pdf' })

const couverture = { taux: null, numerateur: 0, denominateur: 0, calculable: false, motifNonCalculable: 'Aucune opération instanciée' }
const vue = (etat: LiaisonVue['etat'], extra: Partial<LiaisonVue> = {}): LiaisonVue => ({
  id: '6f1c2a90-0000-4000-8000-000000000001', typeProjet: 'CARTE_INTERBANCAIRE', institutionNom: 'Banque B', etat, chefDeProjetNom: 'Conducteur 1',
  dateOuverture: '2026-09-29', phaseCourante: 'CADRAGE', couvertureCertification: couverture, couvertureMiseEnService: couverture, operationsEnRetard: 2, ...extra,
})

describe('versResume', () => {
  it('traduit l’automate technique en statut et étape d’affichage, sans règle', () => {
    expect(versResume(vue('OUVERTE'))).toMatchObject({ statut: 'CADREE', etapeCourante: 'CADRAGE', reference: '6F1C2A90', bloquee: false, tachesEnRetard: 2, conducteurNom: 'Conducteur 1' })
    expect(versResume(vue('GELEE'))).toMatchObject({ statut: 'SUSPENDUE', bloquee: true })
    expect(versResume(vue('APTE_CERTIFICATION')).etapeCourante).toBe('CERTIFICATION')
    expect(versResume(vue('CERTIFIEE')).etapeCourante).toBe('APPROBATION_SECURITE')
    expect(versResume(vue('MISE_EN_SERVICE_AUTORISEE')).statut).toBe('APPROUVEE_SECURITE')
    expect(versResume(vue('CLOSE')).statut).toBe('EN_PRODUCTION')
    expect(versResume(vue('EN_HOMOLOGATION', { id: 'LIA-1' })).reference).toBe('LIA-1')
  })
  it('préfère les champs du modèle hybride quand le service les sert', () => {
    const r = versResume(vue('EN_HOMOLOGATION', { statut: 'EN_TEST', etapeCourante: 'TESTS', bloquee: true, prochaineEcheance: '2026-10-02', dateCibleProduction: '2026-12-01' }))
    expect(r).toMatchObject({ statut: 'EN_TEST', etapeCourante: 'TESTS', bloquee: true, prochaineEcheance: '2026-10-02', dateCibleProduction: '2026-12-01' })
  })
})

describe('parcoursHttp : étape 3 (routes réelles)', () => {
  it('liste : pagination à partir de 0, filtres, mes projets ; conversion des vues', async () => {
    brancher({ contenu: [vue('OUVERTE')], total: 1 })
    const p = await parcoursHttp.listerLiaisons({ page: 3, taille: 20, recherche: 'banque', typeProjet: 'DELEGATAIRE', mesProjets: true, bloquees: true, statut: 'CADREE', etape: 'CADRAGE' })
    expect(url(0).pathname).toBe('/bff/des/homologation/liaisons')
    expect(Object.fromEntries(url(0).searchParams)).toEqual({ page: '2', taille: '20', recherche: 'banque', typeProjet: 'DELEGATAIRE', mesProjets: 'true', bloquees: 'true', statut: 'CADREE', etape: 'CADRAGE' })
    expect(p).toMatchObject({ total: 1, contenu: [{ statut: 'CADREE' }] })
  })
  it('liste sans filtre : aucun paramètre vide', async () => {
    brancher({ contenu: [], total: 0 })
    await parcoursHttp.listerLiaisons({ page: 1, taille: 25 })
    expect(Object.fromEntries(url(0).searchParams)).toEqual({ page: '0', taille: '25' })
  })
  it('conducteurs, ouverture et désignation', async () => {
    brancher([{ id: 'u1', nom: 'Conducteur 1' }], vue('OUVERTE'), {})
    expect(await parcoursHttp.conducteurs()).toEqual([{ id: 'u1', nom: 'Conducteur 1', fonction: 'CONDUCTEUR' }])
    const l = await parcoursHttp.ouvrirLiaison({ institutionId: 'i1', typeProjet: 'CARTE_INTERBANCAIRE', chefDeProjetId: 'u1', banquePartenaireId: null })
    await parcoursHttp.designerConducteur('l1', 'u2', 'relève')
    expect([0, 1, 2].map(route)).toEqual(['GET /bff/des/homologation/liaisons/conducteurs', 'POST /bff/des/homologation/liaisons', 'PUT /bff/des/homologation/liaisons/l1/chef-de-projet'])
    expect(corps(1)).toEqual({ institutionId: 'i1', typeProjet: 'CARTE_INTERBANCAIRE', chefDeProjetId: 'u1', banquePartenaireId: null })
    expect(corps(2)).toEqual({ chefDeProjetId: 'u2', motif: 'relève' })
    expect(l.statut).toBe('CADREE')
  })
})

describe('parcoursHttp : étapes 4 à 10 (routes prévues)', () => {
  it('lectures du dossier, des opérations, des tableaux de bord et des intervenants', async () => {
    brancher({}, [], {}, {}, [])
    await parcoursHttp.dossier('l1')
    await parcoursHttp.operations('l1', { etape: 'TESTS', fonction: 'SNA' })
    await parcoursHttp.tableauActeur(['SNA', 'RSSI'])
    await parcoursHttp.pilotage()
    await parcoursHttp.personnes('RSSI')
    expect([0, 1, 2, 3, 4].map(route)).toEqual([
      'GET /bff/des/homologation/liaisons/l1/dossier',
      'GET /bff/des/homologation/liaisons/l1/parcours/operations?etape=TESTS&fonction=SNA',
      'GET /bff/des/homologation/tableaux-de-bord/acteur?fonctions=SNA%2CRSSI',
      'GET /bff/des/homologation/tableaux-de-bord/pilotage',
      'GET /bff/des/homologation/intervenants?fonction=RSSI',
    ])
  })
  it('dépôts : document WORM puis rattachement par référence et empreinte', async () => {
    brancher({ id: 'd1', sha256: 'x' }, {}, { id: 'd2', sha256: 'y' }, {})
    await parcoursHttp.deposerLivrable('l1:7', fichier, 'relu')
    await parcoursHttp.deposerPreuve('l1:C1', fichier, { codeControle: 'A1B2C3' })
    expect([0, 1, 2, 3].map(route)).toEqual([
      'POST /bff/des/document/documents', 'POST /bff/des/homologation/parcours/operations/l1:7/livrable',
      'POST /bff/des/document/documents', 'POST /bff/des/homologation/parcours/operations/l1:C1/preuves',
    ])
    expect(corps(1)).toMatchObject({ documentId: 'd1', nom: 'preuve.pdf', commentaire: 'relu', empreinte: expect.stringMatching(/^[0-9a-f]{64}$/) })
    expect(corps(3)).toMatchObject({ documentId: 'd2', codeControle: 'A1B2C3' })
  })
  it('conduite : ressources, qualités, validation FIDO2, blocages, couverture, service supplémentaire', async () => {
    brancher({}, {}, {}, {}, {}, {}, vue('OUVERTE'))
    await parcoursHttp.affecterRessources('l1', [{ fonction: 'SNA', personneId: 'u1' }])
    await parcoursHttp.attribuer('l1:3', 'V', 'u2', 'motif')
    await parcoursHttp.validerOperation('l1:3', 'YXNzZXJ0')
    await parcoursHttp.declarerBlocage('l1', { motif: 'm', prochaineAction: 'p', responsableId: 'u1', echeanceLevee: '2026-10-01' })
    await parcoursHttp.leverBlocage('b1', 'levé')
    await parcoursHttp.constaterCouverture('l1')
    await parcoursHttp.ouvrirServiceSupplementaire('l1', ['RETRAIT'])
    expect([0, 1, 2, 3, 4, 5, 6].map(route)).toEqual([
      'PUT /bff/des/homologation/liaisons/l1/ressources', 'PUT /bff/des/homologation/parcours/operations/l1:3/intervenants',
      'POST /bff/des/homologation/parcours/operations/l1:3/validation', 'POST /bff/des/homologation/liaisons/l1/blocages',
      'POST /bff/des/homologation/blocages/b1/levee', 'POST /bff/des/homologation/liaisons/l1/couverture/constat',
      'POST /bff/des/homologation/liaisons/l1/services-supplementaires',
    ])
    expect(entete(2, 'X-Reauth-Assertion')).toBe('YXNzZXJ0')
    expect(corps(1)).toEqual({ qualite: 'V', personneId: 'u2', motif: 'motif' })
    expect(corps(6)).toEqual({ services: ['RETRAIT'] })
  })
})

describe('decisionHttp : verrous et mise en service', () => {
  it('procès-verbal, approbation de la sécurité, visas, fiche de suivi', async () => {
    brancher({}, {}, {}, {}, { id: 'd', sha256: 's' }, {}, {}, {}, {}, {}, {}, {}, {}, {})
    await decisionHttp.procesVerbal('l1')
    await decisionHttp.etablirProcesVerbal('l1')
    await decisionHttp.signerProcesVerbalGimac('l1', 'A1')
    await decisionHttp.approbation('l1')
    await decisionHttp.constituerDossierChangement('l1', fichier)
    await decisionHttp.referencerFicheChangement('l1', 'CHG-2026-0412')
    await decisionHttp.relireFicheChangement('l1')
    await decisionHttp.verifierAttestation('l1', 'PCI_DSS', false, 'périmée')
    await decisionHttp.archiverAttestations('l1')
    await decisionHttp.signerVisa('l1', 'DIRECTION_GENERALE', true, 'favorable', 'A2')
    await decisionHttp.ficheSuivi('l1')
    await decisionHttp.remettreFicheSuivi('l1')
    await decisionHttp.constaterMiseEnService('l1')
    const C = '/bff/des/certification/liaisons/l1'
    const H = '/bff/des/homologation/liaisons/l1'
    expect(Array.from({ length: 14 }, (_, i) => route(i))).toEqual([
      `GET ${C}/proces-verbal`, `POST ${C}/proces-verbal`, `POST ${C}/proces-verbal/signature-gimac`, `GET ${C}/approbation-securite`,
      'POST /bff/des/document/documents', `POST ${C}/approbation-securite/dossier`, `PUT ${C}/approbation-securite/fiche`,
      `POST ${C}/approbation-securite/fiche/lecture`, `POST ${C}/approbation-securite/attestations/PCI_DSS/verification`,
      `POST ${C}/approbation-securite/archivage`, `POST ${C}/approbation-securite/visas/DIRECTION_GENERALE`,
      `GET ${H}/fiche-suivi`, `POST ${H}/fiche-suivi/remise`, `POST ${H}/fiche-suivi/constat`,
    ])
    expect(entete(2, 'X-Reauth-Assertion')).toBe('A1')
    expect(entete(10, 'X-Reauth-Assertion')).toBe('A2')
    expect(corps(8)).toEqual({ conforme: false, motif: 'périmée' })
    expect(corps(10)).toEqual({ favorable: true, motif: 'favorable' })
  })
})

describe('portailParcoursHttp', () => {
  it('routes du portail, cloisonnées par le jeton ; dépôts en multipart', async () => {
    brancher([vue('EN_HOMOLOGATION')], {}, {}, {}, {}, {}, {}, {}, {}, {}, {})
    expect((await portailParcoursHttp.mesLiaisons())[0].statut).toBe('EN_INTEGRATION')
    await portailParcoursHttp.dossier('l1')
    await portailParcoursHttp.mesTaches()
    await portailParcoursHttp.communiquerCoordonnees('l1', { adresse: 'essai.exemple', port: '8443' })
    await portailParcoursHttp.deposerLivrable('l1:9', fichier, 'cr')
    await portailParcoursHttp.procesVerbal('l1')
    await portailParcoursHttp.signerProcesVerbal('l1', 'A3')
    await portailParcoursHttp.approbation('l1')
    await portailParcoursHttp.deposerAttestation('l1', 'ISO_27001', fichier)
    await portailParcoursHttp.ficheSuivi('l1')
    await portailParcoursHttp.renseignerFicheSuivi('l1', [{ serviceCode: 'RETRAIT', ouvert: true, commentaire: null }])
    const H = '/bff/portail/homologation'
    const C = '/bff/portail/certification/liaisons/l1'
    expect(Array.from({ length: 11 }, (_, i) => route(i))).toEqual([
      `GET ${H}/liaisons`, `GET ${H}/liaisons/l1/dossier`, `GET ${H}/taches`, `PUT ${H}/liaisons/l1/coordonnees-techniques`,
      `POST ${H}/operations/l1:9/livrable`, `GET ${C}/proces-verbal`, `POST ${C}/proces-verbal/signature`, `GET ${C}/approbation-securite`,
      `POST ${C}/attestations/ISO_27001`, `GET ${H}/liaisons/l1/fiche-suivi`, `PUT ${H}/liaisons/l1/fiche-suivi`,
    ])
    expect(appels()[4][1].body).toBeInstanceOf(FormData)
    expect(entete(6, 'X-Reauth-Assertion')).toBe('A3')
    expect(corps(10)).toEqual({ lignes: [{ serviceCode: 'RETRAIT', ouvert: true, commentaire: null }] })
  })
})
