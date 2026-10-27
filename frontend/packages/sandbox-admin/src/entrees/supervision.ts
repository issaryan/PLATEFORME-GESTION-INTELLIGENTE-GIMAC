import { lazy } from 'react'
import { PERMISSION_SUPERVISION } from '../modele'
import type { EntreeSandbox } from './types'

// Entrée « Supervision de la sandbox » (D-121) : quatre lectures de SBX-TRACE, sans appel aux services de monitoring.
export const entreeSupervision: EntreeSandbox = {
  cle: 'sandboxSupervision',
  chemin: 'supervision',
  permission: PERMISSION_SUPERVISION,
  routes: [{ chemin: 'supervision', page: lazy(() => import('../pages/SupervisionPage')) }],
}
