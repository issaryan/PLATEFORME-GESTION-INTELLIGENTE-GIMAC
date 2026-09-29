import { useRef, type ReactNode } from 'react'
import { cx } from '../../lib/cx'
import { useFocusTrap } from '../../lib/useFocusTrap'
import { IconButton } from '../../actions/IconButton/IconButton'
import { Overlay } from '../Overlay/Overlay'
import { useLibelle } from '../../i18n/LangueProvider'

export interface DialogProps {
  title: string
  description?: string
  children?: ReactNode
  /** Pied d'actions : au plus un bouton principal. */
  footer?: ReactNode
  onClose?: () => void
  size?: 'md' | 'lg'
  open?: boolean
  /** Confine le voile au conteneur parent. */
  inline?: boolean
  id?: string
  role?: 'dialog' | 'alertdialog'
  className?: string
}

// Fenêtre modale centrée de 560 px (720 en lg) sur le voile (§7.5).
export function Dialog(p: DialogProps) {
  const t = useLibelle()
  const ref = useRef<HTMLDivElement>(null)
  useFocusTrap(ref, p.open !== false, p.onClose)
  if (p.open === false) return null
  const id = p.id || 'gm-dialog'
  return (
    <Overlay inline={p.inline} onClose={p.onClose}>
      <div
        ref={ref}
        className={cx('gm-dialog', p.size === 'lg' && 'gm-dialog-lg', p.className)}
        role={p.role || 'dialog'}
        aria-modal="true"
        aria-labelledby={id + '-title'}
        aria-describedby={p.description ? id + '-desc' : undefined}
      >
        <header className="gm-dialog-head">
          <h2 id={id + '-title'} className="gm-card-title">{p.title}</h2>
          {p.onClose ? <IconButton variant="ghost" size="sm" icon="x" label={t('commun.fermer')} onClick={p.onClose} /> : null}
        </header>
        {p.description ? <p id={id + '-desc'} className="gm-dialog-desc">{p.description}</p> : null}
        <div className="gm-dialog-body">{p.children}</div>
        {p.footer ? <footer className="gm-dialog-foot">{p.footer}</footer> : null}
      </div>
    </Overlay>
  )
}
