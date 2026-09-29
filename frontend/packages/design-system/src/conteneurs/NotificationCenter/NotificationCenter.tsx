import { useState } from 'react'
import { cx } from '../../lib/cx'
import type { Status } from '../../lib/types'
import { Icon } from '../../icones/Icon/Icon'
import type { IconName } from '../../icones/icons'
import { Button } from '../../actions/Button/Button'
import { IconButton } from '../../actions/IconButton/IconButton'
import { Link } from '../../actions/Link/Link'
import { StatusBadge } from '../../affichage/StatusBadge/StatusBadge'
import { Tabs } from '../../navigation/Tabs/Tabs'
import { useLibelle } from '../../i18n/LangueProvider'
import type { CleLibelle } from '../../i18n/libelles'

const LEVELS: [Notification['level'], CleLibelle, Status][] = [
  ['critical', 'notif.critique', 'crit'],
  ['urgent', 'notif.urgent', 'warn'],
  ['important', 'notif.important', 'info'],
  ['info', 'notif.information', 'neutral'],
]
const NOTIF_ICON: Record<string, IconName> = {
  alert: 'bell-ring',
  ticket: 'ticket',
  deadline: 'calendar-clock',
  document: 'file-text',
  liaison: 'route',
  audit: 'scroll-text',
}

export interface Notification {
  id: string | number
  level: 'critical' | 'urgent' | 'important' | 'info'
  kind?: 'alert' | 'ticket' | 'deadline' | 'document' | 'liaison' | 'audit'
  title: string
  text: string
  /** Horodatage relatif. */
  at: string
  /** Référence de l'objet lié. */
  ref: string
  read?: boolean
  acked?: boolean
}

export interface NotificationCenterProps {
  notifications: Notification[]
  tab?: 'unread' | 'all'
  onClose?: () => void
  height?: number
}

// Centre de notifications de 400 px ouvert depuis la cloche (§6.5).
export function NotificationCenter(p: NotificationCenterProps) {
  const t = useLibelle()
  const [tab, setTab] = useState<string>(p.tab || 'unread')
  const list = (p.notifications || []).filter((n) => tab === 'all' || !n.read)
  return (
    <div className="gm-notif" role="dialog" aria-label={t('notif.centre')} style={{ height: p.height || 480 }}>
      <div className="gm-notif-head">
        <strong>{t('notif.titre')}</strong>
        <span className="gm-field-help">{t('notif.nonLuesN', { n: (p.notifications || []).filter((n) => !n.read).length })}</span>
        <span style={{ marginLeft: 'auto' }} />
        <Button size="sm" variant="ghost">{t('notif.toutLu')}</Button>
        <IconButton variant="ghost" size="sm" icon="x" label={t('commun.fermer')} onClick={p.onClose} />
      </div>
      <div style={{ padding: '0 var(--gm-space-3)' }}>
        <Tabs items={[{ key: 'unread', label: t('notif.nonLues') }, { key: 'all', label: t('notif.toutes') }]} value={tab} onChange={setTab} />
      </div>
      <div className="gm-notif-list">
        {LEVELS.map((L) => {
          const items = list.filter((n) => n.level === L[0])
          if (!items.length) return null
          return (
            <section key={L[0]} aria-label={t(L[1])}>
              <div className="gm-notif-group">
                <StatusBadge status={L[2]} label={t(L[1]).toLowerCase()} icon={L[0] === 'info' ? 'info' : undefined} />
                <span className="gm-field-help gm-num">{items.length}</span>
              </div>
              {items.map((n) => (
                <article key={n.id} className={cx('gm-notif-item', !n.read && 'gm-notif-unread')}>
                  <Icon name={(n.kind && NOTIF_ICON[n.kind]) || 'info'} size={20} />
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div className="gm-notif-title">{n.title}</div>
                    <div className="gm-field-help">{n.text}</div>
                    <div className="gm-row" style={{ marginTop: 4, gap: 'var(--gm-space-3)' }}>
                      <span className="gm-field-help gm-num">{n.at}</span>
                      <Link href="#" mono>{n.ref}</Link>
                    </div>
                  </div>
                  {(n.level === 'critical' || n.level === 'urgent') && !n.acked ? (
                    <Button size="sm" variant="primary">{t('commun.acquitter')}</Button>
                  ) : n.acked ? (
                    <StatusBadge status="ok" label={t('notif.acquittee')} />
                  ) : null}
                </article>
              ))}
            </section>
          )
        })}
        {list.length ? null : <div className="gm-cmd-empty">{t('notif.aucune')}</div>}
      </div>
      <div className="gm-notif-foot">
        <Link href="#">{t('notif.pleinePage')}</Link>
        <Link href="#">{t('notif.preferences')}</Link>
      </div>
    </div>
  )
}
