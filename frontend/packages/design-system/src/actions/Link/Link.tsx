import type { ReactNode } from 'react'
import { cx } from '../../lib/cx'
import type { BaseProps } from '../../lib/types'

export interface LinkProps extends BaseProps {
  href?: string
  /** Nouvelle fenêtre avec rel="noopener". */
  external?: boolean
  /** Référence d'objet en chasse fixe. */
  mono?: boolean
  children: ReactNode
}

// Lien texte (§7.1) : navigue, ne déclenche jamais une action à effet.
export function Link(p: LinkProps) {
  return (
    <a
      className={cx('gm-link', p.mono && 'gm-mono', p.className)}
      href={p.href || '#'}
      target={p.external ? '_blank' : undefined}
      rel={p.external ? 'noopener' : undefined}
      data-testid={p['data-testid']}
    >
      {p.children}
    </a>
  )
}
