import { cx } from '../../lib/cx'
import { StatusBadge } from '../../affichage/StatusBadge/StatusBadge'

export interface SlaClockProps {
  label?: string
  elapsedMin: number
  targetMin: number
  frozen?: boolean
  frozenMin?: number
}

function fmt(m: number): string {
  const hh = Math.floor(m / 60)
  const mm = m % 60
  return (hh ? hh + ' h ' : '') + mm + ' min'
}

// Délai imputable d'un incident (§7.7) : engagement, gel en attente de tiers, dépassement.
export function SlaClock(p: SlaClockProps) {
  const pct = Math.min(100, Math.round((p.elapsedMin / p.targetMin) * 100))
  const over = p.elapsedMin > p.targetMin
  return (
    <div className="gm-sla" role="group" aria-label="Délai d'engagement">
      <div className="gm-row" style={{ justifyContent: 'space-between' }}>
        <span className="gm-field-help">{p.label || 'Délai de résolution imputable'}</span>
        {p.frozen ? (
          <StatusBadge status="neutral" label="gelé, en attente de tiers" icon="pause" />
        ) : over ? (
          <StatusBadge status="crit" label="engagement dépassé" />
        ) : (
          <StatusBadge status={pct >= 80 ? 'warn' : 'ok'} label={pct >= 80 ? 'échéance proche' : "dans l'engagement"} />
        )}
      </div>
      <div className="gm-progress" role="progressbar" aria-label={p.label || 'Délai de résolution imputable'} aria-valuenow={pct} aria-valuemin={0} aria-valuemax={100}>
        <div className={cx('gm-progress-bar', over && 'gm-progress-over')} style={{ width: pct + '%' }} />
      </div>
      <div className="gm-row gm-num" style={{ justifyContent: 'space-between', fontSize: 12, color: 'var(--gm-color-text-secondary)' }}>
        <span>{'Écoulé : ' + fmt(p.elapsedMin)}</span>
        <span>{'Engagement : ' + fmt(p.targetMin)}</span>
        {p.frozenMin ? <span>{'Gel : ' + fmt(p.frozenMin)}</span> : null}
      </div>
    </div>
  )
}
