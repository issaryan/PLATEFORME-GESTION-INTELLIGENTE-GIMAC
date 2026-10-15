import type { CoverageValue, Permitted, TypeProjet } from './commun'
import type { Geste, Jalon, Livrable, Particularite } from './homologation'

// Parcours d'une liaison, étapes 3 à 10 (CR-CERT-GIMAC-01, D-56 à D-59). L'objet suivi est la liaison,
// non le participant. Tout ce qui est calculé (étape courante, couvertures, SLA, compteurs, actions
// permises) l'est par les services ; l'interface l'affiche.

/** Les dix étapes du parcours ; APPROBATION_SECURITE n'est pas une étape mais le verrou 2 (D-56). */
export type EtapeCode =
  | 'PRISE_CONTACT' | 'ADHESION' | 'CADRAGE' | 'LANCEMENT' | 'ATELIERS' | 'INTEGRATION' | 'TESTS' | 'CERTIFICATION'
  | 'APPROBATION_SECURITE' | 'MISE_EN_PRODUCTION' | 'SUIVI'
export const ETAPES: EtapeCode[] = [
  'PRISE_CONTACT', 'ADHESION', 'CADRAGE', 'LANCEMENT', 'ATELIERS', 'INTEGRATION', 'TESTS', 'CERTIFICATION', 'APPROBATION_SECURITE', 'MISE_EN_PRODUCTION', 'SUIVI',
]

/** Statut du CR porté comme étape courante de la liaison (D-57). */
export type StatutParcours = 'CADREE' | 'EN_INTEGRATION' | 'EN_TEST' | 'CERTIFIEE' | 'APPROUVEE_SECURITE' | 'EN_PRODUCTION' | 'SUSPENDUE' | 'CLOSE_SANS_SUITE'

export type SegmentParcours = 'AMONT' | 'BRANCHE' | 'AVAL'
export type GrainPreuve = 'PHASE' | 'OPERATION'
export type PlateformeExecution = 'SWITCH_INTERBANCAIRE' | 'SWITCH_DELEGATAIRE' | 'PLATEFORME_MOBILE' | 'SIMULATEUR'

/**
 * Fonction du parcours (D-91) : code du catalogue des fonctions de MS-HOMOLOGATION (^[A-Z][A-Z0-9_]{1,39}$), plus une
 * énumération ; les seize fonctions reprises (DDRM, CHEF_DES, CONDUCTEUR, SNA, PARTICIPANT…) gardent leurs codes.
 */
export type Fonction = string
/** Fonction de l'utilisateur courant (GET /moi/fonctions, D-94 §6.5) : conférée par les profils de son jeton. */
export type FonctionUtilisateur = { code: string; libelle: string; externe: boolean; profils: string[] }

/** Qualités d'un intervenant sur une tâche (CR §3.6.1) : un seul responsable principal. */
export type Qualite = 'R' | 'C' | 'V' | 'I' | 'E'
/** Personne tenant une fonction ; `aAffecter` (D-93) : fonction encore à affecter sur la liaison (identifiant A_AFFECTER:<FONCTION>). */
export type Personne = { id: string; nom: string; fonction: Fonction; aAffecter?: boolean }
export type Raci = { R: Personne | null; C: Personne[]; V: Personne | null; I: Personne[]; E: Personne[] }

export type Sla = 'DANS_LES_DELAIS' | 'ECHEANCE_PROCHE' | 'DEPASSE'
export type Preuve = {
  documentRef: string
  nom: string
  empreinte: string
  deposeLe: string
  deposePar: string
  /** Valeur de référence relisible (adresse de point de terminaison, code membre, BIN) : CR §6.4.3. */
  valeurReference?: { libelle: string; valeur: string }
  /** Code de contrôle d'une clé : jamais la clé elle-même (CR §6.4.3, D-59). */
  codeControle?: string
  /** Cérémonie de clés (C1, CR §5.4.1) : date de tenue, porteurs des trois composantes et leurs codes de contrôle. */
  ceremonie?: Ceremonie
}
export type Ceremonie = { tenueLe: string; porteurs: string[]; codesControle: string[] }

