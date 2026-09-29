import { cx } from '../../lib/cx'
import { Icon } from '../../icones/Icon/Icon'
import { Button } from '../../actions/Button/Button'
import { StatusBadge } from '../../affichage/StatusBadge/StatusBadge'
import { Avatar } from '../../affichage/Avatar/Avatar'
import { useLibelle } from '../../i18n/LangueProvider'

export interface ShiftOperator {
  name: string
  lead?: boolean
  swap?: boolean
}

export interface CalendarGridProps {
  title?: string
  days?: string[]
  grid: { jour?: ShiftOperator[][]; soir?: ShiftOperator[][]; nuit?: ShiftOperator[][] }
  uncovered?: number
}

const SLOTS = [['jour', 'astreinte.jour'], ['soir', 'astreinte.soir'], ['nuit', 'astreinte.nuit']] as const
const WEEK = ['astreinte.lun', 'astreinte.mar', 'astreinte.mer', 'astreinte.jeu', 'astreinte.ven', 'astreinte.sam', 'astreinte.dim'] as const

// Grille d'astreinte hebdomadaire, semaine × plages (§7.6, DES-18).
export function CalendarGrid(p: CalendarGridProps) {
  const t = useLibelle()
  const g = p.grid || {}
  const days = p.days || WEEK.map((d) => t(d))
  const u = p.uncovered ?? 0
  return (
    <div className="gm-astreinte">
      <div className="gm-dag-toolbar">
        <strong>{p.title || t('astreinte.titre')}</strong>
        <span style={{ flex: 1 }} />
        <span className="gm-row" style={{ gap: 'var(--gm-space-3)' }}>
          <StatusBadge status="warn" label={t(u > 1 ? 'astreinte.plagesNonCouvertes' : 'astreinte.plageNonCouverte', { n: u })} />
          <Button size="sm" variant="secondary" icon="refresh">{t('astreinte.regenerer')}</Button>
          <Button size="sm" variant="primary" icon="pen-line">{t('astreinte.publier')}</Button>
        </span>
      </div>
      <table className="gm-cal-table" role="grid" aria-label={t('astreinte.label')}>
        <thead>
          <tr>
            <th scope="col">{t('astreinte.plage')}</th>
            {days.map((d) => (
              <th key={d} scope="col">{d}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {SLOTS.map((s) => (
            <tr key={s[0]}>
              <th scope="row">{t(s[1])}</th>
              {days.map((d, di) => {
                const cell = (g[s[0]] || [])[di]
                const empty = !cell || !cell.length
                return (
                  <td key={d} className={cx('gm-cal-cell', empty && 'gm-cal-uncovered')}>
                    {empty ? (
                      <span className="gm-row" style={{ gap: 4, color: 'var(--gm-color-status-warn-fg)', fontSize: 12, fontWeight: 600 }}>
                        <Icon name="triangle-alert" size={14} />
                        {t('astreinte.nonCouverte')}
                      </span>
                    ) : (
                      cell.map((op) => (
                        <span key={op.name} className={cx('gm-cal-op', op.lead && 'gm-cal-lead', op.swap && 'gm-cal-swap')}>
                          <Avatar name={op.name} />
                          <span>
                            <span style={{ display: 'block' }}>{op.name}</span>
                            {op.lead ? <span className="gm-field-help">{t('astreinte.chefDeQuart')}</span> : op.swap ? <span className="gm-field-help">{t('astreinte.echange')}</span> : null}
                          </span>
                        </span>
                      ))
                    )}
                  </td>
                )
              })}
            </tr>
          ))}
        </tbody>
      </table>
      <div className="gm-field-help" style={{ padding: 'var(--gm-space-2) var(--gm-space-3)' }}>
        {t('astreinte.parametres')}
      </div>
    </div>
  )
}
