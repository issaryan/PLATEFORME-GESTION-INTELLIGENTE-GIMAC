import { useState } from 'react'
import { cx } from '../../lib/cx'
import type { ApiError, TimelineEvent } from '../../lib/types'
import { Button } from '../../actions/Button/Button'
import { IconButton } from '../../actions/IconButton/IconButton'
import { Timeline } from '../../affichage/Timeline/Timeline'
import { EmptyState } from '../../affichage/EmptyState/EmptyState'
import { ConfirmDialog } from '../../conteneurs/ConfirmDialog/ConfirmDialog'
import { SignDialog } from '../../conteneurs/SignDialog/SignDialog'
import { Drawer } from '../../conteneurs/Drawer/Drawer'
import { FilterBar } from '../../donnees/FilterBar/FilterBar'
import { DataTable, type Column } from '../../donnees/DataTable/DataTable'
import { ResourceDetail } from '../ResourceDetail/ResourceDetail'
import type { ListQuery, ResourceAction, ResourceDescriptor, ResourceState } from '../descripteur'
import { actionButton, allowed, champ, optionsOf, renderField, statusOf, type Runner } from '../outils'

export interface ResourceListProps<T> {
  descriptor: ResourceDescriptor<T>
  rows: T[]
  total?: number
  state?: ResourceState
  error?: ApiError
  /** Permissions effectives du jeton ; sans elles, tout est affiché. */
  permissions?: string[]
  defaultQuery?: Partial<ListQuery>
  /** Dans un onglet de fiche : sans titre ni actions de page. */
  embedded?: boolean
  fixedFilters?: Record<string, string>
  density?: 'normal' | 'compact'
  liveUpdates?: boolean
  activeFilters?: { key: string; label: string }[]
  favorites?: string[]
  /** Chronologie affichée dans le tiroir. */
  timeline?: TimelineEvent[]
  signHash?: string
  inline?: boolean
}

