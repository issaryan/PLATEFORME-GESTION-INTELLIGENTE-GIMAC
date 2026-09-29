import { cx } from '../../lib/cx'
import { Button } from '../../actions/Button/Button'
import { IconButton } from '../../actions/IconButton/IconButton'

const DAYS = ['lu', 'ma', 'me', 'je', 've', 'sa', 'di']

export interface CalendarPopoverProps {
  month?: string
  selected?: number[]
  range?: [number, number]
  shortcuts?: string[]
}

// Calendrier accessible en popover (interne à DateField et DateRangeField).
export function CalendarPopover(p: CalendarPopoverProps) {
  const days: number[] = []
  for (let i = 1; i <= 30; i++) days.push(i)
  return (
    <div className="gm-popover gm-cal" role="dialog" aria-label="Calendrier">
      <div className="gm-cal-head">
        <IconButton variant="ghost" size="sm" icon="chevron-right" label="Mois précédent" className="gm-flip" />
        <strong>{p.month || 'Septembre 2026'}</strong>
        <IconButton variant="ghost" size="sm" icon="chevron-right" label="Mois suivant" />
      </div>
      <div className="gm-cal-grid" role="group" aria-label="Jours du mois">
        {DAYS.map((d) => (
          <span key={d} className="gm-cal-dow">{d}</span>
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
        {(p.shortcuts || ["aujourd'hui", 'J-7', 'ce mois']).map((s) => (
          <Button key={s} size="sm" variant="ghost">{s}</Button>
        ))}
      </div>
      <div className="gm-cal-tz">Fuseau : Afrique centrale (UTC+1)</div>
    </div>
  )
}
