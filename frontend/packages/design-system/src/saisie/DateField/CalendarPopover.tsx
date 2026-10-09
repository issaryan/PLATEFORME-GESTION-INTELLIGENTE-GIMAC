import { cx } from '../../lib/cx'
import { Button } from '../../actions/Button/Button'
import { IconButton } from '../../actions/IconButton/IconButton'
import { useLangue, useLibelle } from '../../i18n/LangueProvider'

const DAYS = ['calendrier.lu', 'calendrier.ma', 'calendrier.me', 'calendrier.je', 'calendrier.ve', 'calendrier.sa', 'calendrier.di'] as const

export interface CalendarPopoverProps {
  month?: string
  selected?: number[]
  range?: [number, number]
  shortcuts?: string[]
  /**
   * Calendrier piloté (DDR-11) : mois affiché (année, mois de 0 à 11), jour choisi, choix d'un jour et changement de mois.
   * Sans `onSelect`, le calendrier reste l'illustration statique du système de conception.
   */
  annee?: number
  mois?: number
  jourChoisi?: { annee: number; mois: number; jour: number } | null
  aujourdhui?: { annee: number; mois: number; jour: number }
  onSelect?: (j: { annee: number; mois: number; jour: number }) => void
  onMois?: (delta: number) => void
  onClose?: () => void
}

// Calendrier accessible en popover (interne à DateField et DateRangeField).
export function CalendarPopover(p: CalendarPopoverProps) {
  const t = useLibelle()
  const langue = useLangue()
  if (p.onSelect && p.annee !== undefined && p.mois !== undefined) return <CalendrierPilote {...p} annee={p.annee} mois={p.mois} onSelect={p.onSelect} langue={langue} />
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

/** Calendrier réel d'un mois : semaine du lundi, jours du mois, jour choisi et jour courant marqués, Échap pour fermer. */
function CalendrierPilote(p: CalendarPopoverProps & { annee: number; mois: number; onSelect: NonNullable<CalendarPopoverProps['onSelect']>; langue: string }) {
  const t = useLibelle()
  const premier = new Date(Date.UTC(p.annee, p.mois, 1))
  const nbJours = new Date(Date.UTC(p.annee, p.mois + 1, 0)).getUTCDate()
  const decalage = (premier.getUTCDay() + 6) % 7
  const titre = new Intl.DateTimeFormat(p.langue, { month: 'long', year: 'numeric', timeZone: 'UTC' }).format(premier)
  const meme = (j: { annee: number; mois: number; jour: number } | null | undefined, d: number) => !!j && j.annee === p.annee && j.mois === p.mois && j.jour === d
  return (
    <div className="gm-popover gm-cal" role="dialog" aria-label={t('calendrier.label')} onKeyDown={(e) => { if (e.key === 'Escape') { e.stopPropagation(); p.onClose?.() } }}>
      <div className="gm-cal-head">
        <IconButton variant="ghost" size="sm" icon="chevron-right" label={t('calendrier.precedent')} className="gm-flip" onClick={() => p.onMois?.(-1)} />
        <strong aria-live="polite">{titre.charAt(0).toUpperCase() + titre.slice(1)}</strong>
        <IconButton variant="ghost" size="sm" icon="chevron-right" label={t('calendrier.suivant')} onClick={() => p.onMois?.(1)} />
      </div>
      <div className="gm-cal-grid" role="group" aria-label={t('calendrier.jours')}>
        {DAYS.map((d) => (
          <span key={d} className="gm-cal-dow">{t(d)}</span>
        ))}
        {Array.from({ length: decalage }, (_, i) => <span key={'e' + i} />)}
        {Array.from({ length: nbJours }, (_, i) => i + 1).map((d) => {
          const sel = meme(p.jourChoisi, d)
          return (
            <button key={d} type="button" className={cx('gm-cal-day', sel && 'gm-cal-sel', meme(p.aujourdhui, d) && 'gm-cal-today')} aria-pressed={sel ? 'true' : 'false'}
              onClick={() => p.onSelect({ annee: p.annee, mois: p.mois, jour: d })}>
              {d}
            </button>
          )
        })}
      </div>
      <div className="gm-cal-foot">
        {p.aujourdhui ? <Button size="sm" variant="ghost" onClick={() => p.onSelect(p.aujourdhui!)}>{t('calendrier.aujourdhui')}</Button> : null}
      </div>
      <div className="gm-cal-tz">{t('calendrier.fuseau')}</div>
    </div>
  )
}
