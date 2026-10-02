import { useEffect, useId, useRef, useState, type KeyboardEvent } from 'react'
import { cx } from '../../lib/cx'
import type { Status } from '../../lib/types'
import { Icon } from '../../icones/Icon/Icon'
import type { IconName } from '../../icones/icons'
import { StatusBadge } from '../../affichage/StatusBadge/StatusBadge'
import { useLibelle } from '../../i18n/LangueProvider'

const CMD_GROUPS = { objets: 'cmd.objets', ecrans: 'cmd.ecrans', actions: 'cmd.actions' } as const
const CMD_ICON: Record<string, IconName> = {
  institution: 'landmark',
  liaison: 'route',
  ticket: 'ticket',
  suspens: 'scale',
  piece: 'file-text',
  compte: 'user',
  conversation: 'messages-square',
  ecran: 'arrow-right',
  action: 'chevron-right',
}
const GROUPES = ['objets', 'ecrans', 'actions'] as const

export interface CommandResult {
  key: string | number
  group: 'objets' | 'ecrans' | 'actions'
  kind: 'institution' | 'liaison' | 'ticket' | 'suspens' | 'piece' | 'compte' | 'conversation' | 'ecran' | 'action'
  code?: string
  label: string
  /** Seconde ligne discrète (institution, fonction, état). */
  detail?: string
  status?: Status
  statusLabel?: string
  shortcut?: string
}

export interface CommandPaletteProps {
  query?: string
  onQuery?: (q: string) => void
  results: CommandResult[]
  /** Index du résultat actif dans l'ordre d'affichage ; à défaut, tenu par la palette (flèches). */
  active?: number
  /** Résultat choisi (Entrée ou clic) ; rend la palette interactive. */
  onSelect?: (r: CommandResult) => void
  /** Échap. */
  onClose?: () => void
  /** Recherche en cours dans les sources : indication sous la liste. */
  chargement?: boolean
}

// Recherche globale Ctrl+K : objets, écrans, actions, résultats groupés (§7.4). Interactive dès que
// onSelect est fourni : flèches pour parcourir, Entrée ou clic pour ouvrir, Échap pour fermer ; champ
// en combobox, option active annoncée (aria-activedescendant).
export function CommandPalette(p: CommandPaletteProps) {
  const t = useLibelle()
  const id = useId()
  const champ = useRef<HTMLInputElement>(null)
  const res = p.results || []
  // Ordre d'affichage : groupe par groupe.
  const ordonnes = GROUPES.flatMap((g) => res.filter((r) => r.group === g))
  const [interne, setInterne] = useState(0)
  const active = Math.min(p.active ?? interne, Math.max(ordonnes.length - 1, 0))
  useEffect(() => setInterne(0), [p.query])
  useEffect(() => {
    if (p.onSelect) champ.current?.focus()
  }, [p.onSelect])
  useEffect(() => {
    document.getElementById(`${id}-opt-${active}`)?.scrollIntoView?.({ block: 'nearest' })
  }, [id, active])
  const touche = (e: KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'ArrowDown' || e.key === 'ArrowUp') {
      e.preventDefault()
      if (!ordonnes.length) return
      const pas = e.key === 'ArrowDown' ? 1 : -1
      setInterne((active + pas + ordonnes.length) % ordonnes.length)
    } else if (e.key === 'Enter') {
      const r = ordonnes[active]
      if (r && p.onSelect) {
        e.preventDefault()
        p.onSelect(r)
      }
    } else if (e.key === 'Escape' && p.onClose) {
      e.preventDefault()
      p.onClose()
    }
  }
  let n = -1
  return (
    <div className="gm-cmd" role="dialog" aria-label={t('cmd.recherche')}>
      <div className="gm-cmd-input">
        <Icon name="search" size={20} />
        <input
          ref={champ}
          type="text"
          role="combobox"
          aria-expanded="true"
          aria-controls={`${id}-liste`}
          aria-autocomplete="list"
          aria-activedescendant={ordonnes.length ? `${id}-opt-${active}` : undefined}
          aria-label={t('cmd.champ')}
          className="gm-cmd-field"
          value={p.query || ''}
          onChange={(e) => p.onQuery && p.onQuery(e.target.value)}
          onKeyDown={touche}
        />
        <span className="gm-menu-kbd">{t('cmd.echap')}</span>
      </div>
      <div id={`${id}-liste`} role="listbox" aria-label={t('cmd.recherche')} className="gm-cmd-list" aria-busy={p.chargement || undefined}>
        {GROUPES.map((g) => {
          const items = res.filter((r) => r.group === g)
          if (!items.length) return null
          return (
            <div key={g} role="group" aria-label={t(CMD_GROUPS[g])}>
              <div className="gm-cmd-group" aria-hidden="true">{t(CMD_GROUPS[g])}</div>
              {items.map((r) => {
                n++
                const rang = n
                const sel = rang === active
                return (
                  <div
                    key={r.key}
                    id={`${id}-opt-${rang}`}
                    role="option"
                    aria-selected={sel ? 'true' : 'false'}
                    className={cx('gm-cmd-item', sel && 'gm-cmd-sel')}
                    onMouseEnter={() => p.onSelect && setInterne(rang)}
                    onMouseDown={(e) => {
                      if (!p.onSelect) return
                      e.preventDefault()
                      p.onSelect(r)
                    }}
                  >
                    <Icon name={CMD_ICON[r.kind] || 'chevron-right'} size={16} />
                    {r.code ? <span className="gm-mono">{r.code}</span> : null}
                    <span className="gm-cmd-libelle">
                      <span>{r.label}</span>
                      {r.detail ? <span className="gm-cmd-detail">{r.detail}</span> : null}
                    </span>
                    {r.status ? <StatusBadge status={r.status} label={r.statusLabel ?? ''} /> : null}
                    {r.shortcut ? <span className="gm-menu-kbd">{r.shortcut}</span> : null}
                  </div>
                )
              })}
            </div>
          )
        })}
        {res.length ? null : p.chargement ? null : <div className="gm-cmd-empty">{t('cmd.aucun', { q: p.query || '' })}</div>}
        {p.chargement ? <div className="gm-cmd-empty" role="status">{t('cmd.chargement')}</div> : null}
      </div>
      <div className="gm-cmd-foot">
        <span><span className="gm-menu-kbd">↑↓</span> {t('cmd.parcourir')}</span>
        <span><span className="gm-menu-kbd">{t('cmd.entree')}</span> {t('cmd.ouvrir')}</span>
        <span style={{ marginLeft: 'auto' }}>Ctrl+K</span>
      </div>
    </div>
  )
}
