import { cx } from '../../lib/cx'
import { StatusBadge } from '../../affichage/StatusBadge/StatusBadge'
import { useLibelle } from '../../i18n/LangueProvider'

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
  const t = useLibelle()
  const pct = Math.min(100, Math.round((p.elapsedMin / p.targetMin) * 100))
  const over = p.elapsedMin > p.targetMin
  return (
    <div className="gm-sla" role="group" aria-label={t('sla.label')}>
      <div className="gm-row" style={{ justifyContent: 'space-between' }}>
        <span className="gm-field-help">{p.label || t('sla.delai')}</span>
        {p.frozen ? (
          <StatusBadge status="neutral" label={t('sla.gele')} icon="pause" />
        ) : over ? (
          <StatusBadge status="crit" label={t('sla.depasse')} />
        ) : (
          <StatusBadge status={pct >= 80 ? 'warn' : 'ok'} label={pct >= 80 ? t('sla.proche') : t('sla.dans')} />
        )}
      </div>
      <div className="gm-progress" role="progressbar" aria-label={p.label || t('sla.delai')} aria-valuenow={pct} aria-valuemin={0} aria-valuemax={100}>
        <div className={cx('gm-progress-bar', over && 'gm-progress-over')} style={{ width: pct + '%' }} />
      </div>
      <div className="gm-row gm-num" style={{ justifyContent: 'space-between', fontSize: 12, color: 'var(--gm-color-text-secondary)' }}>
        <span>{t('sla.ecoule', { duree: fmt(p.elapsedMin) })}</span>
        <span>{t('sla.engagement', { duree: fmt(p.targetMin) })}</span>
        {p.frozenMin ? <span>{t('sla.gel', { duree: fmt(p.frozenMin) })}</span> : null}
      </div>
    </div>
  )
}