/** EN_SIGNATURE (D-91) : geste abouti, signatures attendues dans l'ordre avant la clôture. */
export type EtatOperationParcours = 'VERROUILLEE' | 'A_FAIRE' | 'EN_COURS' | 'EN_VALIDATION' | 'EN_SIGNATURE' | 'FAITE' | 'BLOQUEE' | 'SANS_OBJET'
/** Vue spécialisée d'une opération (D-93 §8.5) : attribut de l'opération, jamais déduit de son rang. */
export type VueOperation = 'AUCUNE' | 'AFFECTATION' | 'COORDONNEES' | 'ESSAIS' | 'DECISION' | 'ATTESTATIONS' | 'MISE_EN_SERVICE' | 'CEREMONIE'
/** Livrable attendu d'une opération à l'exécution (D-93 §8.5) et son dépôt. */
export type LivrableAttendu = {
  code: string; libelle: string; nature: 'LIVRABLE' | 'PREUVE'; deposant: Fonction | null; libelleDeposant?: string | null; typesPieces: string[]; obligatoire: boolean
  depot?: DepotLivrable | null
  peutDeposer: boolean
  /** D-109 : une pièce pour l'opération, ou une par service inclus au périmètre. */
  grain?: 'OPERATION' | 'SERVICE'
  /** D-109 : nature précise et format de la pièce attendue. */
  consigne?: string | null
  /** D-109 : état calculé par le service. */
  etat?: EtatLivrable
  /** D-109 : grain du service, une ligne par service inclus. */
  services?: { serviceCode: string; etat: EtatLivrable; depot?: DepotLivrable | null }[] | null
}
/**
 * État d'un livrable attendu (D-109) : attendu, déposé, validé, refusé (renvoyé après dépôt), sans objet (motivé), reprise
 * sans pièce (opération reprise de l'existant, pièce non numérisée), non exigé (opération close avant l'exigence).
 */
