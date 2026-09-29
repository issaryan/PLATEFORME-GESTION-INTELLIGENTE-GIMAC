import type { ReactNode } from 'react'
import { useQuery, type UseQueryOptions } from '@tanstack/react-query'
import { Button, EmptyState, InlineAlert, Skeleton } from '@gimac/design-system'
import { useTranslation } from 'react-i18next'
import type { ApiError } from '../http/errors'

export type QueryBoundaryProps<R> = {
  query: UseQueryOptions<R, ApiError>
  isEmpty?: (r: R) => boolean
  /** Clé i18n du titre de l'état vide. */
  emptyKey?: string
  children: (r: R) => ReactNode
}

// Les quatre états d'une donnée (DS-GIMAC-01 §0.4, règle 5) : chargement, vide, erreur avec
// identifiant de corrélation et réessai, nominal. Seul point de rendu des requêtes d'onglet.
export function QueryBoundary<R>({ query, isEmpty, emptyKey = 'commun.vide', children }: QueryBoundaryProps<R>) {
  const { t } = useTranslation()
  const q = useQuery(query)
  return <EtatsRequete etat={q} isEmpty={isEmpty} emptyTitle={t(emptyKey)} errorTitle={t('commun.erreur.titre')} retryLabel={t('commun.reessayer')}>{children}</EtatsRequete>
}

type EtatQ<R> = { isPending: boolean; isError: boolean; error: ApiError | null; data: R | undefined; refetch: () => unknown }

/** Rendu des quatre états à partir d'une requête déjà lancée (hook de données). */
export function EtatsRequete<R>(p: {
  etat: EtatQ<R>
  isEmpty?: (r: R) => boolean
  emptyTitle: string
  errorTitle: string
  retryLabel: string
  children: (r: R) => ReactNode
}) {
  const { etat } = p
  if (etat.isPending) {
    return (
      <div className="gm-col" style={{ gap: 'var(--gm-space-2)' }} aria-busy="true">
        <Skeleton width="40%" height={22} />
        <Skeleton />
        <Skeleton width="85%" />
      </div>
    )
  }
  if (etat.isError || etat.data === undefined) {
    return (
      <InlineAlert
        level="crit"
        title={p.errorTitle}
        correlationId={etat.error?.correlationId || undefined}
        action={<Button size="sm" icon="refresh" onClick={() => void etat.refetch()}>{p.retryLabel}</Button>}
      >
        {etat.error?.message}
      </InlineAlert>
    )
  }
  if (p.isEmpty?.(etat.data)) return <EmptyState title={p.emptyTitle} />
  return <>{p.children(etat.data)}</>
}
