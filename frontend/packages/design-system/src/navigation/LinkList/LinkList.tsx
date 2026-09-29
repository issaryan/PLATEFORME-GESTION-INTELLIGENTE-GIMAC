import type { ReactNode } from 'react'
import type { Status } from '../../lib/types'
import { Link } from '../../actions/Link/Link'
import { StatusBadge } from '../../affichage/StatusBadge/StatusBadge'

export interface LinkListItem {
  key: string
  href: string
  label: string
  /** Complément à droite du lien : référence, date, type… */
  meta?: ReactNode
  status?: Status
  statusLabel?: string
}

export interface LinkListProps {
  /** Nom de la liste, annoncé aux technologies d'assistance. */
  label: string
  items: LinkListItem[]
  /** Texte affiché quand la liste est vide. */
  empty?: string
}

// Liste de liens vers des fiches (« Mes dossiers », « Mes projets », raccourcis, D-63) : un lien par
// ligne, complément et statut alignés à droite ; liste sans puces, espacements des jetons.
export function LinkList(p: LinkListProps) {
  if (p.items.length === 0) return p.empty ? <p style={{ margin: 0 }}>{p.empty}</p> : null
  return (
    <ul className="gm-col" aria-label={p.label} style={{ listStyle: 'none', margin: 0, padding: 0, gap: 'var(--gm-space-2)' }}>
      {p.items.map((i) => (
        <li key={i.key} className="gm-row" style={{ justifyContent: 'space-between' }}>
          <Link href={i.href}>{i.label}</Link>
          {i.meta !== undefined || i.status ? (
            <span className="gm-row">
              {/* Texte courant : l'aide atténuée n'atteint pas 4,5:1 sur le fond de page (audit axe). */}
              {i.meta !== undefined ? <span>{i.meta}</span> : null}
              {i.status ? <StatusBadge status={i.status} label={i.statusLabel || i.status} /> : null}
            </span>
          ) : null}
        </li>
      ))}
    </ul>
  )
}
