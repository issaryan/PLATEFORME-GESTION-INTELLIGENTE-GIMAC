import { useState, type ReactNode } from 'react'
import { cx } from '../../lib/cx'

export interface TooltipProps {
  /** Texte court, jamais de contenu essentiel. */
  text: string
  placement?: 'top' | 'bottom'
  open?: boolean
  children?: ReactNode
}

// Infobulle au survol et au focus (§7.5).
export function Tooltip(p: TooltipProps) {
  const [open, setOpen] = useState(!!p.open)
  return (
    <span
      className="gm-tip-wrap"
      onMouseEnter={() => setOpen(true)}
      onMouseLeave={() => setOpen(false)}
      onFocus={() => setOpen(true)}
      onBlur={() => setOpen(false)}
    >
      {p.children}
      {open ? <span role="tooltip" className={cx('gm-tip', 'gm-tip-' + (p.placement || 'top'))}>{p.text}</span> : null}
    </span>
  )
}
