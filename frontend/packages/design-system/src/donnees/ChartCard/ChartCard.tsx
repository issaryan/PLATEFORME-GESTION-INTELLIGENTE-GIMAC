import { useState, type ReactNode } from 'react'
import { formatMoney as fmt } from '../../lib/format'
import type { Status } from '../../lib/types'
import { IconButton } from '../../actions/IconButton/IconButton'
import { StatusBadge } from '../../affichage/StatusBadge/StatusBadge'
import { Tabs } from '../../navigation/Tabs/Tabs'
import { useLibelle } from '../../i18n/LangueProvider'

export interface ChartSeries {
  name: string
  data: number[]
}

export interface ChartCardProps {
  title: string
  subtitle?: string
  type: 'line' | 'bar' | 'gauge'
  labels?: string[]
  /** Au plus huit séries, palette gm-data-1 à gm-data-8 dans l'ordre fixe. */
  series: ChartSeries[]
  unit?: string
  threshold?: number
  min?: number
  max?: number
  status?: Status
  statusLabel?: string
  width?: number
  height?: number
  xLabel?: string
  /** Export de la vue (CSV, image) ; le bouton n'apparaît que s'il est fourni, jamais sans effet. */
  onExporter?: () => void
}

function niceMax(v: number): number {
  const p = Math.pow(10, Math.floor(Math.log10(v || 1)))
  const n = Math.ceil(v / p)
  return n * p
}

