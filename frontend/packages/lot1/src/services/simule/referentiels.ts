import type { ApiError } from '@gimac/socle'
import type { TypeProjet } from '../../modele/commun'
import type {
  AnomalieGraphe, CatalogueModelisateur, CreationTypeProjet, FonctionCatalogue, ModeleTypeProjet, TypeProjetVue, ControleResultat, CouleurCatalogue, CouleurEtape, EtapeCanonique, EtapeReferentiel, FiltreReferentiel, Geste, GrapheReferentiel, Jalon,
  Livrable, NoeudGraphe, Particularite, ReferenceOperation, ReferentielVersion, SaisieOperation, TypeAnomalie,
} from '../../modele/homologation'
import { PROFILS } from '../../permissions'
import { ETAPES, type EtapeCode, type Fonction, type VueOperation } from '../../modele/parcours'
import { MODELES_TYPE_PROJET } from '../../modele/homologation'
import { referentielDe, type OperationReferentiel } from '../../modele/referentielCR'
import type { ServiceHomologation } from '../ports'
import { LIBELLES_CR } from './libellesCR'

// Serveur de substitution du modélisateur de processus (D-77, docs/lot1/modelisateur-processus.md) : version 1
// fidèle à l'annexe A du CR-CERT-GIMAC-01 v1.4 (même conversion que ConversionAnnexeA de MS-HOMOLOGATION),
// graphe étendu (couches de Kahn, dates au plus tôt, chemin critique, anomalies) et écritures du brouillon avec
// les codes d'erreur du service. Ces règles sont celles du serveur simulé ; l'interface n'en porte aucune.

const EXTERNES: Fonction[] = ['PARTICIPANT', 'BANQUE_PARTENAIRE', 'PERSONNALISATEUR']
const PDF = ['application/pdf']
export const DECLENCHEURS: Record<string, string> = {
  'certification.pv-established.v1': 'Procès-verbal de certification établi',
  'certification.pv-signed.v1': 'Procès-verbal signé des deux parties',
  'certification.change-record-constituted.v1': 'Dossier de changement constitué',
  'certification.attestations-archived.v1': 'Attestations vérifiées et archivées',
  'certification.security-approved.v1': 'Changement approuvé par la sécurité',
  'certification.change-record-referenced.v1': 'Fiche de changement référencée',
}
const JALONS: Jalon[] = ['APTE_CERTIFICATION', 'CERTIFIEE', 'APPROUVEE_SECURITE', 'EN_PRODUCTION']
const GESTES_DEDIES: Geste[] = ['AFFECTER_RESSOURCES', 'EVENEMENT_EXTERNE', 'FICHE_SUIVI', 'AUCUN']
const SEGMENTS = ['AMONT', 'BRANCHE', 'AVAL'] as const

/** Geste des rangs autrefois codés en dur (version 1, §1). */
export function gesteDe(o: Pick<OperationReferentiel, 'rang' | 'grain'>): Geste {
  if (o.rang === '6') return 'AFFECTER_RESSOURCES'
  if (o.rang === '12') return 'COORDONNEES_TECHNIQUES'
  if (o.rang === '17') return 'CONSTATER_COUVERTURE'
  if (/^(1[89]|2[0-5])$/.test(o.rang)) return 'EVENEMENT_EXTERNE'
  if (o.rang === '32' || o.rang === '33') return 'FICHE_SUIVI'
  if (/^([7-9]|1[01])$/.test(o.rang)) return 'DEPOSER_LIVRABLE'
  return o.grain === 'PHASE' ? 'DEPOSER_LIVRABLE' : 'DEPOSER_PREUVE'
}
/** Geste réalisé par sa route propre : aucune pièce libre (Geste.dedie de MS-HOMOLOGATION). */
export const estDedie = (g: Geste | null) => !!g && GESTES_DEDIES.includes(g)
/** Action proposée sur une opération ouverte : constat, livrable, sinon preuve (Geste.action). */
export const actionDuGeste = (g: Geste | null) => (g === 'CONSTATER_COUVERTURE' ? 'CONSTATER_COUVERTURE' : g === 'DEPOSER_LIVRABLE' ? 'DEPOSER_LIVRABLE' : 'DEPOSER_PREUVE')
const jalonDe = (rang: string): Jalon | null => ({ 17: 'APTE_CERTIFICATION', 19: 'CERTIFIEE', 25: 'APPROUVEE_SECURITE', 31: 'EN_PRODUCTION' } as Record<string, Jalon>)[rang] ?? null
const declencheurDe = (rang: string): string | null =>
  ({ 18: 'certification.pv-established.v1', 19: 'certification.pv-signed.v1', 20: 'certification.change-record-constituted.v1', 21: 'certification.attestations-archived.v1',
    22: 'certification.attestations-archived.v1', 23: 'certification.security-approved.v1', 24: 'certification.security-approved.v1', 25: 'certification.security-approved.v1',
    26: 'certification.change-record-referenced.v1' } as Record<string, string>)[rang] ?? null
/** Vue spécialisée (D-93 §8.5) : reprise des versions existantes par les rangs, comme MS-HOMOLOGATION. */
export const vueDe = (rang: string): VueOperation => (rang === '6' ? 'AFFECTATION' : rang === '12' ? 'COORDONNEES' : /^1[5-7]$/.test(rang) ? 'ESSAIS'
  : /^(1[89]|20|2[3-6])$/.test(rang) ? 'DECISION' : /^2[12]$/.test(rang) ? 'ATTESTATIONS' : /^3[2-5]$/.test(rang) ? 'MISE_EN_SERVICE' : rang === 'C1' ? 'CEREMONIE' : 'AUCUNE')
const particulariteDe = (rang: string): Particularite | null => ({ 15: 'ENTREE_ESSAIS', 16: 'FIN_ESSAIS', C1: 'CEREMONIE_CLES', 35: 'SERVICE_SUPPLEMENTAIRE' } as Record<string, Particularite>)[rang] ?? null

/** Version 1 d'un type de projet, convertie de l'annexe A (rangs 3 et suivants, branche du type, tronc aval). */
export function versionCR(type: TypeProjet): ReferenceOperation[] {
  const suite = referentielDe(type).filter((o) => o.etape !== 'PRISE_CONTACT' && o.etape !== 'ADHESION')
  const codes = new Set(suite.map((o) => o.rang))
  const finBranche = suite.filter((o) => o.segment === 'BRANCHE').at(-1)?.rang
  const lignes = new Map<EtapeCode, number>()
  return suite.map((o, i): ReferenceOperation => {
    const [libelle, preuve, valeur] = LIBELLES_CR[o.rang] ?? [o.rang, '', '']
    const r = o.porteurs[0]
    const autres = o.porteurs.slice(1)
    const ligne = lignes.get(o.etape) ?? 0
    lignes.set(o.etape, ligne + 1)
    const predecesseurs = o.rang === '27' && finBranche ? [finBranche] : o.predecesseurs.filter((p) => codes.has(p))
    // HOM-01 : la réunion de lancement (rang 7) porte la signature VISA_CONDUITE de son compte rendu : son livrable L1 est déclaré.
    const livrables: Livrable[] = o.grain === 'PHASE' || o.pivot || o.rang === '7' ? [{ code: 'L1', libelle: preuve || libelle, nature: 'LIVRABLE', deposant: r, typesPieces: PDF, obligatoire: true }] : []
    return {
      code: o.rang, libelle, description: preuve ? `Preuve attendue : ${preuve}.` : null, etape: o.etape, segment: o.segment, ordre: i + 1,
      responsable: r, validateur: r === 'CONDUCTEUR' ? 'CHEF_DES' : 'CONDUCTEUR', consultes: autres.filter((f) => !EXTERNES.includes(f)), informes: ['CHEF_DDRM'],
      externes: autres.filter((f) => EXTERNES.includes(f)), predecesseurs, delaiJours: 3, grain: o.grain,
      plateforme: o.rang === '15' || o.rang === '16' ? (type === 'PAIEMENT_MOBILE' ? 'SIMULATEUR' : null) : o.plateforme,
      cle: !!o.cle, secret: !!o.secret, oubliSansSymptome: !!o.oubliSansSymptome, pivot: !!o.pivot, valeurReference: o.valeurReference ? valeur || 'Valeur de référence' : null,
      livrables, geste: gesteDe(o), jalon: jalonDe(o.rang), declencheur: declencheurDe(o.rang), particularite: particulariteDe(o.rang),
      position: { x: ETAPES.indexOf(o.etape) * 300, y: ligne * 120 + SEGMENTS.indexOf(o.segment) * 40 },
      phase: null, partie: EXTERNES.includes(r) ? 'PARTICIPANT' : 'DES', serviceCode: 'TRANSVERSE', natureCouverture: 'AUCUNE', preuveAttendue: {}, idScenario: null,
      fonctionResponsable: r, grille: null,
      vue: vueDe(o.rang), consultesRecoiventTache: false,
      // D-91 : signatures de démonstration sur la réunion de lancement (rang 7) ; les autres opérations n'en ont pas.
      signatures: o.rang === '7' ? [{ code: 'VISA_CONDUITE', libelle: 'Visa du conducteur sur le compte rendu', signataire: 'CONDUCTEUR', ordre: 1, porte: ['L1'] }] : [],
    }
  })
}

const parType = new Map<TypeProjet, ReferenceOperation[]>()
/** Codes de la version 1 simulée d'un type clos par un sujet d'événement (déclencheur lu sur l'opération, §3). */
export function codesDeclenches(type: TypeProjet, sujet: string): string[] {
  if (!parType.has(type)) parType.set(type, versionCR(type))
  return (parType.get(type) ?? []).filter((o) => o.declencheur === sujet).map((o) => o.code)
}

