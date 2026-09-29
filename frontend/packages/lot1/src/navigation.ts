import type { GroupeNavigation } from '@gimac/socle'

// Entrées de navigation du lot 1, avec la permission qui les rend visibles (DS-GIMAC-01 §6.2).
export const NAVIGATION_DES: GroupeNavigation[] = [
  {
    libelleKey: 'nav.groupe.homologation',
    entrees: [
      { cle: 'liaisons', libelleKey: 'nav.liaisons', icone: 'route', chemin: '/des/liaisons', permission: 'HOM_LIAISON_LIST' },
      { cle: 'referentiels', libelleKey: 'nav.referentiels', icone: 'kanban', chemin: '/des/referentiels/operations', permission: 'HOM_REFERENTIEL_READ' },
    ],
  },
]
export const NAVIGATION_PORTAIL: GroupeNavigation[] = [
  { entrees: [{ cle: 'maLiaison', libelleKey: 'nav.maLiaison', icone: 'route', chemin: '/portail/liaisons', permission: 'HOM_PORTAL_READ' }] },
]
