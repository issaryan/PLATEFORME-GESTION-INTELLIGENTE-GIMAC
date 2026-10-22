// Sandbox GIMAC dans la console (D-119, docs/lot1/sandbox-existant-adapte.md §6) : contrat exact des routes
// /api/v1/sandbox de MS-IAM (SandboxConsoleResource), relayées par /bff/console/iam/sandbox.

/** Page rendue par MS-IAM (page à partir de 0, 100 lignes au plus). */
export type PageSandbox<T> = { contenu: T[]; total: number; page: number; taille: number }

/** Indicateur de suspension posé sur le périmètre d'une institution par la saga SUSPENSION de SBX-ESPACE. */
export type PerimetreSuspendu = { code: string; depuis: string; acteurId: string; motif: string | null }

/** État d'un périmètre. */
export type EtatPerimetre = { code: string; suspendu: boolean; depuis: string | null; sessionsRevoquees: number; rejouee: boolean }

/** Compte protégé : dernier administrateur actif de son institution. */
export type CompteProtege = { compteId: string; nom: string; institutionId: string; depuis: string }
