import { useEffect, useId, useState } from 'react'
import { Icon } from '../../icones/Icon/Icon'
import type { IconName } from '../../icones/icons'
import { useLibelle } from '../../i18n/LangueProvider'

export interface SideNavGroup {
  /** Clé stable de la section (mémoire de l'état ouvert) ; le libellé, puis le rang, à défaut. */
  cle?: string
  label?: string
  items: { key: string; label: string; icon: IconName; href?: string; badge?: number }[]
}

export interface SideNavProps {
  current?: string
  groups?: SideNavGroup[]
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
  /**
   * Mode réduit aux pictogrammes (D-84, élargissement de la messagerie) : libellés lus par les technologies
   * d'assistance et en infobulle, groupes séparés par un filet, pastilles réduites à un point chiffré.
   */
  reduite?: boolean
  /**
   * Sections repliables dans la barre (accordéons, sans superposition) : chaque groupe titré devient un bouton
   * aria-expanded ; la section de l'écran courant s'ouvre à chaque navigation ; l'état ouvert est mémorisé sous la
   * clé `memoire` du stockage local (lecture et écriture protégées). Sans effet en mode réduit, tout déplié.
   */
  repliable?: boolean
  memoire?: string
}

function lire(cle: string | undefined): string[] {
  if (!cle) return []
  try {
    const v: unknown = JSON.parse(localStorage.getItem(cle) ?? '[]')
    return Array.isArray(v) ? v.filter((x): x is string => typeof x === 'string') : []
  } catch {
    return []
  }
}

function ecrire(cle: string | undefined, ouverts: string[]) {
  if (!cle) return
  try {
    localStorage.setItem(cle, JSON.stringify(ouverts))
  } catch {
    // Stockage refusé (navigation privée, quota) : l'état vaut pour la session de la page.
  }
}

// Navigation latérale de 264 px (DS-GIMAC-01 §6.2) : fond uni (bleu institutionnel en clair, gris
// anthracite du thème en sombre), aucun dégradé ; logo seul en blanc, sans drapeaux ni cartouche ;
// entrée active marquée par une teinte de fond et un texte blanc, sans filet vertical (D-66). Les
// entrées non autorisées sont absentes du DOM ; les entrées d'une section repliée aussi.
export function SideNav(p: SideNavProps) {
  const t = useLibelle()
  const id = useId()
  const groupes = (p.groups || []).map((g, gi) => ({ ...g, cle: g.cle ?? g.label ?? String(gi) }))
  const repliable = !!p.repliable && !p.reduite
  const active = groupes.find((g) => g.items.some((it) => it.key === p.current))?.cle
  const [ouverts, setOuverts] = useState<string[]>(() => lire(p.memoire))
  // Section de l'écran courant ouverte à chaque navigation, et mémorisée comme telle.
  useEffect(() => {
    if (!repliable || !active) return
    setOuverts((o) => {
      if (o.includes(active)) return o
      const n = [...o, active]
      ecrire(p.memoire, n)
      return n
    })
  }, [repliable, active, p.memoire])
  const basculer = (cle: string) =>
    setOuverts((o) => {
      const n = o.includes(cle) ? o.filter((x) => x !== cle) : [...o, cle]
      ecrire(p.memoire, n)
      return n
    })
  return (
    <nav className={p.reduite ? 'gm-sidenav gm-sidenav-reduite' : 'gm-sidenav'} aria-label={t('sidenav.label')} style={{ height: p.height || 480 }}>
      {p.logo ? (
        <div className="gm-sidenav-marque">
          <img src={p.logo} alt="GIMAC" />
        </div>
      ) : null}
      <div className="gm-sidenav-entrees">
        {groupes.map((g, gi) => {
          const section = repliable && !!g.label
          const ouvert = !section || ouverts.includes(g.cle)
          const liste = `${id}-section-${gi}`
          return (
            <div key={g.cle}>
              {section ? (
                <button type="button" className="gm-sidenav-group gm-sidenav-section" aria-expanded={ouvert} aria-controls={liste} onClick={() => basculer(g.cle)}>
                  <span>{g.label}</span>
                  {!ouvert && g.cle === active ? <span className="gm-sidenav-courant" role="img" title={t('sidenav.ecranCourant')} aria-label={t('sidenav.ecranCourant')} /> : null}
                  <Icon name={ouvert ? 'chevron-down' : 'chevron-right'} size={16} />
                </button>
              ) : g.label ? (
                <div className="gm-sidenav-group" aria-hidden={p.reduite || undefined}>{g.label}</div>
              ) : null}
              {ouvert ? (
                <ul id={liste}>
                  {g.items.map((it) => (
                    <li key={it.key}>
                      <a
                        href={it.href || '#'}
                        aria-current={it.key === p.current ? 'page' : undefined}
                        title={p.reduite ? it.label : undefined}
                        onClick={p.onNavigate ? (e) => { e.preventDefault(); p.onNavigate!(it.href || '#') } : undefined}
                      >
                        <Icon name={it.icon} size={20} />
                        <span className="gm-sidenav-libelle">{it.label}</span>
                        {it.badge ? <span className="gm-sidenav-badge" aria-label={t('messagerie.nonLus', { n: it.badge })}>{it.badge > 99 ? '99+' : it.badge}</span> : null}
                      </a>
                    </li>
                  ))}
                </ul>
              ) : null}
            </div>
          )
        })}
      </div>
    </nav>
  )
}
