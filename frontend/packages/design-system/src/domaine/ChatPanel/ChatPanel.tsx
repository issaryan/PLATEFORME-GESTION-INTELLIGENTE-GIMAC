import { cx } from '../../lib/cx'
import { Icon } from '../../icones/Icon/Icon'
import { StatusBadge } from '../../affichage/StatusBadge/StatusBadge'
import { InlineAlert } from '../../conteneurs/InlineAlert/InlineAlert'
import { Button } from '../../actions/Button/Button'
import { IconButton } from '../../actions/IconButton/IconButton'
import { useLibelle } from '../../i18n/LangueProvider'

export interface ChatMessage {
  author: string
  at: string
  text: string
  mine?: boolean
  read?: boolean
  attachment?: string
}

export interface ChatPanelProps {
  room: string
  members: number
  /** Tous les terminaux du salon sont vérifiés. */
  verified: boolean
  messages?: ChatMessage[]
  /** Envoi refusé : un membre utilise un terminal non vérifié. */
  blocked?: boolean
  draft?: string
  onDraft?: (v: string) => void
  height?: number
}

// Salon de messagerie chiffrée (§7.7, DES-29).
export function ChatPanel(p: ChatPanelProps) {
  const t = useLibelle()
  return (
    <div className="gm-chat" style={{ height: p.height || 360 }}>
      <div className="gm-chat-head">
        <Icon name="message-square-lock" size={20} />
        <strong>{p.room}</strong>
        <span className="gm-field-help">{t('salon.membres', { n: p.members })}</span>
        <span style={{ marginLeft: 'auto' }}>
          <StatusBadge status={p.verified ? 'ok' : 'warn'} label={p.verified ? t('salon.verifie') : t('salon.nonVerifie')} icon="smartphone" />
        </span>
      </div>
      <ol className="gm-chat-list" aria-label={t('salon.messages')}>
        {(p.messages || []).map((m, i) => (
          <li key={i} className={cx('gm-msg', m.mine && 'gm-msg-mine')}>
            <div className="gm-msg-meta">
              <strong>{m.author}</strong>
              <span className="gm-num">{m.at}</span>
            </div>
            <div className="gm-msg-body">
              {m.text}
              {m.attachment ? (
                <div className="gm-msg-att">
                  <Icon name="paperclip" size={14} /> {m.attachment} <Icon name="lock" size={12} />
                </div>
              ) : null}
            </div>
            {m.mine ? (
              <div className="gm-msg-ack">
                <Icon name={m.read ? 'square-check' : 'check'} size={12} /> {m.read ? t('salon.lu') : t('salon.remis')}
              </div>
            ) : null}
          </li>
        ))}
      </ol>
      {p.blocked ? (
        <InlineAlert level="crit" title={t('salon.refuse')}>
          {t('salon.refuseTexte')}
        </InlineAlert>
      ) : null}
      <div className="gm-chat-input">
        <IconButton variant="ghost" icon="paperclip" label={t('salon.joindre')} />
        <input type="text" className="gm-input" aria-label={t('salon.message')} value={p.draft || ''} onChange={(e) => p.onDraft && p.onDraft(e.target.value)} />
        <Button variant="primary" icon="send" disabled={!!p.blocked} disabledReason={t('salon.nonVerifieSalon')}>{t('salon.envoyer')}</Button>
      </div>
    </div>
  )
}
