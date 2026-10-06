import { useEffect, useId, useRef, useState, type KeyboardEvent, type ReactNode, type TouchEvent } from 'react'
import { cx } from '../../lib/cx'
import { Avatar } from '../../affichage/Avatar/Avatar'
import { Tag } from '../../affichage/Tag/Tag'
import { EmptyState } from '../../affichage/EmptyState/EmptyState'
import { Button } from '../../actions/Button/Button'
import { IconButton } from '../../actions/IconButton/IconButton'
import { Icon } from '../../icones/Icon/Icon'
import { InlineAlert } from '../../conteneurs/InlineAlert/InlineAlert'
import { Dialog } from '../../conteneurs/Dialog/Dialog'
import { Skeleton } from '../../affichage/Skeleton/Skeleton'
import { useLibelle } from '../../i18n/LangueProvider'
import type { IconName } from '../../icones/icons'
import type { CleLibelle } from '../../i18n/libelles'
import { ApercuPdf } from './ApercuPdf'

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
  /** Photo de profil de l'interlocuteur (échange direct) ; initiales à défaut. */
  avatar?: string | null
  /** Interlocuteur en ligne (D-72) : point vert sur l'avatar. */
  enLigne?: boolean
  /** « Aminata écrit… » (D-72) : remplace l'extrait tant que quelqu'un écrit. */
  saisie?: string
}

/** Point « en ligne » (D-72) sur un avatar, annoncé aux technologies d'assistance. */
export function Presence({ enLigne, children }: { enLigne?: boolean; children: ReactNode }) {
  const t = useLibelle()
  return (
    <span className="gm-presence">
      {children}
      {enLigne ? <span className="gm-presence-point" role="img" aria-label={t('messagerie.enLigne')} /> : null}
    </span>
  )
}

/** Trois points animés de « … écrit » (D-72) ; immobiles si l'utilisateur réduit les animations. */
function Points() {
  return <span className="gm-points" aria-hidden="true"><i /><i /><i /></span>
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
            {c.groupe ? <span className="gm-avatar" aria-hidden="true"><Icon name="users" size={16} /></span> : <Presence enLigne={c.enLigne}><Avatar name={c.titre} src={c.avatar} /></Presence>}
            <span className="gm-conv-corps">
              <span className="gm-conv-ligne">
                <strong className="gm-conv-titre">{c.titre}</strong>
                {c.quand ? <span className="gm-num gm-conv-quand">{c.quand}</span> : null}
              </span>
              {c.sousTitre ? <span className="gm-conv-sous">{c.sousTitre}</span> : null}
              <span className="gm-conv-ligne">
                {c.saisie ? <span className="gm-conv-extrait gm-conv-saisie">{c.saisie}</span> : <span className="gm-conv-extrait">{c.extrait}</span>}
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
  /** Photo de profil de l'auteur ; initiales à défaut. */
  avatar?: string | null
  /** Corps déjà mis en forme (liens des liaisons citées, retours à la ligne). */
  contenu: ReactNode
  /** Étiquettes de projet, en liens. */
  etiquettes?: ReactNode
  pieces?: ThreadAttachment[]
  /** Événement de la vie d'un groupe (D-68) : ligne centrée, sans bulle ni auteur. */
  evenement?: boolean
  /** Message cité (D-82) : bloc en tête de la bulle, qui mène au message d'origine. */
  citation?: ThreadCitation
  /** Le lecteur y est mentionné, ou le message répond à l'un des siens (D-82) : bulle légèrement signalée. */
  mentionneMoi?: boolean
}

/** Message cité par une réponse (D-82), dans la bulle ou au-dessus de la zone de saisie. */
export interface ThreadCitation {
  /** Auteur du message cité ; absent pour un message supprimé. */
  auteur?: string | null
  /** Extrait du message cité (120 caractères au plus, fourni par le service). */
  extrait: string
  /** Nombre de pièces jointes du message cité : trombone devant l'extrait. */
  pieces?: number
  /** Message cité purgé ou illisible : « Message supprimé », sans auteur ni date. */
  supprime?: boolean
  /** Message cité écrit par l'utilisateur : « Vous ». */
  mine?: boolean
  /** Clic sur la citation : défilement jusqu'au message d'origine. */
  onOuvrir?: () => void
}

