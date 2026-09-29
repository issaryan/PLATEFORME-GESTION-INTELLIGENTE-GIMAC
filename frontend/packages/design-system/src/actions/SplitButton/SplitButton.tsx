import { useState } from 'react'
import { cx } from '../../lib/cx'
import { Icon } from '../../icones/Icon/Icon'
import { Button, type ButtonProps } from '../Button/Button'
import { Menu, type MenuItem } from '../../conteneurs/Menu/Menu'
import { useLibelle } from '../../i18n/LangueProvider'

export interface SplitButtonProps extends Omit<ButtonProps, 'iconOnly'> {
  /** Actions secondaires du menu. */
  items: MenuItem[]
  open?: boolean
}

// Action principale et menu d'actions secondaires, ouvert par la flèche ou ArrowDown (§7.1).
export function SplitButton(p: SplitButtonProps) {
  const t = useLibelle()
  const [open, setOpen] = useState(!!p.open)
  return (
    <div className="gm-split">
      <Button variant={p.variant || 'primary'} icon={p.icon} size={p.size} onClick={p.onClick} disabled={p.disabled} disabledReason={p.disabledReason}>
        {p.children}
      </Button>
      <button
        type="button"
        className={cx('gm-btn', 'gm-btn-' + (p.variant || 'primary'), 'gm-split-arrow', p.size && 'gm-btn-' + p.size)}
        aria-label={t('splitButton.autres')}
        aria-haspopup="menu"
        aria-expanded={open ? 'true' : 'false'}
        onClick={() => setOpen(!open)}
        onKeyDown={(e) => {
          if (e.key === 'ArrowDown') {
            e.preventDefault()
            setOpen(true)
          }
        }}
      >
        <Icon name="chevron-down" size={16} />
      </button>
      {open ? <div className="gm-split-menu"><Menu items={p.items} label={t('splitButton.autres')} /></div> : null}
    </div>
  )
}
