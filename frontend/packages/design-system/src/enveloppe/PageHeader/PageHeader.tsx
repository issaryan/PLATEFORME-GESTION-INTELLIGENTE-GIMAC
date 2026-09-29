import type { ReactNode } from 'react'
import { cx } from '../../lib/cx'

export interface PageHeaderProps {
  /** Titre de la page (seul h1 de l'écran). */
  title: ReactNode
  /** Surtitre : rubrique de la navigation, en petites capitales. */
  overline?: string
  /** Ligne sous le titre : accueil, profils (Tag), statut… */
  meta?: ReactNode
  /** Actions de page (une principale au plus). */
  actions?: ReactNode
  className?: string
}

// En-tête d'écran (gabarits G1 à G3, D-63) : assemblage des classes d'en-tête du catalogue, sans style
// propre ; remplace les en-têtes composés à la main dans les pages.
export function PageHeader(p: PageHeaderProps) {
  return (
    <header className={cx('gm-resource-head', p.className)}>
      <div className="gm-col" style={{ gap: 'var(--gm-space-2)' }}>
        {p.overline ? <p className="gm-type-micro" style={{ margin: 0 }}>{p.overline}</p> : null}
        <h1 className="gm-heading-display">{p.title}</h1>
        {p.meta ? <div className="gm-row">{p.meta}</div> : null}
      </div>
      {p.actions ? <div className="gm-row">{p.actions}</div> : null}
    </header>
  )
}
