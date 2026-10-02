import type { ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { EtatsRequete, type ApiError } from '@gimac/socle'

// Compositions communes des écrans de la console, uniquement à partir du design system.


export function Section({ titre, id, action, children }: { titre: string; id: string; action?: ReactNode; children: ReactNode }) {
  return (
    <section className="gm-col" aria-labelledby={id}>
      <div className="gm-row" style={{ justifyContent: 'space-between' }}>
        <h2 id={id} className="gm-type-h3" style={{ margin: 0 }}>{titre}</h2>
        {action}
      </div>
      {children}
    </section>
  )
}

type Requete<R> = { isPending: boolean; isError: boolean; error: ApiError | null; data: R | undefined; refetch: () => unknown }

/** Les quatre états d'une donnée, libellés traduits. */
export function Donnees<R>({ requete, vide, estVide, children }: { requete: Requete<R>; vide?: string; estVide?: (r: R) => boolean; children: (r: R) => ReactNode }) {
  const { t } = useTranslation()
  return (
    <EtatsRequete etat={requete} isEmpty={estVide} emptyTitle={vide ?? t('commun.vide')} errorTitle={t('commun.erreur.titre')} retryLabel={t('commun.reessayer')}>
      {children}
    </EtatsRequete>
  )
}
