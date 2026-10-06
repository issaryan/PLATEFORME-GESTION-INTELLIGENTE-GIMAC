// Exploitation de l'infrastructure (D-78, docs/exploitation/operations-infrastructure.md §3 et §5) : formes de
// InfrastructureResource de MS-GATEWAY (/api/plateforme/infrastructure). Dates ISO 8601.

export type Orchestrateur = 'DOCKER' | 'KUBERNETES' | 'AUCUN'
export type EtatCharge = 'EN_COURS' | 'ARRETEE' | 'REDEMARRAGE' | 'EN_ERREUR' | 'PARTIELLE'
export type SanteCharge = 'SAINE' | 'DEGRADEE' | 'INCONNUE'
export type Geste = 'REDEMARRER' | 'ARRETER' | 'DEMARRER' | 'ECHELLE' | 'RETABLIR'
export type ResultatOperation = 'REUSSIE' | 'ECHOUEE' | 'EN_COURS'

export type Charge = {
  nom: string
  orchestrateur: Exclude<Orchestrateur, 'AUCUN'>
  etat: EtatCharge
  sante: SanteCharge
  repliques: { voulues: number; pretes: number }
  redemarrages: number
  image: string | null
  demarreeLe: string | null
  /** Charge protégée : ni arrêt ni mise à zéro réplique (409 OPS_CHARGE_PROTEGEE). */
  protegee: boolean
  /** Gestes permis à cet instant ; vide pendant une opération. */
  gestes: Geste[]
  /** Kubernetes : image de la version précédente à laquelle RETABLIR reviendrait ; null s'il n'y en a pas. */
  imagePrecedente?: string | null
}

/** Liste des charges et orchestrateur configuré (en-tête X-Orchestrateur ; AUCUN : exploitation non configurée). */
export type EtatInfrastructure = { orchestrateur: Orchestrateur; charges: Charge[] }

export type LigneJournal = { quand: string | null; flux: 'stdout' | 'stderr'; texte: string }
export type JournauxCharge = { charge: string; lignes: LigneJournal[]; tronque: boolean }

/** Corps d'un geste : motif (5 à 500 caractères) ; répliques (0 à 5) pour la mise à l'échelle seulement. */
export type DemandeGeste = { motif: string; repliques?: number }

export type Operation = {
  id: string
  charge: string
  geste: Geste
  motif: string
  acteur: { id: string; nomAffiche: string | null }
  avant: string | null
  apres: string | null
  repliquesAvant?: number | null
  repliquesApres?: number | null
  resultat: ResultatOperation
  erreur: string | null
  demandeeLe: string
  termineeLe: string | null
}

/** Historique paginé (page à partir de 0, taille 1 à 100). */
export type PageOperations = { contenu: Operation[]; total: number; page: number; taille: number }

/** Bornes du contrat (§2, §5). */
export const BORNES_INFRASTRUCTURE = { motifMin: 5, motifMax: 500, repliquesMin: 0, repliquesMax: 5, lignesMax: 500 } as const
/** Durées de lecture des journaux proposées (ISO 8601, 30 minutes au plus). */
export const DUREES_JOURNAUX = ['PT5M', 'PT10M', 'PT30M'] as const

/** Pod d'une charge Kubernetes (GET /charges/{nom}/pods). */
export type PodCharge = { nom: string; phase: string | null; pret: boolean; redemarrages: number; noeud: string | null; demarreLe: string | null; attente: string | null }
/** Événement Kubernetes d'une charge (GET /charges/{nom}/evenements), le plus récent d'abord. */
export type EvenementCharge = { instant: string | null; type: string | null; raison: string | null; message: string | null; objet: string; nombre: number }
/** Nœud du cluster (GET /noeuds). */
export type NoeudCluster = { nom: string; pret: boolean; roles: string[]; versionKubelet: string | null; cpuAllouable: string | null; memoireAllouableOctets: number | null; nonPlanifiable: boolean }
export type EtatTache = 'EN_ATTENTE' | 'EN_COURS' | 'REUSSIE' | 'ECHEC'
/** CronJob ou Job de l'espace de noms (GET /taches). */
export type TacheCluster = { nom: string; nature: 'CRONJOB' | 'JOB'; planification: string | null; suspendue: boolean | null; etat: EtatTache | null; debut: string | null; fin: string | null; origine: string | null }
