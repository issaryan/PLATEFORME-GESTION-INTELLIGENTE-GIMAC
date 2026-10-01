import type { ReactNode } from 'react'
import { cx } from '../../lib/cx'
import { Link } from '../../actions/Link/Link'

export interface PageHeaderUser {
  /** Nom de la personne connectée. */
  name: string
  /** Profils détenus, affichés en texte simple après le nom. */
  roles?: string[]
}

export interface PageHeaderProps {
  /** Titre de la page (seul h1 de l'écran). */
  title: ReactNode
  /** Lien de retour vers l'écran parent, au-dessus du titre. */
  back?: { href: string; label: string }
  /** Rubrique de rattachement, en texte secondaire au-dessus du titre. */
  overline?: string
  /** Une ligne de description de l'écran, en texte secondaire. */
  subtitle?: ReactNode
  /** Personne connectée et ses profils, en texte simple à droite. */
  user?: PageHeaderUser
  /** Ligne complémentaire (statut, références) sous le titre. */
  meta?: ReactNode
  /** Actions de page : une principale au plus, le reste en secondaires. */
  actions?: ReactNode
  className?: string
  id?: string
}

// En-tête d'écran (gabarits G1 à G3, D-63) : à plat, filet inférieur de 1 px. À gauche le titre et
// une ligne de description ; à droite la personne connectée et ses profils en texte simple, puis les
// actions. Aucun texte d'en-tête n'est posé à nu dans une page.
export function PageHeader(p: PageHeaderProps) {
  return (
    <header className={cx('gm-pagehead', p.className)}>
      <div className="gm-pagehead-texte">
        {p.back ? <span className="gm-pagehead-retour"><Link href={p.back.href}>{`← ${p.back.label}`}</Link></span> : null}
        {p.overline ? <p className="gm-pagehead-sur">{p.overline}</p> : null}
        <h1 className="gm-heading-display" id={p.id}>{p.title}</h1>
        {p.subtitle ? <p className="gm-pagehead-sous">{p.subtitle}</p> : null}
        {p.meta ? <div className="gm-pagehead-meta">{p.meta}</div> : null}
      </div>
      {p.user || p.actions ? (
        <div className="gm-pagehead-droite">
          {p.user ? (
            <p className="gm-pagehead-user">
              <span className="gm-pagehead-nom">{p.user.name}</span>
              {p.user.roles && p.user.roles.length ? ` · ${p.user.roles.join(', ')}` : ''}
            </p>
          ) : null}
          {p.actions ? <div className="gm-pagehead-actions">{p.actions}</div> : null}
        </div>
      ) : null}
    </header>
  )
}
