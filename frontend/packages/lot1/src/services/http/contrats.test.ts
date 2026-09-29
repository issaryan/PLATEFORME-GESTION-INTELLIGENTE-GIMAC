import { afterEach, describe, expect, it, vi } from 'vitest'
import { homologationHttp } from './homologation'
import { certificationHttp } from './certification'
import { portailHttp } from './portail'
import { servicesHttp } from './index'

// Contrats HTTP des services du lot 1 : chemins de la passerelle (/bff/des/homologation,
// /bff/des/certification, /bff/portail/…), méthodes, corps et paramètres ; cadrage (LiaisonsCadrageResource).
const reponses: unknown[] = []
type Appel = [URL, RequestInit]
const appels = () => (globalThis.fetch as unknown as { mock: { calls: Appel[] } }).mock.calls
afterEach(() => {
  vi.restoreAllMocks()
  reponses.length = 0
})
/** Fichier renvoyé par la passerelle (PDF ou CSV), avec son empreinte Repr-Digest. */
const FICHIER = Symbol('fichier')
function brancher(...r: unknown[]) {
  reponses.push(...r)
  vi.spyOn(globalThis, 'fetch').mockImplementation(async (u) => {
    const chemin = new URL(String(u)).pathname
    const b = chemin.endsWith('/collaborateurs') ? { contenu: [{ id: 'U-1', nom: 'A', role: 'CHEF_DES' }] } : reponses.length ? reponses.shift() : {}
    if (b === FICHIER) return new Response('%PDF', { status: 200, headers: { 'Content-Type': 'application/pdf', 'Repr-Digest': 'sha-256=:q80=:' } })
    return new Response(JSON.stringify(b ?? {}), { status: 200, headers: { 'Content-Type': 'application/json' } })
  })
}
const ligne = (i: number) => {
  const [u, o] = appels()[i]
  const url = new URL(String(u))
  return `${o.method} ${url.pathname}${url.search}`
}
const corps = (i: number) => {
  const b = appels()[i][1].body
  return b instanceof FormData ? Object.fromEntries([...b.entries()].map(([k, v]) => [k, typeof v === 'string' ? v : 'fichier'])) : JSON.parse(String(b ?? 'null'))
}

describe('homologationHttp : cadrage (étape 3, D-57, D-58)', () => {
  it('liste : pagination du service à partir de 0, filtres vides omis, « Mes projets »', async () => {
    brancher({ contenu: [], total: 0 }, { contenu: [], total: 0 })
    await homologationHttp.listerLiaisons({ page: 2, taille: 25, recherche: 'banque', filtres: { etat: 'OUVERTE', typeProjet: '', mesProjets: true, institutionId: undefined } })
    await homologationHttp.listerLiaisons({ page: 1, taille: 10, filtres: { mesProjets: false } })
    expect(ligne(0)).toBe('GET /bff/des/homologation/liaisons?page=1&taille=25&recherche=banque&etat=OUVERTE&mesProjets=true')
    expect(ligne(1)).toBe('GET /bff/des/homologation/liaisons?page=0&taille=10')
  })
  it('ouverture, lecture, conducteurs et désignation motivée', async () => {
    brancher({ id: 'LIA-1' }, { id: 'LIA-1' }, [{ id: 'U-1', nom: 'A' }], { id: 'LIA-1' }, { id: 'LIA-1' })
    expect(await homologationHttp.ouvrirLiaison({ institutionId: 'i1', typeProjet: 'CARTE_INTERBANCAIRE', chefDeProjetId: 'U-1', banquePartenaireId: null })).toEqual({ id: 'LIA-1' })
    await homologationHttp.lireLiaison('LIA-1')
    expect(await homologationHttp.conducteurs()).toEqual([{ id: 'U-1', nom: 'A' }])
    await homologationHttp.designerChefDeProjet('LIA-1', 'U-2', 'Remplacement')
    await homologationHttp.designerChefDeProjet('LIA-1', 'U-3')
    expect([0, 1, 2, 3, 4].map(ligne)).toEqual([
      'POST /bff/des/homologation/liaisons',
      'GET /bff/des/homologation/liaisons/LIA-1',
      'GET /bff/des/homologation/liaisons/conducteurs',
      'PUT /bff/des/homologation/liaisons/LIA-1/chef-de-projet',
      'PUT /bff/des/homologation/liaisons/LIA-1/chef-de-projet',
    ])
    expect(corps(0)).toEqual({ institutionId: 'i1', typeProjet: 'CARTE_INTERBANCAIRE', chefDeProjetId: 'U-1', banquePartenaireId: null })
    expect(corps(3)).toEqual({ chefDeProjetId: 'U-2', motif: 'Remplacement' })
    expect(corps(4)).toEqual({ chefDeProjetId: 'U-3' })
  })
})

