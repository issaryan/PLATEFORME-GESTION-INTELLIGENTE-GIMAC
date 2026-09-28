import { Icon } from '../../icones/Icon/Icon'
import type { IconName } from '../../icones/icons'

export interface SideNavProps {
  current?: string
  groups?: { label?: string; items: { key: string; label: string; icon: IconName; href?: string }[] }[]
  version?: string
  height?: number
}

// Navigation latérale de 264 px (§6.2) ; les entrées non autorisées sont absentes du DOM.
export function SideNav(p: SideNavProps) {
  return (
    <nav className="gm-sidenav" aria-label="Navigation principale" style={{ height: p.height || 480 }}>
      {(p.groups || []).map((g, gi) => (
        <div key={gi}>
          {g.label ? <div className="gm-sidenav-group">{g.label}</div> : null}
          <ul>
            {g.items.map((it) => (
              <li key={it.key}>
                <a href={it.href || '#'} aria-current={it.key === p.current ? 'page' : undefined}>
                  <Icon name={it.icon} size={20} />
                  {it.label}
                </a>
              </li>
            ))}
          </ul>
        </div>
      ))}
      <div className="gm-sidenav-foot">
        {p.version || 'Plateforme DES v1.0'}
        <br />
        Usage interne, confidentiel
      </div>
    </nav>
  )
}
