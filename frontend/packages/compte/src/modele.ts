// Modèle de « Mon compte » et du premier accès (D-62) : formes des réponses de MS-IAM
// (MonCompteResource, ComptesResource.CompteVue, SessionCompte), relayées par /bff/<couche>/iam.

/** Actions exigées par l’administration (console, D-62), tenues par MS-IAM (attribut gimac_actions_requises, D-65). */
export type ActionRequise = 'MOT_DE_PASSE' | 'SECOND_FACTEUR' | 'FICHE' | 'VERIFIER_COURRIEL'
export const ACTIONS_REQUISES: ActionRequise[] = ['MOT_DE_PASSE', 'SECOND_FACTEUR', 'FICHE', 'VERIFIER_COURRIEL']

/**
 * Étapes de l'assistant de premier accès (D-65), dans l'ordre : mot de passe définitif, code TOTP,
 * fiche, charte. Toutes se font sur la plateforme ; la connexion suit avec le nouveau mot de passe.
 */
export type EtapePremierAcces = 'MOT_DE_PASSE' | 'SECOND_FACTEUR' | 'FICHE' | 'CHARTE'
export const ETAPES_ASSISTANT: EtapePremierAcces[] = ['MOT_DE_PASSE', 'SECOND_FACTEUR', 'FICHE', 'CHARTE']

// Noms historiques des actions exigées (actions requises du fournisseur d'identité), ramenés aux étapes.
const ALIAS_ETAPES: Record<string, EtapePremierAcces> = { UPDATE_PASSWORD: 'MOT_DE_PASSE', CONFIGURE_TOTP: 'SECOND_FACTEUR', UPDATE_PROFILE: 'FICHE' }

/** Étapes de l'assistant restant à faire, dans l'ordre, depuis les étapes restantes de MS-IAM. */
export const etapesAssistant = (etapes: string[]): EtapePremierAcces[] => {
  const vues = new Set(etapes.map((e) => ALIAS_ETAPES[e] ?? e))
  return ETAPES_ASSISTANT.filter((e) => vues.has(e))
}

export const LANGUES_COMPTE = ['fr', 'en', 'es'] as const
export type LangueCompte = (typeof LANGUES_COMPTE)[number]

/** Compte tel que MS-IAM le décrit (aucun secret). Dates au format ISO 8601. */
export type Compte = {
  id: string
  identifiant: string
  prenom: string | null
  nom: string | null
  nomAffiche: string
  email: string | null
  emailVerifie: boolean
  actif: boolean
  creeLe: string | null
  telephone: string | null
  fonction: string | null
  langue: string | null
  institutionId: string | null
  profilComplet: boolean
  premierAccesEnAttente: boolean
  profils: string[]
  actionsRequises: string[]
  secondFacteur: boolean
  /** Version de la photo de profil (empreinte courte de MS-IAM), null sans photo (docs/lot1/mon-compte.md §1). */
  avatarVersion?: string | null
  /** Rattachement organisationnel (D-76, docs/lot1/mon-activite.md §1.2) : code de l'unité la plus fine, null sans unité. */
  unite?: string | null
  /** Identifiant du compte responsable (n+1), null sans responsable. */
  responsableId?: string | null
}

/**
 * Rattachement du titulaire (GET /bff/des/iam/organisation/moi, D-76 §1.3) : unité et son chemin, responsable,
 * chaîne des responsables et nombre de comptes dont il est le n+1. Lu par « Mon compte » pour les libellés.
 */
export type UniteRattachement = { code: string; libelle: string; type: string; parent: string | null; active: boolean }
export type RattachementCompte = {
  id: string
  nomAffiche: string
  unite: UniteRattachement | null
  chemin: UniteRattachement[]
  responsable: { id: string; nomAffiche: string; fonction: string | null } | null
  chaine: string[]
  equipeDirecte: number
}

/** GET /moi : compte, clés de sécurité, sessions ouvertes et étapes de premier accès restantes. */
export type Moi = { compte: Compte; clesSecurite: number; sessions: number; etapesRestantes: string[] }

export type SessionCompte = { id: string; adresseIp: string | null; debut: string | null; dernierAcces: string | null; applications: string[] }

/** POST /moi/premiere-configuration. */
export type PremiereConfiguration = { telephone: string; langue: LangueCompte; fonction?: string | null; charteAcceptee: boolean }

/** PUT /moi : coordonnées modifiables par le titulaire. */
export type FichePersonnelle = { prenom?: string | null; nom?: string | null; telephone?: string | null; fonction?: string | null; langue?: LangueCompte | null }

/** POST /moi/mot-de-passe (D-65) ; fermerAutresSessions ferme les autres sessions du compte (mon-compte.md §2). */
export type ChangementMotDePasse = { actuel: string; nouveau: string; fermerAutresSessions?: boolean }

/** Photo de profil (mon-compte.md §1) : formats, poids et dimensions admis par MS-IAM, vérifiés aussi avant l'envoi. */
export const PHOTO_PROFIL = {
  types: ['image/png', 'image/jpeg', 'image/webp'] as readonly string[],
  tailleMaxOctets: 2 * 1024 * 1024,
  dimensionMin: 64,
  dimensionMax: 4096,
  /** Côté de l'image envoyée après recadrage ; MS-IAM la ramène à 256 × 256 et la réencode. */
  coteEnvoi: 512,
} as const

/** PUT /moi/avatar : nouvelle version de la photo. */
export type VersionPhoto = { version: string }

/** Appareil déduit de l'en-tête User-Agent à la connexion (mon-compte.md §3). */
export type TypeAppareil = 'ORDINATEUR' | 'MOBILE' | 'TABLETTE' | 'INCONNU'
export type DescriptionAppareil = { navigateur: string | null; systeme: string | null; type: TypeAppareil }

/** GET /api/session/appareils : session ouverte du compte (identifiant opaque, IP masquée hors session courante). */
export type Appareil = {
  id: string
  courante: boolean
  appareil: DescriptionAppareil
  adresseIp: string | null
  couche: string
  ouverteLe: string
  derniereActivite: string | null
  expireA: string | null
}

export type ResultatConnexion = 'REUSSIE' | 'ECHEC_MOT_DE_PASSE' | 'ECHEC_CODE' | 'BLOQUEE'
export const RESULTATS_CONNEXION: ResultatConnexion[] = ['REUSSIE', 'ECHEC_MOT_DE_PASSE', 'ECHEC_CODE', 'BLOQUEE']

/** Tentative de connexion de l'historique (90 jours, la plus récente d'abord). */
export type Connexion = { le: string; resultat: ResultatConnexion; appareil: DescriptionAppareil | string | null; adresseIp: string | null; couche: string | null }

/** GET /api/session/historique?page=&taille= */
export type PageHistorique = { contenu: Connexion[]; total: number }

/** POST /moi/second-facteur/totp/enrolement : secret base32 et URI otpauth, gardés 10 minutes par MS-IAM. */
export type EnrolementTotp = { enrolementId: string; secret: string; uri: string }

/** POST /moi/second-facteur/totp : code de vérification de l'application d'authentification. */
export type ConfirmationTotp = { enrolementId: string; code: string; libelle?: string }

/** Actions exigées de l'administration restant à faire, dans l'ordre (écrans de la console). */
export const actionsFournisseur = (etapes: string[]): ActionRequise[] => ACTIONS_REQUISES.filter((a) => etapes.includes(a))
