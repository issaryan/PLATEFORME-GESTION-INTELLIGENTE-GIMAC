import { useState, type ReactNode } from 'react'
import type { Status } from '../../lib/types'
import { Link } from '../../actions/Link/Link'
import { StatusBadge } from '../../affichage/StatusBadge/StatusBadge'
import { Pagination } from '../Pagination/Pagination'

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
  /** Lignes par page (5 par défaut) ; la pagination est toujours visible, comme celle des tableaux. */
  pageSize?: number
  /** Pagination côté serveur : page courante, total des éléments et changement de page. */
  page?: number
  total?: number
  onPage?: (page: number) => void
  onPageSize?: (taille: number) => void
  /** false : liste courte de raccourcis, sans pagination. */
  pagination?: boolean
}

// Liste de liens vers des fiches (« Mes dossiers », « Mes projets », raccourcis, D-63) : une ligne par
// fiche, complément et statut alignés à droite, séparateurs légers ; pagination visible sous la liste
// (côté client par défaut, côté serveur si total et onPage sont fournis).
export function LinkList(p: LinkListProps) {
  const [pageLocale, setPageLocale] = useState(1)
  const [tailleLocale, setTailleLocale] = useState(p.pageSize ?? 5)
  const serveur = p.total !== undefined && p.onPage !== undefined
  const taille = p.pageSize !== undefined && serveur ? p.pageSize : tailleLocale
  const total = serveur ? p.total! : p.items.length
  const pages = Math.max(1, Math.ceil(total / taille))
  const page = Math.min(serveur ? p.page ?? 1 : pageLocale, pages)
  const visibles = serveur || p.pagination === false ? p.items : p.items.slice((page - 1) * taille, page * taille)
  if (p.items.length === 0 && total === 0) return p.empty ? <p className="gm-section-desc">{p.empty}</p> : null
  return (
    <div className="gm-linklist">
      <ul aria-label={p.label}>
        {visibles.map((i) => (
          <li key={i.key}>
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
      {p.pagination === false ? null : (
        <Pagination
          page={page}
          total={total}
          pageSize={taille}
          onPage={serveur ? p.onPage : setPageLocale}
          onPageSize={serveur ? p.onPageSize : (n) => { setTailleLocale(n); setPageLocale(1) }}
        />
      )}
    </div>
  )
}
