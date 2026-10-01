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
}

/** GET /moi : compte, clés de sécurité, sessions ouvertes et étapes de premier accès restantes. */
export type Moi = { compte: Compte; clesSecurite: number; sessions: number; etapesRestantes: string[] }

export type SessionCompte = { id: string; adresseIp: string | null; debut: string | null; dernierAcces: string | null; applications: string[] }

/** POST /moi/premiere-configuration. */
export type PremiereConfiguration = { telephone: string; langue: LangueCompte; fonction?: string | null; charteAcceptee: boolean }

/** PUT /moi : coordonnées modifiables par le titulaire. */
export type FichePersonnelle = { prenom?: string | null; nom?: string | null; telephone?: string | null; fonction?: string | null; langue?: LangueCompte | null }

/** POST /moi/mot-de-passe (D-65). */
export type ChangementMotDePasse = { actuel: string; nouveau: string }

/** POST /moi/second-facteur/totp/enrolement : secret base32 et URI otpauth, gardés 10 minutes par MS-IAM. */
export type EnrolementTotp = { enrolementId: string; secret: string; uri: string }

/** POST /moi/second-facteur/totp : code de vérification de l'application d'authentification. */
export type ConfirmationTotp = { enrolementId: string; code: string; libelle?: string }

/** Actions exigées de l'administration restant à faire, dans l'ordre (écrans de la console). */
export const actionsFournisseur = (etapes: string[]): ActionRequise[] => ACTIONS_REQUISES.filter((a) => etapes.includes(a))
