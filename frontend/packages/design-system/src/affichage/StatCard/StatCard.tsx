import type { ReactNode } from 'react'
import { cx } from '../../lib/cx'
import type { Status } from '../../lib/types'
import { Icon } from '../../icones/Icon/Icon'
import { StatusBadge } from '../StatusBadge/StatusBadge'

export interface StatCardProps {
  label: ReactNode
  value: ReactNode
  unit?: string
  trend?: { delta: string; direction: 'up' | 'down' }
  threshold?: string
  status?: Status
  statusLabel?: string
  onClick?: () => void
  className?: string
}

// Carte d'indicateur (§7.3).
export function StatCard(p: StatCardProps) {
  const st = p.status
  const t = p.trend
  return (
    <div className={cx('gm-card', 'gm-stat', p.className)} role={p.onClick ? 'button' : undefined} tabIndex={p.onClick ? 0 : undefined} onClick={p.onClick}>
      <div className="gm-stat-label">{p.label}</div>
      <div className="gm-stat-value">
        {p.value}
        {p.unit ? <span className="gm-stat-unit">{p.unit}</span> : null}
      </div>
      <div className="gm-stat-meta">
        {t ? (
          <span className="gm-row" style={{ gap: '2px' }}>
            <Icon name={t.direction === 'up' ? 'arrow-up' : 'arrow-down'} />
            {t.delta}
          </span>
        ) : null}
        {p.threshold ? <span>{'seuil ' + p.threshold}</span> : null}
        {st ? <StatusBadge status={st} label={p.statusLabel || st} /> : null}
      </div>
    </div>
  )
}
