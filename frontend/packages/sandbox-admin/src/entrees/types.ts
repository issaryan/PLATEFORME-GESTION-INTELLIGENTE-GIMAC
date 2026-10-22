import type { ComponentType } from 'react'

/**
 * Entrée de l'espace sandbox du DES (D-121, D-122) : un écran de la navigation, sa fonctionnalité au catalogue de
 * MS-IAM (`cle`, groupe « sandbox »), son chemin sous /des/sandbox, la permission qui le garde et ses routes.
 * Une nouvelle entrée (par exemple « Supervision », SBX-TRACE) s'ajoute dans son propre fichier et à ENTREES_SANDBOX.
 */
export type EntreeSandbox = {
  cle: string
  chemin: string
  permission: string
  routes: { chemin: string; page: ComponentType }[]
}
