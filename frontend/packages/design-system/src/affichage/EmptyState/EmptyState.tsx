import type { ReactNode } from 'react'
import { Icon } from '../../icones/Icon/Icon'
import type { IconName } from '../../icones/icons'

export interface EmptyStateProps {
  icon?: IconName
  title: ReactNode
  text?: ReactNode
  action?: ReactNode
}

// État vide sur motif de trame (§7.3).
export function EmptyState(p: EmptyStateProps) {
  return (
    <div className="gm-empty">
      <div className="gm-empty-art gm-pattern" aria-hidden="true"><Icon name={p.icon || 'inbox'} size={24} /></div>
      <h3 className="gm-empty-title">{p.title}</h3>
      {p.text ? <p className="gm-empty-text">{p.text}</p> : null}
      {p.action ? <div style={{ marginTop: 'var(--gm-space-2)' }}>{p.action}</div> : null}
    </div>
  )
}
