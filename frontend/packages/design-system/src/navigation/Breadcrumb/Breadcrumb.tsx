import { Link } from '../../actions/Link/Link'
import { useLibelle } from '../../i18n/LangueProvider'

export interface BreadcrumbProps {
  items?: { label: string; href?: string }[]
}

// Fil d'Ariane à 3 niveaux au plus, dernier élément non cliquable (§7.4).
export function Breadcrumb(p: BreadcrumbProps) {
  const t = useLibelle()
  return (
    <nav aria-label={t('breadcrumb.label')}>
      <ol className="gm-breadcrumb">
        {(p.items || []).slice(0, 3).map((it, i, arr) => (
          <li key={i}>{i === arr.length - 1 ? <span aria-current="page">{it.label}</span> : <Link href={it.href}>{it.label}</Link>}</li>
        ))}
      </ol>
    </nav>
  )
}
