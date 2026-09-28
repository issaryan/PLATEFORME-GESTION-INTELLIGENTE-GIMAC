import { useState } from 'react'
import { cx } from '../../lib/cx'
import type { Status } from '../../lib/types'
import { IconButton } from '../../actions/IconButton/IconButton'
import { StatusBadge } from '../../affichage/StatusBadge/StatusBadge'
import { Tabs } from '../../navigation/Tabs/Tabs'
import { DataTable } from '../DataTable/DataTable'
import { OperationNode, type OperationNodeProps } from '../../domaine/OperationNode/OperationNode'

export interface DagNode extends OperationNodeProps {
  phase: string
  deps?: string[]
}

export interface DagViewProps {
  phases: { key: string; label: string }[]
  nodes: DagNode[]
  view?: 'graph' | 'list'
}

const NODE_W = 240
const NODE_H = 118
const COL_GAP = 64
const ROW_GAP = 16
const ETATS: Record<string, [Status, string]> = {
  eligible: ['info', 'éligible'],
  locked: ['neutral', 'verrouillée'],
  closed: ['ok', 'close'],
  out: ['neutral', 'hors périmètre'],
  late: ['warn', 'en retard'],
}

// Graphe orienté acyclique des opérations d'une liaison : disposition par phase (colonnes),
// liens orthogonaux, chemin critique, zoom et vue liste équivalente (§7.6).
export function DagView(p: DagViewProps) {
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
      <Tabs items={[{ key: 'graph', label: 'Graphe' }, { key: 'list', label: 'Vue liste' }]} value={view} onChange={setView} />
      <span style={{ flex: 1 }} />
      <span className="gm-row" style={{ gap: 'var(--gm-space-3)', fontSize: 12, color: 'var(--gm-color-text-secondary)' }}>
        <span className="gm-dag-key gm-dag-key-critical">chemin critique</span>
        <StatusBadge status="info" label="éligible" />
        <StatusBadge status="ok" label="close" />
        <StatusBadge status="warn" label="en retard" />
        <StatusBadge status="neutral" label="verrouillée" />
      </span>
      {view === 'graph' ? (
        <span className="gm-row">
          <IconButton variant="ghost" size="sm" icon="x" label="Réduire" onClick={() => setZoom(Math.max(0.5, zoom - 0.1))} />
          <span className="gm-num gm-field-help">{Math.round(zoom * 100) + ' %'}</span>
          <IconButton variant="ghost" size="sm" icon="plus" label="Agrandir" onClick={() => setZoom(Math.min(1.5, zoom + 0.1))} />
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
            { key: 'code', header: 'Code', type: 'mono', width: 140 },
            { key: 'label', header: 'Opération' },
            { key: 'phase', header: 'Phase', render: (r: DagNode) => phases.filter((x) => x.key === r.phase)[0]?.label },
            { key: 'owner', header: 'Incombe à' },
            { key: 'state', header: 'État', render: (r: DagNode) => { const m = ETATS[r.state]; return <StatusBadge status={m[0]} label={m[1]} /> } },
            { key: 'elements', header: 'Quatre éléments', render: (r: DagNode) => { const e = r.elements || {}; const n = (['owner', 'proof', 'timestamp', 'validation'] as const).filter((k) => e[k]).length; return <span className="gm-num">{n + ' / 4'}</span> } },
            { key: 'deps', header: 'Dépend de', render: (r: DagNode) => <span className="gm-mono">{(r.deps || []).join(', ') || 'aucune'}</span> },
          ]}
          rows={nodes}
          density="compact"
        />
      </div>
    )
  return (
    <div className="gm-dag">
      {toolbar}
      <div className="gm-dag-canvas" role="img" aria-label={'Graphe des opérations : ' + nodes.length + ' opérations en ' + phases.length + ' phases. La vue liste donne les mêmes informations.'}>
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
                <div key={n.code} className="gm-dag-node" style={{ left: q.x, top: q.y, width: NODE_W }}>
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
