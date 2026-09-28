import type { ReactNode } from 'react'
import { cx } from '../../lib/cx'
import { Icon } from '../../icones/Icon/Icon'

export interface FieldWrapperProps {
  id: string
  label: string
  required?: boolean
  help?: string
  error?: string
  className?: string
  children?: ReactNode
}

// Anatomie commune des champs (§7.2) : étiquette permanente, aide, erreur liée.
export function FieldWrapper(p: FieldWrapperProps) {
  const id = p.id
  return (
    <div className={cx('gm-field', p.className)}>
      <label className="gm-field-label" htmlFor={id}>
        {p.label}
        {p.required ? <span className="gm-field-required" aria-hidden="true"> *</span> : null}
      </label>
      {p.children}
      {p.help && !p.error ? <div className="gm-field-help" id={id + '-help'}>{p.help}</div> : null}
      {p.error ? (
        <div className="gm-field-error" id={id + '-error'} role="alert">
          <Icon name="octagon-alert" />
          {p.error}
        </div>
      ) : null}
    </div>
  )
}
