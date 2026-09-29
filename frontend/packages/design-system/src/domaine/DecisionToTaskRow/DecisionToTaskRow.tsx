import { cx } from '../../lib/cx'
import type { Status } from '../../lib/types'
import { Icon } from '../../icones/Icon/Icon'
import { StatusBadge } from '../../affichage/StatusBadge/StatusBadge'
import { Button } from '../../actions/Button/Button'

export interface DecisionToTaskRowProps {
  decisionId: string
  decision: string
  taskId?: string
  task?: string
  owner: string
  due: string
  late?: boolean
  status: Status
  statusLabel: string
}

// Traçabilité d'une décision de réunion vers la tâche engendrée (§7.7, DES-25).
export function DecisionToTaskRow(p: DecisionToTaskRowProps) {
  return (
    <div className="gm-dtt" role="group" aria-label={'Décision ' + p.decisionId}>
      <div className="gm-dtt-col" style={{ flex: 3 }}>
        <div className="gm-field-help">
          <Icon name="gavel" /> Décision <span className="gm-mono">{p.decisionId}</span>
        </div>
        <div>{p.decision}</div>
      </div>
      <Icon name="arrow-right" size={20} className="gm-dtt-arrow" />
      <div className="gm-dtt-col" style={{ flex: 3 }}>
        <div className="gm-field-help">
          <Icon name="kanban" /> Tâche {p.taskId ? <span className="gm-mono">{p.taskId}</span> : <em>non créée</em>}
        </div>
        <div>{p.task || <Button size="sm" variant="secondary">Créer la tâche</Button>}</div>
      </div>
      <div className="gm-dtt-col" style={{ flex: 2 }}>
        <div className="gm-field-help">Responsable</div>
        <div>{p.owner}</div>
      </div>
      <div className="gm-dtt-col" style={{ flex: 1 }}>
        <div className="gm-field-help">Échéance</div>
        <div className={cx('gm-num', p.late && 'gm-late')}>{p.due}</div>
      </div>
      <div className="gm-dtt-col" style={{ flex: 'none' }}>
        <StatusBadge status={p.status} label={p.statusLabel} />
      </div>
    </div>
  )
}
