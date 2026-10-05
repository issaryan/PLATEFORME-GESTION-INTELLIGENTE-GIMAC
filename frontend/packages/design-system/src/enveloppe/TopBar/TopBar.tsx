import { useCallback, useEffect, useRef, useState, type ReactNode, type RefObject } from 'react'
import { cx } from '../../lib/cx'
import { IconButton } from '../../actions/IconButton/IconButton'
import { Breadcrumb } from '../../navigation/Breadcrumb/Breadcrumb'
import { Avatar } from '../../affichage/Avatar/Avatar'
import { Icon } from '../../icones/Icon/Icon'
import { Menu, type MenuItem } from '../../conteneurs/Menu/Menu'
import { useLibelle } from '../../i18n/LangueProvider'

export type Layer = 'des' | 'portail' | 'console'


export interface TopBarProps {
  layer?: Layer
  /** Adresse du logo monochrome (28 px) ; à défaut, le nom GIMAC en titrage. */
  logo?: string
  /** Masque le logo et le nom de la couche quand la navigation latérale les porte déjà. */
  sansMarque?: boolean
  breadcrumb?: { label: string; href?: string }[]
  user?: string
  /** Photo de profil de l'utilisateur (adresse versionnée) ; initiales à défaut. */
  avatar?: string | null
  /** Seconde ligne du menu utilisateur : fonction ou profil. */
  userDetail?: string
  /** Entrées du menu utilisateur (Mon compte, Se déconnecter) ; sans entrée, l'avatar seul. */
  userMenu?: MenuItem[]
  live?: boolean
  institution?: string
  /** Conservé pour compatibilité : aucun bandeau d'environnement n'est plus affiché. */
  env?: 'prod' | 'recette' | 'integration'
  clock?: string
  /** Appelé à l'ouverture et à la fermeture du menu des préférences (langue, thème). */
  onLangue?: () => void
  /** Contenu du menu surgissant ancré au bouton du globe (langue FR, EN, ES, thème). */
  preferences?: ReactNode
  /** Replie ou déplie la navigation latérale. */
  onMenu?: () => void
  /** Ouvre la recherche globale (Ctrl+K) ; sans elle, aucun champ de recherche n'est affiché. */
  onRecherche?: () => void
  /**
   * Cloche des notifications : pastille du nombre de non-lues (99+ au-delà) et panneau surgissant ancré,
   * reçu avec la fonction qui le ferme (ouverture d'une notification). Sans elle, la cloche reste inerte.
   */
  notifications?: { nonLues: number; panneau: (fermer: () => void) => ReactNode }
}

/** Pastille d'un compteur : « 99+ » au-delà de 99. */
export const pastilleCompteur = (n: number) => (n > 99 ? '99+' : String(n))

/** Menu surgissant ancré : Échap ou un clic au dehors le ferment et rendent le focus au bouton. */
function useSurgissant(ancre: RefObject<HTMLSpanElement | null>, panneau: RefObject<HTMLDivElement | null>, ouvert: boolean, fermer: () => void) {
  useEffect(() => {
    if (!ouvert) return
    const premier = panneau.current?.querySelector<HTMLElement>('select, button, input, [tabindex]:not([tabindex="-1"])')
    premier?.focus()
    const dehors = (e: MouseEvent) => {
      if (ancre.current && !ancre.current.contains(e.target as Node)) fermer()
    }
    const touche = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        fermer()
        ancre.current?.querySelector<HTMLElement>('button')?.focus()
      }
    }
    document.addEventListener('mousedown', dehors)
    document.addEventListener('keydown', touche)
    return () => {
      document.removeEventListener('mousedown', dehors)
      document.removeEventListener('keydown', touche)
    }
  }, [ouvert, ancre, panneau, fermer])
}

