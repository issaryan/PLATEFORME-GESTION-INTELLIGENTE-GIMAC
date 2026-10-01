import { useEffect, useRef, useState, type ReactNode } from 'react'
import { cx } from '../../lib/cx'
import { IconButton } from '../../actions/IconButton/IconButton'
import { Breadcrumb } from '../../navigation/Breadcrumb/Breadcrumb'
import { SearchInput } from '../../saisie/SearchInput/SearchInput'
import { Avatar } from '../../affichage/Avatar/Avatar'
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
}

// Barre supérieure de 56 px commune aux trois couches (§6.1). Les préférences (langue, thème) s'ouvrent
// dans un menu surgissant ancré au bouton du globe : la page n'est jamais déformée ; Échap ou un clic au
// dehors le ferment et rendent le focus au bouton. Aucun bandeau d'environnement.
export function TopBar(p: TopBarProps) {
  const t = useLibelle()
  const layer = p.layer || 'des'
  const [ouvert, setOuvert] = useState(false)
  const ancre = useRef<HTMLSpanElement>(null)
  const panneau = useRef<HTMLDivElement>(null)
  const basculer = (v: boolean) => {
    setOuvert(v)
    if (p.onLangue) p.onLangue()
  }
  useEffect(() => {
    if (!ouvert) return
    const premier = panneau.current?.querySelector<HTMLElement>('select, button, input, [tabindex]:not([tabindex="-1"])')
    premier?.focus()
    const dehors = (e: MouseEvent) => {
      if (ancre.current && !ancre.current.contains(e.target as Node)) setOuvert(false)
    }
    const touche = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        setOuvert(false)
        ancre.current?.querySelector<HTMLElement>('button')?.focus()
      }
    }
    document.addEventListener('mousedown', dehors)
    document.addEventListener('keydown', touche)
    return () => {
      document.removeEventListener('mousedown', dehors)
      document.removeEventListener('keydown', touche)
    }
  }, [ouvert])
  return (
    <div>
      <header className="gm-topbar">
        {p.sansMarque ? null : p.logo ? <img src={p.logo} alt="GIMAC" height={28} /> : <span style={{ fontFamily: 'var(--font-heading)', fontWeight: 700, color: 'var(--gm-color-action)', fontSize: 18 }}>GIMAC</span>}
        {layer === 'portail' && p.institution ? <span className="gm-topbar-institution" style={{ fontWeight: 600 }}>{p.institution}</span> : null}
        {p.onMenu ? <IconButton variant="ghost" icon="menu" label={t('topbar.replier')} onClick={p.onMenu} /> : null}
        {p.breadcrumb ? <Breadcrumb items={p.breadcrumb} /> : null}
        <span style={{ flex: 1 }} />
        {layer !== 'portail' ? <SearchInput label={t('topbar.recherche')} width={260} /> : null}
        <span className="gm-topbar-clock">{p.clock || '14:05:32'}</span>
        <span className="gm-row gm-topbar-live" style={{ gap: 'var(--gm-space-1)', fontSize: 12, color: 'var(--gm-color-text-secondary)' }}>
          <span className={cx('gm-dot', p.live === false ? 'gm-dot-off' : 'gm-dot-ok')} aria-hidden="true" />
          {p.live === false ? t('topbar.horsLigne') : t('topbar.tempsReel')}
        </span>
        <IconButton variant="ghost" icon="bell-ring" label={t('notif.titre')} />
        <span ref={ancre} className="gm-topbar-ancre">
          <IconButton variant="ghost" icon="globe" label={t('topbar.langue')} onClick={() => basculer(!ouvert)} aria-haspopup="dialog" aria-expanded={ouvert} />
          {ouvert && p.preferences ? (
            <div ref={panneau} className="gm-popover gm-topbar-pop" role="dialog" aria-label={t('topbar.preferences')}>
              {p.preferences}
            </div>
          ) : null}
        </span>
        <Avatar name={p.user || t('topbar.utilisateur')} />
      </header>
    </div>
  )
}
