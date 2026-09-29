// Modèle de « Mon compte » et du premier accès (D-62) : formes des réponses de MS-IAM
// (MonCompteResource, ComptesResource.CompteVue, SessionCompte), relayées par /bff/<couche>/iam.

/** Actions que le fournisseur d'identité impose à la prochaine authentification (actions requises). */
export type ActionRequise = 'UPDATE_PASSWORD' | 'CONFIGURE_TOTP' | 'UPDATE_PROFILE' | 'VERIFY_EMAIL'
export const ACTIONS_REQUISES: ActionRequise[] = ['UPDATE_PASSWORD', 'CONFIGURE_TOTP', 'UPDATE_PROFILE', 'VERIFY_EMAIL']

/** Étape de premier accès : la fiche (plateforme) ou une action requise (fournisseur d'identité). */
export type EtapePremierAcces = 'FICHE' | ActionRequise

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
}

/** GET /moi : compte, clés de sécurité, sessions ouvertes et étapes de premier accès restantes. */
export type Moi = { compte: Compte; clesSecurite: number; sessions: number; etapesRestantes: string[] }

export type SessionCompte = { id: string; adresseIp: string | null; debut: string | null; dernierAcces: string | null; applications: string[] }

/** POST /moi/premiere-configuration. */
export type PremiereConfiguration = { telephone: string; langue: LangueCompte; fonction?: string | null; charteAcceptee: boolean }

/** PUT /moi : coordonnées modifiables par le titulaire. */
export type FichePersonnelle = { prenom?: string | null; nom?: string | null; telephone?: string | null; fonction?: string | null; langue?: LangueCompte | null }

/** Actions disponibles en libre-service (POST /moi/actions). */
export type ActionLibreService = 'UPDATE_PASSWORD' | 'CONFIGURE_TOTP'

/** Les quatre actions requises du fournisseur d'identité restant à faire, dans l'ordre d'exécution. */
export const actionsFournisseur = (etapes: string[]): ActionRequise[] => ACTIONS_REQUISES.filter((a) => etapes.includes(a))
