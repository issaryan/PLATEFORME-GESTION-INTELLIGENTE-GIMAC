import { StatusBadge } from '../StatusBadge/StatusBadge'
import type { Status } from '../../lib/types'

export interface ProgressRingProps {
  label: string
  /** Pourcentage de 0 à 100, calculé par le service. */
  value: number
  size?: number
  status?: Status
  statusLabel?: string
}

// Anneau de couverture avec pourcentage au centre (§7.3).
export function ProgressRing(p: ProgressRingProps) {
  const size = p.size || 72
  const r = (size - 8) / 2
  const c = 2 * Math.PI * r
  const v = Math.max(0, Math.min(100, p.value || 0))
  return (
    <div className="gm-ring" style={{ width: size }}>
      <svg width={size} height={size} viewBox={'0 0 ' + size + ' ' + size} role="img" aria-label={p.label + ' : ' + v + ' %'}>
        <circle cx={size / 2} cy={size / 2} r={r} className="gm-ring-track" />
        <circle cx={size / 2} cy={size / 2} r={r} className="gm-ring-value" strokeDasharray={c} strokeDashoffset={c * (1 - v / 100)} transform={'rotate(-90 ' + size / 2 + ' ' + size / 2 + ')'} />
        <text x="50%" y="50%" dy="0.35em" textAnchor="middle" className="gm-ring-text">
          {v + ' %'}
        </text>
      </svg>
      <div className="gm-ring-label">
        {p.label}
        {p.status ? <StatusBadge status={p.status} label={p.statusLabel || p.status} /> : null}
      </div>
    </div>
  )
}
