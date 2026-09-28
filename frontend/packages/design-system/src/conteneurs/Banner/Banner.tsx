import type { ReactNode } from 'react'
import { cx } from '../../lib/cx'
import { Icon } from '../../icones/Icon/Icon'
import { STATUS_ICON } from '../../icones/statusIcon'
import { IconButton } from '../../actions/IconButton/IconButton'

export type AlertLevel = 'info' | 'warn' | 'crit'

export interface BannerProps {
  level?: AlertLevel
  title?: ReactNode
  action?: ReactNode
  onDismiss?: () => void
  children?: ReactNode
}

// Bandeau pleine largeur sous la barre supérieure (§7.5).
export function Banner(p: BannerProps) {
  const lvl = p.level || 'info'
  return (
    <div className={cx('gm-banner', 'gm-banner-' + lvl)} role={lvl === 'crit' ? 'alert' : 'status'}>
      <Icon name={STATUS_ICON[lvl] || 'info'} size={20} />
      <div className="gm-banner-text">
        {p.title ? <span className="gm-banner-title">{p.title} </span> : null}
        {p.children}
      </div>
      {p.action || null}
      {p.onDismiss ? <IconButton variant="ghost" size="sm" icon="x" label="Fermer" onClick={p.onDismiss} /> : null}
    </div>
  )
}
