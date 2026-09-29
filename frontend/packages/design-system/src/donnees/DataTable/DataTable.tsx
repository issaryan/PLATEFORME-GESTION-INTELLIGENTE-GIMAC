import type { ReactNode } from 'react'
import { cx } from '../../lib/cx'
import type { ApiError, Severity, Status } from '../../lib/types'
import { Icon } from '../../icones/Icon/Icon'
import { Button } from '../../actions/Button/Button'
import { StatusBadge } from '../../affichage/StatusBadge/StatusBadge'
import { SeverityBadge } from '../../affichage/SeverityBadge/SeverityBadge'
import { Money } from '../../affichage/Money/Money'
import { DateTime } from '../../affichage/DateTime/DateTime'
import { Skeleton } from '../../affichage/Skeleton/Skeleton'
import { EmptyState } from '../../affichage/EmptyState/EmptyState'
import { InlineAlert } from '../../conteneurs/InlineAlert/InlineAlert'
import { Pagination } from '../../navigation/Pagination/Pagination'
import { useLibelle } from '../../i18n/LangueProvider'

/** Éléments interactifs d'une ligne : leur clic ne l'ouvre pas. */
const CONTROLES = 'button, a, input, select, textarea, label'

export type ColumnType = 'text' | 'mono' | 'number' | 'money' | 'date' | 'datetime' | 'status' | 'severity' | 'progress' | 'tags' | 'link' | 'actions' | 'custom'

export type TableRow = object

export interface Column<Row extends TableRow = TableRow> {
  key: string
  header: ReactNode
  type?: ColumnType
  width?: number
  sortable?: boolean
  render?: (row: Row) => ReactNode
}

export interface DataTableProps<Row extends TableRow = TableRow> {
  columns?: Column<Row>[]
  rows?: Row[]
  rowKey?: (r: Row) => string
  total?: number
  page?: number
  pageSize?: number
  sort?: { key: string; dir: 'asc' | 'desc' }
  onSort?: (key: string) => void
  onPage?: (p: number) => void
  onPageSize?: (n: number) => void
  onRowOpen?: (r: Row) => void
  selected?: string[]
  density?: 'normal' | 'compact'
  state?: 'loading' | 'empty' | 'error' | 'ok'
  error?: ApiError
  onRetry?: () => void
  emptyText?: string
  emptyHelp?: string
  liveUpdates?: boolean
  toolbar?: ReactNode
  className?: string
}

function cellule<Row extends TableRow>(c: Column<Row>, r: Row): ReactNode {
  const v = (r as Record<string, unknown>)[c.key]
  if (c.render) return c.render(r)
  if (c.type === 'status') {
    const s = v as { status?: Status; label?: ReactNode } | undefined
    return <StatusBadge status={s && s.status} label={s && s.label} />
  }
  if (c.type === 'severity') return <SeverityBadge level={v as Severity} />
  if (c.type === 'money') return <Money amount={v as number} />
  if (c.type === 'mono') return <span className="gm-mono">{v as ReactNode}</span>
  if (c.type === 'datetime') return <DateTime local={v as string} />
  return v as ReactNode
}

// Composant central des listes (§7.6) : quatre états, tri, pagination serveur.
export function DataTable<Row extends TableRow = TableRow>(p: DataTableProps<Row>) {
  const t = useLibelle()
  const cols = p.columns || []
  const rows = p.rows || []
  const state = p.state || 'ok'
  let body: ReactNode
  if (state === 'loading')
    body = (
      <tbody>
        {[0, 1, 2, 3, 4].map((i) => (
          <tr key={i}>{cols.map((c) => <td key={c.key}><Skeleton width="70%" /></td>)}</tr>
        ))}
      </tbody>
    )
  else if (state === 'empty')
    body = (
      <tbody>
        <tr><td colSpan={cols.length} style={{ height: 'auto' }}><EmptyState title={p.emptyText || t('commun.aucunElement')} text={p.emptyHelp} /></td></tr>
      </tbody>
    )
  else if (state === 'error')
    body = (
      <tbody>
        <tr>
          <td colSpan={cols.length} style={{ height: 'auto', padding: 'var(--gm-space-4)' }}>
            <InlineAlert level="crit" title={t('commun.chargementEchoue')} correlationId={p.error && p.error.correlationId} action={<Button size="sm" icon="refresh" onClick={p.onRetry}>{t('commun.reessayer')}</Button>}>
              {p.error && p.error.message}
            </InlineAlert>
          </td>
        </tr>
      </tbody>
    )
  else
    body = (
      <tbody>
        {rows.map((r, i) => {
          const key = p.rowKey ? p.rowKey(r) : String(i)
          const onRowOpen = p.onRowOpen
          return (
            <tr
              key={key}
              aria-selected={p.selected && p.selected.indexOf(key) >= 0 ? 'true' : undefined}
              // Un clic sur un contrôle de la ligne (bouton d'action, lien, case) agit seul, sans ouvrir la ligne.
              onClick={onRowOpen ? (e) => { if (!(e.target as HTMLElement).closest(CONTROLES)) onRowOpen(r) } : undefined}
              style={onRowOpen ? { cursor: 'pointer' } : undefined}
            >
              {cols.map((c) => (
                <td key={c.key} data-label={c.header || undefined} className={cx((c.type === 'number' || c.type === 'money') && 'gm-num', c.type === 'actions' && 'gm-row')}>
                  {cellule(c, r)}
                </td>
              ))}
            </tr>
          )
        })}
      </tbody>
    )
  const sort = p.sort
  return (
    <div className={cx('gm-table-wrap', p.className)}>
      {p.toolbar ? <div className="gm-table-toolbar">{p.toolbar}</div> : null}
      <table className={cx('gm-table', p.density === 'compact' && 'gm-table-compact', p.liveUpdates && 'gm-table-live')}>
        <thead>
          <tr>
            {cols.map((c) => {
              const sorted = !!sort && sort.key === c.key
              return (
                <th
                  key={c.key}
                  scope="col"
                  className={cx((c.type === 'number' || c.type === 'money') && 'gm-num')}
                  style={c.width ? { width: c.width } : undefined}
                  aria-sort={sorted ? (sort.dir === 'asc' ? 'ascending' : 'descending') : undefined}
                >
                  {c.sortable ? (
                    <button type="button" onClick={() => { if (p.onSort) p.onSort(c.key) }}>
                      {c.header}
                      {sorted ? <Icon name={sort.dir === 'asc' ? 'arrow-up' : 'arrow-down'} size={14} /> : null}
                    </button>
                  ) : (
                    c.header
                  )}
                </th>
              )
            })}
          </tr>
        </thead>
        {body}
      </table>
      {p.total !== undefined && state === 'ok' ? (
        <div className="gm-table-toolbar" style={{ borderTop: 'var(--gm-border-1) solid var(--gm-color-border)', borderBottom: 0, justifyContent: 'flex-end' }}>
          <Pagination total={p.total} page={p.page || 1} pageSize={p.pageSize || 25} onPage={p.onPage} onPageSize={p.onPageSize} />
        </div>
      ) : null}
    </div>
  )
}
