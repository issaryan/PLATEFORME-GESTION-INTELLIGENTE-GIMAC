import { useLayoutEffect, useRef, useState, type CSSProperties } from 'react'
import { cx } from '../../lib/cx'
import { Button } from '../../actions/Button/Button'

export interface Milestone {
  key: string | number
  label: string
  owner: string
  start: number
  duration: number
  done?: number
  slack?: number
  critical?: boolean
  probability?: number
}

export interface GanttViewProps {
  start?: string
  days?: number
  today?: number
  weeks?: { day: number; label: string }[]
  milestones: Milestone[]
}

// Jalons et chemin critique d'un projet (§7.6, DES-24).
export function GanttView(p: GanttViewProps) {
  const days = p.days || 60
  const start = p.start || '01/09'
  // Échelle : 14 px par jour au plus (valeur publiée) ; réduite, jusqu'à 6 px, pour que la
  // frise tienne dans la largeur disponible au lieu de déborder de la carte.
  const scroll = useRef<HTMLDivElement>(null)
  const noms = useRef<HTMLTableCellElement>(null)
  const tenue = useRef<HTMLTableCellElement>(null)
  const [colW, setColW] = useState(14)
  useLayoutEffect(() => {
    const el = scroll.current
    if (!el) return
    const ajuster = () => {
      const fixe = (noms.current?.offsetWidth ?? 0) + (tenue.current?.offsetWidth ?? 0)
      setColW(Math.max(6, Math.min(14, Math.floor((el.clientWidth - fixe) / days))))
    }
    ajuster()
    if (typeof ResizeObserver === 'undefined') return
    const obs = new ResizeObserver(ajuster)
    obs.observe(el)
    return () => obs.disconnect()
  }, [days])
  const W = days * colW
  const semaine = { '--gm-gantt-semaine': 7 * colW + 'px' } as CSSProperties
  return (
    <div className="gm-gantt">
      <div className="gm-dag-toolbar">
        <span className="gm-field-help">{'Du ' + start + ' sur ' + days + ' jours'}</span>
        <span style={{ flex: 1 }} />
        <span className="gm-row" style={{ gap: 'var(--gm-space-3)', fontSize: 12, color: 'var(--gm-color-text-secondary)' }}>
          <span className="gm-gantt-key gm-gantt-key-bar">jalon planifié</span>
          <span className="gm-gantt-key gm-gantt-key-done">réalisé</span>
          <span className="gm-gantt-key gm-gantt-key-crit">chemin critique</span>
          <span className="gm-gantt-key gm-gantt-key-slack">marge</span>
        </span>
        <Button size="sm" variant="ghost">Vue tableau</Button>
      </div>
      <div className="gm-gantt-scroll" ref={scroll}>
        <table className="gm-gantt-table" role="grid" aria-label="Jalons du projet">
          <thead>
            <tr>
              <th scope="col" className="gm-gantt-name" ref={noms}>Jalon</th>
              <th scope="col" className="gm-gantt-name gm-num" ref={tenue}>Tenue</th>
              <th scope="col" style={{ padding: 0 }}>
                <div className="gm-gantt-axis" style={{ ...semaine, width: W }}>
                  {(p.weeks || []).map((w, i) => (
                    <span key={i} style={{ left: w.day * colW }}>{w.label}</span>
                  ))}
                  {p.today !== undefined ? <span className="gm-gantt-today" style={{ left: p.today * colW }} /> : null}
                </div>
              </th>
            </tr>
          </thead>
          <tbody>
            {(p.milestones || []).map((m) => (
              <tr key={m.key}>
                <th scope="row" className="gm-gantt-name">
                  <div>{m.label}</div>
                  <div className="gm-field-help">{m.owner}</div>
                </th>
                <td className="gm-gantt-name gm-num">
                  {m.probability !== undefined ? <span className={cx(m.probability < 50 && 'gm-late')}>{m.probability + ' %'}</span> : ''}
                </td>
                <td style={{ padding: 0, position: 'relative' }}>
                  <div className="gm-gantt-lane" style={{ ...semaine, width: W }}>
                    {m.slack ? <span className="gm-gantt-slack" style={{ left: (m.start + m.duration) * colW, width: m.slack * colW }} /> : null}
                    <span
                      className={cx('gm-gantt-bar', m.critical && 'gm-gantt-bar-crit')}
                      style={{ left: m.start * colW, width: m.duration * colW }}
                      role="img"
                      aria-label={m.label + ', du jour ' + m.start + ' sur ' + m.duration + ' jours' + (m.critical ? ', chemin critique' : '')}
                    >
                      {m.done ? <span className="gm-gantt-done" style={{ width: m.done + '%' }} /> : null}
                      <span className="gm-gantt-bar-label">{m.done ? m.done + ' %' : ''}</span>
                    </span>
                    {p.today !== undefined ? <span className="gm-gantt-today" style={{ left: p.today * colW }} /> : null}
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}
