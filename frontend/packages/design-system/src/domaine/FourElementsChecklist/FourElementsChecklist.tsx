import { cx } from '../../lib/cx'
import { Icon } from '../../icones/Icon/Icon'
import { Button } from '../../actions/Button/Button'
import { ELEMENTS, reuni, type FourElementKey, type FourElements } from '../quatreElements'
import { StatusBadge } from '../../affichage/StatusBadge/StatusBadge'
import { useLibelle } from '../../i18n/LangueProvider'

export type { FourElements } from '../quatreElements'

export interface FourElementsChecklistProps {
  elements?: FourElements
  /** Explication de l'élément manquant. */
  missing?: Partial<Record<FourElementKey, string>>
  /** Libellé de l'action attendue pour l'élément manquant. */
  actions?: Partial<Record<FourElementKey, string>>
  /** Bouton d'action d'un élément manquant. */
  onAction?: (element: FourElementKey) => void
}

// Liste de contrôle de la règle des quatre éléments (§7.7, écran DES-10).
export function FourElementsChecklist(p: FourElementsChecklistProps) {
  const t = useLibelle()
  const el = p.elements || {}
  return (
    <ul className="gm-check4">
      {ELEMENTS.map((e) => {
        const v = el[e[0]]
        const ok = reuni(v)
        const enAttente = v === 'pending'
        const action = p.actions && p.actions[e[0]]
        return (
          <li key={e[0]} className={cx('gm-check4-row', ok ? 'gm-check4-ok' : 'gm-check4-missing')}>
            <Icon name={ok ? 'circle-check' : enAttente ? 'clock' : 'circle-x'} size={20} />
            <div style={{ flex: 1 }}>
              <div className="gm-banner-title">{t(e[1])}</div>
              <div className="gm-field-help">{ok ? (typeof v === 'string' ? v : t('elements.etatPresent')) : enAttente ? <span className="gm-row" style={{ gap: 'var(--gm-space-2)' }}><StatusBadge status="warn" label={t('elements.enAttente')} />{p.missing && p.missing[e[0]]}</span> : (p.missing && p.missing[e[0]]) || t('elements.etatManquant')}</div>
            </div>
            {!ok && action ? <Button size="sm" variant="secondary" onClick={p.onAction ? () => p.onAction!(e[0]) : undefined}>{action}</Button> : null}
          </li>
        )
      })}
    </ul>
  )
}
