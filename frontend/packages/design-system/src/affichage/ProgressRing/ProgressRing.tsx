import { StatusBadge } from '../StatusBadge/StatusBadge'
import type { Status } from '../../lib/types'
import { useLibelle } from '../../i18n/LangueProvider'

export interface ProgressRingProps {
  label: string
  /** Pourcentage de 0 à 100, calculé par le service ; null lorsqu'il n'est pas calculable (« n/c »). */
  value: number | null
  size?: number
  status?: Status
  statusLabel?: string
  /** Colonne de tableau : anneau de 24 px, pourcentage à côté, sans légende ni statut. */
  compact?: boolean
}

// Anneau de couverture avec pourcentage au centre (§7.3).
export function ProgressRing(p: ProgressRingProps) {
  const t = useLibelle()
  const size = p.compact ? 24 : p.size || 72
  const r = (size - 8) / 2
  const c = 2 * Math.PI * r
  const nc = p.value === null
  const v = nc ? 0 : Math.max(0, Math.min(100, p.value || 0))
  if (p.compact) {
    return (
      <span className="gm-ring-compact">
        <svg width={size} height={size} viewBox={'0 0 ' + size + ' ' + size} role="img" aria-label={nc ? t('anneau.nonCalculable', { libelle: p.label }) : p.label + ' : ' + v + ' %'}>
          <circle cx={size / 2} cy={size / 2} r={r} className="gm-ring-track" style={{ strokeWidth: 4 }} />
          <circle cx={size / 2} cy={size / 2} r={r} className="gm-ring-value" style={{ strokeWidth: 4 }} strokeDasharray={c} strokeDashoffset={c * (1 - v / 100)} transform={'rotate(-90 ' + size / 2 + ' ' + size / 2 + ')'} />
        </svg>
        <span className="gm-num" aria-hidden="true">{nc ? t('anneau.nc') : v + ' %'}</span>
      </span>
    )
  }
  return (
    <div className="gm-ring" style={{ minWidth: size }}>
      <svg width={size} height={size} viewBox={'0 0 ' + size + ' ' + size} role="img" aria-label={nc ? t('anneau.nonCalculable', { libelle: p.label }) : p.label + ' : ' + v + ' %'}>
        <circle cx={size / 2} cy={size / 2} r={r} className="gm-ring-track" />
        <circle cx={size / 2} cy={size / 2} r={r} className="gm-ring-value" strokeDasharray={c} strokeDashoffset={c * (1 - v / 100)} transform={'rotate(-90 ' + size / 2 + ' ' + size / 2 + ')'} />
        <text x="50%" y="50%" dy="0.35em" textAnchor="middle" className="gm-ring-text">
          {nc ? t('anneau.nc') : v + ' %'}
        </text>
      </svg>
      <div className="gm-ring-label">
        {p.label}
        {p.status ? <StatusBadge status={p.status} label={p.statusLabel || p.status} /> : null}
      </div>
    </div>
  )
}
