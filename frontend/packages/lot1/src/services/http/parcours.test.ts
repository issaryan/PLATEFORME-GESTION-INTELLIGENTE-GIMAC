import { afterEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '@gimac/socle'
import { decisionHttp, parcoursHttp, portailParcoursHttp, versResume, type LiaisonVue } from './parcours'

// Contrats HTTP du parcours (étapes 3 à 10, D-56 à D-59) : chemins de la passerelle, méthodes, paramètres,
// corps, réauthentification, dépôt en deux temps (MS-DOCUMENT puis rattachement) et erreurs normalisées.
const reponses: unknown[] = []
type Appel = [URL, RequestInit]
const appels = () => (globalThis.fetch as unknown as { mock: { calls: Appel[] } }).mock.calls
afterEach(() => {
  vi.restoreAllMocks()
  reponses.length = 0
})
function brancher(...r: unknown[]) {
  reponses.push(...r)
  vi.spyOn(globalThis, 'fetch').mockImplementation(async () =>
    new Response(JSON.stringify(reponses.length ? reponses.shift() : {}), { status: 200, headers: { 'Content-Type': 'application/json' } }))
}
const ligne = (i: number) => {
  const [u, o] = appels()[i]
  const url = new URL(String(u))
  return `${o.method} ${url.pathname}${url.search}`
}
const lignes = () => appels().map((_, i) => ligne(i))
const corps = (i: number) => {
  const b = appels()[i][1].body
  return b instanceof FormData ? Object.fromEntries([...b.entries()].map(([k, v]) => [k, typeof v === 'string' ? v : `fichier:${(v as File).name}`])) : JSON.parse(String(b ?? 'null'))
}
const assertion = (i: number) => new Headers(appels()[i][1].headers).get('X-Reauth-Assertion')
const fichier = (nom = 'cr.pdf') => new File(['abc'], nom, { type: 'application/pdf' })
const SHA_ABC = 'ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad'

const vue = (o: Partial<LiaisonVue> = {}): LiaisonVue => ({
  id: 'LIA-1', typeProjet: 'CARTE_INTERBANCAIRE', institutionNom: 'Banque B', etat: 'OUVERTE', chefDeProjetNom: 'Conducteur 1', dateOuverture: '2026-09-01',
  phaseCourante: 'CADRAGE', couvertureCertification: { taux: 0, numerateur: 0, denominateur: 4, calculable: true },
  couvertureMiseEnService: { taux: 0, numerateur: 0, denominateur: 4, calculable: true }, operationsEnRetard: 2, ...o,
})

describe('versResume : vue de MS-HOMOLOGATION vers le résumé du parcours', () => {
  it('traduit l’automate technique en statut et étape du CR tant que le service ne les sert pas', () => {
    const cas: [LiaisonVue['etat'], string, string][] = [
      ['OUVERTE', 'CADREE', 'CADRAGE'], ['EN_HOMOLOGATION', 'EN_INTEGRATION', 'INTEGRATION'], ['GELEE', 'SUSPENDUE', 'INTEGRATION'],
      ['APTE_CERTIFICATION', 'EN_TEST', 'CERTIFICATION'], ['CERTIFIEE', 'CERTIFIEE', 'APPROBATION_SECURITE'],
      ['MISE_EN_SERVICE_AUTORISEE', 'APPROUVEE_SECURITE', 'MISE_EN_PRODUCTION'], ['CLOSE', 'EN_PRODUCTION', 'SUIVI'],
    ]
    cas.forEach(([etat, statut, etape]) => expect(versResume(vue({ etat }))).toMatchObject({ statut, etapeCourante: etape, bloquee: etat === 'GELEE' }))
    // État inconnu : affichage de repli sur le cadrage, jamais d'exception.
    expect(versResume(vue({ etat: 'INCONNU' as LiaisonVue['etat'] }))).toMatchObject({ statut: 'CADREE', etapeCourante: 'CADRAGE' })
  })
  it('les champs du modèle hybride servis par le service priment sur la correspondance', () => {
    const r = versResume(vue({ etat: 'GELEE', statut: 'EN_TEST', etapeCourante: 'TESTS', bloquee: false, prochaineEcheance: '2026-10-01', dateCibleProduction: '2026-12-01' }))
    expect(r).toEqual({
      id: 'LIA-1', reference: 'LIA-1', institutionNom: 'Banque B', typeProjet: 'CARTE_INTERBANCAIRE', statut: 'EN_TEST', etapeCourante: 'TESTS',
      conducteurNom: 'Conducteur 1', couvertureCertification: vue().couvertureCertification, couvertureMiseEnService: vue().couvertureMiseEnService,
      bloquee: false, tachesEnRetard: 2, prochaineEcheance: '2026-10-01', dateCibleProduction: '2026-12-01',
    })
    expect(versResume(vue())).toMatchObject({ prochaineEcheance: null, dateCibleProduction: null })
  })
  it('référence courte : un identifiant long (UUID) est abrégé à huit caractères en majuscules', () => {
    expect(versResume(vue({ id: '3f2a9c1e-0000-4000-8000-000000000001' })).reference).toBe('3F2A9C1E')
    expect(versResume(vue({ id: 'LIA-2026-041' })).reference).toBe('LIA-2026-041')
  })
})

describe('parcoursHttp : étape 3 (routes réelles de MS-HOMOLOGATION)', () => {
  it('liste : pages à partir de 0, filtres vides omis, résumés traduits', async () => {
    brancher({ contenu: [vue({ etat: 'EN_HOMOLOGATION' })], total: 7 }, { contenu: [], total: 0 })
    const p = await parcoursHttp.listerLiaisons({ page: 3, taille: 25, recherche: 'banque', statut: 'EN_TEST', typeProjet: 'DELEGATAIRE', etape: 'TESTS', mesProjets: true, bloquees: true })
    expect(p).toEqual({ contenu: [expect.objectContaining({ id: 'LIA-1', statut: 'EN_INTEGRATION' })], total: 7 })
    await parcoursHttp.listerLiaisons({ page: 0, taille: 10, recherche: '', statut: '', mesProjets: false, bloquees: false })
    expect(lignes()).toEqual([
      'GET /bff/des/homologation/liaisons?page=2&taille=25&recherche=banque&typeProjet=DELEGATAIRE&mesProjets=true&statut=EN_TEST&etape=TESTS&bloquees=true',
      'GET /bff/des/homologation/liaisons?page=0&taille=10',
    ])
  })
  it('conducteurs : la fonction CONDUCTEUR est ajoutée à chaque personne', async () => {
    brancher([{ id: 'U-1', nom: 'A' }, { id: 'U-2', nom: 'B' }])
    expect(await parcoursHttp.conducteurs()).toEqual([{ id: 'U-1', nom: 'A', fonction: 'CONDUCTEUR' }, { id: 'U-2', nom: 'B', fonction: 'CONDUCTEUR' }])
    expect(ligne(0)).toBe('GET /bff/des/homologation/liaisons/conducteurs')
  })
  it('ouverture (résumé traduit) et désignation motivée du conducteur', async () => {
    brancher(vue(), {})
    const o = { institutionId: 'INST-1', typeProjet: 'PAIEMENT_MOBILE' as const, chefDeProjetId: 'U-1', banquePartenaireId: 'INST-2' }
    expect(await parcoursHttp.ouvrirLiaison(o)).toMatchObject({ id: 'LIA-1', statut: 'CADREE' })
    await parcoursHttp.designerConducteur('LIA-1', 'U-2', 'Réorganisation')
    expect(lignes()).toEqual(['POST /bff/des/homologation/liaisons', 'PUT /bff/des/homologation/liaisons/LIA-1/chef-de-projet'])
    expect(corps(0)).toEqual(o)
    expect(corps(1)).toEqual({ chefDeProjetId: 'U-2', motif: 'Réorganisation' })
  })
})

describe('parcoursHttp : étapes 4 à 10 (routes de la correspondance)', () => {
  it('lectures : dossier, opérations filtrées, tableaux de bord et intervenants', async () => {
    brancher({ id: 'LIA-1' }, [], [], {}, {}, [])
    expect(await parcoursHttp.dossier('LIA-1')).toEqual({ id: 'LIA-1' })
    await parcoursHttp.operations('LIA-1', { etape: 'TESTS', fonction: 'SNA' })
    await parcoursHttp.operations('LIA-1')
    await parcoursHttp.tableauActeur(['SNA', 'RSSI'])
    await parcoursHttp.pilotage()
    await parcoursHttp.personnes('POLE_RESEAU')
    expect(lignes()).toEqual([
      'GET /bff/des/homologation/liaisons/LIA-1/dossier',
      'GET /bff/des/homologation/liaisons/LIA-1/parcours/operations?etape=TESTS&fonction=SNA',
      'GET /bff/des/homologation/liaisons/LIA-1/parcours/operations',
      'GET /bff/des/homologation/tableaux-de-bord/acteur?fonctions=SNA%2CRSSI',
      'GET /bff/des/homologation/tableaux-de-bord/pilotage',
      'GET /bff/des/homologation/intervenants?fonction=POLE_RESEAU',
    ])
  })
  it('conduite : ressources, attribution d’une qualité, validation réauthentifiée, blocages, constat', async () => {
    brancher({}, {}, {}, {}, { id: 'BLQ-1' }, {}, {})
    await parcoursHttp.affecterRessources('LIA-1', [{ fonction: 'SNA', personneId: 'U-SNA-1' }])
    await parcoursHttp.attribuer('LIA-1:10', 'V', 'U-2', 'Absence')
    await parcoursHttp.validerOperation('LIA-1:10', 'assertion-fido2')
    await parcoursHttp.validerOperation('LIA-1:11')
    const b = { motif: 'Clé', prochaineAction: 'Relancer', responsableId: 'U-1', echeanceLevee: '2026-10-10' }
    expect(await parcoursHttp.declarerBlocage('LIA-1', b)).toEqual({ id: 'BLQ-1' })
    await parcoursHttp.leverBlocage('BLQ-1', 'Clé reçue')
    await parcoursHttp.constaterCouverture('LIA-1')
    expect(lignes()).toEqual([
      'PUT /bff/des/homologation/liaisons/LIA-1/ressources',
      'PUT /bff/des/homologation/parcours/operations/LIA-1:10/intervenants',
      'POST /bff/des/homologation/parcours/operations/LIA-1:10/validation',
      'POST /bff/des/homologation/parcours/operations/LIA-1:11/validation',
      'POST /bff/des/homologation/liaisons/LIA-1/blocages',
      'POST /bff/des/homologation/blocages/BLQ-1/levee',
      'POST /bff/des/homologation/liaisons/LIA-1/couverture/constat',
    ])
    expect(corps(0)).toEqual({ affectations: [{ fonction: 'SNA', personneId: 'U-SNA-1' }] })
    expect(corps(1)).toEqual({ qualite: 'V', personneId: 'U-2', motif: 'Absence' })
    expect(corps(2)).toEqual({})
    expect(assertion(2)).toBe('assertion-fido2')
    expect(assertion(3)).toBeNull()
    expect(corps(4)).toEqual(b)
    expect(corps(5)).toEqual({ commentaire: 'Clé reçue' })
    expect(corps(6)).toEqual({})
  })
  it('livrable et preuve : dépôt WORM dans MS-DOCUMENT puis rattachement par référence et empreinte', async () => {
    brancher({ id: 'DOC-1', sha256: 'x' }, {}, { id: 'DOC-2', sha256: 'y' }, {})
    await parcoursHttp.deposerLivrable('LIA-1:7', fichier('cr-lancement.pdf'), 'Réunion tenue')
    await parcoursHttp.deposerPreuve('LIA-1:C1', fichier('ceremonie.pdf'), { codeControle: 'A1B2C3' })
    expect(lignes()).toEqual([
      'POST /bff/des/document/documents',
      'POST /bff/des/homologation/parcours/operations/LIA-1:7/livrable',
      'POST /bff/des/document/documents',
      'POST /bff/des/homologation/parcours/operations/LIA-1:C1/preuves',
    ])
    expect(corps(0)).toEqual({ fichier: 'fichier:cr-lancement.pdf', finalite: 'LIVRABLE', reference: 'LIA-1:7' })
    expect(corps(1)).toEqual({ documentId: 'DOC-1', empreinte: SHA_ABC, nom: 'cr-lancement.pdf', commentaire: 'Réunion tenue' })
    expect(corps(2)).toMatchObject({ finalite: 'PREUVE', reference: 'LIA-1:C1' })
    expect(corps(3)).toEqual({ documentId: 'DOC-2', empreinte: SHA_ABC, nom: 'ceremonie.pdf', codeControle: 'A1B2C3' })
  })
  it('service supplémentaire (rang 35) : nouvelle liaison rendue en résumé', async () => {
    brancher(vue({ id: 'LIA-2', etat: 'OUVERTE' }))
    expect(await parcoursHttp.ouvrirServiceSupplementaire('LIA-1', ['COMMERCE_ELECTRONIQUE'])).toMatchObject({ id: 'LIA-2', statut: 'CADREE' })
    expect(ligne(0)).toBe('POST /bff/des/homologation/liaisons/LIA-1/services-supplementaires')
    expect(corps(0)).toEqual({ services: ['COMMERCE_ELECTRONIQUE'] })
  })
  it('un refus du service (ProblemDetail) remonte en ApiError, sans rattachement après un dépôt refusé', async () => {
    vi.spyOn(globalThis, 'fetch').mockImplementation(async () => new Response(JSON.stringify({ code: 'HOM_OPERATION_NOT_OPEN', message: 'Opération fermée', correlationId: 'c-1' }), { status: 409, headers: { 'Content-Type': 'application/problem+json' } }))
    const e = await parcoursHttp.validerOperation('LIA-1:10', 'a').catch((x: unknown) => x)
    expect(e).toBeInstanceOf(ApiError)
    expect(e).toMatchObject({ status: 409, code: 'HOM_OPERATION_NOT_OPEN', message: 'Opération fermée', correlationId: 'c-1' })
    await expect(parcoursHttp.deposerPreuve('LIA-1:10', fichier(), {})).rejects.toMatchObject({ code: 'HOM_OPERATION_NOT_OPEN' })
    expect(appels()).toHaveLength(2)
    await expect(parcoursHttp.listerLiaisons({ page: 1, taille: 10 })).rejects.toMatchObject({ status: 409 })
  })
})

describe('decisionHttp : verrous 1 et 2, visas, fiche de suivi', () => {
  it('procès-verbal : lecture, établissement, signature GIMAC réauthentifiée', async () => {
    brancher({ etat: 'ETABLI' }, {}, {})
    expect(await decisionHttp.procesVerbal('LIA-1')).toEqual({ etat: 'ETABLI' })
    await decisionHttp.etablirProcesVerbal('LIA-1')
    await decisionHttp.signerProcesVerbalGimac('LIA-1', 'assertion-fido2')
    expect(lignes()).toEqual([
      'GET /bff/des/certification/liaisons/LIA-1/proces-verbal',
      'POST /bff/des/certification/liaisons/LIA-1/proces-verbal',
      'POST /bff/des/certification/liaisons/LIA-1/proces-verbal/signature-gimac',
    ])
    expect(assertion(1)).toBeNull()
    expect(assertion(2)).toBe('assertion-fido2')
  })
  it('approbation de la sécurité : dossier, fiche lue, attestations, archivage, visas', async () => {
    brancher({ etat: 'EN_ATTENTE_DOSSIER' }, { id: 'DOC-9', sha256: '' }, {}, {}, {}, {}, {}, {})
    await decisionHttp.approbation('LIA-1')
    await decisionHttp.constituerDossierChangement('LIA-1', fichier('dossier.pdf'))
    await decisionHttp.referencerFicheChangement('LIA-1', 'CHG-2026-0412')
    await decisionHttp.relireFicheChangement('LIA-1')
    await decisionHttp.verifierAttestation('LIA-1', 'PCI_DSS', false, 'Périmètre expiré')
    await decisionHttp.archiverAttestations('LIA-1')
    await decisionHttp.signerVisa('LIA-1', 'DIRECTION_GENERALE', true, 'Favorable', 'assertion-dg')
    expect(lignes()).toEqual([
      'GET /bff/des/certification/liaisons/LIA-1/approbation-securite',
      'POST /bff/des/document/documents',
      'POST /bff/des/certification/liaisons/LIA-1/approbation-securite/dossier',
      'PUT /bff/des/certification/liaisons/LIA-1/approbation-securite/fiche',
      'POST /bff/des/certification/liaisons/LIA-1/approbation-securite/fiche/lecture',
      'POST /bff/des/certification/liaisons/LIA-1/approbation-securite/attestations/PCI_DSS/verification',
      'POST /bff/des/certification/liaisons/LIA-1/approbation-securite/archivage',
      'POST /bff/des/certification/liaisons/LIA-1/approbation-securite/visas/DIRECTION_GENERALE',
    ])
    expect(corps(1)).toMatchObject({ finalite: 'DOSSIER_CHANGEMENT', reference: 'LIA-1' })
    expect(corps(2)).toEqual({ documentId: 'DOC-9', empreinte: SHA_ABC, nom: 'dossier.pdf' })
    expect(corps(3)).toEqual({ reference: 'CHG-2026-0412' })
    expect(corps(5)).toEqual({ conforme: false, motif: 'Périmètre expiré' })
    expect(corps(7)).toEqual({ favorable: true, motif: 'Favorable' })
    expect(assertion(7)).toBe('assertion-dg')
  })
  it('fiche de suivi (étape 10) : lecture, remise, constat', async () => {
    brancher({ etat: 'A_REMETTRE' }, {}, {})
    await decisionHttp.ficheSuivi('LIA-1')
    await decisionHttp.remettreFicheSuivi('LIA-1')
    await decisionHttp.constaterMiseEnService('LIA-1')
    expect(lignes()).toEqual([
      'GET /bff/des/homologation/liaisons/LIA-1/fiche-suivi',
      'POST /bff/des/homologation/liaisons/LIA-1/fiche-suivi/remise',
      'POST /bff/des/homologation/liaisons/LIA-1/fiche-suivi/constat',
    ])
  })
})

describe('portailParcoursHttp : tâches du participant (qualité Externe)', () => {
  it('lectures : liaisons (résumés), dossier, tâches, procès-verbal, approbation, fiche', async () => {
    brancher([vue({ etat: 'CERTIFIEE' })], {}, {}, {}, {}, {})
    expect(await portailParcoursHttp.mesLiaisons()).toEqual([expect.objectContaining({ id: 'LIA-1', statut: 'CERTIFIEE', etapeCourante: 'APPROBATION_SECURITE' })])
    await portailParcoursHttp.dossier('LIA-1')
    await portailParcoursHttp.mesTaches()
    await portailParcoursHttp.procesVerbal('LIA-1')
    await portailParcoursHttp.approbation('LIA-1')
    await portailParcoursHttp.ficheSuivi('LIA-1')
    expect(lignes()).toEqual([
      'GET /bff/portail/homologation/liaisons',
      'GET /bff/portail/homologation/liaisons/LIA-1/dossier',
      'GET /bff/portail/homologation/taches',
      'GET /bff/portail/certification/liaisons/LIA-1/proces-verbal',
      'GET /bff/portail/certification/liaisons/LIA-1/approbation-securite',
      'GET /bff/portail/homologation/liaisons/LIA-1/fiche-suivi',
    ])
  })
  it('écritures : coordonnées, livrable et attestation en multipart, signature réauthentifiée, fiche renseignée', async () => {
    brancher({}, {}, {}, {}, {})
    await portailParcoursHttp.communiquerCoordonnees('LIA-1', { adresse: 'essai.exemple', port: '8443' })
    await portailParcoursHttp.deposerLivrable('LIA-1:21', fichier('attestations.pdf'), 'Jointes')
    await portailParcoursHttp.signerProcesVerbal('LIA-1', 'assertion-part')
    await portailParcoursHttp.deposerAttestation('LIA-1', 'ISO_27001', fichier('iso.pdf'))
    const l = [{ serviceCode: 'RETRAIT', ouvert: true, commentaire: null }]
    await portailParcoursHttp.renseignerFicheSuivi('LIA-1', l)
    expect(lignes()).toEqual([
      'PUT /bff/portail/homologation/liaisons/LIA-1/coordonnees-techniques',
      'POST /bff/portail/homologation/operations/LIA-1:21/livrable',
      'POST /bff/portail/certification/liaisons/LIA-1/proces-verbal/signature',
      'POST /bff/portail/certification/liaisons/LIA-1/attestations/ISO_27001',
      'PUT /bff/portail/homologation/liaisons/LIA-1/fiche-suivi',
    ])
    expect(corps(0)).toEqual({ adresse: 'essai.exemple', port: '8443' })
    expect(corps(1)).toEqual({ fichier: 'fichier:attestations.pdf', commentaire: 'Jointes' })
    expect(assertion(2)).toBe('assertion-part')
    expect(corps(3)).toEqual({ fichier: 'fichier:iso.pdf' })
    expect(corps(4)).toEqual({ lignes: l })
  })
  it('refus du portail : ApiError avec le code du service', async () => {
    vi.spyOn(globalThis, 'fetch').mockImplementation(async () => new Response(JSON.stringify({ code: 'CER_PV_NOT_SIGNABLE', detail: 'Déjà signé' }), { status: 409, headers: { 'Content-Type': 'application/problem+json' } }))
    await expect(portailParcoursHttp.signerProcesVerbal('LIA-1', 'a')).rejects.toMatchObject({ status: 409, code: 'CER_PV_NOT_SIGNABLE', message: 'Déjà signé' })
  })
})
