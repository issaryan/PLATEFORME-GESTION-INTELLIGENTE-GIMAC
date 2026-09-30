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

/** Déconnexion de la passerelle : invalide la session et renvoie l'adresse de fin de session du fournisseur d'identité. */
export const DECONNEXION = '/api/logout'

/** Session de la passerelle : matrice des rôles et niveau d'assurance (D-61). */
export const SESSION = '/api/session'
/** Connexion par la passerelle (OIDC, Keycloak exige le second facteur). */
export const CONNEXION = '/oauth2/authorization/oidc'

export type Couche = 'des' | 'portail' | 'console'