// Liste paginée, filtrée, triée, avec actions, tiroir de détail et confirmations (§8.3, gabarit G2).
export function ResourceList<T extends object>(p: ResourceListProps<T>) {
  const desc = p.descriptor
  const perms = p.permissions
  const [q] = useState<ListQuery>(Object.assign({ page: 1, pageSize: 25 }, p.defaultQuery || {}))
  const [open, setOpen] = useState<T | null>(null)
  const [dlg, setDlg] = useState<{ action: ResourceAction<T>; row: T | null } | null>(null)
  const fields = desc.fields.filter((f) => f.inList && (!f.permission || !perms || perms.indexOf(f.permission) >= 0))
  const rows = p.rows || []
  const ctx: Runner<T> = {
    run: (a, row) => {
      if (a.confirm === 'confirm' || a.confirm === 'sign') setDlg({ action: a, row })
      else if (a.run) a.run({ row, descriptor: desc })
    },
  }
  const rowActions = desc.actions.filter((a) => a.scope === 'row')
  const pageActions = desc.actions.filter((a) => a.scope === 'page' && allowed(a, null, perms))
  const bulk = desc.actions.filter((a) => a.scope === 'bulk' && allowed(a, null, perms))
  const columns: Column<T>[] = fields
    .map((f): Column<T> => ({
      key: f.name,
      header: f.label,
      width: f.width,
      sortable: f.sortable,
      type: f.kind === 'money' || f.kind === 'number' ? f.kind : undefined,
      render: (r: T) => renderField(f, champ(r, f.name), r, desc),
    }))
    .concat([
      {
        key: '__actions',
        header: '',
        type: 'actions',
        render: (r: T) => {
          const acts = rowActions.filter((a) => allowed(a, r, perms))
          return (
            <span className="gm-row" style={{ justifyContent: 'flex-end', flexWrap: 'nowrap' }}>
              {acts.slice(0, 1).map((a) => actionButton(a, r, ctx))}
              {acts.length > 1 ? <IconButton variant="ghost" size="sm" icon="menu" label="Plus d'actions" /> : null}
            </span>
          )
        },
      },
    ])
  const filters = fields
    .filter((f) => f.filterable)
    .map((f) => ({
      key: f.name,
      label: f.label,
      type: (f.kind === 'date' ? 'daterange' : 'select') as 'daterange' | 'select',
      options:
        f.kind === 'status'
          ? Object.keys(desc.statusDictionary || {}).map((k) => ({ value: k, label: (desc.statusDictionary || {})[k].label }))
          : optionsOf(f),
    }))
  const title = desc.label.plural.charAt(0).toUpperCase() + desc.label.plural.slice(1)
  const live = p.liveUpdates !== false && !!desc.liveTopic
  return (
    <div className={cx('gm-resource', p.embedded && 'gm-resource-embedded')}>
      {p.embedded ? null : (
        <div className="gm-resource-head">
          <h1 className="gm-heading-display">{title}</h1>
          <div className="gm-row">
            {pageActions.filter((a) => a.variant !== 'primary').slice(0, 2).map((a) => actionButton(a, null, ctx, 'md'))}
            {pageActions.filter((a) => a.variant === 'primary').slice(0, 1).map((a) => actionButton(a, null, ctx, 'md'))}
          </div>
        </div>
      )}
      <FilterBar filters={filters} query={q.q || ''} active={p.activeFilters || []} favorites={p.favorites} />
      <DataTable
        columns={columns}
        rows={rows}
        rowKey={(r: T) => String(champ(r, desc.idField))}
        total={p.total !== undefined ? p.total : rows.length}
        page={q.page}
        pageSize={q.pageSize}
        state={p.state || 'ok'}
        error={p.error}
        emptyText={'Aucune ' + desc.label.singular + ' ne correspond aux filtres'}
        emptyHelp="Élargissez la période ou retirez un filtre."
        density={p.density}
        liveUpdates={live}
        onRowOpen={(r: T) => setOpen(r)}
        toolbar={[
          <span key={1} className="gm-num">{rows.length + ' ' + (rows.length > 1 ? desc.label.plural : desc.label.singular) + ' sur cette page'}</span>,
          live ? (
            <span key={2} className="gm-row" style={{ gap: 4 }}>
              <span className="gm-dot gm-dot-ok" />
              {'mises à jour en temps réel (' + desc.liveTopic + ')'}
            </span>
          ) : null,
          <span key={3} className="gm-grow" />,
          bulk.map((a) => (
            <Button key={a.key} size="sm" variant="ghost" icon={a.icon} disabled disabledReason="Sélectionnez au moins une ligne">
              {a.label}
            </Button>
          )),
        ]}
      />
      {open ? (
        <Drawer
          inline={p.inline}
          type={desc.label.singular}
          reference={String(champ(open, desc.idField))}
          status={statusOf(desc, open).status}
          statusLabel={statusOf(desc, open).label}
          title={String(champ(open, desc.titleField))}
          onClose={() => setOpen(null)}
          render={(tab) =>
            tab === 'detail' ? (
              <ResourceDetail descriptor={desc} row={open} mode="drawer" permissions={perms} onAction={ctx.run} />
            ) : tab === 'chronologie' ? (
              <Timeline events={p.timeline || []} />
            ) : (
              <EmptyState title="Aucun élément" text="Rien à afficher dans cet onglet pour le moment." />
            )
          }
          actions={rowActions.filter((a) => allowed(a, open, perms)).map((a) => actionButton(a, open, ctx, 'md'))}
        />
      ) : null}
      {dlg ? (
        dlg.action.confirm === 'sign' ? (
          <SignDialog
            inline={p.inline}
            title={dlg.action.label}
            action={dlg.action.label}
            hash={p.signHash || '0000000000000000000000000000000000000000000000000000000000000000'}
            object={[{ key: desc.label.singular, value: String(dlg.row ? champ(dlg.row, desc.titleField) : '') }]}
            onClose={() => setDlg(null)}
          />
        ) : (
          <ConfirmDialog
            inline={p.inline}
            title={dlg.action.label}
            confirmLabel={dlg.action.label}
            danger={dlg.action.variant === 'danger'}
            reason={!!dlg.action.reason}
            object={
              dlg.row
                ? [
                    { key: desc.label.singular, value: String(champ(dlg.row, desc.titleField)) },
                    { key: 'Référence', value: String(champ(dlg.row, desc.idField)), mono: true },
                  ]
                : undefined
            }
            consequences={dlg.action.consequences}
            onClose={() => setDlg(null)}
            onConfirm={() => {
              setDlg(null)
              if (dlg.action.run) dlg.action.run({ row: dlg.row, descriptor: desc })
            }}
          />
        )
      ) : null}
    </div>
  )
}
