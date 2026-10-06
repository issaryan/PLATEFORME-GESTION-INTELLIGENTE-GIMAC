// Erreur normalisée de toute réponse non conforme (DS-GIMAC-01 §8.6) : code, message, identifiant
// de corrélation affiché à l'utilisateur. Compatible avec l'ApiError des composants du catalogue.
export class ApiError extends Error {
  readonly status: number
  readonly code: string
  readonly correlationId: string
  readonly details?: Readonly<Record<string, unknown>>
  constructor(status: number, code: string, message: string, correlationId: string, details?: Readonly<Record<string, unknown>>) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
    this.correlationId = correlationId
    this.details = details
  }
}

/** Contrainte non respectée d'un champ (ExceptionTranslator JHipster des services). */
export type FieldErrorProbleme = { objectName?: string; field?: string; message?: string }

/** Corps d'erreur des services (problème RFC 9457 enrichi). */
export type Problem = {
  code?: string
  message?: string
  detail?: string
  details?: Record<string, unknown>
  violations?: unknown[]
  /** Validation de l'entrée (Bean Validation) : un élément par champ refusé. */
  fieldErrors?: FieldErrorProbleme[]
  correlationId?: string
}

/** Code d'une saisie refusée champ par champ (fieldErrors, sans code métier) : détails dans details.champs. */
export const SAISIE_INVALIDE = 'SAISIE_INVALIDE'

/**
 * Champs refusés par le service, nom du champ vers message du service (premier message par champ) :
 * details.champs de l'ApiError, alimenté par fieldErrors ; vide pour toute autre erreur.
 */
export function champsRefuses(e: unknown): Record<string, string> {
  const champs = e instanceof ApiError ? e.details?.champs : undefined
  if (!champs || typeof champs !== 'object') return {}
  return Object.fromEntries(Object.entries(champs as Record<string, unknown>).filter(([, v]) => typeof v === 'string')) as Record<string, string>
}

/** fieldErrors JHipster ramenés à { champ: message } ; null si le corps n'en porte pas. */
export function champsDe(p: Problem): Record<string, string> | null {
  if (!Array.isArray(p.fieldErrors) || p.fieldErrors.length === 0) return null
  const champs: Record<string, string> = {}
  for (const f of p.fieldErrors) {
    if (f && typeof f.field === 'string' && f.field && !(f.field in champs)) champs[f.field] = typeof f.message === 'string' ? f.message : ''
  }
  return Object.keys(champs).length ? champs : null
}

export const estApiError = (e: unknown): e is ApiError => e instanceof ApiError