export type EtatLivrable = 'ATTENDUE' | 'DEPOSEE' | 'VALIDEE' | 'REFUSEE' | 'SANS_OBJET' | 'REPRISE_SANS_PIECE' | 'NON_EXIGEE'
/** Dépôt d'un livrable (D-93, D-109) : pièce et empreinte, ou « sans objet » motivé ; service visé ; origine automatique. */
export type DepotLivrable = {
  livrable?: string
  documentRef?: string | null; nom?: string | null; empreinte?: string | null; deposeLe: string; deposePar: string; supervision?: boolean | null; repris?: boolean | null
  service?: string | null; sansObjet?: boolean | null; motif?: string | null; origine?: string | null
}
/** Événement de la chronologie réelle d'une liaison (D-109), lu dans l'état du parcours. */
export type EvenementPreuve = {
  date: string | null; instant?: string | null
  type: 'DEPOT' | 'DEPOT_REPRIS' | 'PIECE_AUTOMATIQUE' | 'SANS_OBJET' | 'RENVOI' | 'SIGNATURE' | 'FAITE' | 'REPRISE' | 'BLOCAGE' | 'LEVEE'
  operationId?: string | null; rang?: string | null; operation?: string | null; etapeCode?: string | null; livrable?: string | null; service?: string | null
  acteur?: string | null; detail?: string | null; documentRef?: string | null; empreinte?: string | null
}
/** Renvoi d'une opération par son validateur (D-93 §8.3). */
export type Renvoi = { motif: string; parId: string; parNom: string; le: string }
/** Signature attendue ou donnée d'une opération (D-91 §5.4). */
export type SignatureOperation = {
  code: string; libelle: string; ordre: number; signataire: Fonction; libelleSignataire?: string | null; porte: string[]
  /** D-109 : NON_RECUEILLIE (opération close avant l'exigence), REPRISE (opération reprise de l'existant). */
  etat: 'ATTENDUE' | 'SIGNEE' | 'NON_RECUEILLIE' | 'REPRISE'; suivante: boolean; attendu?: { id: string; nom: string; fonction: Fonction } | null
  signePar?: string | null; signeParNom?: string | null; signeLe?: string | null; empreinte?: string | null
}
export type OperationParcours = Permitted & {
  id: string
  liaisonId: string
  rang: string
  etape: EtapeCode
  segment: SegmentParcours
  plateforme: PlateformeExecution | null
  grain: GrainPreuve
  etat: EtatOperationParcours
  raci: Raci
  echeance: string | null
  sla: Sla | null
  faiteLe: string | null
  preuve: Preuve | null
  cle: boolean
  /** Secret non cryptographique : trace seule, hors valeurs. */
  secret: boolean
  valeurReference: boolean
  oubliSansSymptome: boolean
  pivot: boolean
  predecesseursNonFaits: string[]
  /** Modélisateur (D-77, §3) : libellé, geste, jalon, déclencheur et livrables lus sur l'opération du référentiel publié. */
  libelle?: string | null
  geste?: Geste | null
  jalon?: Jalon | null
  declencheur?: string | null
  livrables?: Livrable[]
  /** D-93 : étape de la version (code configuré, D-89), vue spécialisée, livrables attendus, renvois ; D-91 : signatures. */
  description?: string | null
  particularite?: Particularite | null
  etapeCode?: string | null
  vue?: VueOperation | null
  livrablesAttendus?: LivrableAttendu[]
  renvois?: Renvoi[]
  signatures?: SignatureOperation[]
  /** D-109 : opération reprise de l'existant (D-107), « reprise, pièce non numérisée » ; rien ne lui est exigé. */
  reprise?: boolean
  /** D-106 : délais cible et maximal (jours). */
  delaiJours?: number
  delaiMaxJours?: number
}

/** Workflow d'une étape : six éléments, l'étape n'est close que s'ils sont tous réunis (CR §7.1). */
export type ElementEtape = 'RESPONSABLE' | 'ACTIONS' | 'LIVRABLES' | 'VALIDATION' | 'PREUVE' | 'DATE'
export type EtatEtape = 'A_VENIR' | 'EN_COURS' | 'CLOSE' | 'BLOQUEE' | 'SANS_OBJET'
export type EtapeParcours = {
  code: EtapeCode
  rang: number | null
  etat: EtatEtape
  porteurs: Fonction[]
  responsable: Personne | null
  elements: Record<ElementEtape, boolean>
  actionsFaites: number
  actionsTotal: number
  /** Livrables de l'étape : libellé servi (D-93), `libelleKey` seulement pour les étapes 1 et 2. */
  livrables: { libelleKey?: string; libelle?: string; operation?: string; code?: string; obligatoire?: boolean; fourni: boolean; etat?: EtatLivrable | null }[]
  validateur: Personne | null
  valideeLe: string | null
  debut: string | null
  closeLe: string | null
  echeance: string | null
  sla: Sla | null
}

export type EtatServicePerimetre = 'NON_DEMARRE' | 'EN_TEST' | 'CERTIFIE' | 'OUVERT' | 'NON_OUVERT'
export type ServicePerimetre = {
  serviceCode: string
  libelle: string
  inclus: boolean
  etat: EtatServicePerimetre
  entreeEssai: string | null
  sortieEssai: string | null
  certifieLe: string | null
  /** Taux d'avancement repris du simulateur pour la branche mobile, jamais recalculé (CR §4.4.1). */
  avancementSimulateur: number | null
}

export type Blocage = Permitted & {
  id: string
  liaisonId: string
  motif: string
  depuis: string
  prochaineAction: string
  responsable: Personne
  echeanceLevee: string
  leveLe: string | null
  etape: EtapeCode
}

