import { FieldWrapper } from '../FieldWrapper/FieldWrapper'
import { Icon } from '../../icones/Icon/Icon'
import { useFieldId2 } from '../../lib/useFieldId2'
import type { BaseProps } from '../../lib/types'
import { CalendarPopover } from './CalendarPopover'
import { useLibelle } from '../../i18n/LangueProvider'

interface DateBaseProps extends BaseProps {
  id?: string
  name?: string
  label: string
  required?: boolean
  help?: string
  error?: string
  disabled?: boolean
}

export interface DateFieldProps extends DateBaseProps {
  /** Date au format JJ/MM/AAAA. */
  value: string
  onChange?: (v: string) => void
  min?: string
  max?: string
  /** Calendrier ouvert. */
  open?: boolean
  selectedDay?: number
}

// Saisie de date au clavier (JJ/MM/AAAA) avec calendrier en popover (§7.2).
export function DateField(p: DateFieldProps) {
  const t = useLibelle()
  const id = useFieldId2(p)
  return (
    <FieldWrapper id={id} label={p.label} required={p.required} help={p.help || t('date.format')} error={p.error} className={p.className}>
      <div className="gm-input-wrap">
        <input
          id={id}
          name={p.name}
          type="text"
          inputMode="numeric"
          className="gm-input gm-num"
          value={p.value}
          aria-invalid={p.error ? 'true' : undefined}
          disabled={p.disabled}
          onChange={(e) => p.onChange && p.onChange(e.target.value)}
        />
        <button type="button" className="gm-input-btn" aria-label={t('date.ouvrir')} aria-expanded={p.open ? 'true' : 'false'}>
          <Icon name="calendar" size={20} />
        </button>
      </div>
      {p.open ? <CalendarPopover selected={[p.selectedDay || 28]} /> : null}
    </FieldWrapper>
  )
}

export interface DateTimeFieldProps extends DateBaseProps {
  date: string
  time: string
  onChange?: (v: { date: string; time: string }) => void
}

// Date et heure locale (UTC+1).
export function DateTimeField(p: DateTimeFieldProps) {
  const t = useLibelle()
  const id = useFieldId2(p)
  return (
    <FieldWrapper id={id} label={p.label} required={p.required} help={p.help || t('date.formatHeure')} error={p.error} className={p.className}>
      <div className="gm-row" style={{ flexWrap: 'nowrap' }}>
        <div className="gm-input-wrap" style={{ flex: 2 }}>
          <input id={id} type="text" inputMode="numeric" className="gm-input gm-num" value={p.date} onChange={(e) => p.onChange && p.onChange({ date: e.target.value, time: p.time })} />
          <button type="button" className="gm-input-btn" aria-label={t('date.ouvrir')}>
            <Icon name="calendar" size={20} />
          </button>
        </div>
        <input type="text" inputMode="numeric" aria-label={t('date.heure')} className="gm-input gm-num" style={{ flex: 1 }} value={p.time} onChange={(e) => p.onChange && p.onChange({ date: p.date, time: e.target.value })} />
      </div>
    </FieldWrapper>
  )
}

export interface DateRangeFieldProps extends DateBaseProps {
  from: string
  to: string
  onChange?: (v: { from: string; to: string }) => void
  open?: boolean
}

// Plage de dates : début et fin, plage surlignée dans le calendrier.
export function DateRangeField(p: DateRangeFieldProps) {
  const t = useLibelle()
  const id = useFieldId2(p)
  return (
    <FieldWrapper id={id} label={p.label} required={p.required} help={p.help || t('date.formatPlage')} error={p.error} className={p.className}>
      <div className="gm-row" style={{ flexWrap: 'nowrap' }}>
        <input id={id} type="text" inputMode="numeric" className="gm-input gm-num" aria-label={t('date.debut')} value={p.from} onChange={(e) => p.onChange && p.onChange({ from: e.target.value, to: p.to })} />
        <span className="gm-field-help">{t('date.au')}</span>
        <div className="gm-input-wrap" style={{ flex: 1 }}>
          <input type="text" inputMode="numeric" className="gm-input gm-num" aria-label={t('date.fin')} value={p.to} onChange={(e) => p.onChange && p.onChange({ from: p.from, to: e.target.value })} />
          <button type="button" className="gm-input-btn" aria-label={t('date.ouvrir')}>
            <Icon name="calendar" size={20} />
          </button>
        </div>
      </div>
      {p.open ? <CalendarPopover selected={[21, 28]} range={[21, 28]} /> : null}
    </FieldWrapper>
  )
}