describe('homologationHttp : référentiels, liaisons, opérations, simulateur', () => {
  it('chaque route du port, sa méthode et son corps', async () => {
    brancher(...Array.from({ length: 33 }, (_, i) => (i === 9 ? { total: 3 } : {})))
    const h = homologationHttp
    const fichier = new File(['x'], 'preuve.pdf')
    await h.listerReferentiels()
    await h.lireReferentiel('R1')
    await h.operationsReferentiel('R1')
    await h.controlerReferentiel('R1')
    await h.simulerMigration('R1')
    await h.creerBrouillon({ typeProjet: 'CARTE_INTERBANCAIRE', libelle: 'v4' })
    await h.deriverBrouillon('R1')
    await h.publierReferentiel('R1')
    await h.migrerLiaisons('R1', ['L1'])
    expect(await h.liaisonsImpactees('R1')).toEqual({ total: 3 })
    await h.historiqueReferentiel('R1')
    await h.tableauDeBordLiaisons({ page: 1, taille: 25, tri: 'etat,asc' })
    await h.historiqueLiaison('L1')
    await h.degelerLiaison('L1', 'levée')
    await h.exporterDossierDePreuves('L1')
    await h.dernierExportPreuves('L1')
    await h.grapheLiaison('L1')
    await h.couverturesLiaison('L1')
    await h.listerOperations('L1', { enRetard: true })
    await h.lireOperation('O1')
    await h.preuvesOperation('O1')
    await h.attributionsOperation('O1')
    await h.grilleOperation('O1')
    await h.renseignerGrille('O1', { G1: 'CONFORME' })
    await h.attribuerOperation('O1', 'U-1', 'charge')
    await h.rouvrirOperation('O1', 'erreur')
    await h.elementsDeCloture('O1')
    await h.deposerPreuve('O1', fichier, 'Rapport')
    await h.validerOperation('O1', { motif: 'ok', empreinte: 'e' }, 'assertion')
    await h.campagnesLiaison('L1')
    await h.scenariosInconnus('L1')
    await h.qualifierScenario('S1', 'HORS_PARCOURS', 'hors')
    await h.etatReprise()
    expect(await h.rechercherCollaborateurs('a')).toEqual([{ value: 'U-1', label: 'A', hint: 'role.CHEF_DES' }])
    const H = '/bff/des/homologation'
    expect(appels().map((_, i) => ligne(i))).toEqual([
      `GET ${H}/referentiels`, `GET ${H}/referentiels/R1`, `GET ${H}/referentiels/R1/operations`, `POST ${H}/referentiels/R1/controle`,
      `POST ${H}/referentiels/R1/migrations/simulation`, `POST ${H}/referentiels`, `POST ${H}/referentiels`, `POST ${H}/referentiels/R1/publication`,
      `POST ${H}/referentiels/R1/migrations`, `GET ${H}/liaisons?versionId=R1&actives=true&taille=1`, `GET ${H}/referentiels/R1/historique`,
      `GET ${H}/tableau-de-bord/liaisons?page=1&taille=25&tri=etat%2Casc`, `GET ${H}/liaisons/L1/historique`, `POST ${H}/liaisons/L1/degel`,
      `POST ${H}/liaisons/L1/dossier-de-preuves`, `GET ${H}/liaisons/L1/dossier-de-preuves`, `GET ${H}/liaisons/L1/graphe`, `GET ${H}/liaisons/L1/couvertures`,
      `GET ${H}/liaisons/L1/operations?enRetard=true`, `GET ${H}/operations/O1`, `GET ${H}/operations/O1/preuves`, `GET ${H}/operations/O1/attributions`,
      `GET ${H}/operations/O1/grille`, `PUT ${H}/operations/O1/grille`, `PUT ${H}/operations/O1/responsable`, `POST ${H}/operations/O1/reouverture`,
      `GET ${H}/operations/O1/elements-de-cloture`, `POST ${H}/operations/O1/preuves`, `POST ${H}/operations/O1/validation`, `GET ${H}/liaisons/L1/campagnes`,
      `GET ${H}/liaisons/L1/scenarios-inconnus`, `POST ${H}/scenarios-inconnus/S1/qualification`, `GET ${H}/reprise/etat`,
      'GET /bff/des/iam/collaborateurs?recherche=a&roles=INGENIEUR_HOMOLOGATION%2CCHEF_DES',
    ])
    expect(corps(5)).toEqual({ typeProjet: 'CARTE_INTERBANCAIRE', libelle: 'v4' })
    expect(corps(6)).toEqual({ versionSourceId: 'R1' })
    expect(corps(8)).toEqual({ liaisonsRetenues: ['L1'] })
    expect(corps(13)).toEqual({ motif: 'levée' })
    expect(corps(23)).toEqual({ reponses: { G1: 'CONFORME' } })
    expect(corps(24)).toEqual({ responsableId: 'U-1', motif: 'charge' })
    expect(corps(27)).toEqual({ fichier: 'fichier', typePiece: 'Rapport' })
    expect(new Headers(appels()[28][1].headers).get('X-Reauth-Assertion')).toBe('assertion')
    expect(corps(31)).toEqual({ qualification: 'HORS_PARCOURS', motif: 'hors' })
  })
})

