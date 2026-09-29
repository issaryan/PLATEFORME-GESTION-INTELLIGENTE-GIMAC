import { cx } from '../../lib/cx'
import type { Severity } from '../../lib/types'
import { useLibelle } from '../../i18n/LangueProvider'

type Niveau = 1 | 2 | 3 | 4 | 5

const LEVELS: Niveau[] = [1, 2, 3, 4, 5]

// Sévérité par défaut de la matrice ; la qualification réelle est calculée par le service.
function sev(i: number, u: number): Severity {
  const s = i + u
  return s >= 9 ? 'S1' : s >= 7 ? 'S2' : s >= 5 ? 'S3' : 'S4'
}

export interface SeverityMatrixProps {
  impact?: Niveau
  urgency?: Niveau
  /** Cellules imposées par le référentiel, clé « impact-urgence ». */
  cells?: Record<string, Severity>
}

// Matrice impact × urgence 5 × 5 pour la qualification d'un incident (§7.7).
export function SeverityMatrix(p: SeverityMatrixProps) {
  const t = useLibelle()
  return (
    <table className="gm-matrix" role="grid" aria-label={t('severite.label')}>
      <thead>
        <tr>
          <th scope="col">{t('severite.axes')}</th>
          {LEVELS.map((u) => (
            <th key={u} scope="col">{u}</th>
          ))}
        </tr>
      </thead>
      <tbody>
        {LEVELS.slice().reverse().map((i) => (
          <tr key={i}>
            <th scope="row">{i}</th>
            {LEVELS.map((u) => {
              const s = (p.cells && p.cells[i + '-' + u]) || sev(i, u)
              const sel = p.impact === i && p.urgency === u
              return (
                <td key={u} className={cx('gm-matrix-cell', 'gm-matrix-' + s, sel && 'gm-matrix-sel')} aria-selected={sel ? 'true' : undefined}>
                  <span className={'gm-severity gm-severity-' + s}>{s}</span>
                  {sel ? <span className="gm-matrix-mark">{t('severite.selection')}</span> : null}
                </td>
              )
            })}
          </tr>
        ))}
      </tbody>
    </table>
  )
}
