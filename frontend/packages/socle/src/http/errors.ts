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

/** Corps d'erreur des services (problème RFC 9457 enrichi). */
export type Problem = { code?: string; message?: string; detail?: string; details?: Record<string, unknown>; violations?: unknown[]; correlationId?: string }

export const estApiError = (e: unknown): e is ApiError => e instanceof ApiError