/** Verrou 1 : procès-verbal de certification (D-56, rangs 17 à 19). */
export type EtatProcesVerbal = 'NON_ETABLI' | 'ETABLI' | 'SIGNE_GIMAC' | 'SIGNE_PARTICIPANT' | 'SIGNE'
export type Signature = { signataire: string; fonction: Fonction; signeLe: string; horodatage: string }
export type ProcesVerbalCertification = Permitted & {
  liaisonId: string
  etat: EtatProcesVerbal
  couverture: CoverageValue
  servicesCouverts: string[]
  etabliPar: string | null
  etabliLe: string | null
  empreinte: string | null
  signatureGimac: Signature | null
  signatureParticipant: Signature | null
  /**
   * Circuit de signature en données (D-94, MS-CERTIFICATION §10.3) : étapes configurées de l'opération qui porte la
   * signature du procès-verbal, ordre croissant (même ordre : indifférent) ; `portail` : signée par l'institution.
   */
  circuit?: EtapeCircuit[]
}
export type EtapeCircuit = { code: string; libelle: string; fonction: Fonction; ordre: number; portail: boolean; etat: 'ATTENDUE' | 'SIGNEE'; signature?: Signature | null }

/** Verrou 2 : approbation du changement par la sécurité, lue sur la fiche de changement (D-56, rangs 20 à 26). */
export type TypeAttestation = 'ISO_27001' | 'PCI_DSS' | 'GSMA' | 'COBAC'
export type Attestation = {
  type: TypeAttestation
  requise: boolean
  preuve: Preuve | null
  verifieeLe: string | null
  verifieePar: string | null
  archiveeLe: string | null
  motifRejet: string | null
}
export type EtatApprobation = 'EN_ATTENTE_DOSSIER' | 'DOSSIER_CONSTITUE' | 'EN_VERIFICATION' | 'VERIFIE_ARCHIVE' | 'APPROUVEE' | 'REFUSEE'
export type Visa = { nature: 'CONFORMITE' | 'DIRECTION_GENERALE'; actif: boolean; etat: 'ATTENDU' | 'SIGNE_FAVORABLE' | 'SIGNE_DEFAVORABLE' | 'CADUC'; signature: Signature | null; motif: string | null }
export type ApprobationSecurite = Permitted & {
  liaisonId: string
  etat: EtatApprobation
  dossierChangement: Preuve | null
  ficheChangement: { reference: string; etatApprobation: 'OUVERTE' | 'EN_EXAMEN' | 'APPROUVEE' | 'REFUSEE'; lueLe: string; aRelire: boolean } | null
  documentParametrageRecu: string | null
  attestations: Attestation[]
  /** Participant fintech : conformité COBAC exigée (rang 22). */
  fintech: boolean
  visas: Visa[]
  approuveeLe: string | null
}

/** Étape 10 : fiche de suivi des services renseignée par le participant, puis constat (rangs 32 et 33). */
export type FicheSuiviServices = Permitted & {
  liaisonId: string
  etat: 'A_REMETTRE' | 'REMISE' | 'RENSEIGNEE' | 'CONSTATEE'
  lignes: { serviceCode: string; libelle: string; ouvert: boolean | null; commentaire: string | null }[]
  renseigneeLe: string | null
  constateeLe: string | null
}

/** Étape réelle de la version du référentiel de la liaison (D-93 §8.5), dans son ordre configuré. */
export type EtapeVersion = {
  code: string; libelle: string; ordre: number; couleur?: string | null; jeton?: string | null; canonique?: EtapeCode | null; etat: EtatEtape
  actionsFaites: number; actionsTotal: number; responsable: Personne | null; validateur: Personne | null; debut: string | null; closeLe: string | null
  echeance: string | null; sla: Sla | null; livrables: EtapeParcours['livrables']
}
/** Intervenant de la liaison lu du RACI (D-93 §8.5) : fonction, qualités tenues, personne désignée ou à affecter. */
export type IntervenantLiaison = { fonction: Fonction; libelle: string; qualites: Qualite[]; personne: Personne | null; aAffecter: boolean; externe?: boolean }

