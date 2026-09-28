import { formatMoney } from '../../lib/format'
import { Button } from '../../actions/Button/Button'

export interface PaginationProps {
  total?: number
  page: number
  pageSize?: number
  onPage?: (p: number) => void
  onPageSize?: (n: number) => void
}

// Pagination côté serveur (§7.4).
export function Pagination(p: PaginationProps) {
  const pages = Math.max(1, Math.ceil((p.total || 0) / (p.pageSize || 25)))
  return (
    <div className="gm-pagination">
      <span>Lignes par page</span>
      <select className="gm-input" aria-label="Taille de page" value={p.pageSize} onChange={(e) => { if (p.onPageSize) p.onPageSize(Number(e.target.value)) }} style={{ width: 72 }}>
        {[25, 50, 100].map((n) => <option key={n} value={n}>{n}</option>)}
      </select>
      <span className="gm-num">{'Page ' + p.page + ' sur ' + pages + ' · ' + formatMoney(p.total || 0) + ' éléments'}</span>
      <span className="gm-row">
        <Button size="sm" variant="ghost" disabled={p.page <= 1} onClick={() => { if (p.onPage) p.onPage(p.page - 1) }}>Précédente</Button>
        <Button size="sm" variant="ghost" disabled={p.page >= pages} onClick={() => { if (p.onPage) p.onPage(p.page + 1) }}>Suivante</Button>
      </span>
    </div>
  )
}
