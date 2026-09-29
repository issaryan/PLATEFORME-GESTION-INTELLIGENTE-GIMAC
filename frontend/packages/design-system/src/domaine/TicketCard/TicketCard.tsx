import { cx } from '../../lib/cx'
import type { Severity, Status } from '../../lib/types'
import { Icon } from '../../icones/Icon/Icon'
import { SeverityBadge } from '../../affichage/SeverityBadge/SeverityBadge'
import { Tag } from '../../affichage/Tag/Tag'
import { StatusBadge } from '../../affichage/StatusBadge/StatusBadge'
import { useLibelle } from '../../i18n/LangueProvider'

export interface TicketCardProps {
  id: string
  title: string
  institution: string
  severity?: Severity
  type?: string
  status: Status
  statusLabel: string
  due: string
  late?: boolean
  selected?: boolean
}

// Carte Kanban ou ligne de ticket (§7.7).
export function TicketCard(p: TicketCardProps) {
  const t = useLibelle()
  return (
    <article className={cx('gm-tcard', p.selected && 'gm-tcard-sel')} tabIndex={0} aria-label={p.id + ' ' + p.title}>
      <div className="gm-row" style={{ justifyContent: 'space-between' }}>
        <span className="gm-mono">{p.id}</span>
        {p.severity ? <SeverityBadge level={p.severity} /> : <Tag>{p.type || t('ticket.demande')}</Tag>}
      </div>
      <div className="gm-tcard-title">{p.title}</div>
      <div className="gm-field-help">{p.institution}</div>
      <div className="gm-row" style={{ justifyContent: 'space-between', marginTop: 'var(--gm-space-2)' }}>
        <StatusBadge status={p.status} label={p.statusLabel} />
        <span className={cx('gm-field-help gm-num', p.late && 'gm-late')}>
          <Icon name="clock" /> {p.due}
        </span>
      </div>
    </article>
  )
}
