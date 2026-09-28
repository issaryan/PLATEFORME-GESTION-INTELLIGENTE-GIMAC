import type { ReactNode } from 'react'
import { cx } from '../../lib/cx'

export interface PopoverProps {
  /** Nom accessible du contenu ancré. */
  label: string
  title?: string
  width?: number
  children?: ReactNode
  className?: string
}

// Contenu riche ancré : calendrier, filtres avancés, colonnes (§7.5).
export function Popover(p: PopoverProps) {
  return (
    <div className={cx('gm-popover', p.className)} role="dialog" aria-label={p.label} style={p.width ? { width: p.width } : undefined}>
      {p.title ? <div className="gm-popover-title">{p.title}</div> : null}
      {p.children}
    </div>
  )
}
