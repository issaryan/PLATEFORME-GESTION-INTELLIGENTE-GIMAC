import { cx } from '../../lib/cx'
import { Icon } from '../../icones/Icon/Icon'
import type { IconName } from '../../icones/icons'
import { useLibelle } from '../../i18n/LangueProvider'

export interface MenuItem {
  key?: string
  label?: string
  icon?: IconName
  shortcut?: string
  /** Action dangereuse : en bas, pictogramme et couleur critique. */
  danger?: boolean
  disabled?: boolean
  disabledReason?: string
  separator?: boolean
  onClick?: () => void
}

export interface MenuProps {
  items: MenuItem[]
  label?: string
  className?: string
}

// Menu d'actions (« Plus », colonne d'actions, SplitButton) (§7.5).
export function Menu(p: MenuProps) {
  const t = useLibelle()
  const items = p.items || []
  return (
    <div className={cx('gm-menu', p.className)} role="menu" aria-label={p.label || t('menu.actions')}>
      {items.map((it, i) => {
        if (it.separator) return <div key={i} className="gm-menu-sep" role="separator" />
        return (
          <button
            key={it.key || i}
            type="button"
            role="menuitem"
            className={cx('gm-menu-item', it.danger && 'gm-menu-danger')}
            disabled={it.disabled}
            title={it.disabled ? it.disabledReason : undefined}
            onClick={it.onClick}
          >
            {it.icon ? <Icon name={it.icon} size={16} /> : <span className="gm-menu-spacer" />}
            {it.label}
            {it.shortcut ? <span className="gm-menu-kbd">{it.shortcut}</span> : null}
          </button>
        )
      })}
    </div>
  )
}
