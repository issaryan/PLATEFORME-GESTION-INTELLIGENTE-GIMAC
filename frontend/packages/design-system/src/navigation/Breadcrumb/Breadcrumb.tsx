import { Link } from '../../actions/Link/Link'

export interface BreadcrumbProps {
  items?: { label: string; href?: string }[]
}

// Fil d'Ariane à 3 niveaux au plus, dernier élément non cliquable (§7.4).
export function Breadcrumb(p: BreadcrumbProps) {
  return (
    <nav aria-label="Fil d'Ariane">
      <ol className="gm-breadcrumb">
        {(p.items || []).slice(0, 3).map((it, i, arr) => (
          <li key={i}>{i === arr.length - 1 ? <span aria-current="page">{it.label}</span> : <Link href={it.href}>{it.label}</Link>}</li>
        ))}
      </ol>
    </nav>
  )
}
