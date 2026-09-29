import { useState } from 'react'
import { cx } from '../../lib/cx'
import type { Status } from '../../lib/types'
import { IconButton } from '../../actions/IconButton/IconButton'
import { StatusBadge } from '../../affichage/StatusBadge/StatusBadge'
import { Tabs } from '../../navigation/Tabs/Tabs'
import { DataTable } from '../DataTable/DataTable'
import { OperationNode, type OperationNodeProps } from '../../domaine/OperationNode/OperationNode'
import { useLibelle } from '../../i18n/LangueProvider'
import type { CleLibelle } from '../../i18n/libelles'

export interface DagNode extends OperationNodeProps {
  phase: string
  deps?: string[]
}

export interface DagViewProps {
  phases: { key: string; label: string }[]
  nodes: DagNode[]
  view?: 'graph' | 'list'
  /** Clic ou Entrée sur un nœud. */
  onSelect?: (code: string) => void
  /** Nœud sélectionné (contour de focus, aria-current). */
  selected?: string
}

const NODE_W = 240
const NODE_H = 118
const COL_GAP = 64
const ROW_GAP = 16
const ETATS: Record<string, [Status, CleLibelle]> = {
  eligible: ['info', 'operation.eligible'],
  locked: ['neutral', 'operation.verrouillee'],
  closed: ['ok', 'operation.close'],
  out: ['neutral', 'operation.hors'],
  late: ['warn', 'operation.retard'],
}

// Graphe orienté acyclique des opérations d'une liaison : disposition par phase (colonnes),
// liens orthogonaux, chemin critique, zoom et vue liste équivalente (§7.6).
export function DagView(p: DagViewProps) {
  const t = useLibelle()
  const [view, setView] = useState<string>(p.view || 'graph')
  const [zoom, setZoom] = useState(1)
  const phases = p.phases || []
  const nodes = p.nodes || []
  const pos: Record<string, { x: number; y: number }> = {}
  phases.forEach((ph, ci) => {
    nodes.filter((n) => n.phase === ph.key).forEach((n, ri) => {
      pos[n.code] = { x: ci * (NODE_W + COL_GAP), y: ri * (NODE_H + ROW_GAP) + 32 }
    })
  })
  const maxRows = Math.max(...phases.map((ph) => nodes.filter((n) => n.phase === ph.key).length).concat([1]))
  const W = phases.length * (NODE_W + COL_GAP) - COL_GAP
  const H = maxRows * (NODE_H + ROW_GAP) + 32
  const toolbar = (
    <div className="gm-dag-toolbar">
      <Tabs items={[{ key: 'graph', label: t('dag.graphe') }, { key: 'list', label: t('dag.liste') }]} value={view} onChange={setView} />
      <span style={{ flex: 1 }} />
      <span className="gm-row" style={{ gap: 'var(--gm-space-3)', fontSize: 12, color: 'var(--gm-color-text-secondary)' }}>
        <span className="gm-dag-key gm-dag-key-critical">{t('gantt.critique')}</span>
        <StatusBadge status="info" label={t('operation.eligible')} />
        <StatusBadge status="ok" label={t('operation.close')} />
        <StatusBadge status="warn" label={t('operation.retard')} />
        <StatusBadge status="neutral" label={t('operation.verrouillee')} />
      </span>
      {view === 'graph' ? (
        <span className="gm-row">
          <IconButton variant="ghost" size="sm" icon="x" label={t('dag.reduire')} onClick={() => setZoom(Math.max(0.5, zoom - 0.1))} />
          <span className="gm-num gm-field-help">{Math.round(zoom * 100) + ' %'}</span>
          <IconButton variant="ghost" size="sm" icon="plus" label={t('dag.agrandir')} onClick={() => setZoom(Math.min(1.5, zoom + 0.1))} />
        </span>
      ) : null}
    </div>
  )
  if (view === 'list')
    return (
      <div className="gm-dag">
        {toolbar}
        <DataTable
          state="ok"
          rowKey={(r: DagNode) => r.code}
          columns={[
            { key: 'code', header: t('dag.code'), type: 'mono', width: 140 },
            { key: 'label', header: t('dag.operation') },
            { key: 'phase', header: t('dag.phase'), render: (r: DagNode) => phases.filter((x) => x.key === r.phase)[0]?.label },
            { key: 'owner', header: t('dag.incombe') },
            { key: 'state', header: t('dag.etat'), render: (r: DagNode) => { const m = ETATS[r.state]; return <StatusBadge status={m[0]} label={t(m[1])} /> } },
            { key: 'elements', header: t('dag.quatreElements'), render: (r: DagNode) => { const e = r.elements || {}; const n = (['owner', 'proof', 'timestamp', 'validation'] as const).filter((k) => e[k] && e[k] !== 'pending').length; return <span className="gm-num">{n + ' / 4'}</span> } },
            { key: 'deps', header: t('dag.depend'), render: (r: DagNode) => <span className="gm-mono">{(r.deps || []).join(', ') || t('dag.aucune')}</span> },
          ]}
          rows={nodes}
          density="compact"
        />
      </div>
    )
  return (
    <div className="gm-dag">
      {toolbar}
      <div className="gm-dag-canvas" role="img" aria-label={t('dag.description', { n: nodes.length, phases: phases.length })}>
        <div className="gm-dag-inner" style={{ width: W * zoom, height: H * zoom }}>
          <div style={{ transform: 'scale(' + zoom + ')', transformOrigin: '0 0', width: W, height: H, position: 'relative' }}>
            {phases.map((ph, ci) => (
              <div key={ph.key} className="gm-dag-phase" style={{ left: ci * (NODE_W + COL_GAP), width: NODE_W }}>{ph.label}</div>
            ))}
            <svg className="gm-dag-links" width={W} height={H} aria-hidden="true">
              {nodes.flatMap((n) =>
                (n.deps || []).map((d) => {
                  const a = pos[d]
                  const b = pos[n.code]
                  if (!a || !b) return null
                  const x1 = a.x + NODE_W, y1 = a.y + NODE_H / 2, x2 = b.x, y2 = b.y + NODE_H / 2, xm = x1 + COL_GAP / 2
                  const crit = n.critical && nodes.filter((m) => m.code === d)[0]?.critical
                  return <path key={d + '>' + n.code} className={cx('gm-dag-link', crit && 'gm-dag-link-critical')} d={'M' + x1 + ' ' + y1 + ' H' + xm + ' V' + y2 + ' H' + x2} />
                }),
              )}
            </svg>
            {nodes.map((n) => {
              const q = pos[n.code]
              return (
                <div
                  key={n.code}
                  className={cx('gm-dag-node', p.selected === n.code && 'gm-dag-node-sel')}
                  style={{ left: q.x, top: q.y, width: NODE_W, cursor: p.onSelect ? 'pointer' : undefined }}
                  aria-current={p.selected === n.code ? 'true' : undefined}
                  tabIndex={p.onSelect ? 0 : undefined}
                  onClick={p.onSelect ? () => p.onSelect!(n.code) : undefined}
                  onKeyDown={p.onSelect ? (e) => { if (e.key === 'Enter') p.onSelect!(n.code) } : undefined}
                >
                  <OperationNode {...n} />
                </div>
              )
            })}
          </div>
        </div>
      </div>
    </div>
  )
}
