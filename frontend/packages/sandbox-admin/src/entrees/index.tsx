import { Suspense, type ReactNode } from 'react'
import { Navigate, Route, Routes } from 'react-router'
import { Skeleton } from '@gimac/design-system'
import { GardePermission } from '@gimac/lot1'
import { entreeEspaces } from './espaces'
import type { EntreeSandbox } from './types'

// Registre des entrées de l'espace sandbox du DES (D-121, D-122). Chaque entrée vit dans son fichier ; en ajouter une
// ne touche à aucune autre. L'application monte <RoutesSandbox /> sous /des/sandbox/* et ECRANS_DES (lot 1) porte le
// chemin de chaque fonctionnalité pour la barre latérale.
export const ENTREES_SANDBOX: readonly EntreeSandbox[] = [
  entreeEspaces,
  // Entrées suivantes (Supervision de la sandbox, SBX-TRACE) : une ligne chacune.
]

/** Base de l'espace sandbox dans l'application du département. */
export const BASE = '/des/sandbox'

/** Chemins des écrans de l'espace sandbox, par clé de fonctionnalité. */
export const CHEMINS_SANDBOX: Record<string, string> = Object.fromEntries(ENTREES_SANDBOX.map((e) => [e.cle, `${BASE}/${e.chemin}`]))

/**
 * Routes de l'espace sandbox : chaque écran est gardé par la fonctionnalité du catalogue (`garde`, fourni par
 * l'application) puis par la permission de son entrée ; toute autre adresse ramène à la première entrée ouverte.
 */
export function RoutesSandbox({ garde = (_cle, page) => page, entrees = ENTREES_SANDBOX }: { garde?: (cle: string, page: ReactNode) => ReactNode; entrees?: readonly EntreeSandbox[] }) {
  return (
    <Suspense fallback={<Skeleton height={32} />}>
      <Routes>
        {entrees.flatMap((e) =>
          e.routes.map((r) => {
            const Page = r.page
            return <Route key={`${e.cle}:${r.chemin}`} path={r.chemin} element={garde(e.cle, <GardePermission permission={e.permission}><Page /></GardePermission>)} />
          }),
        )}
        <Route path="*" element={<Navigate to={`${BASE}/${entrees[0].chemin}`} replace />} />
      </Routes>
    </Suspense>
  )
}