/** Dossier numérique de la liaison (CR Fig. 7.3) : l'écran principal est l'état du périmètre. */
export type DossierLiaison = Permitted & {
  id: string
  reference: string
  institutionId: string
  institutionNom: string
  typeProjet: TypeProjet
  /** CER-05 : libellé du type servi par MS-HOMOLOGATION (le portail ne lit pas le catalogue des types). */
  typeProjetLibelle?: string | null
  banquePartenaireNom: string | null
  personnalisateurNom: string | null
  statut: StatutParcours
  etapeCourante: EtapeCode
  conducteur: Personne | null
  /** Plus renseignés depuis D-93 (DDRM et SNA figés) : les intervenants viennent du RACI. */
  responsableFonctionnel?: Personne | null
  responsableTechnique?: Personne | null
  /** D-93 : étapes réelles de la version et intervenants lus du RACI. */
  etapesVersion?: EtapeVersion[]
  intervenants?: IntervenantLiaison[]
  dateDemarrage: string
  dateCibleProduction: string | null
  versionReferentiel: number | null
  perimetre: ServicePerimetre[]
  couvertureCertification: CoverageValue
  couvertureMiseEnService: CoverageValue
  /** D-110 : avancement global du projet, pondéré par étape (essais au taux mesuré sur la sandbox). */
  avancement?: CoverageValue
  etapes: EtapeParcours[]
  blocages: Blocage[]
  interlocuteursParticipant: { nom: string; fonction: string }[]
  /** Liaison d'origine quand celle-ci ouvre un service supplémentaire (rang 35). */
  liaisonOrigineId: string | null
  /** Projection du portail : coordonnées techniques d'essai communiquées par le participant (rang 12), relisibles. */
  coordonneesTechniques?: { adresse: string; port: string; pointsTerminaison: string | null; communiqueesLe: string } | null
  /** D-104 : intitulé libre du projet ; référence d'origine (reprise), jamais servie au portail. */
  intituleProjet?: string | null
  referenceOrigine?: string | null
}

export type LiaisonResume = {
  id: string
  reference: string
  institutionNom: string
  typeProjet: TypeProjet
  /** CER-05 : libellé du type servi par MS-HOMOLOGATION. */
  typeProjetLibelle?: string | null
  statut: StatutParcours
  etapeCourante: EtapeCode
  conducteurNom: string | null
  couvertureCertification: CoverageValue
  couvertureMiseEnService: CoverageValue
  /** D-110 : avancement global du projet, pondéré par étape. */
  avancement?: CoverageValue
  bloquee: boolean
  tachesEnRetard: number
  prochaineEcheance: string | null
  dateCibleProduction: string | null
  /** D-104 : intitulé libre du projet ; distingue les liaisons d'une même institution, y compris du même type. */
  intituleProjet?: string | null
  /** D-104 : référence du projet dans l'outil d'origine (reprise) ; jamais servie au portail. */
  referenceOrigine?: string | null
}

/** Tâche à réaliser par l'utilisateur courant (tableau de bord par acteur). */
export type TacheActeur = {
  operationId: string
  liaisonId: string
  liaisonReference: string
  institutionNom: string
  rang: string
  etape: EtapeCode
  qualite: Qualite
  echeance: string | null
  sla: Sla | null
  oubliSansSymptome: boolean
  /** Chemin de l'écran où l'action se réalise. */
  lien: string
  /** Portail : gestes que le participant peut accomplir sur cette tâche, calculés par le service. */
  actionsPermises?: string[]
  /** D-93 : libellé de l'opération, tâche à prendre (fonction à affecter), signature attendue (code). */
  libelle?: string | null
  aPrendre?: boolean
  signature?: string | null
}

