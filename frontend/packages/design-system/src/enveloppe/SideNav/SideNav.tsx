import { Icon } from '../../icones/Icon/Icon'
import type { IconName } from '../../icones/icons'
import { useLibelle } from '../../i18n/LangueProvider'

export interface SideNavProps {
  current?: string
  groups?: { label?: string; items: { key: string; label: string; icon: IconName; href?: string; badge?: number }[] }[]
  /** Logo en couleur du groupement, avec la bande des États membres (même visuel que la page de connexion). */
  logo?: string
  /** Couche nommée sous le logo (application du département, portail, console). */
  couche?: 'des' | 'portail' | 'console'
  /** Conservé pour compatibilité : le bas de la navigation ne porte plus de texte. */
  version?: string
  height?: number
  /** Navigation de l'application (routeur) : appelée au clic d'une entrée, à la place du lien natif. */
  onNavigate?: (href: string) => void
}

// Navigation latérale de 264 px (§6.2), à l'identité de la page de connexion : dégradé du bleu
// institutionnel, cartouche du logo avec la bande des États membres, entrée courante marquée du vert
// de l'emblème. Les entrées non autorisées sont absentes du DOM ; aucun texte d'ornement en bas.
export function SideNav(p: SideNavProps) {
  const t = useLibelle()
  return (
    <nav className="gm-sidenav" aria-label={t('sidenav.label')} style={{ height: p.height || 480 }}>
      {p.logo ? (
        <div className="gm-sidenav-marque">
          <img src={p.logo} alt="GIMAC" />
          {p.couche ? <span>{t(`topbar.couche.${p.couche}` as const)}</span> : null}
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
      {/* Bande aux couleurs des six États membres de la CEMAC, reprise du logo : ornement sans texte. */}
      <div className="gm-sidenav-bande" aria-hidden="true" />
    </nav>
  )
}
