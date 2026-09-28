import type { ReactNode } from 'react'
import { cx } from '../../lib/cx'
import { Icon } from '../../icones/Icon/Icon'
import { STATUS_ICON } from '../../icones/statusIcon'
import { CorrelationIdChip } from '../../affichage/CorrelationIdChip/CorrelationIdChip'
import type { AlertLevel } from '../Banner/Banner'

export interface InlineAlertProps {
  level?: AlertLevel
  title?: ReactNode
  correlationId?: string
  action?: ReactNode
  children?: ReactNode
}

// Message contextuel dans une carte ou un formulaire (§7.5).
export function InlineAlert(p: InlineAlertProps) {
  const lvl = p.level || 'info'
  return (
    <div className={cx('gm-inline', 'gm-banner-' + lvl)} role={lvl === 'crit' ? 'alert' : 'status'} style={{ borderLeft: 'var(--gm-border-4) solid' }}>
      <Icon name={STATUS_ICON[lvl] || 'info'} size={20} />
      <div className="gm-banner-text">
        {p.title ? <div className="gm-banner-title">{p.title}</div> : null}
        {p.children}
        {p.correlationId ? <div style={{ marginTop: 'var(--gm-space-2)' }}><CorrelationIdChip id={p.correlationId} /></div> : null}
      </div>
      {p.action || null}
    </div>
  )
}
