import type { ReactNode } from 'react'
import { cx } from '../../lib/cx'
import { Icon } from '../../icones/Icon/Icon'
import type { IconName } from '../../icones/icons'

export interface ToastProps {
  action?: ReactNode
  children?: ReactNode
  /** Pictogramme du retour (coche par défaut) : une cloche ou une bulle pour une notification reçue. */
  icon?: IconName
  className?: string
}

// Retour d'action non bloquant (§7.5).
export function Toast(p: ToastProps) {
  return (
    <div className={cx('gm-toast', p.className)} role="status">
      <Icon name={p.icon ?? 'circle-check'} size={20} />
      <span style={{ flex: 1, minWidth: 0 }}>{p.children}</span>
      {p.action || null}
    </div>
  )
}
