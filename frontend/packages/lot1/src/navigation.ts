import type { GroupeNavigation } from '@gimac/socle'
import { navigationDepuis } from './fonctionnalites'

export { ADMINISTRATION, PLATEFORME, urlConsole, versConsole } from './fonctionnalites'

// Barre latérale de l'application interne (D-79) : dérivée du registre des fonctionnalités (fonctionnalites.ts),
// chaque entrée visible selon le prédicat de permissions du jeton (DS-GIMAC-01 §6.2), jamais selon le rôle.
export const NAVIGATION_DES: GroupeNavigation[] = navigationDepuis()

// Portail du participant (D-58) : son tableau de bord, puis ses liaisons et le suivi de chacune.
export const NAVIGATION_PORTAIL: GroupeNavigation[] = [
  {
    entrees: [
      { cle: 'tableauDeBordPortail', libelleKey: 'pp.nav.tableauDeBord', icone: 'gauge', chemin: '/portail/tableau-de-bord', permission: 'HOM_PORTAL_READ' },
      { cle: 'maLiaison', libelleKey: 'pp.nav.mesLiaisons', icone: 'route', chemin: '/portail/liaisons', permission: 'HOM_PORTAL_READ' },
    ],
  },
]