/** Bloc cité : barre colorée, auteur, extrait (une ou deux lignes), trombone si le message portait des pièces. */
function Citation({ c }: { c: ThreadCitation }) {
  const t = useLibelle()
  const auteur = c.supprime ? null : c.mine ? t('messagerie.vous') : c.auteur
  const corps = (
    <>
      <span className="gm-sr-only">{t('messagerie.citation.reponseA')}</span>
      {auteur ? <span className="gm-citation-auteur">{auteur}</span> : null}
      <span className={cx('gm-citation-extrait', c.supprime && 'gm-citation-supprime')}>
        {c.supprime ? t('messagerie.citation.supprime') : (
          <>
            {c.pieces ? <Icon name="paperclip" size={12} /> : null}
            {c.extrait || (c.pieces ? t('messagerie.citation.pieces', { n: c.pieces }) : '')}
          </>
        )}
      </span>
    </>
  )
  if (c.onOuvrir && !c.supprime) {
    return <button type="button" className="gm-citation" title={t('messagerie.citation.aller')} onClick={c.onOuvrir}>{corps}</button>
  }
  return <div className="gm-citation">{corps}</div>
}

/** Geste de balayage vers la droite sur un écran tactile (64 px, presque horizontal) : répondre au message. */
function useBalayage(agir: (() => void) | undefined) {
  const depart = useRef<{ x: number; y: number } | null>(null)
  if (!agir) return {}
  return {
    onTouchStart: (e: TouchEvent) => {
      const p = e.touches[0]
      depart.current = p ? { x: p.clientX, y: p.clientY } : null
    },
    onTouchEnd: (e: TouchEvent) => {
      const d = depart.current
      const p = e.changedTouches[0]
      depart.current = null
      if (d && p && p.clientX - d.x > 64 && Math.abs(p.clientY - d.y) < 40) agir()
    },
  }
}

export interface MessageThreadProps {
  titre: string
  sousTitre?: ReactNode
  /** Avatar de l'en-tête (interlocuteur d'un échange direct, D-72) ; pictogramme de messagerie à défaut. */
  avatar?: ReactNode
  /** « Aminata écrit… » (D-72), sous le dernier message. */
  ecrit?: string
  actions?: ReactNode
  messages: ThreadMessage[]
  plusAnciens?: boolean
  onPlusAnciens?: () => void
  onPiece?: (id: string) => void
  /** Repartage d'une pièce par son ticket (D-74) : bouton « Transférer » à côté de chaque pièce. */
  onTransferer?: (id: string) => void
  /** Zone de saisie, sous le fil. */
  saisie?: ReactNode
  height?: number | string
  /**
   * Répondre à un message précis (D-82) : bouton au survol ou au focus de chaque bulle, toujours visible sur
   * un écran tactile, et balayage vers la droite.
   */
  onRepondre?: (id: string) => void
  /** Message mis en évidence (citation ouverte, lien d'une notification) : le fil y défile, la bulle est surlignée. */
  enEvidence?: string
  /** Annonce brève sous le fil (« Message plus ancien non chargé »). */
  annonce?: string
}

