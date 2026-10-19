import { useLayoutEffect, useRef, useState, type CSSProperties } from 'react'
import { cx } from '../../lib/cx'
import { Button } from '../../actions/Button/Button'
import { useLibelle } from '../../i18n/LangueProvider'

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
  const t = useLibelle()
  const days = p.days || 60
  // Colonne « Tenue » seulement si au moins un jalon porte une probabilité : jamais de colonne vide.
  const avecTenue = (p.milestones || []).some((m) => m.probability !== undefined)
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
        <span className="gm-field-help">{t('gantt.periode', { debut: start, jours: days })}</span>
        <span style={{ flex: 1 }} />
        <span className="gm-row" style={{ gap: 'var(--gm-space-3)', fontSize: 12, color: 'var(--gm-color-text-secondary)' }}>
          <span className="gm-gantt-key gm-gantt-key-bar">{t('gantt.planifie')}</span>
          <span className="gm-gantt-key gm-gantt-key-done">{t('gantt.realise')}</span>
          <span className="gm-gantt-key gm-gantt-key-crit">{t('gantt.critique')}</span>
          <span className="gm-gantt-key gm-gantt-key-slack">{t('gantt.marge')}</span>
        </span>
        <Button size="sm" variant="ghost">{t('commun.vueTableau')}</Button>
      </div>
      <div className="gm-gantt-scroll" ref={scroll}>
        <table className="gm-gantt-table" role="grid" aria-label={t('gantt.label')}>
          <thead>
            <tr>
              <th scope="col" className="gm-gantt-name" ref={noms}>{t('gantt.jalon')}</th>
              {avecTenue ? <th scope="col" className="gm-gantt-name gm-num" ref={tenue}>{t('gantt.tenue')}</th> : null}
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
                {avecTenue ? (
                  <td className="gm-gantt-name gm-num">
                    {m.probability !== undefined ? <span className={cx(m.probability < 50 && 'gm-late')}>{m.probability + ' %'}</span> : ''}
                  </td>
                ) : null}
                <td style={{ padding: 0, position: 'relative' }}>
                  <div className="gm-gantt-lane" style={{ ...semaine, width: W }}>
                    {m.slack ? <span className="gm-gantt-slack" style={{ left: (m.start + m.duration) * colW, width: m.slack * colW }} /> : null}
                    <span
                      className={cx('gm-gantt-bar', m.critical && 'gm-gantt-bar-crit')}
                      style={{ left: m.start * colW, width: m.duration * colW }}
                      role="img"
                      aria-label={t(m.critical ? 'gantt.barreCritique' : 'gantt.barre', { libelle: m.label, debut: m.start, duree: m.duration })}
                    >
                      {m.done ? <span className="gm-gantt-done" style={{ width: m.done + '%' }} /> : null}
                      {/* Le pourcentage n'est écrit que si la barre peut le contenir ; il reste lu dans la vue tableau et l'étiquette. */}
                      <span className="gm-gantt-bar-label">{m.done && m.duration * colW >= 44 ? m.done + ' %' : ''}</span>
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
