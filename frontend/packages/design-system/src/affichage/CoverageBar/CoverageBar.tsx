import { cx } from '../../lib/cx'
import { useLibelle } from '../../i18n/LangueProvider'

const COVER_STEPS = [
  ['couverture.inclus', 'gm-cov-1'],
  ['couverture.essai', 'gm-cov-2'],
  ['couverture.certifie', 'gm-cov-3'],
  ['couverture.service', 'gm-cov-4'],
] as const

export interface CoverageBarProps {
  service: string
  /** 0 hors périmètre, 1 inclus, 2 en essai, 3 certifié, 4 en service. */
  level: 0 | 1 | 2 | 3 | 4
}

// Barre de couverture d'un service avec légende textuelle (§7.3).
export function CoverageBar(p: CoverageBarProps) {
  const t = useLibelle()
  const lvl = p.level || 0
  const texte = lvl ? t(COVER_STEPS[lvl - 1][0]) : t('couverture.hors')
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
