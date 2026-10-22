import { lazy } from 'react'
import { PERMISSION } from '../modele'
import type { EntreeSandbox } from './types'

// Entrée « Espaces des institutions » (D-118 point 3, D-122) : SBX-ESPACE, lecture SBX_HAB_CONSULTATION.
export const entreeEspaces: EntreeSandbox = {
  cle: 'sandboxEspaces',
  chemin: 'espaces',
  permission: PERMISSION.consultation,
  routes: [
    { chemin: 'espaces', page: lazy(() => import('../pages/EspacesPage')) },
    { chemin: 'espaces/:code', page: lazy(() => import('../pages/EspacePage')) },
  ],
}
