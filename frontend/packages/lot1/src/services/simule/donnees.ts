import type { CoverageValue } from '../../modele/commun'
import type {
  CampaignResult, ElementKey, ElementState, Graphe, Liaison, OperationInstance, OperationNodeData, ReferenceOperation,
  ReferentielVersion, ScenarioInconnu, ServiceCouvert,
} from '../../modele/homologation'
import type { Attestation, Circuit, Condition, Dossier, EntreeRegistre, FicheLue, Horodatage } from '../../modele/certification'

// Jeu de données de développement (PI-GIMAC-01 « Utilisateurs de test et jeu de données ») : institutions
// fictives, aucune donnée réelle d'institution, de personne ni d'infrastructure.

export const INSTITUTIONS = [
  { id: 'INST-0042', code: 'INST-0042', denomination: 'Établissement de paiement A' },
  { id: 'INST-0017', code: 'INST-0017', denomination: 'Banque B' },
  { id: 'INST-0088', code: 'INST-0088', denomination: 'Établissement de microfinance C' },
  { id: 'INST-0061', code: 'INST-0061', denomination: 'Banque D' },
]
export const COLLABORATEURS = [
  { id: 'U-101', nom: 'Ingénieur d\'homologation A', role: 'INGENIEUR_HOMOLOGATION' },
  { id: 'U-102', nom: 'Ingénieur d\'homologation B', role: 'INGENIEUR_HOMOLOGATION' },
  { id: 'U-001', nom: 'Chef du DES', role: 'CHEF_DES' },
]

const hex = (graine: string) => {
  let h = 2166136261
  let s = ''
  for (let i = 0; s.length < 64; i++) {
    h ^= graine.charCodeAt(i % graine.length) + i
    h = Math.imul(h, 16777619) >>> 0
    s += h.toString(16).padStart(8, '0')
  }
  return s.slice(0, 64)
}
export const empreinte = hex

const cov = (numerateur: number, denominateur: number): CoverageValue => ({
  taux: denominateur ? Math.round((numerateur / denominateur) * 100) : null, numerateur, denominateur, calculable: denominateur > 0,
  motifNonCalculable: denominateur ? undefined : 'aucun service souscrit',
})

// Référentiels d'opérations (HOM-01)
export const referentiels: ReferentielVersion[] = [
  { id: 'REF-CI-3', typeProjet: 'CARTE_INTERBANCAIRE', numero: 3, libelle: 'Carte interbancaire, version 3', etat: 'PUBLIEE', dateCreation: '02/06/2026 09:10', datePublication: '15/06/2026 16:40', versionSourceId: 'REF-CI-2', versionActiveId: 'REF-CI-3', transitionsPermises: [], actionsPermises: ['DERIVER', 'CONTROLER'] },
  { id: 'REF-CI-4', typeProjet: 'CARTE_INTERBANCAIRE', numero: null, libelle: 'Carte interbancaire, brouillon 4', etat: 'BROUILLON', dateCreation: '22/09/2026 11:02', datePublication: null, versionSourceId: 'REF-CI-3', versionActiveId: 'REF-CI-3', transitionsPermises: ['PUBLIEE'], actionsPermises: ['CONTROLER', 'PUBLIER', 'MODIFIER', 'MIGRER'] },
  { id: 'REF-CI-2', typeProjet: 'CARTE_INTERBANCAIRE', numero: 2, libelle: 'Carte interbancaire, version 2', etat: 'RETIREE', dateCreation: '10/01/2026 08:30', datePublication: '20/01/2026 10:00', versionSourceId: null, versionActiveId: 'REF-CI-3', transitionsPermises: [], actionsPermises: [] },
  { id: 'REF-PM-2', typeProjet: 'PAIEMENT_MOBILE', numero: 2, libelle: 'Paiement mobile, version 2', etat: 'PUBLIEE', dateCreation: '04/05/2026 14:00', datePublication: '12/05/2026 09:15', versionSourceId: null, versionActiveId: 'REF-PM-2', transitionsPermises: [], actionsPermises: ['DERIVER', 'CONTROLER'] },
]

