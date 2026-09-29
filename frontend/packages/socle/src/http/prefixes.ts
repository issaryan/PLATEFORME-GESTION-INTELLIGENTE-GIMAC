// Préfixes des couches de présentation (PI-GIMAC-01 §0.6). La passerelle retire /bff/<couche>/<service>
// et ajoute /api/v1 (et /api/v1/portail pour la couche portail) avant de relayer au service.
export const P = {
  desHomologation: '/bff/des/homologation',
  desCertification: '/bff/des/certification',
  portailHomologation: '/bff/portail/homologation',
  portailCertification: '/bff/portail/certification',
} as const

export type Couche = 'des' | 'portail'
