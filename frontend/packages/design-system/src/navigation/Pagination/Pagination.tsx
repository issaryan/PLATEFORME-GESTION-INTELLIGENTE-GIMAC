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
  const taille = p.pageSize || 25
  const pages = Math.max(1, Math.ceil((p.total || 0) / taille))
  // La taille affichée est toujours celle appliquée ; le choix n'est offert que si l'écran sait en changer.
  const tailles = [...new Set([10, 25, 50, 100, taille])].sort((x, y) => x - y)
  return (
    <div className="gm-pagination">
      {p.onPageSize ? (
        <>
          <span>{t('pagination.lignesParPage')}</span>
          <select className="gm-input" aria-label={t('pagination.taillePage')} value={taille} onChange={(e) => p.onPageSize!(Number(e.target.value))} style={{ width: 72 }}>
            {tailles.map((n) => <option key={n} value={n}>{n}</option>)}
          </select>
        </>
      ) : null}
      <span className="gm-num">{p.total === 1 ? t('pagination.resumeUn', { page: p.page, pages }) : t('pagination.resume', { page: p.page, pages, total: formatMoney(p.total || 0) })}</span>
      <span className="gm-row">
        <Button size="sm" variant="ghost" disabled={p.page <= 1} onClick={() => { if (p.onPage) p.onPage(p.page - 1) }}>{t('pagination.precedente')}</Button>
        <Button size="sm" variant="ghost" disabled={p.page >= pages} onClick={() => { if (p.onPage) p.onPage(p.page + 1) }}>{t('pagination.suivante')}</Button>
      </span>
    </div>
  )
}
