import { useState } from 'react'
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

/** Jour de Douala (UTC+1) : année, mois (0 à 11) et jour. */
const jourDouala = () => {
  const d = new Date(Date.now() + 3_600_000)
  return { annee: d.getUTCFullYear(), mois: d.getUTCMonth(), jour: d.getUTCDate() }
}
/** Saisie JJ/MM/AAAA (ou AAAA-MM-JJ) lue en jour existant ; null sinon. */
const lireJour = (v: string) => {
  const m = /^(\d{2})\/(\d{2})\/(\d{4})$/.exec(v.trim()) ?? null
  const iso = /^(\d{4})-(\d{2})-(\d{2})$/.exec(v.trim())
  const [annee, mois, jour] = m ? [Number(m[3]), Number(m[2]) - 1, Number(m[1])] : iso ? [Number(iso[1]), Number(iso[2]) - 1, Number(iso[3])] : [NaN, NaN, NaN]
  const d = new Date(Date.UTC(annee, mois, jour))
  return Number.isNaN(d.getTime()) || d.getUTCDate() !== jour || d.getUTCMonth() !== mois ? null : { annee, mois, jour }
}
const deux = (n: number) => String(n).padStart(2, '0')

// Saisie de date au clavier (JJ/MM/AAAA) avec calendrier en popover (§7.2). DDR-11 : le bouton ouvre et ferme le
// calendrier (sauf s'il est piloté par `open`), un jour choisi remplit le champ ; l'aide et l'erreur sont reliées au champ.
export function DateField(p: DateFieldProps) {
  const t = useLibelle()
  const id = useFieldId2(p)
  const [ouvertLocal, setOuvert] = useState(false)
  const ouvert = p.open ?? ouvertLocal
  const choisi = lireJour(p.value)
  const [vue, setVue] = useState<{ annee: number; mois: number } | null>(null)
  const affiche = vue ?? choisi ?? jourDouala()
  const basculer = () => {
    setVue(null)
    setOuvert((o) => !o)
  }
  const decrit = p.error ? `${id}-error` : `${id}-help`
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
          aria-describedby={decrit}
          aria-required={p.required ? 'true' : undefined}
          disabled={p.disabled}
          onChange={(e) => p.onChange && p.onChange(e.target.value)}
        />
        <button type="button" className="gm-input-btn" aria-label={t('date.ouvrir')} aria-expanded={ouvert ? 'true' : 'false'} aria-haspopup="dialog" disabled={p.disabled} onClick={basculer}>
          <Icon name="calendar" size={20} />
        </button>
      </div>
      {ouvert ? (
        p.open !== undefined && !p.onChange ? (
          <CalendarPopover selected={[p.selectedDay || 28]} />
        ) : (
          <CalendarPopover
            annee={affiche.annee}
            mois={affiche.mois}
            jourChoisi={choisi}
            aujourdhui={jourDouala()}
            onMois={(delta) => {
              const d = new Date(Date.UTC(affiche.annee, affiche.mois + delta, 1))
              setVue({ annee: d.getUTCFullYear(), mois: d.getUTCMonth() })
            }}
            onSelect={(j) => {
              p.onChange?.(`${deux(j.jour)}/${deux(j.mois + 1)}/${j.annee}`)
              setOuvert(false)
            }}
            onClose={() => setOuvert(false)}
          />
        )
      ) : null}
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
