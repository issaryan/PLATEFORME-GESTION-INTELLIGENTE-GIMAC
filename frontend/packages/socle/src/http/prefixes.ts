// Préfixes des couches de présentation (PI-GIMAC-01 §0.6). La passerelle retire /bff/<couche>/<service>
// et ajoute /api/v1 (et /api/v1/portail pour la couche portail) avant de relayer au service.
export const P = {
  desHomologation: '/bff/des/homologation',
  desCertification: '/bff/des/certification',
  portailHomologation: '/bff/portail/homologation',
  portailCertification: '/bff/portail/certification',
  desParticipant: '/bff/des/participant',
  desDocument: '/bff/des/document',
  // Messagerie instantanée de travail (D-64), MS-COLLABORATION : couche du département seulement.
  desCollaboration: '/bff/des/collaboration',
  portailParticipant: '/bff/portail/participant',
  // Administration des comptes et de la configuration de l'authentification (D-62), console seulement.
  consoleIam: '/bff/console/iam',
} as const

/** Préfixe de MS-IAM pour une couche : « Mon compte », premier accès et réauthentification (D-62). */
export const prefixeIam = (couche: Couche) => `/bff/${couche}/iam`

/** Déconnexion de la passerelle : ferme la session de la passerelle et celle du fournisseur d'identité, côté serveur. */
export const DECONNEXION = '/api/logout'

/** Session de la passerelle : matrice des rôles, niveau d'assurance (D-61) et fin d'inactivité prévue (D-65 §6). */
export const SESSION = '/api/session'
/** Écran de connexion de la plateforme (D-65) : route de chaque couche, jamais une page du fournisseur d'identité. */
export const CONNEXION = '/connexion'

/** Routes publiques de connexion de la passerelle (D-65 §2), protégées contre la CSRF. */
export const API_CONNEXION = {
  connexion: '/api/connexion',
  motDePasseOublie: '/api/connexion/mot-de-passe-oublie',
  nouveauMotDePasse: '/api/connexion/nouveau-mot-de-passe',
  /** Activité explicite (« Rester connecté ») : prolonge la session d'inactivité (D-65 §6). */
  prolongation: '/api/session/prolongation',
} as const

export type Couche = 'des' | 'portail' | 'console'
