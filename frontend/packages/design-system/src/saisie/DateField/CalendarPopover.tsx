import { cx } from '../../lib/cx'
import { Button } from '../../actions/Button/Button'
import { IconButton } from '../../actions/IconButton/IconButton'
import { useLibelle } from '../../i18n/LangueProvider'

const DAYS = ['calendrier.lu', 'calendrier.ma', 'calendrier.me', 'calendrier.je', 'calendrier.ve', 'calendrier.sa', 'calendrier.di'] as const

export interface CalendarPopoverProps {
  month?: string
  selected?: number[]
  range?: [number, number]
  shortcuts?: string[]
}

// Calendrier accessible en popover (interne à DateField et DateRangeField).
export function CalendarPopover(p: CalendarPopoverProps) {
  const t = useLibelle()
  const days: number[] = []
  for (let i = 1; i <= 30; i++) days.push(i)
  return (
    <div className="gm-popover gm-cal" role="dialog" aria-label={t('calendrier.label')}>
      <div className="gm-cal-head">
        <IconButton variant="ghost" size="sm" icon="chevron-right" label={t('calendrier.precedent')} className="gm-flip" />
        <strong>{p.month || t('calendrier.moisDefaut')}</strong>
        <IconButton variant="ghost" size="sm" icon="chevron-right" label={t('calendrier.suivant')} />
      </div>
      <div className="gm-cal-grid" role="group" aria-label={t('calendrier.jours')}>
        {DAYS.map((d) => (
          <span key={d} className="gm-cal-dow">{t(d)}</span>
        ))}
        {[null, null].map((_, i) => (
          <span key={'e' + i} />
        ))}
        {days.map((d) => {
          const sel = !!p.selected && p.selected.indexOf(d) >= 0
          const inr = !!p.range && d > p.range[0] && d < p.range[1]
          return (
            <button key={d} type="button" className={cx('gm-cal-day', sel && 'gm-cal-sel', inr && 'gm-cal-inr', d === 28 && 'gm-cal-today')} aria-pressed={sel ? 'true' : 'false'}>
              {d}
            </button>
          )
        })}
      </div>
      <div className="gm-cal-foot">
        {(p.shortcuts || [t('calendrier.aujourdhui'), t('calendrier.j7'), t('calendrier.ceMois')]).map((s) => (
          <Button key={s} size="sm" variant="ghost">{s}</Button>
        ))}
      </div>
      <div className="gm-cal-tz">{t('calendrier.fuseau')}</div>
    </div>
  )
}
