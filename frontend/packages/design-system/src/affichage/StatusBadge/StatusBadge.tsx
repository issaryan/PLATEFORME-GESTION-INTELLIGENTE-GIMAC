import type { ReactNode } from 'react'
import { cx } from '../../lib/cx'
import type { Status } from '../../lib/types'
import { Icon } from '../../icones/Icon/Icon'
import type { IconName } from '../../icones/icons'
import { STATUS_ICON } from '../../icones/statusIcon'

export interface StatusBadgeProps {
  status?: Status
  label?: ReactNode
  /** null retire le pictogramme ; par défaut, pictogramme réservé du statut. */
  icon?: IconName | null
  className?: string
  children?: ReactNode
}

// Puce de statut 24 px : pictogramme + libellé + couleur (§7.3).
export function StatusBadge(p: StatusBadgeProps) {
  const st = p.status || 'neutral'
  const icon = p.icon !== undefined ? p.icon : STATUS_ICON[st]
  return (
    <span className={cx('gm-badge', 'gm-badge-' + st, p.className)}>
      {icon ? <Icon name={icon} /> : null}
      {p.label || p.children}
    </span>
  )
}
