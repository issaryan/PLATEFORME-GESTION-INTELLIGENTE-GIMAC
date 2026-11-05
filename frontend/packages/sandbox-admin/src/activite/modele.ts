// Statistiques des tests des participants (D-121) : routes console de SBX-LECTURE (StatistiquesConsoleController),
// servies sous /bff/des/sbx-lecture/statistiques et réécrites en /api/v1/console/statistiques. Agrégats calculés en
// SQL par le service : l'écran ne fait que les afficher. Dates ISO 8601 en temps universel, période demi-ouverte.

export const PERMISSION_ACTIVITE = 'SBX_SUP_CONSULTATION'
/** Lecture d'une liaison de MS-HOMOLOGATION, pour la chaîne sandbox, homologation, certification. */
export const PERMISSION_LIAISON = 'HOM_LIAISON_READ'

export const INTERFACES = ['MOBILE', 'ISO20022', 'CARTE'] as const
export type InterfaceTest = (typeof INTERFACES)[number]
export type Granularite = 'JOUR' | 'SEMAINE' | 'MOIS'
export type TriCampagnes = 'TAUX_REUSSITE' | 'DERNIER_ESSAI'

/** Taux tronqué à quatre décimales (« 0.5000 »), zéro quand le dénominateur est nul. */
export type Taux = { numerateur: number; denominateur: number; valeur: string }
export type Verdicts = { favorables: number; defavorables: number; nonExecutes: number; enAttente: number }

export type FiltreActivite = { du: string; au: string; participant?: string; interface?: InterfaceTest }

export type Synthese = {
  du: string
  au: string
  calculeLe: string
  executions: number
  participantsActifs: number
  campagnes: number
  campagnesParEtat: Record<string, number>
  verdicts: Verdicts
  tauxReussiteCas: Taux
}

export type PointActivite = { periode: string; interfaceTest: string | null; executions: number; verdicts: Verdicts }
export type Activite = { du: string; au: string; granularite: Granularite; points: PointActivite[] }

export type LigneParticipant = {
  codeParticipant: string
  identifiantLiaison: string | null
  interfaces: string[]
  campagnes: number
  executions: number
  verdicts: Verdicts
  tauxReussite: Taux
  premierEssai: string | null
  dernierEssai: string | null
  derniereCampagne: { identifiantCampagne: string | null; etat: string | null }
}

export type LigneCampagne = {
  identifiantCampagne: string
  code: string | null
  identifiantLiaison: string | null
  codeParticipant: string
  interfaceTest: string | null
  etat: string
  executions: number
  tauxReussite: Taux
  tauxAvancement: Taux | null
  premierEssai: string | null
  dernierEssai: string | null
  ouverteLe: string | null
  closeLe: string | null
  versionBilan: number | null
}

export type LigneCas = { codeCas: string; codeService: string | null; interfaceTest: string | null; executions: number; verdicts: Verdicts; tauxReussite: Taux }

export type PageStatistiques<T> = { elements: T[]; total: number; page: number; taille: number; suivant: string | null }

/** Code de retour des échanges (SBX-TRACE), branché quand exchange-captured.v1 le portera. */
export type CodeRetour = { interface: string; code: string; occurrences: number; participants: number; dernierLe: string }

/** Liaison de MS-HOMOLOGATION réduite à ce que montre la chaîne de certification. */
export type EtatLiaison = 'OUVERTE' | 'EN_HOMOLOGATION' | 'GELEE' | 'APTE_CERTIFICATION' | 'MISE_EN_SERVICE_AUTORISEE' | 'CERTIFIEE' | 'CLOSE'
export type LiaisonCertification = { id: string; institutionNom: string | null; etat: EtatLiaison; etatsParcourus: EtatLiaison[] | null }

/** Étapes de la chaîne affichée : la sandbox puis les états de la liaison au DES. */
export const CHAINE = ['SANDBOX', 'EN_HOMOLOGATION', 'APTE_CERTIFICATION', 'MISE_EN_SERVICE_AUTORISEE', 'CERTIFIEE'] as const
