import { useState } from 'react'
import { cx } from '../../lib/cx'
import { Icon } from '../../icones/Icon/Icon'
import type { IconName } from '../../icones/icons'
import { StatusBadge, type StatusBadgeProps } from '../../affichage/StatusBadge/StatusBadge'

export interface TreeNode {
  key: string
  label: string
  code?: string
  icon?: IconName
  count?: number
  badge?: StatusBadgeProps
  children?: TreeNode[]
}

export interface TreeViewProps {
  label: string
  nodes: TreeNode[]
  expanded?: string[]
  selected?: string
  onSelect?: (key: string) => void
}

interface TreeItemProps {
  node: TreeNode
  level: number
  expanded: string[]
  selected?: string
  onToggle: (key: string) => void
  onSelect: (key: string) => void
}

function TreeItem(p: TreeItemProps) {
  const n = p.node
  const open = p.expanded.indexOf(n.key) >= 0
  const has = n.children && n.children.length
  const sel = p.selected === n.key
  return (
    <li role="treeitem" aria-expanded={has ? (open ? 'true' : 'false') : undefined} aria-selected={sel ? 'true' : 'false'} aria-level={p.level}>
      <div className={cx('gm-tree-row', sel && 'gm-tree-sel')} style={{ paddingLeft: 8 + (p.level - 1) * 20 }} tabIndex={sel ? 0 : -1} onClick={() => p.onSelect && p.onSelect(n.key)}>
        {has ? (
          <button type="button" className="gm-tree-toggle" aria-label={open ? 'Replier' : 'Déplier'} tabIndex={-1} onClick={(e) => { e.stopPropagation(); p.onToggle(n.key) }}>
            <Icon name="chevron-right" size={14} className={open ? 'gm-rot90' : undefined} />
          </button>
        ) : (
          <span className="gm-tree-toggle" />
        )}
        {n.icon ? <Icon name={n.icon} size={16} /> : null}
        {n.code ? <span className="gm-mono">{n.code}</span> : null}
        <span className="gm-tree-label">{n.label}</span>
        {n.badge ? <StatusBadge {...n.badge} /> : null}
        {n.count !== undefined ? <span className="gm-field-help gm-num">{n.count}</span> : null}
      </div>
      {has && open ? (
        <ul role="group">
          {(n.children || []).map((c) => (
            <TreeItem key={c.key} node={c} level={p.level + 1} expanded={p.expanded} selected={p.selected} onToggle={p.onToggle} onSelect={p.onSelect} />
          ))}
        </ul>
      ) : null}
    </li>
  )
}

// Arbre (référentiel d'opérations, rôles et permissions), §7.6.
export function TreeView(p: TreeViewProps) {
  const [exp, setExp] = useState<string[]>(p.expanded || [])
  const [sel, setSel] = useState<string | undefined>(p.selected)
  const toggle = (k: string) => setExp(exp.indexOf(k) >= 0 ? exp.filter((x) => x !== k) : exp.concat([k]))
  return (
    <ul className="gm-tree" role="tree" aria-label={p.label}>
      {(p.nodes || []).map((n) => (
        <TreeItem key={n.key} node={n} level={1} expanded={exp} selected={sel} onToggle={toggle} onSelect={(k) => { setSel(k); if (p.onSelect) p.onSelect(k) }} />
      ))}
    </ul>
  )
}