const op = (versionId: string, code: string, libelle: string, phase: string, segment: ReferenceOperation['segment'], partie: ReferenceOperation['partie'], predecesseurs: string[], serviceCode = 'RETRAIT', idScenario: string | null = null): ReferenceOperation => ({
  versionId, code, libelle, phase, segment, partie, serviceCode, natureCouverture: idScenario ? 'CERTIFICATION' : 'AUCUNE', idScenario, predecesseurs,
  fonctionResponsable: partie === 'DES' ? 'Ingénieur d\'homologation' : 'Correspondant technique', preuveAttendue: { rapport: 'Rapport de tests signé' },
  transitionsPermises: [], actionsPermises: versionId === 'REF-CI-4' ? ['MODIFIER'] : [],
})
const opsCarte = (v: string, brouillon: boolean): ReferenceOperation[] => [
  op(v, 'OP-E01', 'Personnalisation et clés', 'Raccordement', 'TRONC_COMMUN_AMONT', 'PARTICIPANT', []),
  op(v, 'OP-E02', 'Scénarios de litige émetteur', 'Certification', 'BRANCHE', 'PARTICIPANT', ['OP-E01'], 'RETRAIT', 'SC-LIT-01'),
  ...(brouillon ? [op(v, 'OP-E05', 'Simulation des autorisations', 'Certification', 'BRANCHE', 'DES', ['OP-E01', 'OP-E02'], 'PAIEMENT', 'SC-AUT-02')] : [op(v, 'OP-E04', 'Rapprochement à blanc', 'Certification', 'BRANCHE', 'DES', ['OP-E02'])]),
  op(v, 'OP-E03', 'Bascule émetteur', 'Mise en service', 'TRONC_COMMUN_AVAL', 'DES', brouillon ? ['OP-E05'] : ['OP-E04']),
]
export const operationsReferentiel: Record<string, ReferenceOperation[]> = {
  'REF-CI-3': opsCarte('REF-CI-3', false),
  'REF-CI-4': opsCarte('REF-CI-4', true),
  'REF-CI-2': opsCarte('REF-CI-2', false).slice(0, 3),
  'REF-PM-2': [
    op('REF-PM-2', 'OP-M01', 'Raccordement du portefeuille', 'Raccordement', 'TRONC_COMMUN_AMONT', 'PARTICIPANT', [], 'TRANSFERT'),
    op('REF-PM-2', 'OP-M02', 'Transferts de bout en bout', 'Certification', 'BRANCHE', 'PARTICIPANT', ['OP-M01'], 'TRANSFERT', 'SC-TRF-01'),
    op('REF-PM-2', 'OP-M03', 'Bascule en production', 'Mise en service', 'TRONC_COMMUN_AVAL', 'DES', ['OP-M02'], 'TRANSFERT'),
  ],
}

// Liaisons (HOM-04)
const liaison = (id: string, inst: number, typeProjet: Liaison['typeProjet'], etat: Liaison['etat'], parcourus: Liaison['etatsParcourus'], phase: string, cert: [number, number], mes: [number, number], retards: number, actions: string[], motif: string | null = null): Liaison => ({
  id, typeProjet, institutionId: INSTITUTIONS[inst].id, institutionNom: INSTITUTIONS[inst].denomination, etat,
  chefDeProjetId: 'U-101', chefDeProjetNom: COLLABORATEURS[0].nom, banquePartenaireId: inst === 0 ? 'INST-0017' : null, banquePartenaireNom: inst === 0 ? 'Banque B' : null,
  dateOuverture: '03/03/2026', motifEtat: motif, versionId: typeProjet === 'PAIEMENT_MOBILE' ? 'REF-PM-2' : 'REF-CI-3', versionNumero: typeProjet === 'PAIEMENT_MOBILE' ? 2 : 3,
  phaseCourante: phase, couvertureCertification: cov(...cert), couvertureMiseEnService: cov(...mes), operationsEnRetard: retards, etatsParcourus: parcourus,
  transitionsPermises: [], actionsPermises: actions,
})
export const liaisons: Liaison[] = [
  liaison('LIA-2026-017', 0, 'CARTE_INTERBANCAIRE', 'EN_HOMOLOGATION', ['OUVERTE'], 'Certification', [3, 5], [1, 4], 1, ['DESIGNER_CHEF_DE_PROJET', 'EXPORTER_PREUVES']),
  liaison('LIA-2025-102', 1, 'CARTE_INTERBANCAIRE', 'APTE_CERTIFICATION', ['OUVERTE', 'EN_HOMOLOGATION'], 'Mise en service', [5, 5], [4, 4], 0, ['EXPORTER_PREUVES']),
  liaison('LIA-2026-021', 3, 'CARTE_INTERBANCAIRE', 'GELEE', ['OUVERTE', 'EN_HOMOLOGATION'], 'Certification', [2, 5], [0, 4], 3, ['DEGELER'], 'Institution suspendue par la production monétique'),
  liaison('LIA-2026-030', 2, 'PAIEMENT_MOBILE', 'OUVERTE', [], 'Raccordement', [0, 3], [0, 2], 0, ['DESIGNER_CHEF_DE_PROJET']),
]

