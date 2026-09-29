import { Icon } from '../../icones/Icon/Icon'
import type { IconName } from '../../icones/icons'
import { useLibelle } from '../../i18n/LangueProvider'

export interface SideNavProps {
  current?: string
  groups?: { label?: string; items: { key: string; label: string; icon: IconName; href?: string }[] }[]
  version?: string
  height?: number
  /** Navigation de l'application (routeur) : appelée au clic d'une entrée, à la place du lien natif. */
  onNavigate?: (href: string) => void
}

// Navigation latérale de 264 px (§6.2) ; les entrées non autorisées sont absentes du DOM.
export function SideNav(p: SideNavProps) {
  const t = useLibelle()
  return (
    <nav className="gm-sidenav" aria-label={t('sidenav.label')} style={{ height: p.height || 480 }}>
      {(p.groups || []).map((g, gi) => (
        <div key={gi}>
          {g.label ? <div className="gm-sidenav-group">{g.label}</div> : null}
          <ul>
            {g.items.map((it) => (
              <li key={it.key}>
                <a
                  href={it.href || '#'}
                  aria-current={it.key === p.current ? 'page' : undefined}
                  onClick={p.onNavigate ? (e) => { e.preventDefault(); p.onNavigate!(it.href || '#') } : undefined}
                >
                  <Icon name={it.icon} size={20} />
                  {it.label}
                </a>
              </li>
            ))}
          </ul>
        </div>
      ))}
      <div className="gm-sidenav-foot">
        {p.version || t('sidenav.version')}
        <br />
        {t('sidenav.classification')}
      </div>
    </nav>
  )
}
