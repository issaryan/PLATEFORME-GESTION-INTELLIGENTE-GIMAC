import { formatMoney } from '../../lib/format'
import { Button } from '../../actions/Button/Button'
import { useLibelle } from '../../i18n/LangueProvider'

export interface PaginationProps {
  total?: number
  page: number
  pageSize?: number
  onPage?: (p: number) => void
  onPageSize?: (n: number) => void
}

// Pagination côté serveur (§7.4).
export function Pagination(p: PaginationProps) {
  const t = useLibelle()
  const pages = Math.max(1, Math.ceil((p.total || 0) / (p.pageSize || 25)))
  return (
    <div className="gm-pagination">
      <span>{t('pagination.lignesParPage')}</span>
      <select className="gm-input" aria-label={t('pagination.taillePage')} value={p.pageSize} onChange={(e) => { if (p.onPageSize) p.onPageSize(Number(e.target.value)) }} style={{ width: 72 }}>
        {[25, 50, 100].map((n) => <option key={n} value={n}>{n}</option>)}
      </select>
      <span className="gm-num">{t('pagination.resume', { page: p.page, pages, total: formatMoney(p.total || 0) })}</span>
      <span className="gm-row">
        <Button size="sm" variant="ghost" disabled={p.page <= 1} onClick={() => { if (p.onPage) p.onPage(p.page - 1) }}>{t('pagination.precedente')}</Button>
        <Button size="sm" variant="ghost" disabled={p.page >= pages} onClick={() => { if (p.onPage) p.onPage(p.page + 1) }}>{t('pagination.suivante')}</Button>
      </span>
    </div>
  )
}
