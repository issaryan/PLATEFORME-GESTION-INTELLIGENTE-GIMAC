import { cx } from '../../lib/cx'
import { IconButton } from '../../actions/IconButton/IconButton'
import { Breadcrumb } from '../../navigation/Breadcrumb/Breadcrumb'
import { SearchInput } from '../../saisie/SearchInput/SearchInput'
import { Avatar } from '../../affichage/Avatar/Avatar'

export type Layer = 'des' | 'portail' | 'console'

const LAYER: Record<Layer, string> = { des: 'Application du département', portail: 'Portail des participants', console: "Console d'administration et d'audit" }

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
}

// Barre supérieure de 56 px commune aux trois couches (§6.1).
export function TopBar(p: TopBarProps) {
  const layer = p.layer || 'des'
  return (
    <div>
      <header className="gm-topbar">
        {p.logo ? <img src={p.logo} alt="GIMAC" height={28} /> : <span style={{ fontFamily: 'var(--font-heading)', fontWeight: 700, color: 'var(--gm-color-action)', fontSize: 18 }}>GIMAC</span>}
        <span className="gm-topbar-layer">{LAYER[layer]}</span>
        {layer === 'portail' && p.institution ? <span style={{ fontWeight: 600 }}>{p.institution}</span> : null}
        {layer !== 'portail' ? <IconButton variant="ghost" icon="menu" label="Replier la navigation" /> : null}
        {p.breadcrumb ? <Breadcrumb items={p.breadcrumb} /> : null}
        <span style={{ flex: 1 }} />
        {layer !== 'portail' ? <SearchInput label="Recherche globale (Ctrl+K)" width={260} /> : null}
        <span className="gm-topbar-clock">{p.clock || '14:05:32'}</span>
        <span className="gm-row" style={{ gap: 'var(--gm-space-1)', fontSize: 12, color: 'var(--gm-color-text-secondary)' }}>
          <span className={cx('gm-dot', p.live === false ? 'gm-dot-off' : 'gm-dot-ok')} aria-hidden="true" />
          {p.live === false ? 'Hors ligne' : 'Temps réel'}
        </span>
        <IconButton variant="ghost" icon="bell-ring" label="Notifications" />
        <IconButton variant="ghost" icon="globe" label="Langue : français" />
        <Avatar name={p.user || 'Utilisateur'} />
      </header>
      {layer === 'console' ? (
        <div className={cx('gm-envband', 'gm-envband-' + (p.env || 'prod'))}>{p.env === 'recette' ? 'RECETTE' : p.env === 'integration' ? 'INTÉGRATION' : 'PRODUCTION'}</div>
      ) : null}
    </div>
  )
}