const MODELES_LIBELLES: [ModeleTypeProjet, string][] = [['CARTE_INTERBANCAIRE', 'Carte interbancaire'], ['PAIEMENT_MOBILE', 'Paiement mobile'], ['DELEGATAIRE', 'Délégataire'], ['GENERIQUE', 'Générique']]

/** Catalogue du modélisateur (GET /referentiels/catalogue), aux libellés français du service. */
export const CATALOGUE: CatalogueModelisateur = {
  fonctions: ([
    ['DDRM', 'Développement du Réseau et du Marché', ['ROLE_CHARGE_DDRM']], ['CHEF_DDRM', 'Chef du département DDRM', ['ROLE_CHEF_DDRM']],
    ['CHEF_DES', 'Chef du département DES', ['ROLE_CHEF_DES']], ['CHEFS_DEPARTEMENT', 'Chefs des deux départements', ['ROLE_CHEF_DES', 'ROLE_CHEF_DDRM']],
    ['CONDUCTEUR', 'Conducteur de projet', ['ROLE_CONDUCTEUR_PROJET']], ['SNA', 'Normes et Agréments', ['ROLE_SNA', 'ROLE_INGENIEUR_HOMOLOGATION']],
    ['PRODUCTION_MONETIQUE', 'Production monétique', ['ROLE_PRODUCTION_MONETIQUE']], ['POLE_RESEAU', 'Pôle réseau', ['ROLE_POLE_RESEAU']],
    ['POLE_SYSTEME', 'Pôle système', ['ROLE_POLE_SYSTEME']], ['RSSI', 'Sécurité des systèmes d\'information', ['ROLE_RSSI']], ['CONFORMITE', 'Conformité', ['ROLE_CONFORMITE']],
    ['DG', 'Direction générale', ['ROLE_DG']], ['EXPLOITATION', 'Exploitation et Supervision', ['ROLE_PRODUCTION_MONETIQUE']], ['PARTICIPANT', 'Participant', ['ROLE_CORRESPONDANT']],
    ['BANQUE_PARTENAIRE', 'Banque partenaire', []], ['PERSONNALISATEUR', 'Personnalisateur', []],
  ] as const).map(([code, libelle, profils]) => ({ code, libelle, profils: [...profils], externe: EXTERNES.includes(code) })),
  gestes: ([
    ['DEPOSER_LIVRABLE', 'Déposer un livrable'], ['DEPOSER_PREUVE', 'Déposer une preuve'], ['CONSTATER_COUVERTURE', 'Constater la couverture du périmètre'],
    ['AFFECTER_RESSOURCES', 'Affecter les intervenants'], ['COORDONNEES_TECHNIQUES', 'Communiquer les coordonnées techniques d\'essai'], ['FICHE_SUIVI', 'Fiche de suivi des services'],
    ['EVENEMENT_EXTERNE', 'Événement d\'un autre service'], ['AUCUN', 'Aucun geste : point de passage'],
  ] as const).map(([code, libelle]) => ({ code, libelle, dedie: GESTES_DEDIES.includes(code) })),
  jalons: [['APTE_CERTIFICATION', 'Apte à la certification'], ['CERTIFIEE', 'Certifiée'], ['APPROUVEE_SECURITE', 'Approuvée par la sécurité'], ['EN_PRODUCTION', 'En production']]
    .map(([code, libelle], i) => ({ code, libelle, rang: i + 1 })),
  declencheurs: Object.entries(DECLENCHEURS).map(([code, libelle]) => ({ code, libelle })),
  etapes: ([
    ['PRISE_CONTACT', 'Prise de contact', 1], ['ADHESION', 'Adhésion', 2], ['CADRAGE', 'Cadrage', 3], ['LANCEMENT', 'Lancement', 4], ['ATELIERS', 'Ateliers', 5],
    ['INTEGRATION', 'Intégration', 6], ['TESTS', 'Tests', 7], ['CERTIFICATION', 'Certification', 8], ['APPROBATION_SECURITE', 'Approbation de la sécurité', undefined],
    ['MISE_EN_PRODUCTION', 'Mise en production', 9], ['SUIVI', 'Suivi post-production', 10],
  ] as const).map(([code, libelle, rang]) => (rang ? { code, libelle, rang } : { code, libelle })),
  segments: [{ code: 'AMONT', libelle: 'Tronc commun amont' }, { code: 'BRANCHE', libelle: 'Branche du type de projet' }, { code: 'AVAL', libelle: 'Tronc commun aval' }],
  plateformes: [{ code: 'SWITCH_INTERBANCAIRE', libelle: 'Switch interbancaire' }, { code: 'SWITCH_DELEGATAIRE', libelle: 'Switch délégataire' },
    { code: 'PLATEFORME_MOBILE', libelle: 'Plateforme de paiement mobile' }, { code: 'SIMULATEUR', libelle: 'Simulateur' }],
  grains: [{ code: 'PHASE', libelle: 'Pièce de phase' }, { code: 'OPERATION', libelle: 'Preuve par opération' }],
  naturesLivrable: [{ code: 'LIVRABLE', libelle: 'Livrable' }, { code: 'PREUVE', libelle: 'Preuve' }],
  particularites: [{ code: 'ENTREE_ESSAIS', libelle: 'Entrée en essai des services' }, { code: 'FIN_ESSAIS', libelle: 'Fin des essais et certification des services' },
    { code: 'CEREMONIE_CLES', libelle: 'Cérémonie de clés en trois composantes' }, { code: 'SERVICE_SUPPLEMENTAIRE', libelle: 'Ouverture d\'un service supplémentaire' }],
  typesPieces: ['application/pdf', 'image/png', 'image/jpeg', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', 'text/csv', 'application/zip'],
  // Recette HOM-14 : pilotes nommés comme les comptes de démonstration des ingénieurs d'homologation.
  pilotes: [{ id: 'U-101', nom: 'Joël Ekotto Mballa' }, { id: 'U-102', nom: 'Arnaud Mvogo Eyenga' }],
  vuesOperation: ([
    ['AUCUNE', 'Aucune vue spécialisée'], ['AFFECTATION', 'Affectation des intervenants'], ['COORDONNEES', 'Coordonnées techniques'], ['ESSAIS', 'Essais'],
    ['DECISION', 'Décision de mise en production'], ['ATTESTATIONS', 'Attestations'], ['MISE_EN_SERVICE', 'Mise en service'], ['CEREMONIE', 'Cérémonie de clés'],
  ] as const).map(([code, libelle]) => ({ code, libelle })),
  modelesTypeProjet: MODELES_LIBELLES.map(([code, libelle]) => ({ code, libelle })),
}

// ---------------------------------------------------------------------------------- étapes configurables (D-89)

/** Couleurs d'étape (CouleurEtape de MS-HOMOLOGATION) : variables CSS du design system. */
export const COULEURS_ETAPE: CouleurCatalogue[] = ([
  ['NEUTRE', 'Neutre', '--gm-color-status-neutral-fg'], ['INFO', 'Information', '--gm-color-status-info-fg'], ['SUCCES', 'Succès', '--gm-color-status-ok-fg'],
  ['ALERTE', 'Alerte', '--gm-color-status-warn-fg'], ['CRITIQUE', 'Critique', '--gm-color-status-crit-fg'], ['ACTION', 'Action', '--gm-color-action'],
  ['ACCENT', 'Accent', '--gm-color-brand-accent'],
] as const).map(([code, libelle, jeton]) => ({ code, libelle, jeton }))
/** Étapes canoniques porteuses d'un jalon ou du verrou 2 : ni retrait ni changement de canonique. */
const PROTEGEES: EtapeCode[] = ['CERTIFICATION', 'APPROBATION_SECURITE', 'MISE_EN_PRODUCTION']
export const ETAPES_CANONIQUES: EtapeCanonique[] = CATALOGUE.etapes.map((e) => ({ code: e.code as EtapeCode, libelle: e.libelle, rang: e.rang ?? null, protegee: PROTEGEES.includes(e.code as EtapeCode) }))
const COULEUR_DEFAUT: Record<EtapeCode, CouleurEtape> = {
  PRISE_CONTACT: 'NEUTRE', ADHESION: 'NEUTRE', CADRAGE: 'INFO', LANCEMENT: 'INFO', ATELIERS: 'ACTION', INTEGRATION: 'ACTION', TESTS: 'ALERTE',
  CERTIFICATION: 'SUCCES', APPROBATION_SECURITE: 'CRITIQUE', MISE_EN_PRODUCTION: 'SUCCES', SUIVI: 'ACCENT',
}
/** Couleur de départ d'une étape canonique et sa variable CSS (étapes réelles servies au dossier, D-93). */
export const jetonEtape = (code: EtapeCode) => COULEURS_ETAPE.find((c) => c.code === COULEUR_DEFAUT[code])!.jeton
type Etape = { code: string; libelle: string; couleur: CouleurEtape; canonique: EtapeCode | null }
/** Version 1 des étapes d'une version : les onze étapes canoniques, chacune liée à elle-même. */
const etapesInitiales = (): Etape[] => ETAPES_CANONIQUES.map((e) => ({ code: e.code, libelle: e.libelle, couleur: COULEUR_DEFAUT[e.code], canonique: e.code }))
const CODE_ETAPE = /^[A-Z][A-Z0-9_]{1,39}$/

/** Vue d'une étape (EtapeVue) : jeton, canonique effective (celle de l'étape précédente à défaut, le cadrage en tête). */
export function vuesEtapes(etapes: Etape[], ops: ReferenceOperation[], brouillon: boolean): EtapeReferentiel[] {
  let precedente: EtapeCode | null = null
  return etapes.map((e, i) => {
    const effective: EtapeCode = e.canonique ?? precedente ?? 'CADRAGE'
    precedente = effective
    const n = ops.filter((o) => o.etape === e.code).length
    const protegee = !!e.canonique && PROTEGEES.includes(e.canonique)
    return {
      code: e.code, libelle: e.libelle, ordre: i + 1, couleur: e.couleur, jeton: COULEURS_ETAPE.find((c) => c.code === e.couleur)!.jeton, canonique: e.canonique,
      canoniqueEffective: effective, protegee, nombreOperations: n, supprimable: brouillon && n === 0 && !protegee,
    }
  })
}

// ---------------------------------------------------------------------------------- lecture filtrée (D-89 §1)

/** Lecture complète : READ_ALL ou un droit d'édition, de publication ou de migration. */
const LECTURE_COMPLETE = ['HOM_REFERENTIEL_READ_ALL', 'HOM_REFERENTIEL_EDIT', 'HOM_REFERENTIEL_PUBLISH', 'HOM_REFERENTIEL_MIGRATE']
/** Fonctions d'un lecteur : déduites de ses profils (catalogue), plus CHEFS_DEPARTEMENT pour un chef de département. */
export function fonctionsDesProfils(profils: string[]): Fonction[] {
  const f = CATALOGUE.fonctions.filter((x) => x.profils?.some((p) => profils.includes(p))).map((x) => x.code as Fonction)
  if (profils.includes('ROLE_CHEF_DES') || profils.includes('ROLE_CHEF_DDRM')) f.push('CHEFS_DEPARTEMENT')
  return [...new Set(f)]
}
const concerne = (o: ReferenceOperation, f: Fonction[]) =>
  [o.responsable, o.validateur, ...(o.consultes ?? []), ...(o.informes ?? []), ...(o.externes ?? []), ...(o.livrables ?? []).map((l) => l.deposant)].some((x) => !!x && f.includes(x))
/** Fantôme : code et libellé seulement (OperationVue.fantome). */
const fantomeDe = (o: ReferenceOperation): ReferenceOperation => ({
  code: o.code, libelle: o.libelle, description: null, etape: null, segment: null, ordre: null, responsable: null, validateur: null, consultes: [], informes: [], externes: [],
  predecesseurs: [], delaiJours: null, grain: null, plateforme: null, cle: false, secret: false, oubliSansSymptome: false, pivot: false, valeurReference: null, livrables: [],
  geste: null, jalon: null, declencheur: null, particularite: null, position: null, phase: null, partie: null, serviceCode: null, natureCouverture: null, idScenario: null,
  fonctionResponsable: null, preuveAttendue: null, grille: null, fantome: true,
})
/** Opérations retenues pour le lecteur : toutes, ou les siennes et leurs voisines directes en fantômes. */
export function visibles(ops: ReferenceOperation[], profils: string[] | undefined): { filtre: FiltreReferentiel; retenues: ReferenceOperation[]; codes: Set<string> } {
  const permissions = new Set((profils ?? []).flatMap((p) => PROFILS[p] ?? []))
  if (!profils?.length || LECTURE_COMPLETE.some((p) => permissions.has(p))) return { filtre: 'COMPLET', retenues: ops, codes: new Set(ops.map((o) => o.code)) }
  const f = fonctionsDesProfils(profils)
  const codes = new Set(ops.filter((o) => concerne(o, f)).map((o) => o.code))
  const voisins = new Set(ops.filter((o) => codes.has(o.code)).flatMap((o) => [...o.predecesseurs, ...ops.filter((x) => x.predecesseurs.includes(o.code)).map((x) => x.code)]))
  return { filtre: 'CONCERNE', codes, retenues: ops.filter((o) => codes.has(o.code) || voisins.has(o.code)).map((o) => (codes.has(o.code) ? o : fantomeDe(o))) }
}

type Rapport = { anomalies: AnomalieGraphe[]; ordre: string[]; couches: Map<string, number>; cycle: string[] }

/**
 * Gestes de certification attendus à la publication (gimac.homologation.referentiel.gestes-certification de
 * MS-HOMOLOGATION, HOM-02) : pour chaque jalon porté, déclencheurs qu'au moins une opération doit porter ; « avecAttestations »
 * s'ajoute quand le modèle du type de projet exige des attestations (AttestationsTypeProjet).
 */
export const GESTES_CERTIFICATION: { toujours: Partial<Record<Jalon, string[]>>; avecAttestations: Partial<Record<Jalon, string[]>> } = {
  toujours: { CERTIFIEE: ['certification.pv-established.v1', 'certification.pv-signed.v1'], APPROUVEE_SECURITE: ['certification.security-approved.v1'] },
  avecAttestations: { APPROUVEE_SECURITE: ['certification.change-record-constituted.v1', 'certification.attestations-archived.v1'] },
}
/** Contexte du contrôle : catalogue des fonctions (actives, externes) et attestations exigées par le modèle du type. */
export type ContexteControle = { fonctions?: { code: string; actif?: boolean; externe?: boolean }[]; attestations?: boolean }
const LIBELLES_JALON: Record<Jalon, string> = { APTE_CERTIFICATION: 'Apte à la certification', CERTIFIEE: 'Certifiée', APPROUVEE_SECURITE: 'Approuvée par la sécurité', EN_PRODUCTION: 'En production' }

/**
 * Contrôle du graphe (ReferentialGraphValidator) : Kahn, cycle, prédécesseurs, rôles, jalons, livrables, secrets, fonctions
 * du catalogue, signatures (D-91) et gestes de certification attendus (GESTE_CERTIFICATION_ABSENT, HOM-01, HOM-02).
 */
export function controlerGraphe(ops: ReferenceOperation[], ctx: ContexteControle = {}): Rapport {
  const anomalies: AnomalieGraphe[] = []
  const ajouter = (type: TypeAnomalie, codes: string[], motif: string) => anomalies.push({ type, codes, motif })
  const parCode = new Map(ops.map((o) => [o.code, o]))
  const catalogue = ctx.fonctions ?? CATALOGUE.fonctions
  const active = (f: string | null | undefined) => !!f && catalogue.some((x) => x.code === f && x.actif !== false)
  const externe = (f: string) => !!catalogue.find((x) => x.code === f)?.externe
  const vus = new Set<string>()
  ops.forEach((o) => {
    if (vus.has(o.code)) ajouter('CODE_DUPLIQUE', [o.code], `Code ${o.code} employé plusieurs fois.`)
    vus.add(o.code)
  })
  const preds = (o: ReferenceOperation) => o.predecesseurs.filter((p) => parCode.has(p))
  ops.forEach((o) => {
    o.predecesseurs.filter((p) => !parCode.has(p)).forEach((p) => ajouter('PREDECESSEUR_INCONNU', [o.code, p], `Prédécesseur ${p} inconnu.`))
    preds(o).forEach((p) => {
      const sp = SEGMENTS.indexOf(parCode.get(p)!.segment ?? 'AMONT')
      if (o.segment && sp > SEGMENTS.indexOf(o.segment)) ajouter('SEGMENT_INCOHERENT', [o.code, p], `${p} appartient à un segment postérieur à celui de ${o.code}.`)
    })
    if (!o.responsable) ajouter('RESPONSABLE_ABSENT', [o.code], `${o.code} n'a pas de responsable (R).`)
    if (!o.validateur) ajouter('VALIDATEUR_ABSENT', [o.code], `${o.code} n'a pas de validateur (V).`)
    else if (o.validateur === o.responsable) ajouter('VALIDATEUR_EGAL_RESPONSABLE', [o.code], `Le validateur de ${o.code} est aussi son responsable.`)
    if (o.geste === 'EVENEMENT_EXTERNE' && !(o.declencheur && DECLENCHEURS[o.declencheur])) ajouter('DECLENCHEUR_INCONNU', [o.code], `${o.code} attend un événement inconnu.`)
    if (o.secret && o.valeurReference) ajouter('SECRET_AVEC_VALEUR', [o.code], `${o.code} est secrète : aucune valeur de référence n'est admise.`)
    const lv = o.livrables.map((l) => l.code)
    if (new Set(lv).size !== lv.length) ajouter('LIVRABLE_DUPLIQUE', [o.code], `${o.code} a deux livrables au même code.`)
    // Fonctions citées (R, V, C, I, E, déposants, signataires) actives au catalogue (ReferenceOperation.fonctionsCitees).
    const citees = [...new Set([o.responsable, o.validateur, ...(o.consultes ?? []), ...(o.informes ?? []), ...(o.externes ?? []),
      ...o.livrables.map((l) => l.deposant), ...(o.signatures ?? []).map((s) => s.signataire)].filter((f): f is Fonction => !!f))]
    citees.filter((f) => !active(f)).forEach((f) => ajouter('FONCTION_INCONNUE', [o.code, f], 'Fonction absente du catalogue ou désactivée'))
  })
  // Signatures (ReferentialGraphValidator.controlerSignatures) : après le tri, l'antériorité d'un livrable porté se lit dans le DAG.
  const controlerSignatures = (acyclique: boolean, ancetre: (a: string, b: string) => boolean) => ops.forEach((o) => {
    const codes = new Set<string>()
    ;(o.signatures ?? []).forEach((s) => {
      if (codes.has(s.code)) ajouter('SIGNATURE_DUPLIQUEE', [o.code, s.code], 'Code de signature présent plusieurs fois')
      codes.add(s.code)
      if (!s.signataire || !active(s.signataire)) ajouter('SIGNATAIRE_INCONNU', s.signataire ? [o.code, s.code, s.signataire] : [o.code, s.code], 'Le signataire doit être une fonction active du catalogue')
      else if (externe(s.signataire)) ajouter('SIGNATAIRE_EXTERNE', [o.code, s.code, s.signataire], 'Une signature d\'opération est donnée par un membre du GIMAC')
      if (!s.porte.length) ajouter('SIGNATURE_SANS_LIVRABLE', [o.code, s.code], 'Une signature porte au moins un livrable')
      s.porte.forEach((ref) => {
        const i = ref.indexOf('/')
        const op = i < 0 ? null : ref.slice(0, i)
        const livrable = i < 0 ? ref : ref.slice(i + 1)
        const porteur = op === null ? o : parCode.get(op)
        const valide = !!porteur && porteur.livrables.some((l) => l.code === livrable) && (op === null || (acyclique && ancetre(op, o.code)))
        if (!valide) ajouter('SIGNATURE_PORTEE_INVALIDE', [o.code, s.code, ref], 'Livrable porté inconnu, ou d\'une opération qui n\'est pas antérieure')
      })
    })
  })
  // Kahn : ordre topologique et couche (plus long chemin depuis une entrée).
  const degre = new Map(ops.map((o) => [o.code, preds(o).length]))
  const couches = new Map<string, number>()
  const file = ops.filter((o) => degre.get(o.code) === 0).map((o) => o.code)
  file.forEach((c) => couches.set(c, 0))
  if (ops.length && !file.length) ajouter('SANS_ENTREE', [], 'Aucune opération sans prédécesseur.')
  const ordre: string[] = []
  while (file.length) {
    const c = file.shift()!
    ordre.push(c)
    ops.filter((o) => preds(o).includes(c)).forEach((s) => {
      couches.set(s.code, Math.max(couches.get(s.code) ?? 0, (couches.get(c) ?? 0) + 1))
      degre.set(s.code, degre.get(s.code)! - 1)
      if (degre.get(s.code) === 0) file.push(s.code)
    })
  }
  let cycle: string[] = []
  if (ordre.length < ops.length) {
    const reste = ops.filter((o) => !ordre.includes(o.code))
    // Suite du cycle : en remontant les prédécesseurs restants depuis la première opération bloquée.
    const chemin: string[] = []
    let c: string | undefined = reste[0].code
    while (c && !chemin.includes(c)) {
      chemin.push(c)
      c = preds(parCode.get(c)!).find((p) => !ordre.includes(p))
    }
    cycle = c ? chemin.slice(chemin.indexOf(c)).reverse() : chemin
    ajouter('CYCLE', cycle, `Cycle : ${cycle.join(' → ')}.`)
  }
  // Jalons : au plus une fois chacun, jamais un jalon antérieur après un jalon postérieur dans le DAG.
  JALONS.forEach((j) => {
    const porteurs = ops.filter((o) => o.jalon === j).map((o) => o.code)
    if (porteurs.length > 1) ajouter('JALON_DUPLIQUE', porteurs, `Jalon ${j} porté par ${porteurs.join(', ')}.`)
  })
  const ancetres = (code: string, acc = new Set<string>()): Set<string> => {
    preds(parCode.get(code)!).forEach((p) => {
      if (!acc.has(p)) {
        acc.add(p)
        ancetres(p, acc)
      }
    })
    return acc
  }
  if (!cycle.length) {
    ops.filter((o) => o.jalon).forEach((o) => {
      ancetres(o.code).forEach((a) => {
        const ja = parCode.get(a)!.jalon
        if (ja && JALONS.indexOf(ja) > JALONS.indexOf(o.jalon!)) ajouter('JALON_DESORDONNE', [a, o.code], `Le jalon ${ja} de ${a} précède le jalon ${o.jalon} de ${o.code}.`)
      })
    })
  }
  controlerSignatures(!cycle.length, (a, b) => !cycle.length && ancetres(b).has(a))
  // GESTE_CERTIFICATION_ABSENT : un jalon porté sans les déclencheurs des gestes de certification qu'il exige.
  const declencheurs = new Set(ops.map((o) => o.declencheur).filter(Boolean))
  JALONS.filter((j) => ops.some((o) => o.jalon === j)).forEach((j) => {
    const attendus = [...(GESTES_CERTIFICATION.toujours[j] ?? [])]
    if (ctx.attestations !== false) (GESTES_CERTIFICATION.avecAttestations[j] ?? []).filter((d) => !attendus.includes(d)).forEach((d) => attendus.push(d))
    const absents = attendus.filter((d) => !declencheurs.has(d))
    if (absents.length) {
      ajouter('GESTE_CERTIFICATION_ABSENT', [j, ...absents],
        `Le jalon « ${LIBELLES_JALON[j]} » exige une opération qui porte chaque geste de certification : ${absents.map((d) => DECLENCHEURS[d] ?? d).join(', ')}.`)
    }
  })
  return { anomalies, ordre, couches, cycle }
}

/**
 * Attributs requis à la publication (ReferentialCompletenessChecker) : ceux du service, l'étape tenant lieu de phase et
 * les attributs historiques dérivés de l'étape et du responsable comme ReferenceOperation.deriverAttributsHistoriques.
 */
export function manquants(ops: ReferenceOperation[]) {
  const vide = (v: unknown) => v === null || v === undefined || (typeof v === 'string' && !v.trim()) || (typeof v === 'object' && !Array.isArray(v) && !Object.keys(v as object).length)
  return ops
    .map((o) => {
      const derive = !!o.etape
      const attributs: string[] = []
      if (vide(o.libelle)) attributs.push('libelle')
      if (vide(o.phase) && !derive) attributs.push('phase')
      if (!o.segment) attributs.push('segment')
      if (!o.partie && !(derive && o.responsable)) attributs.push('partie')
      if (vide(o.serviceCode) && !derive) attributs.push('serviceCode')
      if (!o.natureCouverture && !derive) attributs.push('natureCouverture')
      if (vide(o.preuveAttendue) && !derive) attributs.push('preuveAttendue')
      if (vide(o.fonctionResponsable) && !(derive && o.responsable)) attributs.push('fonctionResponsable')
      if (o.natureCouverture === 'CERTIFICATION' && vide(o.idScenario)) attributs.push('idScenario')
      return { code: o.code, attributs }
    })
    .filter((m) => m.attributs.length)
    .sort((a, b) => a.code.localeCompare(b.code))
}

/** Successeurs déduits des prédécesseurs. */
const avecSuccesseurs = (ops: ReferenceOperation[]) => ops.map((o) => ({ ...o, successeurs: ops.filter((x) => x.predecesseurs.includes(o.code)).map((x) => x.code).sort() }))

/** Graphe étendu (GET /referentiels/{id}/graphe). */
export function calculerGraphe(v: ReferentielVersion, ops: ReferenceOperation[], d89: { etapes?: EtapeReferentiel[]; profils?: string[]; controle?: ContexteControle } = {}): GrapheReferentiel {
  const vue = visibles(ops, d89.profils)
  const complet = calculerGrapheComplet(v, ops, d89.controle)
  if (vue.filtre === 'COMPLET') return { ...complet, ...(d89.etapes ? { etapes: d89.etapes } : {}), filtre: 'COMPLET' }
  // Lecture concernée : nœuds visibles et fantômes, arcs touchant une opération visible, contrôles réduits aux visibles.
  const parCode = new Map(complet.noeuds.map((n) => [n.code, n]))
  const garde = (codes: string[]) => codes.filter((c) => vue.codes.has(c))
  return {
    ...complet,
    noeuds: vue.retenues.map((o) => (o.fantome ? { ...o, couche: null, debutAuPlusTot: null, finAuPlusTot: null, critique: false, anomalies: [] } : parCode.get(o.code)!)),
    arcs: complet.arcs.filter((a) => (vue.codes.has(a.de) || vue.codes.has(a.vers)) && vue.retenues.some((o) => o.code === a.de) && vue.retenues.some((o) => o.code === a.vers)),
    anomalies: complet.anomalies.filter((a) => a.codes.some((c) => vue.codes.has(c))),
    manquants: complet.manquants.filter((m) => vue.codes.has(m.code)),
    cheminCritique: { ...complet.cheminCritique, codes: garde(complet.cheminCritique.codes) },
    ...(d89.etapes ? { etapes: d89.etapes } : {}),
    filtre: 'CONCERNE',
  }
}

function calculerGrapheComplet(v: ReferentielVersion, ops: ReferenceOperation[], ctx: ContexteControle = {}): GrapheReferentiel {
  const r = controlerGraphe(ops, ctx)
  const parCode = new Map(ops.map((o) => [o.code, o]))
  const debut = new Map<string, number>()
  const fin = new Map<string, number>()
  r.ordre.forEach((c) => {
    const o = parCode.get(c)!
    const d = Math.max(0, ...o.predecesseurs.filter((p) => fin.has(p)).map((p) => fin.get(p)!))
    debut.set(c, d)
    fin.set(c, d + (o.delaiJours ?? 0))
  })
  // Chemin critique : depuis la fin la plus tardive, en remontant le prédécesseur qui la fixe.
  const chemin: string[] = []
  let c: string | undefined = [...fin.entries()].sort((a, b) => b[1] - a[1] || r.ordre.indexOf(a[0]) - r.ordre.indexOf(b[0]))[0]?.[0]
  while (c) {
    chemin.unshift(c)
    const d: number = debut.get(c) ?? 0
    c = parCode.get(c)!.predecesseurs.find((p: string): boolean => fin.get(p) === d && d > 0)
  }
  const critiques = new Set(chemin)
  const arcsCritiques = new Set(chemin.slice(1).map((x, i) => `${chemin[i]}>${x}`))
  const noeuds: NoeudGraphe[] = avecSuccesseurs(ops)
    .sort((a, b) => (a.ordre ?? 0) - (b.ordre ?? 0) || a.code.localeCompare(b.code))
    .map((o) => ({
      ...o, couche: r.couches.get(o.code) ?? null, debutAuPlusTot: debut.get(o.code) ?? null, finAuPlusTot: fin.get(o.code) ?? null, critique: critiques.has(o.code),
      anomalies: [...new Set(r.anomalies.filter((a) => a.codes.includes(o.code)).map((a) => a.type))],
    }))
  return {
    versionId: v.id, typeProjet: v.typeProjet, numero: v.numero, etat: v.etat, piloteId: v.piloteId ?? null, noeuds,
    arcs: ops.flatMap((o) => o.predecesseurs.filter((p) => parCode.has(p)).map((p) => ({ de: p, vers: o.code, critique: arcsCritiques.has(`${p}>${o.code}`) }))),
    anomalies: r.anomalies, manquants: manquants(ops), cheminCritique: { codes: chemin, dureeJours: Math.max(0, ...fin.values()) }, actionsPermises: v.actionsPermises,
  }
}

/** Restitution du contrôle à blanc (ControleVue) : anomalies brutes et « conforme » (publiable) comme le service. */
export function controleDe(ops: ReferenceOperation[], ctx: ContexteControle = {}): ControleResultat {
  const r = controlerGraphe(ops, ctx)
  const m = manquants(ops)
  return {
    acyclique: !r.cycle.length, ordreTopologique: r.ordre, cycle: r.cycle,
    incoherences: r.anomalies.filter((a) => a.type !== 'CYCLE').map((a) => ({ code: a.codes[0] ?? '', predecesseur: a.codes[1] ?? '', motif: a.motif })),
    manquants: m, anomalies: r.anomalies, conforme: !r.anomalies.length && !m.length,
  }
}

// ------------------------------------------------------------------------------------------------ jeu et état

export function versionsInitiales(): { versions: ReferentielVersion[]; operations: Record<string, ReferenceOperation[]> } {
  const carte = versionCR('CARTE_INTERBANCAIRE')
  // Brouillon 4 : C9 retirée (C10 rattachée à C2), C11 ajoutée sans validateur (anomalie montrée au modélisateur).
  const brouillon = carte
    .filter((o) => o.code !== 'C9')
    .map((o) => (o.code === 'C10' ? { ...o, predecesseurs: ['C2'] } : o.code === '27' ? { ...o, predecesseurs: ['C11'] } : o))
  const c10 = carte.find((o) => o.code === 'C10')!
  brouillon.splice(brouillon.findIndex((o) => o.code === 'C10') + 1, 0, {
    ...c10, code: 'C11', libelle: 'Contrôle croisé de la table des BIN après chargement', predecesseurs: ['C8', 'C10'], validateur: null, ordre: c10.ordre! + 1,
    position: { x: c10.position!.x + 150, y: c10.position!.y + 40 }, description: null,
  })
  const v = (id: string, typeProjet: TypeProjet, numero: number | null, libelle: string, etat: ReferentielVersion['etat'], dateCreation: string, datePublication: string | null,
    versionSourceId: string | null, versionActiveId: string | null, actionsPermises: string[], piloteId: string | null = null): ReferentielVersion => ({
    id, typeProjet, numero, libelle, etat, dateCreation, datePublication, versionSourceId, versionActiveId, transitionsPermises: etat === 'BROUILLON' ? ['PUBLIEE'] : [],
    actionsPermises, piloteId,
  })
  const versions = [
    v('REF-CI-3', 'CARTE_INTERBANCAIRE', 3, 'Carte interbancaire, version 3', 'PUBLIEE', '02/06/2026 09:10', '15/06/2026 16:40', 'REF-CI-2', 'REF-CI-3', ['DERIVER', 'CONTROLER']),
    v('REF-CI-4', 'CARTE_INTERBANCAIRE', null, 'Carte interbancaire, brouillon 4', 'BROUILLON', '22/09/2026 11:02', null, 'REF-CI-3', 'REF-CI-3', ['CONTROLER', 'PUBLIER', 'MODIFIER', 'MIGRER'], 'U-101'),
    v('REF-CI-2', 'CARTE_INTERBANCAIRE', 2, 'Carte interbancaire, version 2', 'RETIREE', '10/01/2026 08:30', '20/01/2026 10:00', null, 'REF-CI-3', []),
    v('REF-PM-2', 'PAIEMENT_MOBILE', 2, 'Paiement mobile, version 2', 'PUBLIEE', '04/05/2026 14:00', '12/05/2026 09:15', null, 'REF-PM-2', ['DERIVER', 'CONTROLER']),
    v('REF-DL-1', 'DELEGATAIRE', null, 'Délégataire, reprise de l\'annexe A', 'BROUILLON', '01/10/2026 08:00', null, null, null, ['CONTROLER', 'PUBLIER', 'MODIFIER'], 'U-102'),
  ]
  return {
    versions,
    operations: {
      'REF-CI-3': carte, 'REF-CI-4': brouillon, 'REF-CI-2': carte.filter((o) => o.code !== '35'),
      'REF-PM-2': versionCR('PAIEMENT_MOBILE'), 'REF-DL-1': versionCR('DELEGATAIRE'),
    },
  }
}

/**
 * Type de projet de démonstration créé dans la plateforme (D-91, modèle GENERIQUE) : « Agrégateur de paiement », version 1
 * publiée, courte, qui parcourt tout le flux de certification (D-98) : preuve du cadrage, essais, constat de la couverture,
 * procès-verbal établi puis signé des deux parties, dossier et fiche de changement, approbation de la sécurité, bascule en
 * production. Chaque opération porte son geste, son jalon, son déclencheur et sa vue en données, sans rang codé.
 */
export const TYPE_DEMO_COURT = 'AGREGATEUR'
// HOM-18 : aucune position enregistrée, les cartes sont disposées dans les couloirs de leur étape (disposition automatique).
export function versionAgregateur(): ReferenceOperation[] {
  const o = (code: string, i: number, libelle: string, etape: EtapeCode, responsable: Fonction, validateur: Fonction | null, geste: Geste, predecesseurs: string[],
    x: Partial<ReferenceOperation> = {}): ReferenceOperation => ({
    code, libelle, description: null, etape, segment: 'AMONT', ordre: i, responsable, validateur, consultes: [], informes: ['CHEF_DDRM'], externes: [], predecesseurs,
    delaiJours: 5, grain: 'OPERATION', plateforme: null, cle: false, secret: false, oubliSansSymptome: false, pivot: false, valeurReference: null, livrables: [], geste,
    jalon: null, declencheur: null, particularite: null, position: null, phase: null, partie: 'DES', serviceCode: 'TRANSVERSE', natureCouverture: 'AUCUNE',
    preuveAttendue: {}, idScenario: null, fonctionResponsable: responsable, grille: null, vue: 'AUCUNE', consultesRecoiventTache: false, signatures: [], ...x,
  })
  return [
    o('A1', 1, 'Arrêter le périmètre et le calendrier du projet', 'CADRAGE', 'DDRM', 'CONDUCTEUR', 'DEPOSER_PREUVE', []),
    o('A2', 2, 'Exécuter les essais de certification', 'TESTS', 'SNA', 'CONDUCTEUR', 'DEPOSER_PREUVE', ['A1']),
    // HOM-01 : chaque opération a un validateur distinct de son responsable, sans quoi le service refuserait la publication.
    o('A3', 3, 'Constater la couverture de certification', 'CERTIFICATION', 'SNA', 'CONDUCTEUR', 'CONSTATER_COUVERTURE', ['A2'], { jalon: 'APTE_CERTIFICATION' }),
    o('A4', 4, 'Établir le procès-verbal de certification', 'CERTIFICATION', 'CONDUCTEUR', 'CHEF_DES', 'EVENEMENT_EXTERNE', ['A3'], { declencheur: 'certification.pv-established.v1', vue: 'DECISION' }),
    o('A5', 5, 'Signer le procès-verbal de certification', 'CERTIFICATION', 'CHEF_DES', 'CONDUCTEUR', 'EVENEMENT_EXTERNE', ['A4'],
      { declencheur: 'certification.pv-signed.v1', jalon: 'CERTIFIEE', vue: 'DECISION', externes: ['PARTICIPANT'], pivot: true }),
    o('A6', 6, 'Constituer le dossier de changement', 'APPROBATION_SECURITE', 'PRODUCTION_MONETIQUE', 'CONDUCTEUR', 'EVENEMENT_EXTERNE', ['A5'],
      { declencheur: 'certification.change-record-constituted.v1', vue: 'DECISION' }),
    o('A7', 7, 'Faire approuver le changement par la sécurité', 'APPROBATION_SECURITE', 'PRODUCTION_MONETIQUE', 'RSSI', 'EVENEMENT_EXTERNE', ['A6'],
      { declencheur: 'certification.security-approved.v1', jalon: 'APPROUVEE_SECURITE', vue: 'DECISION' }),
    o('A8', 8, 'Basculer le participant en production', 'MISE_EN_PRODUCTION', 'PRODUCTION_MONETIQUE', 'CONDUCTEUR', 'DEPOSER_PREUVE', ['A7'], { jalon: 'EN_PRODUCTION' }),
  ]
}

type Refus = (status: number, code: string, message: string, details?: Record<string, unknown>) => ApiError
type Attendre = <T>(v: T | (() => T)) => Promise<T>
const CODE = /^[A-Z0-9][A-Z0-9_-]*$/

/** Port des référentiels simulé, état propre à l'instance (versions, opérations, positions). */
export function referentielsSimules(attendre: Attendre, refus: Refus, o: {
  utilisateur?: string; profils?: string[]; maintenant: () => string; liaisonsActives: (versionId: string) => number; liaisonsDuType?: (type: string) => number
}) {
  const { versions, operations } = versionsInitiales()
  // D-98 : type créé dans la plateforme, version 1 publiée (démonstration du flux complet de certification).
  versions.push({
    id: 'REF-AG-1', typeProjet: TYPE_DEMO_COURT, numero: 1, libelle: 'Agrégateur de paiement, version 1', etat: 'PUBLIEE', dateCreation: '01/09/2026 09:00',
    datePublication: '15/09/2026 10:00', versionSourceId: null, versionActiveId: 'REF-AG-1', transitionsPermises: [], actionsPermises: ['DERIVER', 'CONTROLER'], piloteId: null,
  })
  operations['REF-AG-1'] = versionAgregateur()
  // Étapes de chaque version (D-89) : les onze étapes canoniques au départ.
  const etapes: Record<string, Etape[]> = Object.fromEntries(versions.map((v) => [v.id, etapesInitiales()]))
  const etapesDe = (id: string) => etapes[id] ?? (etapes[id] = etapesInitiales())
  const vuesDe = (id: string) => vuesEtapes(etapesDe(id), operations[id] ?? [], version(id).etat === 'BROUILLON')
  const etape = (id: string, code: string) => {
    const e = etapesDe(id).find((x) => x.code === code)
    if (!e) throw refus(404, 'HOM_ETAPE_NOT_FOUND', `Étape ${code} introuvable.`)
    return e
  }
  const vueEtape = (id: string, code: string) => vuesDe(id).find((x) => x.code === code)!
  const libelleEtape = (l: string) => {
    const libelle = (l ?? '').trim()
    if (!libelle || libelle.length > 120) throw refus(400, 'VALIDATION_ERROR', 'Libellé d\'étape : 1 à 120 caractères.')
    return libelle
  }
  const etapeConnue = (id: string, s: SaisieOperation) => {
    if (s.etape && !etapesDe(id).some((e) => e.code === s.etape)) throw refus(422, 'HOM_ETAPE_INCONNUE', `L'étape ${s.etape} n'existe pas dans cette version.`)
  }
  const lecture = (id: string) => visibles(operations[id] ?? [], o.profils)
  /** Export CSV (séparateur point-virgule) : mêmes colonnes et même filtre que le service. */
  const csv = (id: string) => {
    const cellule = (v: unknown) => (v === null || v === undefined ? '' : Array.isArray(v) ? v.join(',') : String(v).replace(/[;\n]/g, ' '))
    const lignes = avecSuccesseurs(lecture(id).retenues).map((x) => [x.code, x.libelle, x.etape, x.segment, x.responsable, x.validateur, x.consultes, x.informes, x.externes, x.predecesseurs, x.delaiJours, !!x.fantome].map(cellule).join(';'))
    return ['code;libelle;etape;segment;responsable;validateur;consultes;informes;externes;predecesseurs;delaiJours;fantome', ...lignes].join('\n') + '\n'
  }
  let numero = 0
  const nouvelId = () => `REF-${Date.now().toString(36).toUpperCase()}-${++numero}`
  // Types de projet et catalogue des fonctions (D-91) : données au lieu d'énumérations.
  const types: Omit<TypeProjetVue, 'referentielActif' | 'brouillonId' | 'fige' | 'proposable' | 'nombreLiaisons' | 'libelleModele'>[] = MODELES_LIBELLES.filter(([c]) => c !== 'GENERIQUE')
    .map(([code, libelle]) => ({ code, libelle, description: null, modele: code, actif: true, creeLe: '01/01/2026 08:00', creePar: 'U-001', modifieLe: null, modifiePar: null }))
  types.push({ code: TYPE_DEMO_COURT, libelle: 'Agrégateur de paiement', description: 'Type créé dans la plateforme (modèle générique) : parcours court de démonstration.', modele: 'GENERIQUE',
    actif: true, creeLe: '01/09/2026 09:00', creePar: 'U-101', modifieLe: null, modifiePar: null })
  // Permissions de départ (§8.8) : celle que seuls les profils de la fonction portent dans le domaine livré.
  const PERMISSIONS_DEPART: Record<string, string[]> = {
    CHEF_DDRM: ['PAR_TRANSITION_APPROVE'], CHEF_DES: ['CER_QUITUS_TECHNIQUE_SIGN'], CHEFS_DEPARTEMENT: ['PAR_TRANSITION_APPROVE', 'CER_QUITUS_TECHNIQUE_SIGN'],
    CONDUCTEUR: ['HOM_PROJECT_CONDUCT'], SNA: ['CER_DOSSIER_VERIFY'], RSSI: ['CER_SECURITY_VERIFY'], CONFORMITE: ['CER_QUITUS_CONFORMITE_SIGN'], DG: ['CER_QUITUS_DG_SIGN'],
    // Fonctions opérationnelles (D-97) : une permission dédiée chacune.
    DDRM: ['HOM_FUNCTION_DDRM'], PRODUCTION_MONETIQUE: ['HOM_FUNCTION_PRODUCTION'], POLE_RESEAU: ['HOM_FUNCTION_NETWORK'], POLE_SYSTEME: ['HOM_FUNCTION_SYSTEM'],
    EXPLOITATION: ['HOM_FUNCTION_OPERATIONS'],
  }
  const fonctions: FonctionCatalogue[] = CATALOGUE.fonctions.map((f) => ({ code: f.code, libelle: f.libelle, profils: [...(f.profils ?? [])], permissions: [...(PERMISSIONS_DEPART[f.code] ?? [])], externe: !!f.externe, actif: true, reprise: true }))
  /** Contexte du contrôle d'une version : catalogue des fonctions courant, attestations exigées par le modèle du type (AttestationsTypeProjet). */
  const contexte = (id: string): ContexteControle => {
    const modele = types.find((t) => t.code === version(id).typeProjet)?.modele
    return { fonctions, attestations: modele !== 'GENERIQUE' }
  }
  const verifierPermissions = (l: string[] | undefined) => {
    if (l && (l.length > 20 || l.some((p) => !/^[A-Z][A-Z0-9_]{1,79}$/.test(p)))) throw refus(422, 'HOM_FONCTION_PERMISSION_INVALIDE', 'Permission invalide : 20 codes au plus, majuscules, chiffres ou souligné.')
  }
  const vueType = (t: (typeof types)[number]): TypeProjetVue => {
    const duType = versions.filter((v) => v.typeProjet === t.code)
    const publiee = duType.find((v) => v.etat === 'PUBLIEE')
    return {
      ...t, libelleModele: MODELES_LIBELLES.find(([c]) => c === t.modele)?.[1], referentielActif: publiee ? { id: publiee.id, version: publiee.numero ?? 1 } : null,
      brouillonId: duType.filter((v) => v.etat === 'BROUILLON').at(-1)?.id ?? null, fige: duType.some((v) => v.numero !== null), proposable: t.actif && !!publiee,
      nombreLiaisons: o.liaisonsDuType?.(t.code) ?? 0,
    }
  }
  const typeExistant = (code: string) => {
    const t = types.find((x) => x.code === code)
    if (!t) throw refus(404, 'HOM_TYPE_PROJET_INTROUVABLE', 'Type de projet introuvable.')
    return t
  }
  const exigerMotif = (motif: string | undefined) => {
    if (!motif?.trim() || motif.length > 500) throw refus(422, 'HOM_MOTIF_REQUIS', 'Un motif (1 à 500 caractères) est exigé.')
  }
  const creerType = (c: CreationTypeProjet): TypeProjetVue => {
    if (!/^[A-Z][A-Z0-9_]{2,39}$/.test(c.code ?? '')) throw refus(422, 'HOM_TYPE_PROJET_CODE_INVALIDE', 'Code invalide : majuscules, chiffres ou souligné, 3 à 40 caractères.')
    if (!c.libelle?.trim() || c.libelle.length > 120) throw refus(422, 'HOM_TYPE_PROJET_LIBELLE_INVALIDE', 'Libellé : 1 à 120 caractères.')
    if ((c.description ?? '').length > 1000) throw refus(422, 'HOM_TYPE_PROJET_DESCRIPTION_INVALIDE', 'Description : 1000 caractères au plus.')
    if (!MODELES_TYPE_PROJET.includes(c.modele)) throw refus(422, 'HOM_TYPE_PROJET_MODELE_INVALIDE', 'Modèle inconnu.')
    if (!c.depart?.mode || (c.depart.mode === 'COPIE' && !c.depart.typeSource)) throw refus(422, 'HOM_TYPE_PROJET_DEPART_INVALIDE', 'Départ du référentiel : vierge, ou copie d\'un type existant.')
    if (types.some((x) => x.code === c.code)) throw refus(409, 'HOM_TYPE_PROJET_CODE_PRIS', 'Ce code de type de projet existe déjà.')
    let source: ReferentielVersion | undefined
    if (c.depart.mode === 'COPIE') {
      typeExistant(c.depart.typeSource!)
      source = versions.find((v) => v.typeProjet === c.depart.typeSource && v.etat === 'PUBLIEE')
      if (!source) throw refus(409, 'HOM_TYPE_PROJET_SOURCE_NON_PUBLIE', 'Le type source n\'a pas de version publiée.')
    }
    const t = { code: c.code, libelle: c.libelle.trim(), description: c.description?.trim() || null, modele: c.modele, actif: true, creeLe: o.maintenant(), creePar: o.utilisateur ?? 'U-001', modifieLe: null, modifiePar: null }
    types.push(t)
    const v: ReferentielVersion = {
      id: nouvelId(), typeProjet: c.code, numero: null, libelle: `Référentiel ${t.libelle} (version 1)`, etat: 'BROUILLON', dateCreation: o.maintenant(), datePublication: null,
      versionSourceId: null, versionActiveId: null, transitionsPermises: ['PUBLIEE'], actionsPermises: ['CONTROLER', 'PUBLIER', 'MODIFIER'], piloteId: o.utilisateur ?? 'U-101',
    }
    versions.push(v)
    operations[v.id] = source ? structuredClone(operations[source.id] ?? []) : []
    etapes[v.id] = source ? structuredClone(etapesDe(source.id)) : etapesInitiales()
    return vueType(t)
  }
  const version = (id: string) => {
    const v = versions.find((x) => x.id === id)
    if (!v) throw refus(404, 'HOM_REFERENTIAL_NOT_FOUND', 'Version introuvable.')
    return v
  }
  const brouillon = (id: string) => {
    const v = version(id)
    if (v.etat !== 'BROUILLON') throw refus(409, 'HOM_REFERENTIAL_FROZEN', 'Une version publiée est figée.')
    return operations[id] ?? (operations[id] = [])
  }
  const trouver = (ops: ReferenceOperation[], code: string) => {
    const op = ops.find((x) => x.code === code)
    if (!op) throw refus(404, 'HOM_OPERATION_NOT_FOUND', `Opération ${code} introuvable.`)
    return op
  }
  const valider = (code: string, s: SaisieOperation) => {
    if (s.secret && s.valeurReference) throw refus(422, 'HOM_OPERATION_SECRET_VALUE', 'Une opération secrète ne relève aucune valeur de référence.')
    ;(s.signatures ?? []).forEach((x, i) => {
      const champ = !/^[A-Z][A-Z0-9_]{1,39}$/.test(x.code) ? 'code' : !x.libelle?.trim() || x.libelle.length > 200 ? 'libelle' : !x.signataire ? 'signataire' : x.ordre < 1 || x.ordre > 99 ? 'ordre' : null
      if (champ) throw refus(422, 'HOM_SIGNATURE_INVALIDE', 'Signature invalide.', { champ: `signatures[${i}].${champ}` })
    })
    if (s.predecesseurs.includes(code)) throw refus(422, 'HOM_OPERATION_SELF_PREDECESSOR', 'Une opération ne peut pas se précéder elle-même.')
  }
  const vers = (code: string, s: SaisieOperation, avant?: ReferenceOperation): ReferenceOperation => ({
    phase: null, partie: null, serviceCode: null, natureCouverture: null, idScenario: null, fonctionResponsable: null, preuveAttendue: null, grille: null,
    ...avant, ...s, code, position: s.position === undefined ? avant?.position ?? null : s.position,
    particularite: s.particularite === undefined ? avant?.particularite ?? null : s.particularite,
  })
  const avecSucc = (ops: ReferenceOperation[], code: string) => avecSuccesseurs(ops).find((x) => x.code === code)!
  /** L'arc de → vers fermerait un cycle si « de » descend déjà de « vers ». */
  const descend = (ops: ReferenceOperation[], depuis: string, cible: string): boolean => {
    const vus = new Set<string>()
    const pile = [depuis]
    while (pile.length) {
      const c = pile.pop()!
      if (c === cible) return true
      if (vus.has(c)) continue
      vus.add(c)
      ops.filter((x) => x.predecesseurs.includes(c)).forEach((x) => pile.push(x.code))
    }
    return false
  }

  const port: Pick<ServiceHomologation,
    'listerReferentiels' | 'lireReferentiel' | 'operationsReferentiel' | 'controlerReferentiel' | 'creerBrouillon' | 'deriverBrouillon' | 'publierReferentiel'
    | 'grapheReferentiel' | 'catalogueReferentiels' | 'creerOperation' | 'enregistrerOperation' | 'supprimerOperation' | 'placerOperations' | 'arcReferentiel' | 'designerPilote'
    | 'liaisonsImpactees' | 'exporterReferentiel' | 'ajouterEtape' | 'modifierEtape' | 'supprimerEtape' | 'ordonnerEtapes'
    | 'typesProjet' | 'creerTypeProjet' | 'modifierTypeProjet' | 'fonctionsReferentiel' | 'creerFonction' | 'modifierFonction'> = {
    typesProjet: (actif) => attendre(() => types.filter((t) => actif === undefined || t.actif === actif).map(vueType)),
    creerTypeProjet: (c) => attendre(() => creerType(c)),
    modifierTypeProjet: (code, m) => attendre(() => {
      const t = typeExistant(code)
      exigerMotif(m.motif)
      if (m.libelle !== undefined && (!m.libelle.trim() || m.libelle.length > 120)) throw refus(422, 'HOM_TYPE_PROJET_LIBELLE_INVALIDE', 'Libellé : 1 à 120 caractères.')
      if (m.modele !== undefined && m.modele !== t.modele && vueType(t).fige) throw refus(409, 'HOM_TYPE_PROJET_FIGE', 'Une version a été publiée : le modèle est figé.')
      Object.assign(t, {
        ...(m.libelle !== undefined ? { libelle: m.libelle.trim() } : {}), ...(m.description !== undefined ? { description: m.description.trim() || null } : {}),
        ...(m.modele !== undefined ? { modele: m.modele } : {}), ...(m.actif !== undefined ? { actif: m.actif } : {}), modifieLe: o.maintenant(), modifiePar: o.utilisateur ?? 'U-001',
      })
      return vueType(t)
    }),
    fonctionsReferentiel: (actif) => attendre(() => fonctions.filter((f) => actif === undefined || f.actif === actif)),
    creerFonction: (c) => attendre(() => {
      if (!/^[A-Z][A-Z0-9_]{1,39}$/.test(c.code ?? '')) throw refus(422, 'HOM_FONCTION_CODE_INVALIDE', 'Code invalide : majuscules, chiffres ou souligné, 2 à 40 caractères.')
      if (!c.libelle?.trim() || c.libelle.length > 120) throw refus(422, 'HOM_FONCTION_LIBELLE_INVALIDE', 'Libellé : 1 à 120 caractères.')
      if (c.profils.some((p) => !/^ROLE_[A-Z][A-Z0-9_]+$/.test(p)) || c.profils.length > 20) throw refus(422, 'HOM_FONCTION_PROFIL_INVALIDE', 'Profil invalide.')
      if (!c.externe && !c.profils.length) throw refus(422, 'HOM_FONCTION_PROFIL_REQUIS', 'Une fonction interne est reliée à au moins un profil.')
      verifierPermissions(c.permissions)
      if (fonctions.some((f) => f.code === c.code)) throw refus(409, 'HOM_FONCTION_CODE_PRIS', 'Ce code de fonction existe déjà.')
      const f: FonctionCatalogue = { code: c.code, libelle: c.libelle.trim(), profils: [...c.profils], permissions: [...(c.permissions ?? [])], externe: c.externe, actif: true, reprise: false, creeLe: o.maintenant(), creePar: o.utilisateur ?? 'U-001', modifieLe: null, modifiePar: null }
      fonctions.push(f)
      return f
    }),
    modifierFonction: (code, m) => attendre(() => {
      const f = fonctions.find((x) => x.code === code)
      if (!f) throw refus(404, 'HOM_FONCTION_INTROUVABLE', 'Fonction introuvable.')
      exigerMotif(m.motif)
      const profils = m.profils ?? f.profils
      if (!(m.externe ?? f.externe) && !profils.length) throw refus(422, 'HOM_FONCTION_PROFIL_REQUIS', 'Une fonction interne est reliée à au moins un profil.')
      verifierPermissions(m.permissions)
      Object.assign(f, { ...(m.libelle !== undefined ? { libelle: m.libelle.trim() } : {}), profils: [...profils], ...(m.permissions !== undefined ? { permissions: [...m.permissions] } : {}), ...(m.externe !== undefined ? { externe: m.externe } : {}),
        ...(m.actif !== undefined ? { actif: m.actif } : {}), modifieLe: o.maintenant(), modifiePar: o.utilisateur ?? 'U-001' })
      return f
    }),
    listerReferentiels: () => attendre(() => versions.map((v) => { const l = lecture(v.id); return { ...v, nombreOperations: l.codes.size, filtre: l.filtre } })),
    lireReferentiel: (id) => attendre(() => { const v = version(id); const l = lecture(id); return { ...v, nombreOperations: l.codes.size, filtre: l.filtre } }),
    operationsReferentiel: (id) => attendre(() => { const l = lecture(id); return { operations: avecSuccesseurs(l.retenues), filtre: l.filtre } }),
    // Le Blob est construit après la latence simulée : la réponse est clonée, un Blob ne se clone pas.
    exporterReferentiel: (id) => attendre(() => {
      const v = version(id)
      return { texte: csv(id), nom: `referentiel-${v.typeProjet.toLowerCase()}-${v.numero === null ? 'brouillon' : `v${v.numero}`}.csv` }
    }).then((r) => ({ blob: new Blob([r.texte], { type: 'text/csv;charset=utf-8' }), empreinte: null, nom: r.nom })),
    ajouterEtape: (id, e) => attendre(() => {
      brouillon(id)
      const l = etapesDe(id)
      if (!CODE_ETAPE.test(e.code ?? '')) throw refus(422, 'HOM_ETAPE_CODE_INVALIDE', 'Code d\'étape invalide : majuscules, chiffres ou souligné, 2 à 40 caractères.')
      if (l.some((x) => x.code === e.code)) throw refus(409, 'HOM_ETAPE_CODE_DUPLIQUE', `L'étape ${e.code} existe déjà dans cette version.`)
      const nouvelle: Etape = { code: e.code, libelle: libelleEtape(e.libelle), couleur: e.couleur ?? 'NEUTRE', canonique: e.canonique ?? null }
      const position = e.ordre && e.ordre >= 1 && e.ordre <= l.length ? e.ordre - 1 : l.length
      l.splice(position, 0, nouvelle)
      return vueEtape(id, e.code)
    }),
    modifierEtape: (id, code, e) => attendre(() => {
      brouillon(id)
      const x = etape(id, code)
      const canonique = e.canonique ?? null
      if (x.canonique && PROTEGEES.includes(x.canonique) && canonique !== x.canonique) throw refus(409, 'HOM_ETAPE_CANONIQUE', 'Cette étape porte un jalon ou le verrou 2 : son étape canonique ne change pas.')
      Object.assign(x, { libelle: libelleEtape(e.libelle), couleur: e.couleur ?? x.couleur, canonique })
      return vueEtape(id, code)
    }),
    supprimerEtape: (id, code) => attendre(() => {
      const ops = brouillon(id)
      const x = etape(id, code)
      if (x.canonique && PROTEGEES.includes(x.canonique)) throw refus(409, 'HOM_ETAPE_CANONIQUE', 'Cette étape porte un jalon ou le verrou 2 : elle ne se retire pas.')
      if (ops.some((op) => op.etape === code)) throw refus(409, 'HOM_ETAPE_NON_VIDE', 'Seule une étape sans opération se retire.')
      etapes[id] = etapesDe(id).filter((y) => y.code !== code)
    }),
    ordonnerEtapes: (id, codes) => attendre(() => {
      brouillon(id)
      const l = etapesDe(id)
      if (codes.length !== l.length || new Set(codes).size !== codes.length || !codes.every((c) => l.some((x) => x.code === c))) {
        throw refus(422, 'HOM_ETAPE_ORDRE_INVALIDE', 'Le nouvel ordre doit citer chaque étape de la version une fois.')
      }
      etapes[id] = codes.map((c) => l.find((x) => x.code === c)!)
      return vuesDe(id)
    }),
    controlerReferentiel: (id) => attendre(() => controleDe(operations[id] ?? [], contexte(id))),
    liaisonsImpactees: (versionId) => attendre({ total: o.liaisonsActives(versionId) }),
    creerBrouillon: (e) => attendre(() => {
      const piloteId = e.piloteId ?? o.utilisateur ?? 'U-101'
      if (!CATALOGUE.pilotes.some((p) => p.id === piloteId) && e.piloteId) throw refus(422, 'HOM_PILOTE_NOT_ELIGIBLE', 'Le pilote désigné ne peut pas modifier les référentiels.')
      const active = versions.find((x) => x.typeProjet === e.typeProjet && x.etat === 'PUBLIEE')
      const v: ReferentielVersion = {
        id: nouvelId(), typeProjet: e.typeProjet as TypeProjet, numero: null, libelle: e.libelle, etat: 'BROUILLON', dateCreation: o.maintenant(),
        datePublication: null, versionSourceId: null, versionActiveId: active?.id ?? null, transitionsPermises: ['PUBLIEE'], actionsPermises: ['CONTROLER', 'PUBLIER', 'MODIFIER'], piloteId,
      }
      versions.push(v)
      operations[v.id] = []
      etapes[v.id] = etapesInitiales()
      return v
    }),
    deriverBrouillon: (source) => attendre(() => {
      const s = version(source)
      const v: ReferentielVersion = {
        ...s, id: nouvelId(), numero: null, etat: 'BROUILLON', libelle: `${s.libelle}, brouillon`, versionSourceId: s.id, datePublication: null,
        dateCreation: o.maintenant(), transitionsPermises: ['PUBLIEE'], actionsPermises: ['CONTROLER', 'PUBLIER', 'MODIFIER'], piloteId: o.utilisateur ?? 'U-101',
        versionActiveId: versions.find((x) => x.typeProjet === s.typeProjet && x.etat === 'PUBLIEE')?.id ?? null,
      }
      versions.push(v)
      operations[v.id] = structuredClone(operations[s.id] ?? [])
      // La dérivation copie les étapes (D-89 §4).
      etapes[v.id] = structuredClone(etapesDe(s.id))
      return v
    }),
    // HOM-01, HOM-02 : refus comme ReferentialLifecycleService.publier, attributs manquants d'abord, puis toute anomalie du contrôle.
    publierReferentiel: (id) => attendre(() => {
      const v = version(id)
      if (v.etat !== 'BROUILLON') throw refus(409, 'HOM_REFERENTIAL_FROZEN', 'Une version publiée est figée.')
      const m = manquants(operations[id] ?? [])
      if (m.length) throw refus(409, 'HOM_REFERENTIAL_INCOMPLETE', 'Publication refusée : opérations incomplètes', { operations: m })
      const r = controlerGraphe(operations[id] ?? [], contexte(id))
      if (r.anomalies.length) throw refus(409, 'HOM_REFERENTIAL_GRAPH_INVALID', 'Publication refusée : graphe des prédécesseurs non conforme', { anomalies: r.anomalies })
      versions.filter((x) => x.typeProjet === v.typeProjet && x.etat === 'PUBLIEE').forEach((x) => {
        x.etat = 'RETIREE'
        x.actionsPermises = []
      })
      v.etat = 'PUBLIEE'
      v.numero = Math.max(0, ...versions.filter((x) => x.typeProjet === v.typeProjet).map((x) => x.numero ?? 0)) + 1
      v.datePublication = o.maintenant()
      v.transitionsPermises = []
      v.actionsPermises = ['DERIVER', 'CONTROLER']
      versions.filter((x) => x.typeProjet === v.typeProjet).forEach((x) => (x.versionActiveId = v.id))
    }),
    grapheReferentiel: (id) => attendre(() => calculerGraphe(version(id), operations[id] ?? [], { etapes: vuesDe(id), profils: o.profils, controle: contexte(id) })),
    catalogueReferentiels: (versionId) => attendre(() => ({
      ...CATALOGUE, fonctions: fonctions.map((f) => ({ code: f.code, libelle: f.libelle, profils: f.profils, externe: f.externe, actif: f.actif })),
      typesProjet: types.map((t) => ({ code: t.code, libelle: t.libelle, actif: t.actif })),
      etapesCanoniques: ETAPES_CANONIQUES, couleurs: COULEURS_ETAPE, ...(versionId ? { etapesVersion: vuesDe(version(versionId).id) } : {}),
    })),
    creerOperation: (id, s) => attendre(() => {
      const ops = brouillon(id)
      if (!CODE.test(s.code) || s.code.length > 40) throw refus(422, 'HOM_OPERATION_CODE_INVALID', 'Code d\'opération invalide.')
      if (ops.some((x) => x.code === s.code)) throw refus(409, 'HOM_OPERATION_CODE_DUPLICATE', `Le code ${s.code} existe déjà dans cette version.`)
      valider(s.code, s)
      etapeConnue(id, s)
      const { code, ...saisie } = s
      ops.push(vers(code, { ...saisie, ordre: saisie.ordre ?? ops.length + 1 }))
      return avecSucc(ops, code)
    }),
    enregistrerOperation: (id, code, s) => attendre(() => {
      const ops = brouillon(id)
      const i = ops.indexOf(trouver(ops, code))
      valider(code, s)
      etapeConnue(id, s)
      ops[i] = vers(code, s, ops[i])
      return avecSucc(ops, code)
    }),
    supprimerOperation: (id, code) => attendre(() => {
      const ops = brouillon(id)
      trouver(ops, code)
      operations[id] = ops.filter((x) => x.code !== code).map((x) => ({ ...x, predecesseurs: x.predecesseurs.filter((p) => p !== code) }))
    }),
    placerOperations: (id, positions) => attendre(() => {
      const ops = brouillon(id)
      positions.forEach((p) => {
        const op = ops.find((x) => x.code === p.code)
        if (op) op.position = { x: p.x, y: p.y }
      })
    }),
    arcReferentiel: (id, a) => attendre(() => {
      const ops = brouillon(id)
      trouver(ops, a.de)
      const cible = trouver(ops, a.vers)
      if (a.de === a.vers) throw refus(422, 'HOM_OPERATION_SELF_PREDECESSOR', 'Une opération ne peut pas se précéder elle-même.')
      if (a.present && !cible.predecesseurs.includes(a.de)) {
        if (descend(ops, a.vers, a.de)) throw refus(409, 'HOM_REFERENTIAL_CYCLE', `L'arc ${a.de} → ${a.vers} fermerait un cycle.`)
        cible.predecesseurs = [...cible.predecesseurs, a.de]
      }
      if (!a.present) cible.predecesseurs = cible.predecesseurs.filter((p) => p !== a.de)
      return avecSucc(ops, a.vers)
    }),
    designerPilote: (id, compteId) => attendre(() => {
      brouillon(id)
      if (!CATALOGUE.pilotes.some((p) => p.id === compteId)) throw refus(422, 'HOM_PILOTE_NOT_ELIGIBLE', 'Le pilote désigné ne peut pas modifier les référentiels.')
      const v = version(id)
      v.piloteId = compteId
      return v
    }),
  }
  /** Opérations de la version publiée d'un type (instanciation d'une liaison, D-91) ; null sans version publiée. */
  const versionPubliee = (type: string): ReferenceOperation[] | null => {
    const v = versions.find((x) => x.typeProjet === type && x.etat === 'PUBLIEE')
    return v ? structuredClone(operations[v.id] ?? []) : null
  }
  return { port, versions, fonctions, types: () => types.map(vueType), versionPubliee }
}
