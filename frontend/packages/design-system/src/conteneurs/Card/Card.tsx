import type { ReactNode } from 'react'
import { cx } from '../../lib/cx'

export interface CardProps {
  title?: ReactNode
  actions?: ReactNode
  footer?: ReactNode
  className?: string
  children?: ReactNode
}

// Surface à bordure 1 px, sans ombre (§7.5).
export function Card(p: CardProps) {
  return (
    <section className={cx('gm-card', p.className)}>
      {p.title ? (
        <header className="gm-card-header">
          <h2 className="gm-card-title">{p.title}</h2>
          {p.actions ? <div className="gm-row">{p.actions}</div> : null}
        </header>
      ) : null}
      <div className="gm-card-body">{p.children}</div>
      {p.footer ? <footer className="gm-card-footer">{p.footer}</footer> : null}
    </section>
  )
}