// Carte de graphique : courbe, barres ou jauge ; légende dès deux séries, étiquettes directes,
// infobulle et vue tableau équivalente (§7.6).
export function ChartCard(p: ChartCardProps) {
  const t = useLibelle()
  const [view, setView] = useState<string>('chart')
  const [hov, setHov] = useState<number[] | null>(null)
  const series = p.series || []
  const labels = p.labels || []
  const W = p.width || 560
  const H = p.height || 200
  const padL = 40, padB = 24, padT = 12
  const padR = p.type === 'bar' || p.type === 'gauge' ? 16 : 96
  const max = p.max !== undefined ? p.max : niceMax(Math.max(...series.map((s) => Math.max(...s.data))))
  const min = p.min || 0
  const iw = W - padL - padR
  const ih = H - padT - padB
  const xs = (i: number) => padL + (labels.length > 1 ? (i * iw) / (labels.length - 1) : iw / 2)
  const ys = (v: number) => padT + ih - ((v - min) * ih) / (max - min)
  const ticks = [0, 0.25, 0.5, 0.75, 1].map((f) => min + f * (max - min))
  const suffixe = p.unit ? ' ' + p.unit : ''
  const table = (
    <table className="gm-table gm-table-compact" style={{ border: 'var(--gm-border-1) solid var(--gm-color-border)' }}>
      <thead>
        <tr>
          <th>{p.xLabel || t('graphique.periode')}</th>
          {series.map((s) => (
            <th key={s.name} className="gm-num">{s.name}</th>
          ))}
        </tr>
      </thead>
      <tbody>
        {labels.map((l, i) => (
          <tr key={l}>
            <td>{l}</td>
            {series.map((s) => (
              <td key={s.name} className="gm-num">{fmt(s.data[i]) + suffixe}</td>
            ))}
          </tr>
        ))}
      </tbody>
    </table>
  )
  const seuil =
    p.threshold !== undefined ? (
      <g>
        <line className="gm-chart-threshold" x1={padL} x2={W - padR} y1={ys(p.threshold)} y2={ys(p.threshold)} />
        <text className="gm-chart-tick" x={W - padR} y={ys(p.threshold) - 4} textAnchor="end">{t('commun.seuil', { valeur: fmt(p.threshold) + suffixe })}</text>
      </g>
    ) : null
  const grille = ticks.map((t) => (
    <g key={t}>
      <line className="gm-chart-grid" x1={padL} x2={W - padR} y1={ys(t)} y2={ys(t)} />
      <text className="gm-chart-tick" x={padL - 6} y={ys(t) + 4} textAnchor="end">{fmt(t)}</text>
    </g>
  ))
  let chart: ReactNode
  if (p.type === 'bar') {
    const groups = labels.length
    const bw = Math.max(6, (iw / groups - 8) / series.length - 2)
    chart = (
      <svg className="gm-chart" width={W} height={H} viewBox={'0 0 ' + W + ' ' + H} role="img" aria-label={t('graphique.description', { titre: p.title })}>
        {grille}
        {labels.map((l, i) => {
          const gx = padL + (i * iw) / groups + 4
          return (
            <g key={l}>
              {series.map((s, si) => {
                const x = gx + si * (bw + 2)
                const y = ys(s.data[i])
                const isH = hov && hov[0] === i && hov[1] === si
                return (
                  <rect
                    key={s.name}
                    className={'gm-chart-bar gm-data-' + (si + 1)}
                    x={x}
                    y={y}
                    width={bw}
                    height={padT + ih - y}
                    opacity={hov && !isH ? 0.45 : 1}
                    onMouseEnter={() => setHov([i, si])}
                    onMouseLeave={() => setHov(null)}
                  />
                )
              })}
              <text className="gm-chart-tick" x={gx + ((bw + 2) * series.length) / 2 - 1} y={H - 6} textAnchor="middle">{l}</text>
            </g>
          )
        })}
        {seuil}
      </svg>
    )
  } else if (p.type === 'gauge') {
    const v = series[0] ? series[0].data[0] : 0
    const r = 70, cxp = W / 2, cyp = 96
    const ang = Math.PI * (1 - v / max)
    const ex = cxp + r * Math.cos(ang), ey = cyp - r * Math.sin(ang)
    let repere: ReactNode = null
    if (p.threshold !== undefined) {
      const a = Math.PI * (1 - p.threshold / max)
      repere = <line className="gm-chart-threshold" x1={cxp + (r - 10) * Math.cos(a)} y1={cyp - (r - 10) * Math.sin(a)} x2={cxp + (r + 10) * Math.cos(a)} y2={cyp - (r + 10) * Math.sin(a)} />
    }
    chart = (
      <svg className="gm-chart" width={W} height={120} viewBox={'0 0 ' + W + ' 120'} role="img" aria-label={p.title + ' : ' + v + (p.unit || '')}>
        <path className="gm-chart-track" d={'M' + (cxp - r) + ' ' + cyp + ' A' + r + ' ' + r + ' 0 0 1 ' + (cxp + r) + ' ' + cyp} />
        <path className="gm-chart-line gm-data-1" d={'M' + (cxp - r) + ' ' + cyp + ' A' + r + ' ' + r + ' 0 ' + (v / max > 0.5 ? 1 : 0) + ' 1 ' + ex + ' ' + ey} style={{ strokeWidth: 10 }} />
        {repere}
        <text className="gm-chart-value" x={cxp} y={cyp - 4} textAnchor="middle">{fmt(v) + suffixe}</text>
        <text className="gm-chart-tick" x={cxp - r} y={cyp + 16} textAnchor="middle">0</text>
        <text className="gm-chart-tick" x={cxp + r} y={cyp + 16} textAnchor="middle">{fmt(max)}</text>
      </svg>
    )
  } else {
    chart = (
      <svg className="gm-chart" width={W} height={H} viewBox={'0 0 ' + W + ' ' + H} role="img" aria-label={t('graphique.description', { titre: p.title })} onMouseLeave={() => setHov(null)}>
        {grille}
        {labels.map((l, i) => (
          <text key={l} className="gm-chart-tick" x={xs(i)} y={H - 6} textAnchor="middle">{l}</text>
        ))}
        {seuil}
        {series.map((s, si) => {
          const d = s.data.map((v, i) => (i ? 'L' : 'M') + xs(i) + ' ' + ys(v)).join(' ')
          return (
            <g key={s.name}>
              <path className={'gm-chart-line gm-data-' + (si + 1)} d={d} />
              <text className="gm-chart-label" x={xs(s.data.length - 1) + 6} y={ys(s.data[s.data.length - 1]) + 4}>{s.name}</text>
            </g>
          )
        })}
        {labels.map((_l, i) => (
          <rect key={'hit' + i} x={xs(i) - iw / labels.length / 2} y={0} width={iw / labels.length} height={H} fill="transparent" onMouseEnter={() => setHov([i])} />
        ))}
        {hov ? (
          <g>
            <line className="gm-chart-cross" x1={xs(hov[0])} x2={xs(hov[0])} y1={padT} y2={padT + ih} />
            {series.map((s, si) => (
              <circle key={s.name} className={'gm-chart-dot gm-data-' + (si + 1)} cx={xs(hov[0])} cy={ys(s.data[hov[0]])} r={4} />
            ))}
          </g>
        ) : null}
      </svg>
    )
  }
  const tip =
    hov && p.type !== 'gauge' ? (
      <div className="gm-chart-tip" role="status">
        <strong>{labels[hov[0]]}</strong>
        {series.map((s, si) => {
          if (hov[1] !== undefined && hov[1] !== si) return null
          return (
            <div key={s.name} className="gm-row" style={{ gap: 6 }}>
              <span className={'gm-chart-swatch gm-data-' + (si + 1)} />
              {s.name}
              <span className="gm-num" style={{ marginLeft: 'auto', fontWeight: 600 }}>{fmt(s.data[hov[0]]) + suffixe}</span>
            </div>
          )
        })}
      </div>
    ) : null
  return (
    <section className="gm-card gm-chartcard" aria-label={p.title}>
      <header className="gm-card-header">
        <div>
          <h2 className="gm-card-title">{p.title}</h2>
          {p.subtitle ? <div className="gm-field-help">{p.subtitle}</div> : null}
        </div>
        <div className="gm-row">
          {p.status ? <StatusBadge status={p.status} label={p.statusLabel || ''} /> : null}
          <Tabs items={[{ key: 'chart', label: t('graphique.graphique') }, { key: 'table', label: t('graphique.tableau') }]} value={view} onChange={setView} />
          {p.onExporter ? <IconButton variant="ghost" size="sm" icon="download" label={t('graphique.exporter')} onClick={p.onExporter} /> : null}
        </div>
      </header>
      <div className="gm-card-body" style={{ position: 'relative' }}>
        {view === 'table' ? table : chart}
        {tip}
      </div>
      {series.length > 1 ? (
        <footer className="gm-chart-legend">
          {series.map((s, si) => (
            <span key={s.name} className="gm-row" style={{ gap: 6 }}>
              <span className={'gm-chart-swatch gm-data-' + (si + 1)} />
              {s.name}
            </span>
          ))}
        </footer>
      ) : null}
    </section>
  )
}
