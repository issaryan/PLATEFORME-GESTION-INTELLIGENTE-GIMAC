import { ApiError } from '@gimac/socle'

// Serveur fictif en mémoire des implémentations simulées (développement des écrans sans passerelle) :
// latence, copie profonde des réponses, refus normalisés avec identifiant de corrélation.
export type OptionsSimulateur = { latenceMs?: number }
export type Simulateur = {
  attendre: <T>(v: T | (() => T)) => Promise<T>
  refus: (status: number, code: string, message: string, details?: Record<string, unknown>) => ApiError
}

let compteur = 0

export function creerSimulateur(o: OptionsSimulateur = {}): Simulateur {
  const latence = o.latenceMs ?? 300
  return {
    attendre: <T>(v: T | (() => T)) =>
      new Promise<T>((res, rej) =>
        window.setTimeout(() => {
          try {
            res(structuredClone(typeof v === 'function' ? (v as () => T)() : v))
          } catch (e) {
            rej(e)
          }
        }, latence),
      ),
    refus: (status, code, message, details) => new ApiError(status, code, message, `sim-${Date.now().toString(36)}-${(++compteur).toString(36)}`, details),
  }
}
