import { lazy } from 'react'
import { PERMISSION_ACTIVITE } from '../activite/modele'
import type { EntreeSandbox } from './types'

// Entrée « Activité des tests » (D-121) : statistiques de SBX-LECTURE, lecture SBX_SUP_CONSULTATION ; la chaîne de
// certification lit en plus MS-HOMOLOGATION sous HOM_LIAISON_READ, en mode dégradé sans cette permission.
export const entreeActivite: EntreeSandbox = {
  cle: 'sandboxActivite',
  chemin: 'activite',
  permission: PERMISSION_ACTIVITE,
  routes: [{ chemin: 'activite', page: lazy(() => import('../pages/ActivitePage')) }],
}
