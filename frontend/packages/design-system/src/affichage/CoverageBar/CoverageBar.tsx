import { cx } from '../../lib/cx'

const COVER_STEPS: [string, string][] = [
  ['inclus', 'gm-cov-1'],
  ['en essai', 'gm-cov-2'],
  ['certifié', 'gm-cov-3'],
  ['en service', 'gm-cov-4'],
]

export interface CoverageBarProps {
  service: string
  /** 0 hors périmètre, 1 inclus, 2 en essai, 3 certifié, 4 en service. */
  level: 0 | 1 | 2 | 3 | 4
}

// Barre de couverture d'un service avec légende textuelle (§7.3).
export function CoverageBar(p: CoverageBarProps) {
  const lvl = p.level || 0
  const texte = lvl ? COVER_STEPS[lvl - 1][0] : 'hors périmètre'
  return (
    <div className="gm-cov">
      <div className="gm-cov-label">{p.service}</div>
      <div className="gm-cov-bar" role="img" aria-label={p.service + ' : ' + texte}>
        {COVER_STEPS.map((s, i) => (
          <span key={i} className={cx('gm-cov-seg', i < lvl && s[1])} />
        ))}
      </div>
      <div className="gm-cov-text">{texte}</div>
    </div>
  )
}