/** Bulle d'un message : ligne d'auteur (et « Répondre »), citation, texte ; les pièces viennent en enfants. */
function Bulle({ m, enEvidence, onRepondre, children }: { m: ThreadMessage; enEvidence: boolean; onRepondre?: (id: string) => void; children?: ReactNode }) {
  const t = useLibelle()
  const balayage = useBalayage(onRepondre ? () => onRepondre(m.id) : undefined)
  const auteur = m.mine ? t('messagerie.vous') : m.auteur
  return (
    <li data-message={m.id} className={cx('gm-msg', m.mine && 'gm-msg-mine', m.mentionneMoi && 'gm-msg-mentionne', enEvidence && 'gm-msg-evidence')} {...balayage}>
      <div className="gm-msg-meta">
        <Avatar name={m.auteur} src={m.avatar} size="sm" decorative />
        <strong>{auteur}</strong>
        <span className="gm-num">{m.quand}</span>
        {m.mentionneMoi ? <span className="gm-msg-pour-moi"><Icon name="at-sign" size={12} label={t('messagerie.mentionneMoi')} /></span> : null}
        {onRepondre ? <IconButton className="gm-msg-repondre" variant="ghost" size="sm" icon="reply" label={m.mine ? t('messagerie.repondreMoi') : t('messagerie.repondre', { auteur })} onClick={() => onRepondre(m.id)} /> : null}
      </div>
      <div className="gm-msg-body">
        {m.citation ? <Citation c={m.citation} /> : null}
        <div className="gm-msg-texte">{m.contenu}</div>
        {children}
      </div>
    </li>
  )
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
  // Message cité ou désigné par un lien : amené au centre du fil (après le retour en bas ci-dessus).
  useEffect(() => {
    if (!p.enEvidence) return
    const cible = Array.from(liste.current?.querySelectorAll<HTMLElement>('[data-message]') ?? []).find((e) => e.dataset.message === p.enEvidence)
    cible?.scrollIntoView?.({ block: 'center', behavior: 'smooth' })
  }, [p.enEvidence])
  return (
    <section className="gm-chat gm-fil" style={{ height: p.height ?? '100%' }} aria-label={p.titre}>
      <header className="gm-chat-head">
        {p.avatar ?? <Icon name="messages-square" size={20} />}
        <span className="gm-col" style={{ gap: 0, minWidth: 0 }}>
          <strong>{p.titre}</strong>
          {p.sousTitre ? <span className="gm-field-help">{p.sousTitre}</span> : null}
        </span>
        {p.actions ? <span className="gm-row gm-chat-head-actions">{p.actions}</span> : null}
      </header>
      {/* Fond texturé (motif de trame atténué, D-66) derrière les messages seulement ; tout texte reste sur fond plein. */}
      <div className="gm-fil-fond gm-trame-fil">
      <ol className="gm-chat-list" ref={liste} aria-label={t('messagerie.messages')}>
        {p.plusAnciens ? (
          <li className="gm-fil-plus">
            <Button size="sm" variant="ghost" icon="arrow-up" onClick={p.onPlusAnciens}>{t('messagerie.plusAnciens')}</Button>
          </li>
        ) : null}
        {p.messages.length === 0 ? <li className="gm-fil-vide"><EmptyState icon="message-square" title={t('messagerie.vide')} /></li> : null}
        {p.messages.map((m) => m.evenement ? (
          <li key={m.id} className="gm-msg-evenement">
            <span>{m.contenu}</span>
            <span className="gm-num">{m.quand}</span>
          </li>
        ) : (
          <Bulle key={m.id} m={m} enEvidence={m.id === p.enEvidence} onRepondre={p.onRepondre}>
              {m.etiquettes ? <div className="gm-row gm-msg-etiquettes">{m.etiquettes}</div> : null}
              {(m.pieces ?? []).map((pj) => (
                <div key={pj.id} className="gm-msg-att">
                  <button type="button" className="gm-link" onClick={() => p.onPiece?.(pj.id)} aria-label={t('messagerie.piece', { nom: pj.nom })}>
                    <Icon name="paperclip" size={14} /> {pj.nom}
                  </button>
                  {pj.taille ? <span className="gm-num gm-field-help"> · {pj.taille}</span> : null}
                  {p.onTransferer ? (
                    <IconButton variant="ghost" icon="forward" label={t('messagerie.transferer', { nom: pj.nom })} onClick={() => p.onTransferer?.(pj.id)} />
                  ) : null}
                </div>
              ))}
          </Bulle>
        ))}
      </ol>
      <div className="gm-fil-saisie" aria-live="polite">
        {p.annonce ? <span className="gm-msg-saisie gm-fil-annonce"><Icon name="info" size={14} />{p.annonce}</span> : null}
        {p.ecrit ? <span className="gm-msg-saisie"><Points />{p.ecrit}</span> : null}
      </div>
      </div>
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
  /**
   * Pièces envoyées par morceaux (dépôts fragmentés, D-74), avec leur progression ; « Envoyer » attend
   * qu'elles soient toutes prêtes. Le sélecteur rend alors seulement les fichiers nouvellement choisis.
   */
  envois?: EnvoiPiece[]
  onAnnulerEnvoi?: (cle: string) => void
  /**
   * Réponse en cours de saisie (D-82) : citation au-dessus du champ, retirée par la croix ou Échap ; `cle`
   * (identifiant du message cité) ramène le focus dans le champ à chaque nouveau choix.
   */
  reponse?: { cle: string; auteur: string; extrait: string; pieces?: number; onAnnuler: () => void }
  /** Membres mentionnables (D-82) : « @ » ouvre la liste, filtrée par la saisie. */
  membres?: MembreMention[]
  /** Membre choisi dans la liste : `@Prénom Nom` est inséré, l'appelant mémorise son identifiant. */
  onMention?: (m: MembreMention) => void
}

/** Personne proposée après « @ » : membre de la conversation (D-82). */
export interface MembreMention {
  id: string
  nom: string
  /** Fonction, en seconde ligne. */
  detail?: string | null
  avatar?: string | null
}

/** Comparaison sans casse ni accents (« aicha » trouve « Aïcha »). */
const plier = (s: string) => s.normalize('NFD').replace(/\p{M}/gu, '').toLowerCase()

/** Mention en cours de frappe : « @ » en début de texte ou après une espace, jusqu'au curseur. */
export function mentionEnCours(texte: string, curseur: number): { debut: number; requete: string } | null {
  const m = /(^|\s)@([^@\n]{0,40})$/.exec(texte.slice(0, curseur))
  return m ? { debut: curseur - m[2].length - 1, requete: m[2] } : null
}

/** Membres proposés pour une requête : nom contenant la saisie, huit au plus. */
export function membresProposes(membres: MembreMention[], requete: string): MembreMention[] {
  const q = plier(requete)
  return membres.filter((m) => plier(m.nom).includes(q)).slice(0, 8)
}

/** Pièce en cours d'envoi : octets partis (envoi), vérification par le service, prête, ou en échec. */
export interface EnvoiPiece {
  cle: string
  nom: string
  phase: 'envoi' | 'verification' | 'pret' | 'erreur'
  /** De 0 à 100, pendant l'envoi. */
  pourcent: number
  /** « 1,2 Go sur 4,0 Go ». */
  detail?: string
  erreur?: string
}

function LigneEnvoi({ e, onAnnuler }: { e: EnvoiPiece; onAnnuler?: (cle: string) => void }) {
  const t = useLibelle()
  const etat = e.phase === 'envoi' ? t('messagerie.envoi.enCours', { pourcent: e.pourcent })
    : e.phase === 'verification' ? t('messagerie.envoi.verification')
    : e.phase === 'pret' ? t('messagerie.envoi.pret')
    : e.erreur ?? t('messagerie.envoi.echec')
  return (
    <li className={cx('gm-envoi', `gm-envoi-${e.phase}`)}>
      <Icon name={e.phase === 'pret' ? 'check' : e.phase === 'erreur' ? 'triangle-alert' : 'paperclip'} size={14} />
      <span className="gm-envoi-corps">
        <span className="gm-envoi-ligne">
          <strong className="gm-envoi-nom">{e.nom}</strong>
          <span className="gm-envoi-etat" aria-live="polite">{etat}</span>
        </span>
        {e.phase === 'envoi' || e.phase === 'verification' ? (
          <progress className="gm-envoi-barre" max={100} value={e.phase === 'envoi' ? e.pourcent : undefined} aria-label={t('messagerie.envoi.progression', { nom: e.nom })} />
        ) : null}
        {e.detail && e.phase === 'envoi' ? <span className="gm-num gm-field-help">{e.detail}</span> : null}
      </span>
      {onAnnuler ? <IconButton variant="ghost" icon="x" label={t(e.phase === 'pret' || e.phase === 'erreur' ? 'messagerie.envoi.retirer' : 'messagerie.envoi.annuler', { nom: e.nom })} onClick={() => onAnnuler(e.cle)} /> : null}
    </li>
  )
}

/** Zone de saisie : texte sur plusieurs lignes, pièces jointes retirables, envoi à l'Entrée. */
export function MessageComposer(p: MessageComposerProps) {
  const t = useLibelle()
  const selecteur = useRef<HTMLInputElement>(null)
  const zone = useRef<HTMLTextAreaElement>(null)
  const idListe = `gm-mentions-${useId().replace(/:/g, '')}`
  const [frappe, setFrappe] = useState<{ debut: number; requete: string } | null>(null)
  const [active, setActive] = useState(0)
  const fichiers = p.fichiers ?? []
  const envois = p.envois ?? []
  const enCours = envois.some((e) => e.phase !== 'pret')
  const peutEnvoyer = !p.disabled && !p.sending && !enCours && (p.value.trim().length > 0 || fichiers.length > 0 || envois.length > 0)
  const proposes = frappe && p.membres?.length ? membresProposes(p.membres, frappe.requete) : []
  const ouverte = proposes.length > 0
  // Réponse choisie : le champ reprend le focus, prêt à écrire.
  const cleReponse = p.reponse?.cle
  useEffect(() => {
    if (cleReponse) zone.current?.focus()
  }, [cleReponse])
  const detecter = (texte: string, curseur: number) => {
    setFrappe(p.membres?.length ? mentionEnCours(texte, curseur) : null)
    setActive(0)
  }
  const choisir = (m: MembreMention) => {
    if (!frappe) return
    const insere = `@${m.nom} `
    const fin = frappe.debut + 1 + frappe.requete.length
    p.onChange(p.value.slice(0, frappe.debut) + insere + p.value.slice(fin))
    p.onMention?.(m)
    setFrappe(null)
    const position = frappe.debut + insere.length
    window.setTimeout(() => {
      zone.current?.focus()
      zone.current?.setSelectionRange(position, position)
    }, 0)
  }
  const touche = (e: KeyboardEvent<HTMLTextAreaElement>) => {
    if (ouverte) {
      if (e.key === 'ArrowDown' || e.key === 'ArrowUp') {
        e.preventDefault()
        const pas = e.key === 'ArrowDown' ? 1 : -1
        setActive((a) => (a + pas + proposes.length) % proposes.length)
        return
      }
      if ((e.key === 'Enter' || e.key === 'Tab') && !e.shiftKey && !e.nativeEvent.isComposing) {
        e.preventDefault()
        choisir(proposes[Math.min(active, proposes.length - 1)])
        return
      }
      if (e.key === 'Escape') {
        e.preventDefault()
        e.stopPropagation()
        setFrappe(null)
        return
      }
    }
    if (e.key === 'Escape' && p.reponse) {
      e.preventDefault()
      e.stopPropagation()
      p.reponse.onAnnuler()
      return
    }
    if (e.key === 'Enter' && !e.shiftKey && !e.nativeEvent.isComposing) {
      e.preventDefault()
      if (peutEnvoyer) p.onSend()
    }
  }
  return (
    <div className="gm-chat-input gm-saisie">
      {p.extras}
      {p.reponse ? (
        <div className="gm-saisie-reponse" role="group" aria-label={t('messagerie.reponse.titre', { auteur: p.reponse.auteur })}>
          <Icon name="reply" size={16} />
          <Citation c={{ auteur: p.reponse.auteur, extrait: p.reponse.extrait, pieces: p.reponse.pieces }} />
          <IconButton variant="ghost" size="sm" icon="x" label={t('messagerie.reponse.annuler')} onClick={p.reponse.onAnnuler} />
        </div>
      ) : null}
      {fichiers.length ? (
        <div className="gm-row" style={{ flexWrap: 'wrap', gap: 'var(--gm-space-1)' }}>
          {fichiers.map((f, i) => (
            <Tag key={`${f.name}-${i}`} onRemove={() => p.onFichiers?.(fichiers.filter((_, j) => j !== i))}>{f.name}</Tag>
          ))}
        </div>
      ) : null}
      {envois.length ? (
        <ul className="gm-envois" aria-label={t('messagerie.envoi.liste')}>
          {envois.map((e) => <LigneEnvoi key={e.cle} e={e} onAnnuler={p.onAnnulerEnvoi} />)}
        </ul>
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
        <div className="gm-saisie-zone">
          <textarea
            ref={zone}
            className="gm-input gm-saisie-texte"
            rows={2}
            aria-label={p.label || t('messagerie.message')}
            placeholder={t(p.membres?.length ? 'messagerie.aideMention' : 'messagerie.aide')}
            value={p.value}
            disabled={p.disabled}
            aria-autocomplete={p.membres?.length ? 'list' : undefined}
            aria-controls={ouverte ? idListe : undefined}
            aria-activedescendant={ouverte ? `${idListe}-${Math.min(active, proposes.length - 1)}` : undefined}
            onChange={(e) => {
              p.onChange(e.target.value)
              detecter(e.target.value, e.target.selectionStart ?? e.target.value.length)
            }}
            onClick={(e) => detecter(e.currentTarget.value, e.currentTarget.selectionStart ?? 0)}
            onBlur={() => setFrappe(null)}
            onKeyDown={touche}
          />
          {ouverte ? (
            <ul id={idListe} role="listbox" className="gm-listbox gm-mentions" aria-label={t('messagerie.mention.liste')}>
              {proposes.map((m, i) => (
                <li
                  key={m.id}
                  id={`${idListe}-${i}`}
                  role="option"
                  aria-selected={i === active}
                  className={cx('gm-option', 'gm-mention-option', i === active && 'gm-option-sel')}
                  onMouseDown={(e) => {
                    e.preventDefault()
                    choisir(m)
                  }}
                >
                  <Avatar name={m.nom} src={m.avatar} size="sm" decorative />
                  <span className="gm-mention-identite">
                    <strong>{m.nom}</strong>
                    {m.detail ? <span className="gm-field-help">{m.detail}</span> : null}
                  </span>
                </li>
              ))}
            </ul>
          ) : null}
          <span className="gm-sr-only" aria-live="polite">{ouverte ? t('messagerie.mention.nombre', { n: proposes.length }) : ''}</span>
        </div>
        <Button variant="primary" icon="send" loading={p.sending} disabled={!peutEnvoyer} onClick={p.onSend}>{t('messagerie.envoyer')}</Button>
      </div>
    </div>
  )
}

export interface AttachmentViewerProps {
  nom: string
  typeMime: string
  /** Taille lisible (« 12,4 Ko »). */
  taille?: string
  /** Contenu de la pièce ; absent pendant le chargement. */
  blob?: Blob | null
  chargement?: boolean
  erreur?: ReactNode
  onTelecharger: () => void
  onClose: () => void
  open?: boolean
  /**
   * Conversion d'un document bureautique en PDF par le service (D-83, POST /bff/{couche}/document/apercus) :
   * fournie par l'application, le système de conception ne fait aucun appel réseau. Sans elle, ces formats
   * invitent au téléchargement. Rejet : erreur portant `status`, `code` et `details.reessayerDans`.
   */
  convertir?: (blob: Blob, nom: string) => Promise<Blob>
}

export type FormatApercu = 'pdf' | 'image' | 'texte' | 'bureautique' | 'aucun'
/** Famille d'un document bureautique : traitement de texte, présentation, tableur (pictogramme du titre). */
export type FamilleBureautique = 'texte' | 'presentation' | 'tableur'

const BUREAUTIQUE: Record<string, FamilleBureautique> = {
  docx: 'texte', doc: 'texte', odt: 'texte', rtf: 'texte',
  pptx: 'presentation', ppt: 'presentation', odp: 'presentation',
  xlsx: 'tableur', xls: 'tableur', ods: 'tableur',
}
const ICONES_BUREAUTIQUE: Record<FamilleBureautique, IconName> = { texte: 'file-word', presentation: 'presentation', tableur: 'file-spreadsheet' }

/** Source acceptée par la conversion (D-83, docs/lot1/apercu-bureautique.md §1) : 20 Mo au plus. */
export const TAILLE_MAX_APERCU_BUREAUTIQUE = 20 * 1024 * 1024

const extensionDe = (nom: string) => (nom.includes('.') ? nom.slice(nom.lastIndexOf('.') + 1).toLowerCase() : '')

/** Famille bureautique d'un fichier, d'après son extension ; null hors des formats convertis. */
export function familleBureautique(nom: string): FamilleBureautique | null {
  return BUREAUTIQUE[extensionDe(nom)] ?? null
}

/**
 * Format affichable dans la plateforme : PDF, image, texte (dont CSV), document bureautique converti en PDF
 * par le service (D-83) ; les autres se téléchargent.
 */
export function formatApercu(typeMime: string, nom: string): FormatApercu {
  const t = typeMime.toLowerCase()
  const ext = extensionDe(nom)
  if (t === 'application/pdf' || ext === 'pdf') return 'pdf'
  if (/^image\/(png|jpe?g|gif|webp)$/.test(t) || ['png', 'jpg', 'jpeg', 'gif', 'webp'].includes(ext)) return 'image'
  if (BUREAUTIQUE[ext]) return 'bureautique'
  if (t.startsWith('text/') || ['txt', 'csv', 'log', 'md'].includes(ext)) return 'texte'
  return 'aucun'
}

/** Cause d'un refus de conversion (D-83 §2), lue sur le statut : la passerelle peut répondre 413 sans corps JSON. */
function cleErreurApercu(e: unknown): { cle: CleLibelle; n?: number } {
  const r = (e ?? {}) as { status?: number; details?: { reessayerDans?: unknown } }
  switch (r.status) {
    case 413: return { cle: 'messagerie.apercu.erreur.volumineux' }
    case 415: return { cle: 'messagerie.apercu.erreur.format' }
    case 422: return { cle: 'messagerie.apercu.erreur.illisible' }
    case 429: {
      const n = Number(r.details?.reessayerDans)
      return Number.isFinite(n) && n > 0 ? { cle: 'messagerie.apercu.erreur.limiteDans', n: Math.ceil(n) } : { cle: 'messagerie.apercu.erreur.limite' }
    }
    case 503: return { cle: 'messagerie.apercu.erreur.indisponible' }
    case 403: return { cle: 'messagerie.apercu.erreur.interdit' }
    default: return { cle: 'messagerie.apercu.erreur.autre' }
  }
}

type Conversion = { phase: 'preparation' } | { phase: 'pret'; pdf: Blob } | { phase: 'erreur'; cle: CleLibelle; n?: number }

/** Conversion du contenu déjà téléchargé ; abandonnée si la fenêtre change de contenu ou se ferme. */
function useConversion(actif: boolean, blob: Blob | null | undefined, nom: string, convertir?: (blob: Blob, nom: string) => Promise<Blob>): Conversion | null {
  const [etat, setEtat] = useState<Conversion | null>(null)
  useEffect(() => {
    if (!actif || !blob || !convertir) {
      setEtat(null)
      return undefined
    }
    // Contrôle avant l'envoi : rien au-delà de 20 Mo ne part vers le service.
    if (blob.size > TAILLE_MAX_APERCU_BUREAUTIQUE) {
      setEtat({ phase: 'erreur', cle: 'messagerie.apercu.erreur.volumineux' })
      return undefined
    }
    let annule = false
    setEtat({ phase: 'preparation' })
    convertir(blob, nom).then(
      (pdf) => !annule && setEtat({ phase: 'pret', pdf }),
      (e: unknown) => !annule && setEtat({ phase: 'erreur', ...cleErreurApercu(e) }),
    )
    return () => {
      annule = true
    }
  }, [actif, blob, nom, convertir])
  return etat
}

/** Adresse locale (blob:) d'un contenu, révoquée au démontage ou au changement de contenu. */
function useAdresseLocale(blob: Blob | null | undefined, type: string): string | undefined {
  const [url, setUrl] = useState<string>()
  useEffect(() => {
    if (!blob || typeof URL.createObjectURL !== 'function') return undefined
    // Type imposé : une image s'affiche même si le service l'a servie en octets bruts.
    const u = URL.createObjectURL(blob.type === type ? blob : new Blob([blob], { type }))
    setUrl(u)
    return () => URL.revokeObjectURL(u)
  }, [blob, type])
  return url
}

/**
 * Aperçu d'une pièce jointe dans une fenêtre modale (D-68) : PDF dessiné par pdf.js (D-73), image, texte,
 * document bureautique converti en PDF par le service (D-83) ; pour les autres formats, un message et le
 * téléchargement. Le contenu n'est jamais exécuté. « Télécharger » reste disponible dans tous les cas.
 */
export function AttachmentViewer(p: AttachmentViewerProps) {
  const t = useLibelle()
  const format = formatApercu(p.typeMime, p.nom)
  const famille = format === 'bureautique' ? familleBureautique(p.nom) : null
  const type = p.typeMime
  const url = useAdresseLocale(format === 'image' ? p.blob : null, type)
  const conversion = useConversion(format === 'bureautique', p.blob, p.nom, p.convertir)
  const [texte, setTexte] = useState<string>()
  useEffect(() => {
    setTexte(undefined)
    if (format === 'texte' && p.blob) void p.blob.text().then(setTexte)
  }, [format, p.blob])
  let corps: ReactNode
  if (p.erreur) corps = <InlineAlert level="crit">{p.erreur}</InlineAlert>
  else if (p.chargement || !p.blob) corps = <div aria-busy="true" aria-label={t('messagerie.apercu.chargement')}><Skeleton height={420} /></div>
  else if (format === 'pdf') corps = <ApercuPdf blob={p.blob} titre={p.nom} />
  else if (format === 'image') corps = url ? <img className="gm-apercu-image" src={url} alt={p.nom} /> : null
  else if (format === 'texte') corps = <pre className="gm-apercu-texte" tabIndex={0}>{texte}</pre>
  else if (format === 'bureautique' && conversion?.phase === 'pret') corps = <ApercuPdf blob={conversion.pdf} titre={p.nom} />
  else if (format === 'bureautique' && conversion?.phase === 'erreur') {
    corps = <InlineAlert level="warn" title={t(conversion.cle, { n: conversion.n ?? 0 })}>{t('messagerie.apercu.indisponibleTexte')}</InlineAlert>
  } else if (format === 'bureautique' && conversion) {
    corps = (
      <div className="gm-apercu-preparation" role="status" aria-busy="true">
        <span className="gm-apercu-preparation-texte">{t('messagerie.apercu.preparation')}</span>
        <Skeleton height={380} />
      </div>
    )
  } else corps = <EmptyState icon="paperclip" title={t('messagerie.apercu.indisponible')} text={t('messagerie.apercu.indisponibleTexte')} />
  return (
    <Dialog
      open={p.open}
      size="lg"
      id="gm-apercu-piece"
      className="gm-apercu"
      title={p.nom}
      icon={famille ? ICONES_BUREAUTIQUE[famille] : undefined}
      subtitle={[p.typeMime, p.taille].filter(Boolean).join(' · ')}
      onClose={p.onClose}
      footer={(
        <>
          <Button variant="ghost" onClick={p.onClose}>{t('commun.fermer')}</Button>
          <Button variant="primary" icon="download" onClick={p.onTelecharger}>{t('messagerie.apercu.telecharger')}</Button>
        </>
      )}
    >
      {corps}
    </Dialog>
  )
}
