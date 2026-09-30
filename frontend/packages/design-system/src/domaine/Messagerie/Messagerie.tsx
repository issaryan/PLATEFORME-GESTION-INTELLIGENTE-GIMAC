import { useEffect, useRef, type KeyboardEvent, type ReactNode } from 'react'
import { cx } from '../../lib/cx'
import { Avatar } from '../../affichage/Avatar/Avatar'
import { Tag } from '../../affichage/Tag/Tag'
import { EmptyState } from '../../affichage/EmptyState/EmptyState'
import { Button } from '../../actions/Button/Button'
import { IconButton } from '../../actions/IconButton/IconButton'
import { Icon } from '../../icones/Icon/Icon'
import { InlineAlert } from '../../conteneurs/InlineAlert/InlineAlert'
import { useLibelle } from '../../i18n/LangueProvider'

// Messagerie de travail (D-64) : liste des conversations, fil et zone de saisie, assemblés à partir de
// l'avatar, de l'étiquette, des boutons et des classes du salon (gm-chat, gm-msg). Le fond vient du
// service : ces composants n'affichent que ce qu'on leur donne.

export interface ConversationItem {
  id: string
  titre: string
  /** Membres ou liaison rattachée, en seconde ligne. */
  sousTitre?: string
  extrait?: string
  quand?: string
  nonLus?: number
  /** Conversation de groupe : pictogramme de groupe à la place des initiales. */
  groupe?: boolean
}

export interface ConversationListProps {
  items: ConversationItem[]
  selected?: string
  onSelect?: (id: string) => void
  empty?: string
}

/** Liste des conversations : la plus récente en tête, non-lus en pastille, conversation ouverte marquée. */
export function ConversationList(p: ConversationListProps) {
  const t = useLibelle()
  if (p.items.length === 0) return <EmptyState icon="messages-square" title={p.empty || t('messagerie.aucuneConversation')} />
  return (
    <ul className="gm-conv-list" aria-label={t('messagerie.conversations')}>
      {p.items.map((c) => (
        <li key={c.id}>
          <button type="button" className={cx('gm-conv', c.nonLus ? 'gm-conv-nonlu' : undefined)} aria-current={c.id === p.selected ? 'true' : undefined} onClick={() => p.onSelect?.(c.id)}>
            {c.groupe ? <span className="gm-avatar" aria-hidden="true"><Icon name="users" size={16} /></span> : <Avatar name={c.titre} />}
            <span className="gm-conv-corps">
              <span className="gm-conv-ligne">
                <strong className="gm-conv-titre">{c.titre}</strong>
                {c.quand ? <span className="gm-num gm-conv-quand">{c.quand}</span> : null}
              </span>
              {c.sousTitre ? <span className="gm-conv-sous">{c.sousTitre}</span> : null}
              <span className="gm-conv-ligne">
                <span className="gm-conv-extrait">{c.extrait}</span>
                {c.nonLus ? <span className="gm-conv-pastille" aria-label={t('messagerie.nonLus', { n: c.nonLus })}>{c.nonLus}</span> : null}
              </span>
            </span>
          </button>
        </li>
      ))}
    </ul>
  )
}

export interface ThreadAttachment {
  id: string
  nom: string
  taille?: string
}

export interface ThreadMessage {
  id: string
  auteur: string
  quand: string
  mine?: boolean
  /** Corps déjà mis en forme (liens des liaisons citées, retours à la ligne). */
  contenu: ReactNode
  /** Étiquettes de projet, en liens. */
  etiquettes?: ReactNode
  pieces?: ThreadAttachment[]
}

export interface MessageThreadProps {
  titre: string
  sousTitre?: ReactNode
  actions?: ReactNode
  messages: ThreadMessage[]
  plusAnciens?: boolean
  onPlusAnciens?: () => void
  onPiece?: (id: string) => void
  /** Zone de saisie, sous le fil. */
  saisie?: ReactNode
  height?: number | string
}

