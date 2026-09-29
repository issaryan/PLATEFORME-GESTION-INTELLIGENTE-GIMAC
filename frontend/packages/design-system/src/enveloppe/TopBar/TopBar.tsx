import { cx } from '../../lib/cx'
import { IconButton } from '../../actions/IconButton/IconButton'
import { Breadcrumb } from '../../navigation/Breadcrumb/Breadcrumb'
import { SearchInput } from '../../saisie/SearchInput/SearchInput'
import { Avatar } from '../../affichage/Avatar/Avatar'
import { useLibelle } from '../../i18n/LangueProvider'

export type Layer = 'des' | 'portail' | 'console'

const LAYER = { des: 'topbar.couche.des', portail: 'topbar.couche.portail', console: 'topbar.couche.console' } as const

export interface TopBarProps {
  layer?: Layer
  /** Adresse du logo monochrome (28 px) ; à défaut, le nom GIMAC en titrage. */
  logo?: string
  breadcrumb?: { label: string; href?: string }[]
  user?: string
  live?: boolean
  institution?: string
  env?: 'prod' | 'recette' | 'integration'
  clock?: string
  /** Ouvre le sélecteur de langue (FR, EN, ES). */
  onLangue?: () => void
  /** Replie ou déplie la navigation latérale. */
  onMenu?: () => void
}

// Barre supérieure de 56 px commune aux trois couches (§6.1).
export function TopBar(p: TopBarProps) {
  const t = useLibelle()
  const layer = p.layer || 'des'
  return (
    <div>
      <header className="gm-topbar">
        {p.logo ? <img src={p.logo} alt="GIMAC" height={28} /> : <span style={{ fontFamily: 'var(--font-heading)', fontWeight: 700, color: 'var(--gm-color-action)', fontSize: 18 }}>GIMAC</span>}
        <span className="gm-topbar-layer">{t(LAYER[layer])}</span>
        {layer === 'portail' && p.institution ? <span className="gm-topbar-institution" style={{ fontWeight: 600 }}>{p.institution}</span> : null}
        {layer !== 'portail' || p.onMenu ? <IconButton variant="ghost" icon="menu" label={t('topbar.replier')} onClick={p.onMenu} /> : null}
        {p.breadcrumb ? <Breadcrumb items={p.breadcrumb} /> : null}
        <span style={{ flex: 1 }} />
        {layer !== 'portail' ? <SearchInput label={t('topbar.recherche')} width={260} /> : null}
        <span className="gm-topbar-clock">{p.clock || '14:05:32'}</span>
        <span className="gm-row gm-topbar-live" style={{ gap: 'var(--gm-space-1)', fontSize: 12, color: 'var(--gm-color-text-secondary)' }}>
          <span className={cx('gm-dot', p.live === false ? 'gm-dot-off' : 'gm-dot-ok')} aria-hidden="true" />
          {p.live === false ? t('topbar.horsLigne') : t('topbar.tempsReel')}
        </span>
        <IconButton variant="ghost" icon="bell-ring" label={t('notif.titre')} />
        <IconButton variant="ghost" icon="globe" label={t('topbar.langue')} onClick={p.onLangue} />
        <Avatar name={p.user || t('topbar.utilisateur')} />
      </header>
      {layer === 'console' ? (
        <div className={cx('gm-envband', 'gm-envband-' + (p.env || 'prod'))}>{p.env === 'recette' ? t('topbar.env.recette') : p.env === 'integration' ? t('topbar.env.integration') : t('topbar.env.production')}</div>
      ) : null}
    </div>
  )
}