// Opérations et graphe (HOM-07) : couches topologiques fournies par le service.
const ELEMENTS = (r: ElementState, p: ElementState, h: ElementState, v: ElementState): Record<ElementKey, ElementState> => ({ responsable: r, preuve: p, horodatage: h, validation: v })
const noeud = (id: string, code: string, libelle: string, phase: string, partie: OperationNodeData['partie'], etat: string, elements: Record<ElementKey, ElementState>, enRetard = false): OperationNodeData => ({ id, code, libelle, phase, partie, etat, elements, enRetard, aRevoir: false })
export function grapheDe(liaisonId: string): Graphe {
  const s = liaisonId.slice(-3)
  const n = [
    noeud(`OPI-${s}-1`, 'OP-E01', 'Personnalisation et clés', 'Raccordement', 'PARTICIPANT', 'CLOSE', ELEMENTS('REUNI', 'REUNI', 'REUNI', 'REUNI')),
    noeud(`OPI-${s}-2`, 'OP-E02', 'Scénarios de litige émetteur', 'Certification', 'PARTICIPANT', 'CLOSE', ELEMENTS('REUNI', 'REUNI', 'REUNI', 'REUNI')),
    noeud(`OPI-${s}-3`, 'OP-E04', 'Rapprochement à blanc', 'Certification', 'DES', liaisonId === 'LIA-2025-102' ? 'CLOSE' : 'PREUVE_DEPOSEE', liaisonId === 'LIA-2025-102' ? ELEMENTS('REUNI', 'REUNI', 'REUNI', 'REUNI') : ELEMENTS('REUNI', 'REUNI', 'EN_ATTENTE', 'MANQUANT'), liaisonId === 'LIA-2026-017'),
    noeud(`OPI-${s}-4`, 'OP-E06', 'Recette du service paiement', 'Certification', 'PARTICIPANT', liaisonId === 'LIA-2025-102' ? 'CLOSE' : 'ELIGIBLE', liaisonId === 'LIA-2025-102' ? ELEMENTS('REUNI', 'REUNI', 'REUNI', 'REUNI') : ELEMENTS('REUNI', 'MANQUANT', 'MANQUANT', 'MANQUANT')),
    noeud(`OPI-${s}-5`, 'OP-E03', 'Bascule émetteur', 'Mise en service', 'DES', liaisonId === 'LIA-2025-102' ? 'ELIGIBLE' : 'VERROUILLEE', ELEMENTS('MANQUANT', 'MANQUANT', 'MANQUANT', 'MANQUANT')),
  ]
  return {
    noeuds: n,
    aretes: [{ from: n[0].id, to: n[1].id }, { from: n[1].id, to: n[2].id }, { from: n[0].id, to: n[3].id }, { from: n[2].id, to: n[4].id }, { from: n[3].id, to: n[4].id }],
    couches: [[n[0].id], [n[1].id, n[3].id], [n[2].id], [n[4].id]],
  }
}
export function operationsDe(l: Liaison): OperationInstance[] {
  return grapheDe(l.id).noeuds.map((n, i) => ({
    id: n.id, liaisonId: l.id, code: n.code, libelle: n.libelle, segment: i === 0 ? 'TRONC_COMMUN_AMONT' : i === 4 ? 'TRONC_COMMUN_AVAL' : 'BRANCHE',
    partie: n.partie, serviceCode: i === 3 ? 'PAIEMENT' : 'RETRAIT', etat: n.etat, responsableId: 'U-101', responsableNom: COLLABORATEURS[0].nom,
    echeance: ['12/09/2026', '20/09/2026', '25/09/2026', '10/10/2026', '30/10/2026'][i], joursDepassement: n.enRetard ? 4 : 0,
    predecesseursNonClos: n.etat === 'VERROUILLEE' ? ['OP-E04', 'OP-E06'] : [], aRevoir: false, transitionsPermises: [],
    actionsPermises: n.etat === 'CLOSE' ? ['ROUVRIR'] : n.etat === 'VERROUILLEE' ? ['ATTRIBUER'] : n.etat === 'ELIGIBLE' ? ['ATTRIBUER', 'DEPOSER_PREUVE'] : ['VALIDER', 'DEPOSER_PREUVE', 'RENSEIGNER_GRILLE'],
  }))
}
export function couverturesDe(l: Liaison): ServiceCouvert[] {
  return [
    { serviceCode: 'RETRAIT', serviceLibelle: 'Retrait', etatPerimetre: 'COUVERT', certification: cov(2, 2), miseEnService: cov(l.etat === 'APTE_CERTIFICATION' ? 2 : 1, 2) },
    { serviceCode: 'PAIEMENT', serviceLibelle: 'Paiement', etatPerimetre: 'EN_COURS', certification: cov(l.etat === 'APTE_CERTIFICATION' ? 2 : 1, 2), miseEnService: cov(l.etat === 'APTE_CERTIFICATION' ? 1 : 0, 1) },
    { serviceCode: 'TRANSFERT', serviceLibelle: 'Transfert', etatPerimetre: 'A_ENGAGER', certification: cov(0, 1), miseEnService: cov(0, 1) },
    { serviceCode: 'COMMERCE_ELECTRONIQUE', serviceLibelle: 'Commerce électronique', etatPerimetre: 'HORS_PERIMETRE', certification: cov(0, 0), miseEnService: cov(0, 0) },
  ]
}
export function campagnesDe(liaisonId: string): CampaignResult[] {
  return [
    { id: `${liaisonId}-C1`, campagneId: 'CMP-2026-09', executionId: 'EXE-8812', idScenario: 'SC-LIT-01', operationCode: 'OP-E02', verdict: 'FAVORABLE', dateExecution: '18/09/2026 10:12', versionScenario: '1.4', referenceTrace: 'TRC-55120', empreinteReponse: hex(liaisonId + 'c1'), dateReprise: '18/09/2026 10:20' },
    { id: `${liaisonId}-C2`, campagneId: 'CMP-2026-09', executionId: 'EXE-8813', idScenario: 'SC-AUT-02', operationCode: 'OP-E06', verdict: 'DEFAVORABLE', dateExecution: '18/09/2026 10:31', versionScenario: '2.0', referenceTrace: 'TRC-55121', empreinteReponse: hex(liaisonId + 'c2'), dateReprise: '18/09/2026 10:40' },
    { id: `${liaisonId}-C3`, campagneId: 'CMP-2026-09', executionId: 'EXE-8814', idScenario: 'SC-RET-07', operationCode: null, verdict: 'NON_EXECUTE', dateExecution: '18/09/2026 10:45', versionScenario: '1.0', referenceTrace: 'TRC-55122', empreinteReponse: hex(liaisonId + 'c3'), dateReprise: '18/09/2026 10:50' },
  ]
}
export function scenariosInconnusDe(liaisonId: string): ScenarioInconnu[] {
  return liaisonId === 'LIA-2026-017'
    ? [{ id: 'SCI-0091', liaisonId, idScenario: 'SC-XYZ-99', dateReception: '18/09/2026 10:47', qualification: null, transitionsPermises: [], actionsPermises: ['QUALIFIER'] }]
    : []
}

