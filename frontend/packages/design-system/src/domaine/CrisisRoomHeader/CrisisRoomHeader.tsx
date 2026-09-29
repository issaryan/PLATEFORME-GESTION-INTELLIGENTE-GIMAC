import { cx } from '../../lib/cx'
import { Icon } from '../../icones/Icon/Icon'
import { StatusBadge } from '../../affichage/StatusBadge/StatusBadge'
import { Button } from '../../actions/Button/Button'
import { useLibelle } from '../../i18n/LangueProvider'

export interface CrisisRoomHeaderProps {
  id: string
  level: 1 | 2 | 3
  since?: string
  title: string
  present: number
  expected: number
  quorum: number
  /** Membres attendus et acquittement de leur convocation. */
  members?: { role: string; ack: boolean }[]
}

// En-tête de la salle de crise (§7.7, DES-20).
export function CrisisRoomHeader(p: CrisisRoomHeaderProps) {
  const t = useLibelle()
  const quorum = p.present >= p.quorum
  return (
    <header className="gm-crisis">
      <div className="gm-crisis-main">
        <div className="gm-row">
          <span className="gm-mono gm-field-help">{p.id}</span>
          <StatusBadge status="crit" label={t('crise.niveau', { n: p.level })} />
          {p.since ? <span className="gm-field-help">{t('crise.depuis', { heure: p.since })}</span> : null}
        </div>
        <h1 className="gm-heading-display">{p.title}</h1>
        <div className="gm-row">
          <StatusBadge status={quorum ? 'ok' : 'warn'} label={quorum ? t('crise.quorumAtteint') : t('crise.quorumNonAtteint')} icon="users" />
          <span className="gm-field-help gm-num">{t('crise.presents', { presents: p.present, attendus: p.expected, quorum: p.quorum })}</span>
          {(p.members || []).map((m) => (
            <span key={m.role} className={cx('gm-badge', m.ack ? 'gm-badge-ok' : 'gm-badge-neutral')}>
              <Icon name={m.ack ? 'check' : 'hourglass'} />
              {m.role}
            </span>
          ))}
        </div>
      </div>
      <div className="gm-row">
        <Button variant="secondary" icon="message-square-lock">{t('crise.salon')}</Button>
        <Button variant="primary" icon="video">{t('crise.conference')}</Button>
      </div>
    </header>
  )
}
