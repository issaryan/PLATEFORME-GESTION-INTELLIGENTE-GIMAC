// Résultats de la sandbox repris par MS-HOMOLOGATION pour une liaison (US-EXI-14, US-EXI-17, D-118 point 1) : formes de
// ResultatsSandbox, servies sous /bff/des/homologation/liaisons/{id}/sandbox. Lecture seule : rien n'est recalculé, les
// verdicts, les taux et les pièces sont ceux que restitue SBX-LECTURE.

/** Mention permanente de l'onglet : résultats repris de la sandbox, jamais recalculés. */
export const MENTION_REPRIS = 'REPRIS_NON_RECALCULE'

/** Taux restitué tel quel (INT-05) : `service` nul pour le taux de la campagne ; nature AVANCEMENT ou REUSSITE. */
export type TauxSandbox = {
  service: string | null
  nature: 'AVANCEMENT' | 'REUSSITE' | string
  numerateur: number
  denominateur: number
  /** Valeur décimale tronquée, telle que restituée (texte, jamais recalculée). */
  valeur: string
  versionFormule: string
  horodatage: string
}

/** Pièce de la campagne : statut DEPOSEE, DEJA_CONSERVEE (même empreinte déjà au dépôt) ou EMPREINTE_DIVERGENTE. */
export type PieceSandbox = {
  nature: string
  version: number
  empreinte: string
  taille: number
  nom: string | null
  documentId: string | null
  statut: 'DEPOSEE' | 'DEJA_CONSERVEE' | 'EMPREINTE_DIVERGENTE' | string
}

export type CampagneSandbox = {
  campagneId: string
  code: string | null
  etat: string
  interfaceTest: string | null
  services: string[]
  versionBilan: number | null
  versionTable: string | null
  /** Campagne remplacée par un signalement (collision de code participant). */
  signalement: string | null
  taux: TauxSandbox[]
  executions: number
  nonRapprochees: number
  pieces: PieceSandbox[]
  releveLe: string | null
}

export type ScenarioNonRapproche = { id: string; idScenario: string; campagneId: string | null; qualification: string; dateDetection: string }

export type ResultatsSandbox = {
  liaisonId: string
  mention: string
  derniereReleveReussie: string | null
  dernierEchec: string | null
  motifEchec: string | null
  enEchec: boolean
  campagnes: CampagneSandbox[]
  scenariosNonRapproches: ScenarioNonRapproche[]
}

/** Exécution retenue (plus haute position) : `operationCode` nul pour un scénario non rapproché. */
export type ExecutionSandbox = {
  identifiantExecution: string
  identifiantCas: string | null
  identifiantScenario: string
  versionScenario: string | null
  dateExecution: string
  verdictNatif: string
  verdictNormalise: string | null
  motif: string | null
  modeExecution: string | null
  referenceTrace: string | null
  nature: string
  versionTable: string | null
  origine: string | null
  operationCode: string | null
}

export type PageExecutionsSandbox = { contenu: ExecutionSandbox[]; total: number; page: number; taille: number }
