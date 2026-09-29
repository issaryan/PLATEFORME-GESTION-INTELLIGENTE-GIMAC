import { useCallback, useEffect, useState, type ReactNode } from 'react'
import { Skeleton } from '@gimac/design-system'
import { useMoi } from './hooks'
import PremierAccesPage from './pages/PremierAccesPage'

/**
 * Garde de premier accès (D-62), placée après GardeSession : lit /bff/<couche>/iam/moi ; tant que des
 * étapes restent (fiche, actions requises), l'assistant de premier accès remplace toutes les pages.
 * `children` reçoit l'assistant à afficher (ou un squelette pendant la lecture), null sinon, pour que
 * l'application le place dans l'enveloppe commune, sans navigation. MS-IAM injoignable : les pages
 * restent servies (mode dégradé), chaque service refaisant ses contrôles.
 */
export function GardePremierAcces({ children }: { children: (assistant: ReactNode | null) => ReactNode }) {
  const moi = useMoi()
  const [enCours, setEnCours] = useState(false)
  const [termine, setTermine] = useState(false)
  const restantes = moi.data?.etapesRestantes.length ?? 0
  useEffect(() => {
    if (restantes > 0) setEnCours(true)
  }, [restantes])
  const terminer = useCallback(() => setTermine(true), [])

  if (moi.isPending) return <>{children(<Skeleton height={32} />)}</>
  if (!moi.data || termine || (!enCours && restantes === 0)) return <>{children(null)}</>
  return <>{children(<PremierAccesPage moi={moi.data} onTermine={terminer} />)}</>
}
