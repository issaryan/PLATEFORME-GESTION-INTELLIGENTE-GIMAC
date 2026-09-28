import type { ReactNode } from 'react'
import { cx } from '../../lib/cx'

export interface PanelProps {
  className?: string
  children?: ReactNode
}

// Cartouche sur fond surface-sunken (§7.5).
export function Panel(p: PanelProps) {
  return <div className={cx('gm-panel', p.className)}>{p.children}</div>
}
