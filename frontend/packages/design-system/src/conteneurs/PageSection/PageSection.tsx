import { useId, type ReactNode } from 'react'
import { cx } from '../../lib/cx'

export interface PageSectionProps {
  title: string
  /** Description de la section, en texte secondaire sous le titre. */
  description?: ReactNode
  /** Actions ou liens de la section (« Voir tout »). */
  actions?: ReactNode
  /** Niveau du titre (2 par défaut). */
  level?: 2 | 3
  className?: string
  children?: ReactNode
}

// Section titrée d'un tableau de bord ou d'une fiche (D-63) : région nommée par son titre, titre et
// actions sur une ligne, contenu en colonne ; espacements des jetons.
export function PageSection(p: PageSectionProps) {
  const id = useId()
  const Titre = p.level === 3 ? 'h3' : 'h2'
  return (
    <section className={cx('gm-col', p.className)} aria-labelledby={id}>
      <div className="gm-row" style={{ justifyContent: 'space-between' }}>
        <Titre id={id} className="gm-type-h3" style={{ margin: 0 }}>{p.title}</Titre>
        {p.actions ? <div className="gm-row">{p.actions}</div> : null}
      </div>
      {p.description ? <p className="gm-section-desc">{p.description}</p> : null}
      {p.children}
    </section>
  )
}
