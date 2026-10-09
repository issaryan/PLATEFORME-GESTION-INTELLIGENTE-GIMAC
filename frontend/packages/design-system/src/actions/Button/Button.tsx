import type { MouseEvent, ReactNode } from 'react'
import { cx } from '../../lib/cx'
import type { BaseProps } from '../../lib/types'
import { Icon } from '../../icones/Icon/Icon'
import type { IconName } from '../../icones/icons'

export type ButtonVariant = 'primary' | 'secondary' | 'danger' | 'ghost'
export type ButtonSize = 'sm' | 'md' | 'lg'

export interface ButtonProps extends BaseProps {
  variant?: ButtonVariant
  size?: ButtonSize
  /** Pictogramme à gauche du libellé. */
  icon?: IconName
  /** Pictogramme seul : exige aria-label. */
  iconOnly?: boolean
  'aria-label'?: string
  /** Bouton qui ouvre un menu ou un dialogue surgissant. */
  'aria-haspopup'?: 'menu' | 'dialog' | 'true'
  'aria-expanded'?: boolean
  /** Élément commandé par le bouton (tiroir, panneau). */
  'aria-controls'?: string
  loading?: boolean
  disabled?: boolean
  /** Affiché en infobulle quand le bouton est désactivé : jamais désactivé sans explication. */
  disabledReason?: string
  type?: 'button' | 'submit'
  /** Formulaire soumis par ce bouton quand il est placé hors du formulaire (pied d'un dialogue). */
  form?: string
  onClick?: (e: MouseEvent<HTMLButtonElement>) => void
  /** Verbe à l'infinitif et son objet. */
  children?: ReactNode
}

// Bouton d'action (DS-GIMAC-01 §7.1).
export function Button(p: ButtonProps) {
  const variant = p.variant || 'secondary'
  const size = p.size || 'md'
  return (
    <button
      type={p.type || 'button'}
      form={p.form}
      className={cx('gm-btn', 'gm-btn-' + variant, size !== 'md' && 'gm-btn-' + size, p.iconOnly && 'gm-btn-icon', p.className)}
      disabled={p.disabled}
      onClick={p.onClick}
      data-testid={p['data-testid']}
      aria-busy={p.loading ? 'true' : undefined}
      aria-haspopup={p['aria-haspopup']}
      aria-expanded={p['aria-expanded']}
      aria-controls={p['aria-controls']}
      title={p.disabled && p.disabledReason ? p.disabledReason : undefined}
      aria-label={p['aria-label'] || (p.iconOnly && typeof p.children === 'string' ? p.children : undefined)}
    >
      {p.loading ? <span className="gm-spinner" aria-hidden="true" /> : p.icon ? <Icon name={p.icon} size={20} /> : null}
      {p.iconOnly ? null : p.children}
    </button>
  )
}
