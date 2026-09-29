import type { ReactNode } from 'react'
import { cx } from '../../lib/cx'

export interface ActionBarProps {
  /** Alignement des boutons : à droite (défaut, pied de formulaire), écartés ou à gauche. */
  align?: 'end' | 'between' | 'start'
  /** Nom du groupe de boutons, s'il doit être annoncé. */
  label?: string
  className?: string
  children?: ReactNode
}

const ALIGNEMENTS = { end: 'flex-end', between: 'space-between', start: 'flex-start' } as const

// Rangée de boutons (pied de carte ou de formulaire, barre d'outils d'une section, D-63) : remplace les
// rangées alignées à la main dans les pages.
export function ActionBar(p: ActionBarProps) {
  return (
    <div className={cx('gm-row', p.className)} role={p.label ? 'group' : undefined} aria-label={p.label} style={{ justifyContent: ALIGNEMENTS[p.align || 'end'] }}>
      {p.children}
    </div>
  )
}
