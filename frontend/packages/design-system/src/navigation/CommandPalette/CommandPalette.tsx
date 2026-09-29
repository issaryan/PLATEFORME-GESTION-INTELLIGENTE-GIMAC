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
  ecran: 'arrow-right',
  action: 'chevron-right',
}

export interface CommandResult {
  key: string | number
  group: 'objets' | 'ecrans' | 'actions'
  kind: 'institution' | 'liaison' | 'ticket' | 'suspens' | 'piece' | 'ecran' | 'action'
  code?: string
  label: string
  status?: Status
  statusLabel?: string
  shortcut?: string
}

export interface CommandPaletteProps {
  query?: string
  onQuery?: (q: string) => void
  results: CommandResult[]
  /** Index du résultat actif dans l'ordre d'affichage. */
  active?: number
}

// Recherche globale Ctrl+K : objets, écrans, actions, résultats groupés (§7.4).
export function CommandPalette(p: CommandPaletteProps) {
  const t = useLibelle()
  const groups = ['objets', 'ecrans', 'actions'] as const
  const res = p.results || []
  const active = p.active || 0
  let n = -1
  return (
    <div className="gm-cmd" role="dialog" aria-label={t('cmd.recherche')}>
      <div className="gm-cmd-input">
        <Icon name="search" size={20} />
        <input
          type="text"
          role="combobox"
          aria-expanded="true"
          aria-controls="gm-cmd-list"
          aria-label={t('cmd.champ')}
          className="gm-cmd-field"
          value={p.query || ''}
          onChange={(e) => p.onQuery && p.onQuery(e.target.value)}
        />
        <span className="gm-menu-kbd">{t('cmd.echap')}</span>
      </div>
      <div id="gm-cmd-list" role="listbox" className="gm-cmd-list">
        {groups.map((g) => {
          const items = res.filter((r) => r.group === g)
          if (!items.length) return null
          return (
            <div key={g} role="group" aria-label={t(CMD_GROUPS[g])}>
              <div className="gm-cmd-group">{t(CMD_GROUPS[g])}</div>
              {items.map((r) => {
                n++
                const sel = n === active
                return (
                  <div key={r.key} role="option" aria-selected={sel ? 'true' : 'false'} className={cx('gm-cmd-item', sel && 'gm-cmd-sel')}>
                    <Icon name={CMD_ICON[r.kind] || 'chevron-right'} size={16} />
                    {r.code ? <span className="gm-mono">{r.code}</span> : null}
                    <span style={{ flex: 1 }}>{r.label}</span>
                    {r.status ? <StatusBadge status={r.status} label={r.statusLabel ?? ''} /> : null}
                    {r.shortcut ? <span className="gm-menu-kbd">{r.shortcut}</span> : null}
                  </div>
                )
              })}
            </div>
          )
        })}
        {res.length ? null : <div className="gm-cmd-empty">{t('cmd.aucun', { q: p.query || '' })}</div>}
      </div>
      <div className="gm-cmd-foot">
        <span><span className="gm-menu-kbd">↑↓</span> {t('cmd.parcourir')}</span>
        <span><span className="gm-menu-kbd">{t('cmd.entree')}</span> {t('cmd.ouvrir')}</span>
        <span style={{ marginLeft: 'auto' }}>Ctrl+K</span>
      </div>
    </div>
  )
}
