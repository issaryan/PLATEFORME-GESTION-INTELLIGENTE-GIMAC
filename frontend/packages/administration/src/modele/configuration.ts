// Configuration de l'authentification depuis la console (D-62) : formes de
// ConfigurationAuthentificationResource de MS-IAM (/bff/console/iam/configuration).

/** Politique de sécurité du realm : mots de passe, second facteur, sessions, force brute (16 champs). */
export type PolitiqueSecurite = {
  longueurMinimale: number
  majusculesMin: number
  minusculesMin: number
  chiffresMin: number
  speciauxMin: number
  historique: number
  expirationJours: number
  interditIdentifiant: boolean
  otpChiffres: number
  otpPeriodeSecondes: number
  otpAlgorithme: string
  sessionInactiviteMinutes: number
  sessionDureeMaxHeures: number
  forceBruteActive: boolean
  forceBruteEchecsMax: number
  forceBruteAttenteMinutes: number
}

export const ALGORITHMES_OTP = ['HmacSHA1', 'HmacSHA256', 'HmacSHA512'] as const
export const CHIFFRES_OTP = [6, 8] as const

/** Planchers de MS-IAM (PolitiqueMotDePasse.verifier), rappelés en aide ; seul le service décide. */
export const PLANCHERS: Partial<Record<keyof PolitiqueSecurite, [number, number]>> = {
  longueurMinimale: [12, 128],
  historique: [0, 24],
  expirationJours: [0, 365],
  otpPeriodeSecondes: [30, 60],
  sessionInactiviteMinutes: [5, 120],
  sessionDureeMaxHeures: [1, 24],
  forceBruteEchecsMax: [3, 10],
  forceBruteAttenteMinutes: [1, 1440],
}

/** Serveur d'envoi des invitations ; le mot de passe n'est jamais relu (motDePasseDefini seulement). */
export type ConfigurationMessagerie = {
  hote: string
  port: number
  expediteur: string
  nomExpediteur: string
  starttls: boolean
  ssl: boolean
  authentification: boolean
  utilisateur: string | null
  motDePasse: string | null
  motDePasseDefini: boolean
}

/** Profil par fonction (rôle composite) et ses permissions (D-58). */
export type Profil = { code: string; description: string | null; permissions: string[]; privilegie: boolean; titulaires: number }

/** Domaines de permissions présentés dans la console (INTERNAL, réservé aux services, exclu). */
export const DOMAINES_PERMISSIONS = ['HOM', 'CER', 'PAR', 'IAM', 'PLATFORM'] as const
