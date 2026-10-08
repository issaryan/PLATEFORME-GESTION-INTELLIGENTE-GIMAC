import type { GroupeNavigation } from '@gimac/socle'

export { ADMINISTRATION, PLATEFORME, urlConsole, versConsole } from './fonctionnalites'

// Barre latérale de l'application interne (D-94) : dérivée du catalogue servi par MS-IAM (useNavigationCatalogue).
// Le portail garde la sienne : un compte d'institution ne lit pas le catalogue (403 IAM_COMPTE_INTERNE_EXIGE).

// Portail du participant (D-58) : son tableau de bord, puis ses liaisons et le suivi de chacune.
export const NAVIGATION_PORTAIL: GroupeNavigation[] = [
  {
    entrees: [
      { cle: 'tableauDeBordPortail', libelleKey: 'pp.nav.tableauDeBord', icone: 'gauge', chemin: '/portail/tableau-de-bord', permission: 'HOM_PORTAL_READ' },
      { cle: 'maLiaison', libelleKey: 'pp.nav.mesLiaisons', icone: 'route', chemin: '/portail/liaisons', permission: 'HOM_PORTAL_READ' },
    ],
  },
]
