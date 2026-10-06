// Supervision de la plateforme (D-61) : formes de PlateformeResource de MS-GATEWAY (/api/plateforme).
// Dates ISO 8601 ; tailles en octets ; taux CPU entre 0 et 1 pour les services, en % pour les hôtes.

export type EtatSante = 'UP' | 'DOWN' | 'OUT_OF_SERVICE' | 'INCONNU' | 'INJOIGNABLE'
export type EtatPlateforme = 'OPERATIONNELLE' | 'DEGRADEE' | 'HORS_SERVICE'

export type InfoService = { version: string | null; artefact: string | null; dateConstruction: string | null; commit: string | null; branche: string | null }

export type Metriques = {
  dureeFonctionnementSecondes: number | null
  cpuProcessus: number | null
  cpuSysteme: number | null
  nombreCpu: number | null
  memoireTasUtiliseeOctets: number | null
  memoireTasMaxOctets: number | null
  memoireTasPourcent: number | null
  disqueLibreOctets: number | null
  disqueTotalOctets: number | null
  chargeMoyenne1m: number | null
  threadsActifs: number | null
}

export type EtatService = {
  nom: string
  url: string
  etat: EtatSante
  latenceMs: number | null
  composants: Record<string, string>
  info: InfoService | null
  metriques: Metriques | null
  erreur: string | null
  controleLe: string | null
}

export type EtatGlobal = { etat: EtatPlateforme; total: number; up: number; down: number; injoignables: number }
export type InstantanePlateforme = { horodatage: string; global: EtatGlobal; services: EtatService[]; keycloak: EtatService | null }

export type CibleCollecte = { job: string; instance: string; etat: string; derniereCollecte: string | null; dureeDerniereCollecteSecondes: number | null; erreur: string | null }
export type EtatHote = { instance: string; cpuPourcent: number | null; memoirePourcent: number | null; memoireTotaleOctets: number | null; disquePourcent: number | null; chargeMoyenne1m: number | null }
export type EtatConteneur = { nom: string; cpuCoeurs: number | null; memoireOctets: number | null }
export type EtatServeurs = {
  horodatage: string
  prometheusJoignable: boolean
  erreur: string | null
  ciblesUp: number
  ciblesDown: number
  cibles: CibleCollecte[]
  nodeExporter: boolean
  hotes: EtatHote[]
  conteneurs: EtatConteneur[]
}

export type Alerte = { nom: string; severite: string | null; resume: string | null; description: string | null; depuis: string | null; cible: string | null; job: string | null; etat: string | null }
export type EtatAlertes = { horodatage: string; joignable: boolean; erreur: string | null; nombre: number; alertes: Alerte[] }

/** Événement « plateforme » du flux SSE : instantané et nombre d'alertes (-1 : Alertmanager injoignable). */
export type ImpulsionPlateforme = { sequence: number; instantane: InstantanePlateforme; nombreAlertes: number }
/** État de la connexion au flux temps réel. */
export type EtatFlux = 'connexion' | 'connecte' | 'perdu'

export type RouteVue = { id: string; uri: string; predicats: string; filtres: string[]; ordre: number }
export type ConfigurationService = { nom: string; url: string; lue: boolean; erreur: string | null; profilsActifs: string[]; proprietes: Record<string, string> }
export type ConfigurationPlateforme = { horodatage: string; routes: RouteVue[]; services: ConfigurationService[] }

export type NiveauLogger = { nom: string; niveauConfigure: string | null; niveauEffectif: string | null }
export type NiveauxJournalisation = { service: string; niveauxDisponibles: string[]; loggers: NiveauLogger[] }
export type DemandeNiveau = { logger: string; niveau: string; motif: string }
export type ChangementJournalisation = { service: string; logger: string; niveau: string; motif: string; auteur: string; horodatage: string }

/** Séries temporelles des graphiques de la supervision (GET /api/plateforme/series) : listes fermées. */
export type MetriqueSerie =
  | 'PLATEFORME_CPU' | 'CONTENEUR_CPU' | 'CONTENEUR_MEMOIRE' | 'HOTE_CPU' | 'HOTE_MEMOIRE' | 'HOTE_DISQUE'
  | 'SERVICE_DEBIT' | 'SERVICE_LATENCE_P95' | 'SERVICE_ERREURS' | 'SERVICE_TAS_JVM'
export type PeriodeSerie = 'UNE_HEURE' | 'SIX_HEURES' | 'UN_JOUR' | 'SEPT_JOURS'
export const PERIODES: PeriodeSerie[] = ['UNE_HEURE', 'SIX_HEURES', 'UN_JOUR', 'SEPT_JOURS']
/** Mesure : instant en millisecondes, valeur nulle quand la mesure manque (trou, jamais zéro). */
export type PointSerie = { t: number; v: number | null }
export type SerieMetrique = { nom: string; points: PointSerie[] }
/** Séries triées de la plus chargée à la moins chargée ; disponible=false : Prometheus injoignable. */
export type SeriesMetriques = {
  metrique: MetriqueSerie
  unite: string
  periode: PeriodeSerie
  debut: string
  fin: string
  pasSecondes: number
  disponible: boolean
  series: SerieMetrique[]
}
