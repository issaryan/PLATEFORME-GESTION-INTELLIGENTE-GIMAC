import { useRef, type ReactNode } from 'react'
import { cx } from '../../lib/cx'
import { useFocusTrap } from '../../lib/useFocusTrap'
import { IconButton } from '../../actions/IconButton/IconButton'
import { Icon } from '../../icones/Icon/Icon'
import type { IconName } from '../../icones/icons'
import { Overlay } from '../Overlay/Overlay'
import { useLibelle } from '../../i18n/LangueProvider'

export interface DialogProps {
  title: string
  /** Sous-titre de contexte sous le titre : l'objet concerné (compte, liaison, institution…). */
  subtitle?: ReactNode
  description?: string
  children?: ReactNode
  /** Pied d'actions : au plus un bouton principal. */
  footer?: ReactNode
  onClose?: () => void
  /** Petite (440 px), moyenne (560 px, défaut) ou large (720 px). */
  size?: 'sm' | 'md' | 'lg'
  open?: boolean
  /** Confine le voile au conteneur parent. */
  inline?: boolean
  id?: string
  role?: 'dialog' | 'alertdialog'
  className?: string
  /** Pictogramme décoratif devant le titre (type d'un document dans l'aperçu, D-83). */
  icon?: IconName
}

// Fenêtre modale centrée de 560 px (440 en sm, 720 en lg) sur le voile (§7.5).
export function Dialog(p: DialogProps) {
  const t = useLibelle()
  const ref = useRef<HTMLDivElement>(null)
  useFocusTrap(ref, p.open !== false, p.onClose)
  if (p.open === false) return null
  const id = p.id || 'gm-dialog'
  const titre = p.icon ? <><Icon name={p.icon} size={20} className="gm-dialog-icone" />{p.title}</> : p.title
  return (
    <Overlay inline={p.inline} onClose={p.onClose}>
      <div
        ref={ref}
        className={cx('gm-dialog', p.size === 'lg' && 'gm-dialog-lg', p.size === 'sm' && 'gm-dialog-sm', p.className)}
        role={p.role || 'dialog'}
        aria-modal="true"
        aria-labelledby={id + '-title'}
        aria-describedby={[p.subtitle ? id + '-sujet' : '', p.description ? id + '-desc' : ''].filter(Boolean).join(' ') || undefined}
      >
        <header className="gm-dialog-head">
          {p.subtitle ? (
            <div>
              <h2 id={id + '-title'} className="gm-card-title">{titre}</h2>
              <span className="gm-dialog-sujet" id={id + '-sujet'}>{p.subtitle}</span>
            </div>
          ) : (
            <h2 id={id + '-title'} className="gm-card-title">{titre}</h2>
          )}
          {p.onClose ? <IconButton variant="ghost" size="sm" icon="x" label={t('commun.fermer')} onClick={p.onClose} /> : null}
        </header>
        {p.description ? <p id={id + '-desc'} className="gm-dialog-desc">{p.description}</p> : null}
        <div className="gm-dialog-body">{p.children}</div>
        {p.footer ? <footer className="gm-dialog-foot">{p.footer}</footer> : null}
      </div>
    </Overlay>
  )
}
