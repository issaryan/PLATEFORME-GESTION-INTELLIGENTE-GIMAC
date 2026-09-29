import { cx } from '../../lib/cx'
import type { Status } from '../../lib/types'
import { Icon } from '../../icones/Icon/Icon'
import { StatusBadge } from '../../affichage/StatusBadge/StatusBadge'
import { Button } from '../../actions/Button/Button'
import { useLibelle } from '../../i18n/LangueProvider'

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
  const t = useLibelle()
  return (
    <div className="gm-dtt" role="group" aria-label={t('decision.label', { id: p.decisionId })}>
      <div className="gm-dtt-col" style={{ flex: 3 }}>
        <div className="gm-field-help">
          <Icon name="gavel" /> {t('decision.decision')} <span className="gm-mono">{p.decisionId}</span>
        </div>
        <div>{p.decision}</div>
      </div>
      <Icon name="arrow-right" size={20} className="gm-dtt-arrow" />
      <div className="gm-dtt-col" style={{ flex: 3 }}>
        <div className="gm-field-help">
          <Icon name="kanban" /> {t('decision.tache')} {p.taskId ? <span className="gm-mono">{p.taskId}</span> : <em>{t('decision.nonCreee')}</em>}
        </div>
        <div>{p.task || <Button size="sm" variant="secondary">{t('decision.creer')}</Button>}</div>
      </div>
      <div className="gm-dtt-col" style={{ flex: 2 }}>
        <div className="gm-field-help">{t('elements.responsable')}</div>
        <div>{p.owner}</div>
      </div>
      <div className="gm-dtt-col" style={{ flex: 1 }}>
        <div className="gm-field-help">{t('decision.echeance')}</div>
        <div className={cx('gm-num', p.late && 'gm-late')}>{p.due}</div>
      </div>
      <div className="gm-dtt-col" style={{ flex: 'none' }}>
        <StatusBadge status={p.status} label={p.statusLabel} />
      </div>
    </div>
  )
}
