import { Icon } from '../../icones/Icon/Icon'
import type { IconName } from '../../icones/icons'
import { useLibelle } from '../../i18n/LangueProvider'

export interface SideNavProps {
  current?: string
  groups?: { label?: string; items: { key: string; label: string; icon: IconName; href?: string; badge?: number }[] }[]
  /** Logo seul, version blanche, sans drapeaux ni cartouche (D-66). */
  logo?: string
  /** Conservé pour compatibilité : le nom de la couche n'est plus affiché sous le logo (information sans utilité pour l'utilisateur). */
  couche?: 'des' | 'portail' | 'console'
  /** Conservé pour compatibilité : le bas de la navigation ne porte plus de texte. */
  version?: string
  /** Hauteur de la navigation : nombre de pixels, ou valeur CSS (« 100% » dans l'enveloppe applicative). */
  height?: number | string
  /** Navigation de l'application (routeur) : appelée au clic d'une entrée, à la place du lien natif. */
  onNavigate?: (href: string) => void
}

// Navigation latérale de 264 px (DS-GIMAC-01 §6.2) : fond uni (bleu institutionnel en clair, gris
// anthracite du thème en sombre), aucun dégradé ; logo seul en blanc, sans drapeaux ni cartouche ;
// entrée active marquée par une teinte de fond et un texte blanc, sans filet vertical (D-66). Les
// entrées non autorisées sont absentes du DOM.
export function SideNav(p: SideNavProps) {
  const t = useLibelle()
  return (
    <nav className="gm-sidenav" aria-label={t('sidenav.label')} style={{ height: p.height || 480 }}>
      {p.logo ? (
        <div className="gm-sidenav-marque">
          <img src={p.logo} alt="GIMAC" />
        </div>
      ) : null}
      <div className="gm-sidenav-entrees">
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
                    {it.badge ? <span className="gm-sidenav-badge" aria-label={t('messagerie.nonLus', { n: it.badge })}>{it.badge > 99 ? '99+' : it.badge}</span> : null}
                  </a>
                </li>
              ))}
            </ul>
          </div>
        ))}
      </div>
    </nav>
  )
}
