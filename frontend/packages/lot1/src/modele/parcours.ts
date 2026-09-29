import type { CoverageValue, Permitted, TypeProjet } from './commun'

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

/** Fonctions du parcours (CR chapitre 3) : départements, services, sécurité, participant et tiers. */
export type Fonction =
  | 'DDRM' | 'CHEF_DDRM' | 'CHEF_DES' | 'CHEFS_DEPARTEMENT' | 'CONDUCTEUR' | 'SNA' | 'PRODUCTION_MONETIQUE' | 'POLE_RESEAU' | 'POLE_SYSTEME'
  | 'RSSI' | 'CONFORMITE' | 'DG' | 'EXPLOITATION' | 'PARTICIPANT' | 'BANQUE_PARTENAIRE' | 'PERSONNALISATEUR'

/** Qualités d'un intervenant sur une tâche (CR §3.6.1) : un seul responsable principal. */
export type Qualite = 'R' | 'C' | 'V' | 'I' | 'E'
export type Personne = { id: string; nom: string; fonction: Fonction }
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

export type EtatOperationParcours = 'VERROUILLEE' | 'A_FAIRE' | 'EN_COURS' | 'EN_VALIDATION' | 'FAITE' | 'BLOQUEE' | 'SANS_OBJET'
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
  livrables: { libelleKey: string; fourni: boolean }[]
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
}

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

/** Dossier numérique de la liaison (CR Fig. 7.3) : l'écran principal est l'état du périmètre. */
export type DossierLiaison = Permitted & {
  id: string
  reference: string
  institutionId: string
  institutionNom: string
  typeProjet: TypeProjet
  banquePartenaireNom: string | null
  personnalisateurNom: string | null
  statut: StatutParcours
  etapeCourante: EtapeCode
  conducteur: Personne | null
  responsableFonctionnel: Personne | null
  responsableTechnique: Personne | null
  dateDemarrage: string
  dateCibleProduction: string | null
  versionReferentiel: number | null
  perimetre: ServicePerimetre[]
  couvertureCertification: CoverageValue
  couvertureMiseEnService: CoverageValue
  etapes: EtapeParcours[]
  blocages: Blocage[]
  interlocuteursParticipant: { nom: string; fonction: string }[]
  /** Liaison d'origine quand celle-ci ouvre un service supplémentaire (rang 35). */
  liaisonOrigineId: string | null
  /** Projection du portail : coordonnées techniques d'essai communiquées par le participant (rang 12), relisibles. */
  coordonneesTechniques?: { adresse: string; port: string; pointsTerminaison: string | null; communiqueesLe: string } | null
}

export type LiaisonResume = {
  id: string
  reference: string
  institutionNom: string
  typeProjet: TypeProjet
  statut: StatutParcours
  etapeCourante: EtapeCode
  conducteurNom: string | null
  couvertureCertification: CoverageValue
  couvertureMiseEnService: CoverageValue
  bloquee: boolean
  tachesEnRetard: number
  prochaineEcheance: string | null
  dateCibleProduction: string | null
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
}

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
  compteurs: { cle: string; valeur: number; ton: 'ok' | 'warn' | 'crit' | 'info' | 'neutral'; lien: string | null }[]
  taches: TacheActeur[]
  liaisons: LiaisonResume[]
  blocages: Blocage[]
}

export type OuvertureLiaison = {
  institutionId: string
  typeProjet: TypeProjet
  chefDeProjetId: string
  banquePartenaireId?: string | null
}
