import { cx } from '../../lib/cx'
import { Icon } from '../../icones/Icon/Icon'
import { Button } from '../../actions/Button/Button'
import { StatusBadge } from '../../affichage/StatusBadge/StatusBadge'
import { Avatar } from '../../affichage/Avatar/Avatar'

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

const SLOTS = [['jour', 'Jour, 06:00 à 14:00'], ['soir', 'Soir, 14:00 à 22:00'], ['nuit', 'Nuit, 22:00 à 06:00']] as const
const WEEK = ['Lun 28', 'Mar 29', 'Mer 30', 'Jeu 01', 'Ven 02', 'Sam 03', 'Dim 04']

// Grille d'astreinte hebdomadaire, semaine × plages (§7.6, DES-18).
export function CalendarGrid(p: CalendarGridProps) {
  const g = p.grid || {}
  const days = p.days || WEEK
  const u = p.uncovered ?? 0
  return (
    <div className="gm-astreinte">
      <div className="gm-dag-toolbar">
        <strong>{p.title || 'Semaine 40, du 28/09 au 04/10'}</strong>
        <span style={{ flex: 1 }} />
        <span className="gm-row" style={{ gap: 'var(--gm-space-3)' }}>
          <StatusBadge status="warn" label={u + ' plage' + (u > 1 ? 's' : '') + ' non couverte' + (u > 1 ? 's' : '')} />
          <Button size="sm" variant="secondary" icon="refresh">Régénérer</Button>
          <Button size="sm" variant="primary" icon="pen-line">Publier la grille</Button>
        </span>
      </div>
      <table className="gm-cal-table" role="grid" aria-label="Grille d'astreinte">
        <thead>
          <tr>
            <th scope="col">Plage</th>
            {days.map((d) => (
              <th key={d} scope="col">{d}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {SLOTS.map((s) => (
            <tr key={s[0]}>
              <th scope="row">{s[1]}</th>
              {days.map((d, di) => {
                const cell = (g[s[0]] || [])[di]
                const empty = !cell || !cell.length
                return (
                  <td key={d} className={cx('gm-cal-cell', empty && 'gm-cal-uncovered')}>
                    {empty ? (
                      <span className="gm-row" style={{ gap: 4, color: 'var(--gm-color-status-warn-fg)', fontSize: 12, fontWeight: 600 }}>
                        <Icon name="triangle-alert" size={14} />
                        non couverte
                      </span>
                    ) : (
                      cell.map((op) => (
                        <span key={op.name} className={cx('gm-cal-op', op.lead && 'gm-cal-lead', op.swap && 'gm-cal-swap')}>
                          <Avatar name={op.name} />
                          <span>
                            <span style={{ display: 'block' }}>{op.name}</span>
                            {op.lead ? <span className="gm-field-help">chef de quart</span> : op.swap ? <span className="gm-field-help">échange demandé</span> : null}
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
        Paramètres de génération : repos de 11 h entre deux vacations, 3 nuits consécutives au plus, effectif minimal de 2 par plage.
      </div>
    </div>
  )
}
