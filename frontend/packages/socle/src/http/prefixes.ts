// Préfixes des couches de présentation (PI-GIMAC-01 §0.6). La passerelle retire /bff/<couche>/<service>
// et ajoute /api/v1 (et /api/v1/portail pour la couche portail) avant de relayer au service.
export const P = {
  desHomologation: '/bff/des/homologation',
  desCertification: '/bff/des/certification',
  portailHomologation: '/bff/portail/homologation',
  portailCertification: '/bff/portail/certification',
  desParticipant: '/bff/des/participant',
  desDocument: '/bff/des/document',
  // Aperçu bureautique (D-83) : la console convertit aussi les pièces de sa messagerie.
  consoleDocument: '/bff/console/document',
  // Messagerie instantanée de travail (D-64), MS-COLLABORATION : tout membre interne, DES et console.
  desCollaboration: '/bff/des/collaboration',
  consoleCollaboration: '/bff/console/collaboration',
  // Présence des comptes internes (D-72), servie par la passerelle elle-même.
  desPresence: '/bff/des/presence',
  consolePresence: '/bff/console/presence',
  portailParticipant: '/bff/portail/participant',
  // Notifications de la plateforme (D-80), MS-NOTIFICATION : cloche, liste, préférences ; couches internes.
  desNotification: '/bff/des/notification',
  consoleNotification: '/bff/console/notification',
  // Administration des comptes et de la configuration de l'authentification (D-62) : console, et
  // département pour les comptes, le journal, la politique et les profils (D-69).
  consoleIam: '/bff/console/iam',
  desIam: '/bff/des/iam',
} as const

/** Préfixe de MS-IAM pour une couche : « Mon compte », premier accès et réauthentification (D-62). */
export const prefixeIam = (couche: Couche) => `/bff/${couche}/iam`

/** Déconnexion de la passerelle : ferme la session de la passerelle et celle du fournisseur d'identité, côté serveur. */
export const DECONNEXION = '/api/logout'

/** Session de la passerelle : matrice des rôles, niveau d'assurance (D-61) et fin d'inactivité prévue (D-65 §6). */
export const SESSION = '/api/session'
/** Appareils connectés du compte : sessions ouvertes de la passerelle (docs/lot1/mon-compte.md §3). */
export const APPAREILS = '/api/session/appareils'
/** Historique des connexions du compte, 90 jours (docs/lot1/mon-compte.md §3). */
export const HISTORIQUE_CONNEXIONS = '/api/session/historique'
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