/** Fil d'une conversation : en-tête, messages du plus ancien au plus récent, pièces à télécharger. */
export function MessageThread(p: MessageThreadProps) {
  const t = useLibelle()
  const liste = useRef<HTMLOListElement>(null)
  const dernier = p.messages[p.messages.length - 1]?.id
  // Le fil s'ouvre sur le dernier message et y revient à chaque nouveau message.
  useEffect(() => {
    const l = liste.current
    if (l) l.scrollTop = l.scrollHeight
  }, [dernier])
  return (
    <section className="gm-chat gm-fil" style={{ height: p.height ?? '100%' }} aria-label={p.titre}>
      <header className="gm-chat-head">
        <Icon name="messages-square" size={20} />
        <span className="gm-col" style={{ gap: 0, minWidth: 0 }}>
          <strong>{p.titre}</strong>
          {p.sousTitre ? <span className="gm-field-help">{p.sousTitre}</span> : null}
        </span>
        {p.actions ? <span className="gm-row" style={{ marginLeft: 'auto' }}>{p.actions}</span> : null}
      </header>
      <ol className="gm-chat-list" ref={liste} aria-label={t('messagerie.messages')}>
        {p.plusAnciens ? (
          <li className="gm-fil-plus">
            <Button size="sm" variant="ghost" icon="arrow-up" onClick={p.onPlusAnciens}>{t('messagerie.plusAnciens')}</Button>
          </li>
        ) : null}
        {p.messages.length === 0 ? <li className="gm-fil-vide"><EmptyState icon="message-square" title={t('messagerie.vide')} /></li> : null}
        {p.messages.map((m) => (
          <li key={m.id} className={cx('gm-msg', m.mine && 'gm-msg-mine')}>
            <div className="gm-msg-meta">
              <strong>{m.mine ? t('messagerie.vous') : m.auteur}</strong>
              <span className="gm-num">{m.quand}</span>
            </div>
            <div className="gm-msg-body">
              <div className="gm-msg-texte">{m.contenu}</div>
              {m.etiquettes ? <div className="gm-row gm-msg-etiquettes">{m.etiquettes}</div> : null}
              {(m.pieces ?? []).map((pj) => (
                <div key={pj.id} className="gm-msg-att">
                  <button type="button" className="gm-link" onClick={() => p.onPiece?.(pj.id)} aria-label={t('messagerie.piece', { nom: pj.nom })}>
                    <Icon name="paperclip" size={14} /> {pj.nom}
                  </button>
                  {pj.taille ? <span className="gm-num gm-field-help"> · {pj.taille}</span> : null}
                </div>
              ))}
            </div>
          </li>
        ))}
      </ol>
      {p.saisie}
    </section>
  )
}

export interface MessageComposerProps {
  value: string
  onChange: (v: string) => void
  fichiers?: File[]
  onFichiers?: (f: File[]) => void
  onSend: () => void
  sending?: boolean
  /** Types acceptés (attribut accept du sélecteur de fichiers). */
  accept?: string
  maxFichiers?: number
  error?: ReactNode
  /** Éléments de saisie complémentaires (étiquettes de liaison…), au-dessus du champ. */
  extras?: ReactNode
  disabled?: boolean
  label?: string
}

/** Zone de saisie : texte sur plusieurs lignes, pièces jointes retirables, envoi à l'Entrée. */
export function MessageComposer(p: MessageComposerProps) {
  const t = useLibelle()
  const selecteur = useRef<HTMLInputElement>(null)
  const fichiers = p.fichiers ?? []
  const peutEnvoyer = !p.disabled && !p.sending && (p.value.trim().length > 0 || fichiers.length > 0)
  const touche = (e: KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey && !e.nativeEvent.isComposing) {
      e.preventDefault()
      if (peutEnvoyer) p.onSend()
    }
  }
  return (
    <div className="gm-chat-input gm-saisie">
      {p.extras}
      {fichiers.length ? (
        <div className="gm-row" style={{ flexWrap: 'wrap', gap: 'var(--gm-space-1)' }}>
          {fichiers.map((f, i) => (
            <Tag key={`${f.name}-${i}`} onRemove={() => p.onFichiers?.(fichiers.filter((_, j) => j !== i))}>{f.name}</Tag>
          ))}
        </div>
      ) : null}
      {p.error ? <InlineAlert level="crit">{p.error}</InlineAlert> : null}
      <div className="gm-row gm-saisie-ligne">
        <IconButton variant="ghost" icon="paperclip" label={t('messagerie.joindre')} disabled={p.disabled || fichiers.length >= (p.maxFichiers ?? 5)} onClick={() => selecteur.current?.click()} />
        <input
          ref={selecteur}
          type="file"
          multiple
          hidden
          accept={p.accept}
          aria-label={t('messagerie.joindre')}
          onChange={(e) => {
            const choisis = Array.from(e.target.files ?? [])
            p.onFichiers?.([...fichiers, ...choisis].slice(0, p.maxFichiers ?? 5))
            e.target.value = ''
          }}
        />
        <textarea
          className="gm-input gm-saisie-texte"
          rows={2}
          aria-label={p.label || t('messagerie.message')}
          placeholder={t('messagerie.aide')}
          value={p.value}
          disabled={p.disabled}
          onChange={(e) => p.onChange(e.target.value)}
          onKeyDown={touche}
        />
        <Button variant="primary" icon="send" loading={p.sending} disabled={!peutEnvoyer} onClick={p.onSend}>{t('messagerie.envoyer')}</Button>
      </div>
    </div>
  )
}
