import { useRef, useState, type ReactNode } from 'react'
import { cx } from '../../lib/cx'
import type { Status } from '../../lib/types'
import { useFocusTrap } from '../../lib/useFocusTrap'
import { IconButton } from '../../actions/IconButton/IconButton'
import { StatusBadge } from '../../affichage/StatusBadge/StatusBadge'
import { Tabs } from '../../navigation/Tabs/Tabs'
import { Overlay } from '../Overlay/Overlay'
import { useLibelle } from '../../i18n/LangueProvider'

const DRAWER_TABS = [
  { key: 'detail', cle: 'drawer.detail' },
  { key: 'chronologie', cle: 'drawer.chronologie' },
  { key: 'pieces', cle: 'drawer.pieces' },
  { key: 'audit', cle: 'drawer.audit' },
] as const

export interface DrawerProps {
  type: string
  /** Référence de l'objet, en chasse fixe. */
  reference: string
  status: Status
  statusLabel: string
  title?: string
  tabs?: { key: string; label: string }[]
  tab?: string
  /** Contenu de l'onglet courant. */
  render?: (tab: string) => ReactNode
  children?: ReactNode
  actions?: ReactNode
  onClose?: () => void
  size?: 'md' | 'lg'
  open?: boolean
  inline?: boolean
}

// Tiroir de détail de 480 px (640 en lg) ouvert à droite sans quitter la liste (§6.4).
export function Drawer(p: DrawerProps) {
  const t = useLibelle()
  const ref = useRef<HTMLElement>(null)
  useFocusTrap(ref, p.open !== false, p.onClose)
  const [tab, setTab] = useState(p.tab || 'detail')
  if (p.open === false) return null
  return (
    <Overlay inline={p.inline} align="right" onClose={p.onClose}>
      <aside ref={ref} className={cx('gm-drawer', p.size === 'lg' && 'gm-drawer-lg')} role="dialog" aria-modal="true" aria-label={p.type + ' ' + p.reference}>
        <header className="gm-drawer-head">
          <div className="gm-col" style={{ gap: 2 }}>
            <span className="gm-field-help">{p.type}</span>
            <div className="gm-row">
              <span className="gm-mono" style={{ fontSize: 16 }}>{p.reference}</span>
              <StatusBadge status={p.status} label={p.statusLabel} />
            </div>
          </div>
          <div className="gm-row">
            <IconButton variant="ghost" size="sm" icon="arrow-right" label={t('drawer.pleinePage')} />
            <IconButton variant="ghost" size="sm" icon="x" label={t('commun.fermer')} onClick={p.onClose} />
          </div>
        </header>
        {p.title ? <div className="gm-drawer-title">{p.title}</div> : null}
        <div className="gm-drawer-tabs">
          <Tabs items={p.tabs || DRAWER_TABS.map((d) => ({ key: d.key, label: t(d.cle) }))} value={tab} onChange={setTab} />
        </div>
        <div className="gm-drawer-body">{p.render ? p.render(tab) : p.children}</div>
        {p.actions ? <footer className="gm-drawer-foot">{p.actions}</footer> : null}
      </aside>
    </Overlay>
  )
}