/**
 * « Mes actions de certification » (D-98 §5, GET /parcours/mes-operations) : opération que je dois faire (qualité R, V ou C,
 * signature attendue, tâche à prendre) ou que je suis (conducteur de la liaison, intervenant désigné), calculée par le service.
 */
export type MonOperation = {
  operationId: string
  liaisonId: string
  liaisonReference: string
  institutionNom: string
  statutLiaison: StatutParcours
  code: string
  libelle?: string | null
  etape: EtapeCode
  etapeCode?: string | null
  etat: EtatOperationParcours
  /** Absente pour une opération suivie (rien à faire maintenant). */
  qualite?: Qualite | null
  aPrendre?: boolean | null
  signature?: string | null
  geste?: Geste | null
  jalon?: Jalon | null
  declencheur?: string | null
  vue?: VueOperation | null
  responsable?: Personne | null
  validateur?: Personne | null
  echeance?: string | null
  sla?: Sla | null
  /** Écran de l'opération dans le dossier de la liaison (onglet du geste et ?operation=). */
  lien: string
  actionsPermises: string[]
}
/** Jalon d'une liaison suivie : atteint quand l'opération qui le porte est faite. */
export type JalonSuivi = { jalon: Jalon; atteint: boolean; atteintLe?: string | null; operationId?: string | null }
/** Liaison que je conduis ou sur laquelle j'interviens : statut, avancement, jalons. */
export type LiaisonSuivie = {
  liaisonId: string
  reference: string
  institutionNom: string
  typeProjet: TypeProjet
  statut: StatutParcours
  etapeCourante: EtapeCode
  conducteur: boolean
  operationsFaites: number
  operationsTotal: number
  jalons: JalonSuivi[]
  lien: string
}
export type MesOperations = { aAgir: MonOperation[]; aSuivre: MonOperation[]; liaisons: LiaisonSuivie[] }

/** Pilotage (CR §7.3, vision diapositives 11 et 12) : tous les chiffres viennent du service. */
export type IndicateursPilotage = {
  projetsActifs: number
  projetsBloques: number
  tachesEnRetard: number
  echeancesProches: number
  repartitionParEtape: { etape: EtapeCode; nombre: number }[]
  delaiMoyenCertificationJours: number | null
  tauxRespectDelais: number | null
  questions: {
    enCertification: number
    servicesPlusDemandes: { serviceCode: string; libelle: string; nombre: number }[]
    servicesPlusLongs: { serviceCode: string; libelle: string; delaiMoyenJours: number }[]
    delaiMoyenAdhesionProductionJours: number | null
    projetsBloques: { liaisonId: string; reference: string; institutionNom: string; motif: string; depuis: string; jours: number }[]
    lancementsParService12Mois: { serviceCode: string; libelle: string; nombre: number }[]
  }
}

/** Tableau de bord d'un acteur : compteurs calculés par le service pour sa fonction et son périmètre. */
export type TableauActeur = {
  fonction: Fonction
  /** Compteurs décrits en données (D-94 §6.5) : libellé et fonction servis. */
  compteurs: { cle: string; valeur: number; ton: 'ok' | 'warn' | 'crit' | 'info' | 'neutral'; lien: string | null; libelle?: string | null; fonction?: Fonction | null }[]
  taches: TacheActeur[]
  liaisons: LiaisonResume[]
  blocages: Blocage[]
}

export type OuvertureLiaison = {
  institutionId: string
  typeProjet: TypeProjet
  chefDeProjetId: string
  banquePartenaireId?: string | null
  /** D-104 : plusieurs liaisons actives par institution, y compris du même type ; l'intitulé les distingue. */
  intituleProjet?: string | null
  /** D-104 : référence du projet dans l'outil d'origine, unique quand elle est renseignée. */
  referenceOrigine?: string | null
}
