import type { ReactNode } from 'react'
import { Icon } from '../../icones/Icon/Icon'

export interface ToastProps {
  action?: ReactNode
  children?: ReactNode
}

// Retour d'action non bloquant (§7.5).
export function Toast(p: ToastProps) {
  return (
    <div className="gm-toast" role="status">
      <Icon name="circle-check" size={20} />
      <span style={{ flex: 1 }}>{p.children}</span>
      {p.action || null}
    </div>
  )
}
