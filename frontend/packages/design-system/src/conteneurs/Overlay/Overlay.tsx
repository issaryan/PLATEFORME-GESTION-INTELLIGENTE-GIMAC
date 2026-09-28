import type { ReactNode } from 'react'
import { cx } from '../../lib/cx'

export interface OverlayProps {
  /** Confine le voile au conteneur parent (aperçus, catalogue). */
  inline?: boolean
  align?: 'center' | 'right'
  onClose?: () => void
  children?: ReactNode
}

// Voile gm-color-overlay : plein écran par défaut ; un clic sur le voile ferme.
export function Overlay(p: OverlayProps) {
  return (
    <div
      className={cx('gm-overlay', p.inline && 'gm-overlay-inline', p.align === 'right' && 'gm-overlay-right')}
      onMouseDown={(e) => {
        if (e.target === e.currentTarget && p.onClose) p.onClose()
      }}
    >
      {p.children}
    </div>
  )
}
