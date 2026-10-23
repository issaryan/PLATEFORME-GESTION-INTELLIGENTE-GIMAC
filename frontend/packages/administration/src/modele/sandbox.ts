// Sandbox GIMAC dans la console (D-119, docs/lot1/sandbox-existant-adapte.md §8) : contrat exact des routes
// /api/v1/sandbox de MS-IAM (SandboxConsoleResource), relayées par /bff/console/iam/sandbox.

/** Page rendue par MS-IAM (page à partir de 0, 100 lignes au plus). */
export type PageSandbox<T> = { contenu: T[]; total: number; page: number; taille: number }

/** Indicateur de suspension posé sur le périmètre d'une institution par la saga SUSPENSION de SBX-ESPACE. */
export type PerimetreSuspendu = { code: string; depuis: string; acteurId: string; motif: string | null }

/** État d'un périmètre. */
export type EtatPerimetre = { code: string; suspendu: boolean; depuis: string | null; sessionsRevoquees: number; rejouee: boolean }

/** Compte protégé : dernier administrateur actif de son institution. */
export type CompteProtege = { compteId: string; nom: string; institutionId: string; depuis: string }

export type TypeDelegation = 'MANDAT' | 'PRESTATAIRE'
export type EtatDelegation = 'DEMANDEE' | 'ACCORDEE' | 'REFUSEE' | 'REVOQUEE'
export type EtatEffectifDelegation = 'DEMANDEE' | 'PROGRAMMEE' | 'ACTIVE' | 'ECHUE' | 'REFUSEE' | 'REVOQUEE'

/** Délégation temporaire de MS-IAM : mandat de partenaire (US-EXI-04) ou accès de prestataire (US-EXI-24). */
export type Delegation = {
  id: string
  type: TypeDelegation
  compteId: string
  institutionId: string | null
  permissions: string[]
  composants: string[]
  debut: string
  fin: string
  etat: EtatDelegation
  demandeurId: string
  approbateurId: string | null
  motif: string | null
  creeLe: string
  closeLe: string | null
}

/** Délégation avec son état à l'instant de la lecture et le nom de son titulaire. */
export type VueDelegation = { delegation: Delegation; etat: EtatEffectifDelegation; nomCompte: string; rejouee: boolean }
