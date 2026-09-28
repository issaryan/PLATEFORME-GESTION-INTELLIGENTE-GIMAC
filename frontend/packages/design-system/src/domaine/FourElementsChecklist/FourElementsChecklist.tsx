import { cx } from '../../lib/cx'
import { Icon } from '../../icones/Icon/Icon'
import { Button } from '../../actions/Button/Button'
import { ELEMENTS, type FourElementKey, type FourElements } from '../quatreElements'

export type { FourElements } from '../quatreElements'

export interface FourElementsChecklistProps {
  elements?: FourElements
  /** Explication de l'élément manquant. */
  missing?: Partial<Record<FourElementKey, string>>
  /** Libellé de l'action attendue pour l'élément manquant. */
  actions?: Partial<Record<FourElementKey, string>>
}

// Liste de contrôle de la règle des quatre éléments (§7.7, écran DES-10).
export function FourElementsChecklist(p: FourElementsChecklistProps) {
  const el = p.elements || {}
  return (
    <ul className="gm-check4">
      {ELEMENTS.map((e) => {
        const v = el[e[0]]
        const ok = !!v
        const action = p.actions && p.actions[e[0]]
        return (
          <li key={e[0]} className={cx('gm-check4-row', ok ? 'gm-check4-ok' : 'gm-check4-missing')}>
            <Icon name={ok ? 'circle-check' : 'circle-x'} size={20} />
            <div style={{ flex: 1 }}>
              <div className="gm-banner-title">{e[1]}</div>
              <div className="gm-field-help">{ok ? (typeof v === 'string' ? v : 'présent') : (p.missing && p.missing[e[0]]) || 'manquant'}</div>
            </div>
            {!ok && action ? <Button size="sm" variant="secondary">{action}</Button> : null}
          </li>
        )
      })}
    </ul>
  )
}