// Barre supérieure de 56 px commune aux trois couches (§6.1). Les préférences (langue, thème) s'ouvrent
// dans un menu surgissant ancré au bouton du globe : la page n'est jamais déformée ; Échap ou un clic au
// dehors le ferment et rendent le focus au bouton. Le menu utilisateur (avatar avec photo ou initiales) s'ouvre
// de même : nom, fonction, puis les entrées fournies (Mon compte, Se déconnecter). Aucun bandeau d'environnement.
export function TopBar(p: TopBarProps) {
  const t = useLibelle()
  const layer = p.layer || 'des'
  const [ouvert, setOuvert] = useState(false)
  const [menu, setMenu] = useState(false)
  const ancre = useRef<HTMLSpanElement>(null)
  const panneau = useRef<HTMLDivElement>(null)
  const ancreMenu = useRef<HTMLSpanElement>(null)
  const panneauMenu = useRef<HTMLDivElement>(null)
  const fermerPrefs = useCallback(() => setOuvert(false), [])
  const fermerMenu = useCallback(() => setMenu(false), [])
  const [cloche, setCloche] = useState(false)
  const ancreCloche = useRef<HTMLSpanElement>(null)
  const panneauCloche = useRef<HTMLDivElement>(null)
  const fermerCloche = useCallback(() => setCloche(false), [])
  useSurgissant(ancre, panneau, ouvert, fermerPrefs)
  useSurgissant(ancreMenu, panneauMenu, menu, fermerMenu)
  useSurgissant(ancreCloche, panneauCloche, cloche, fermerCloche)
  const basculer = (v: boolean) => {
    setOuvert(v)
    if (v) {
      setMenu(false)
      setCloche(false)
    }
    if (p.onLangue) p.onLangue()
  }
  const nonLues = p.notifications?.nonLues ?? 0
  const nom = p.user || t('topbar.utilisateur')
  const entrees = (p.userMenu ?? []).map((it) => ({
    ...it,
    onClick: () => {
      setMenu(false)
      it.onClick?.()
    },
  }))
  return (
    <div>
      <header className="gm-topbar">
        {p.sansMarque ? null : p.logo ? <img src={p.logo} alt="GIMAC" height={28} /> : <span style={{ fontFamily: 'var(--font-heading)', fontWeight: 700, color: 'var(--gm-color-action)', fontSize: 18 }}>GIMAC</span>}
        {layer === 'portail' && p.institution ? <span className="gm-topbar-institution" style={{ fontWeight: 600 }}>{p.institution}</span> : null}
        {p.onMenu ? <IconButton variant="ghost" icon="menu" label={t('topbar.replier')} onClick={p.onMenu} /> : null}
        {p.breadcrumb ? <Breadcrumb items={p.breadcrumb} /> : null}
        <span style={{ flex: 1 }} />
        {p.onRecherche ? (
          <button type="button" className="gm-topbar-recherche" aria-label={t('topbar.recherche')} aria-haspopup="dialog" aria-keyshortcuts="Control+K" onClick={p.onRecherche}>
            <Icon name="search" size={16} />
            <span className="gm-topbar-recherche-texte">{t('topbar.rechercher')}</span>
            <span className="gm-menu-kbd">Ctrl K</span>
          </button>
        ) : null}
        <span className="gm-topbar-clock">{p.clock || '14:05:32'}</span>
        <span className="gm-row gm-topbar-live" style={{ gap: 'var(--gm-space-1)', fontSize: 12, color: 'var(--gm-color-text-secondary)' }}>
          <span className={cx('gm-dot', p.live === false ? 'gm-dot-off' : 'gm-dot-ok')} aria-hidden="true" />
          {p.live === false ? t('topbar.horsLigne') : t('topbar.tempsReel')}
        </span>
        {p.notifications ? (
          <span ref={ancreCloche} className="gm-topbar-ancre">
            <IconButton variant="ghost" icon="bell-ring" label={nonLues > 0 ? t('notif.clocheN', { n: pastilleCompteur(nonLues) }) : t('notif.titre')} aria-haspopup="dialog" aria-expanded={cloche}
              onClick={() => { setCloche(!cloche); setOuvert(false); setMenu(false) }} />
            {nonLues > 0 ? <span className="gm-topbar-pastille gm-num" aria-hidden="true">{pastilleCompteur(nonLues)}</span> : null}
            {cloche ? (
              <div ref={panneauCloche} className="gm-popover gm-topbar-pop gm-topbar-notif" role="dialog" aria-label={t('notif.centre')}>
                {p.notifications.panneau(fermerCloche)}
              </div>
            ) : null}
          </span>
        ) : (
          <IconButton variant="ghost" icon="bell-ring" label={t('notif.titre')} />
        )}
        <span ref={ancre} className="gm-topbar-ancre">
          <IconButton variant="ghost" icon="globe" label={t('topbar.langue')} onClick={() => basculer(!ouvert)} aria-haspopup="dialog" aria-expanded={ouvert} />
          {ouvert && p.preferences ? (
            <div ref={panneau} className="gm-popover gm-topbar-pop" role="dialog" aria-label={t('topbar.preferences')}>
              {p.preferences}
            </div>
          ) : null}
        </span>
        {entrees.length ? (
          <span ref={ancreMenu} className="gm-topbar-ancre">
            <button type="button" className="gm-topbar-user" aria-label={t('topbar.menuUtilisateur', { nom })} aria-haspopup="menu" aria-expanded={menu}
              onClick={() => { setMenu(!menu); setOuvert(false); setCloche(false) }}>
              <Avatar name={nom} src={p.avatar} decorative />
              <Icon name="chevron-down" size={16} />
            </button>
            {menu ? (
              <div ref={panneauMenu} className="gm-popover gm-topbar-pop gm-topbar-menu">
                <div className="gm-topbar-menu-tete">
                  <Avatar name={nom} src={p.avatar} size="lg" decorative />
                  <span className="gm-topbar-menu-identite">
                    <strong className="gm-topbar-menu-nom">{nom}</strong>
                    {p.userDetail ? <span className="gm-topbar-menu-detail">{p.userDetail}</span> : null}
                    {layer === 'portail' && p.institution ? <span className="gm-topbar-menu-detail">{p.institution}</span> : null}
                  </span>
                </div>
                <Menu items={entrees} label={t('topbar.menuUtilisateur', { nom })} />
              </div>
            ) : null}
          </span>
        ) : (
          <Avatar name={nom} src={p.avatar} />
        )}
      </header>
    </div>
  )
}
