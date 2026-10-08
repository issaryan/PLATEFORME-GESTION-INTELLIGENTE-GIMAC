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

/**
 * Profil par fonction (rôle composite) et ses permissions (D-58) ; libellé (1 à 80, attribut gimac_libelle, le code à
 * défaut) et description (0 à 500) modifiables (D-86 §2), dernière modification depuis la console (null sinon).
 */
export type Profil = {
  code: string
  libelle?: string
  description: string | null
  modifieLe?: string | null
  modifiePar?: { id: string; nomAffiche: string } | null
  permissions: string[]
  privilegie: boolean
  titulaires: number
  /** Profil créé par l'administration (D-81 §5), supprimable sans titulaire ; les profils livrés ne le sont pas. */
  cree?: boolean
  /** Profil externe (compte d'institution, portail ; D-94 §6.1), modifiable tant que le profil n'a pas de titulaire. */
  externe?: boolean
}

/** Bornes de la présentation d'un profil (D-86 §5), après retrait des espaces de tête et de fin. */
export const LIMITES_PROFIL = { libelleMax: 80, descriptionMax: 500 } as const

/**
 * Domaines de permissions présentés dans la console (INTERNAL, réservé aux services, exclu) ; Mon activité (PIL),
 * messagerie (COL) et notifications (NOT) depuis la vue avancée de la composition par fonctionnalité (D-90).
 */
export const DOMAINES_PERMISSIONS = ['HOM', 'CER', 'PAR', 'PIL', 'COL', 'NOT', 'IAM', 'PLATFORM'] as const
