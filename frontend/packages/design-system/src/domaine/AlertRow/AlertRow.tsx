import { cx } from '../../lib/cx'
import type { Severity } from '../../lib/types'
import { Icon } from '../../icones/Icon/Icon'
import { SeverityBadge } from '../../affichage/SeverityBadge/SeverityBadge'
import { Button } from '../../actions/Button/Button'
import { useLibelle } from '../../i18n/LangueProvider'

export interface AlertRowProps {
  id: string
  severity: Severity
  liaison: string
  message: string
  at: string
  /** Délai d'acquittement restant, en minutes. */
  remainingMin: number
  onAck?: () => void
  onTicket?: () => void
}

// Ligne d'alerte de supervision (§7.7, DES-16).
export function AlertRow(p: AlertRowProps) {
  const t = useLibelle()
  return (
    <div className={cx('gm-alert', 'gm-alert-' + (p.severity === 'S1' ? 'crit' : 'warn'))} role="listitem">
      <Icon name="bell-ring" size={20} />
      <div className="gm-col" style={{ gap: 2, flex: 1 }}>
        <div className="gm-row">
          <span className="gm-mono">{p.id}</span>
          <SeverityBadge level={p.severity} />
          <strong>{p.liaison}</strong>
        </div>
        <div className="gm-field-help">{p.message + ' · ' + p.at}</div>
      </div>
      <div className="gm-col" style={{ gap: 2, alignItems: 'flex-end' }}>
        <span className="gm-field-help">{t('alerte.sous')}</span>
        <span className={cx('gm-num', p.remainingMin <= 5 && 'gm-late')} style={{ fontWeight: 600 }}>
          {p.remainingMin + ' min'}
        </span>
      </div>
      <div className="gm-row">
        <Button size="sm" variant="primary" onClick={p.onAck}>{t('commun.acquitter')}</Button>
        <Button size="sm" variant="secondary" icon="ticket" onClick={p.onTicket}>{t('alerte.ticket')}</Button>
      </div>
    </div>
  )
}
