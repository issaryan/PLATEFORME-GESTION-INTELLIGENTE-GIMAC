import { cx } from '../../lib/cx'
import type { Status } from '../../lib/types'
import { Icon } from '../../icones/Icon/Icon'
import { StatusBadge } from '../../affichage/StatusBadge/StatusBadge'
import { ELEMENTS, type FourElements } from '../quatreElements'

export type OperationState = 'eligible' | 'locked' | 'closed' | 'out' | 'late'

const NODE_STATE: Record<OperationState, [Status, string]> = {
  eligible: ['info', 'éligible'],
  locked: ['neutral', 'verrouillée'],
  closed: ['ok', 'close'],
  out: ['neutral', 'hors périmètre'],
  late: ['warn', 'en retard'],
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
  const st = NODE_STATE[p.state] || NODE_STATE.eligible
  const el = p.elements || {}
  return (
    <div className={cx('gm-node', 'gm-node-' + p.state, p.critical && 'gm-node-critical')} role="group" aria-label={p.code + ' ' + p.label}>
      <div className="gm-row" style={{ justifyContent: 'space-between' }}>
        <span className="gm-mono">{p.code}</span>
        <StatusBadge status={st[0]} label={st[1]} />
      </div>
      <div className="gm-node-title">{p.label}</div>
      <div className="gm-field-help">{'Incombe à : ' + p.owner}</div>
      <div className="gm-node-el">
        {ELEMENTS.map((e) => {
          const ok = !!el[e[0]]
          return (
            <span key={e[0]} className={cx('gm-node-dot', ok && 'gm-node-dot-ok')} title={e[1] + (ok ? ' : présent' : ' : manquant')} aria-label={e[1] + (ok ? ' présent' : ' manquant')} role="img">
              <Icon name={e[2]} size={16} />
            </span>
          )
        })}
      </div>
    </div>
  )
}
