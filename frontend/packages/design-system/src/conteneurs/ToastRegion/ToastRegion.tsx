import type { ReactNode } from 'react'

export interface ToastRegionProps {
  children?: ReactNode
}

// Emplacement des retours d'action (Toast) en bas à droite de la fenêtre, au-dessus du contenu et sous
// les dialogues (D-63) : remplace le positionnement écrit à la main dans les couches.
export function ToastRegion(p: ToastRegionProps) {
  if (!p.children) return null
  return (
    <div className="gm-col" style={{ position: 'fixed', right: 'var(--gm-space-5)', bottom: 'var(--gm-space-5)', zIndex: 60, gap: 'var(--gm-space-2)' }}>
      {p.children}
    </div>
  )
}