describe('certificationHttp', () => {
  it('dossiers, circuit, quitus signés, conditions, fiche, procès-verbal, attestations, registre', async () => {
    brancher({ contenu: [{ id: 'D1' }] }, { id: 'D1' }, { contenu: [] }, ...Array.from({ length: 14 }, () => ({})), FICHIER, {}, {}, {}, { contenu: [] }, FICHIER)
    const c = certificationHttp
    expect(await c.dossierParLiaison('L1')).toEqual({ id: 'D1' })
    expect(await c.dossierParLiaison('L2')).toBeNull()
    await c.lireDossier('D1')
    await c.historiqueDossier('D1')
    await c.circuit('C1')
    await c.signerQuitus('C1', 'DIRECTION_GENERALE', { decisionFavorable: true, motif: 'ok', socleEmpreinte: 's', operationsDesignees: [], servicesDesignes: [] }, 'assertion')
    await c.conditions('D1')
    await c.verifierDossier('D1')
    await c.verifierDossier('D1', 'PROCES_VERBAL')
    await c.ficheDeChangement('D1')
    await c.lierFicheDeChangement('D1', 'CHG-1')
    await c.procesVerbal('D1')
    await c.deposerProcesVerbal('D1', new File(['x'], 'pv.pdf'), '2026-10-05 22:00')
    await c.horodatages('D1')
    await c.emettreAttestation('D1')
    await c.lireAttestation('ATT-1')
    const t = await c.telechargerAttestation('ATT-1')
    expect(t.empreinte).toBe('abcd')
    await c.demanderStatut('ATT-1', { typeDemande: 'SUSPENSION', motif: 'risque', piece: new File(['x'], 'p.pdf'), conditionLevee: 'C' })
    await c.demanderStatut('ATT-1', { typeDemande: 'SUSPENSION', motif: 'risque', piece: new File(['x'], 'p.pdf') })
    await c.approuverStatut('DS1', 'ok', 'assertion')
    await c.registre({ page: 1, taille: 25, recherche: 'banque' })
    await c.exporterRegistre({ page: 1, taille: 25 })
    const C = '/bff/des/certification'
    expect(appels().map((_, i) => ligne(i))).toEqual([
      `GET ${C}/dossiers?liaisonId=L1&taille=1`, `GET ${C}/dossiers/D1`, `GET ${C}/dossiers?liaisonId=L2&taille=1`, `GET ${C}/dossiers/D1`,
      `GET ${C}/dossiers/D1/historique`, `GET ${C}/circuits/C1`, `POST ${C}/circuits/C1/quitus/direction-generale`, `GET ${C}/dossiers/D1/conditions`,
      `POST ${C}/dossiers/D1/verification`, `POST ${C}/dossiers/D1/verification`, `GET ${C}/dossiers/D1/fiche-de-changement`, `PUT ${C}/dossiers/D1/fiche-de-changement`,
      `GET ${C}/dossiers/D1/proces-verbal`, `POST ${C}/dossiers/D1/proces-verbal`, `GET ${C}/dossiers/D1/horodatages`, `POST ${C}/dossiers/D1/attestation`,
      `GET ${C}/attestations/ATT-1/fiche`, `GET ${C}/attestations/ATT-1`, `POST ${C}/attestations/ATT-1/demandes-de-statut`, `POST ${C}/attestations/ATT-1/demandes-de-statut`,
      `POST ${C}/demandes-de-statut/DS1/approbation`, `GET ${C}/registre?page=1&taille=25&recherche=banque`, `GET ${C}/registre/export?page=1&taille=25`,
    ])
    expect(corps(8)).toEqual({})
    expect(corps(9)).toEqual({ condition: 'PROCES_VERBAL' })
    expect(corps(18)).toEqual({ typeDemande: 'SUSPENSION', motif: 'risque', piece: 'fichier', conditionLevee: 'C' })
    expect(corps(19)).toEqual({ typeDemande: 'SUSPENSION', motif: 'risque', piece: 'fichier' })
  })
})

describe('portailHttp et servicesHttp', () => {
  it('routes du portail, cloisonnées par le jeton côté service', async () => {
    brancher({}, {}, {}, {}, {}, {}, FICHIER)
    const p = portailHttp
    await p.mesLiaisons()
    await p.maLiaison('L1')
    await p.aFaire('L1')
    await p.operationAFaire('L1', 'O1')
    await p.deposerPreuve('O1', new File(['x'], 'p.pdf'), 'Rapport')
    await p.certifications()
    await p.telechargerAttestation('CERT-1')
    expect(appels().map((_, i) => ligne(i))).toEqual([
      'GET /bff/portail/homologation/liaisons', 'GET /bff/portail/homologation/liaisons/L1', 'GET /bff/portail/homologation/liaisons/L1/a-faire',
      'GET /bff/portail/homologation/liaisons/L1/a-faire/O1', 'POST /bff/portail/homologation/operations/O1/preuves', 'GET /bff/portail/certification/certifications',
      'GET /bff/portail/certification/certifications/CERT-1/attestation',
    ])
  })
  it('servicesHttp : les quatre ports et la réauthentification de la couche', () => {
    const s = servicesHttp('des')
    expect(s.homologation).toBe(homologationHttp)
    expect(s.certification).toBe(certificationHttp)
    expect(s.portail).toBe(portailHttp)
    expect(typeof s.reauth.assertion).toBe('function')
  })
})
