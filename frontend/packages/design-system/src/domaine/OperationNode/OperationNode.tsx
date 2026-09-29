import { cx } from '../../lib/cx'
import type { Status } from '../../lib/types'
import { Icon } from '../../icones/Icon/Icon'
import { StatusBadge } from '../../affichage/StatusBadge/StatusBadge'
import { ELEMENTS, reuni, type FourElements } from '../quatreElements'
import { useLibelle } from '../../i18n/LangueProvider'
import type { CleLibelle } from '../../i18n/libelles'

export type OperationState = 'eligible' | 'locked' | 'closed' | 'out' | 'late'

const NODE_STATE: Record<OperationState, [Status, CleLibelle]> = {
  eligible: ['info', 'operation.eligible'],
  locked: ['neutral', 'operation.verrouillee'],
  closed: ['ok', 'operation.close'],
  out: ['neutral', 'operation.hors'],
  late: ['warn', 'operation.retard'],
}

export interface OperationNodeProps {
  code: string
  label: string
  owner: string
  state: OperationState
  critical?: boolean
  elements?: FourElements
}

// Nœud du graphe des opérations d'une liaison (§7.7) : quatre pictogrammes des quatre éléments.
export function OperationNode(p: OperationNodeProps) {
  const t = useLibelle()
  const st = NODE_STATE[p.state] || NODE_STATE.eligible
  const el = p.elements || {}
  return (
    <div className={cx('gm-node', 'gm-node-' + p.state, p.critical && 'gm-node-critical')} role="group" aria-label={p.code + ' ' + p.label}>
      <div className="gm-row" style={{ justifyContent: 'space-between' }}>
        <span className="gm-mono">{p.code}</span>
        <StatusBadge status={st[0]} label={t(st[1])} />
      </div>
      <div className="gm-node-title">{p.label}</div>
      <div className="gm-field-help">{t('operation.incombe', { qui: p.owner })}</div>
      <div className="gm-node-el">
        {ELEMENTS.map((e) => {
          const ok = reuni(el[e[0]])
          return (
            <span key={e[0]} className={cx('gm-node-dot', ok && 'gm-node-dot-ok')} title={t(ok ? 'elements.titrePresent' : 'elements.titreManquant', { element: t(e[1]) })} aria-label={t(ok ? 'elements.present' : 'elements.manquant', { element: t(e[1]) })} role="img">
              <Icon name={e[2]} size={16} />
            </span>
          )
        })}
      </div>
    </div>
  )
}