// Certification (CER-01 à CER-15) : dossier de la liaison apte.
export const circuit: Circuit = {
  id: 'CIR-2026-004',
  socle: { empreinte: hex('socle-LIA-2025-102'), operations: [{ code: 'OP-E01', libelle: 'Personnalisation et clés' }, { code: 'OP-E02', libelle: 'Scénarios de litige émetteur' }, { code: 'OP-E04', libelle: 'Rapprochement à blanc' }], services: ['RETRAIT', 'PAIEMENT'] },
  quitus: [
    { nature: 'TECHNIQUE', etat: 'SIGNE_FAVORABLE', signataireRole: 'CHEF_DES', dateSignature: '25/09/2026 16:10', dateHorodatage: '25/09/2026 16:10', actionsPermises: [] },
    { nature: 'SECURITE', etat: 'ATTENDU', signataireRole: 'RSSI', actionsPermises: ['SIGNER'] },
    { nature: 'CONFORMITE', etat: 'SIGNE_FAVORABLE', signataireRole: 'RESPONSABLE_CONFORMITE', dateSignature: '26/09/2026 09:02', dateHorodatage: '26/09/2026 09:02', actionsPermises: [] },
    { nature: 'DIRECTION_GENERALE', etat: 'ATTENDU', signataireRole: 'DG', actionsPermises: [] },
  ],
}
export const dossier: Dossier = {
  id: 'DMS-2026-004', liaisonId: 'LIA-2025-102', circuitId: circuit.id, institutionNom: 'Banque B', cycle: 1, etat: 'OUVERT', etatsParcourus: [],
  etatScellement: null, socleEmpreinte: circuit.socle.empreinte, dateOuverture: '24/09/2026', dateAutorisation: null,
  referenceFicheChangement: 'CHG-2026-0412', numeroAttestation: null, transitionsPermises: ['AUTORISE'], actionsPermises: ['VERIFIER', 'RELIRE_FICHE'],
}
export const conditions: Condition[] = [
  { code: 'COUVERTURE_CERTIFICATION', satisfaite: true, source: 'MS-HOMOLOGATION', detail: '5 sur 5', dateVerification: '28/09/2026 08:00', transitionsPermises: [], actionsPermises: ['VERIFIER'] },
  { code: 'COUVERTURE_MISE_EN_SERVICE', satisfaite: true, source: 'MS-HOMOLOGATION', detail: '4 sur 4', dateVerification: '28/09/2026 08:00', transitionsPermises: [], actionsPermises: ['VERIFIER'] },
  { code: 'QUATRE_QUITUS', satisfaite: false, source: 'MS-CERTIFICATION', detail: '2 sur 4 signés', dateVerification: '28/09/2026 08:00', transitionsPermises: [], actionsPermises: [] },
  { code: 'FICHE_CHANGEMENT_APPROUVEE', satisfaite: true, source: 'Gestion des changements', detail: 'CHG-2026-0412 approuvée', dateVerification: '28/09/2026 07:55', transitionsPermises: [], actionsPermises: ['VERIFIER'] },
  { code: 'PROCES_VERBAL', satisfaite: false, source: 'MS-DOCUMENT', detail: null, dateVerification: null, transitionsPermises: [], actionsPermises: [] },
  { code: 'EMPREINTES_VERIFIEES', satisfaite: true, source: 'MS-DOCUMENT', detail: '12 pièces', dateVerification: '28/09/2026 08:01', transitionsPermises: [], actionsPermises: ['VERIFIER'] },
]
export const fiche: FicheLue = { reference: 'CHG-2026-0412', etatApprobation: 'APPROUVEE', fenetreDebut: '05/10/2026 22:00', fenetreFin: '06/10/2026 04:00', dateLecture: '29/09/2026 07:40', aRelire: true }
export const horodatagesDossier: Horodatage[] = [
  { id: 'TST-1', objetType: 'QUITUS', empreinte: hex('q-tech'), dateJeton: '25/09/2026 16:10', verifie: true, autoriteChaine: 'AH interne GIMAC' },
  { id: 'TST-2', objetType: 'QUITUS', empreinte: hex('q-conf'), dateJeton: '26/09/2026 09:02', verifie: true, autoriteChaine: 'AH interne GIMAC' },
]
export const attestations: Attestation[] = [
  { numero: 'ATT-2026-0007', institutionNom: 'Établissement de paiement A', typeProjet: 'CARTE_INTERBANCAIRE', servicesCertifies: ['RETRAIT'], dateEmission: '12/03/2026', socleEmpreinte: hex('socle-A'), empreinteDocument: hex('att-7'), etat: 'EN_VIGUEUR', demandeEnAttente: null, transitionsPermises: [], actionsPermises: ['TELECHARGER', 'DEMANDER_STATUT'] },
]
export const registre: EntreeRegistre[] = [
  { numero: 'ATT-2026-0007', institutionNom: 'Établissement de paiement A', typeProjet: 'CARTE_INTERBANCAIRE', services: ['RETRAIT'], dateEmission: '12/03/2026', echeanceRenouvellement: '12/03/2029', etat: 'EN_VIGUEUR' },
  { numero: 'ATT-2025-0031', institutionNom: 'Banque D', typeProjet: 'CARTE_INTERBANCAIRE', services: ['RETRAIT', 'PAIEMENT'], dateEmission: '02/11/2025', echeanceRenouvellement: '02/11/2028', etat: 'SUSPENDUE' },
]
