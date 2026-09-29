import type { GroupeNavigation } from '@gimac/socle'

// Entrées de navigation du lot 1, avec la permission qui les rend visibles (DS-GIMAC-01 §6.2).
export const NAVIGATION_DES: GroupeNavigation[] = [
  // Tableau de bord propre à chaque acteur (D-58) : sections composées selon les permissions du jeton.
  { entrees: [{ cle: 'tableauDeBord', libelleKey: 'nav.tableauDeBord', icone: 'gauge', chemin: '/des/tableau-de-bord' }] },
  {
    libelleKey: 'nav.groupe.registre',
    entrees: [{ cle: 'institutions', libelleKey: 'nav.institutions', icone: 'landmark', chemin: '/des/institutions', permission: 'PAR_INSTITUTION_READ' }],
  },
  {
    libelleKey: 'nav.groupe.homologation',
    entrees: [
      // Étapes 3 à 10 : liste et dossier numérique des liaisons (D-57) ; les écrans DES-08 et DES-09 restent
      // joignables depuis le dossier (« Graphe des opérations »), sans entrée propre dans le menu.
      { cle: 'liaisons', libelleKey: 'nav.liaisons', icone: 'route', chemin: '/des/parcours', permission: 'HOM_LIAISON_LIST' },
      // « Mes projets » : les liaisons confiées au conducteur de projet (D-58), seul à établir le procès-verbal.
      { cle: 'mesProjets', libelleKey: 'nav.mesProjets', icone: 'user', chemin: '/des/parcours?mesProjets=1', permission: 'CER_PV_ESTABLISH' },
      { cle: 'referentiels', libelleKey: 'nav.referentiels', icone: 'kanban', chemin: '/des/referentiels/operations', permission: 'HOM_REFERENTIEL_READ' },
    ],
  },
]
// Portail du participant (D-58) : son tableau de bord, puis ses liaisons et le suivi de chacune.
export const NAVIGATION_PORTAIL: GroupeNavigation[] = [
  {
    entrees: [
      { cle: 'tableauDeBordPortail', libelleKey: 'pp.nav.tableauDeBord', icone: 'gauge', chemin: '/portail/tableau-de-bord', permission: 'HOM_PORTAL_READ' },
      { cle: 'maLiaison', libelleKey: 'pp.nav.mesLiaisons', icone: 'route', chemin: '/portail/liaisons', permission: 'HOM_PORTAL_READ' },
    ],
  },
]
