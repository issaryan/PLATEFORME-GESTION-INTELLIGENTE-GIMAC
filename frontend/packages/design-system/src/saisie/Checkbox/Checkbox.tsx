import { cx } from '../../lib/cx'
import { useId } from 'react'
import { Icon } from '../../icones/Icon/Icon'

export interface CheckboxProps {
  name?: string
  label: string
  checked?: boolean
  onChange?: (v: boolean) => void
  disabled?: boolean
  className?: string
  /** Erreur liée à la case (charte non acceptée…), annoncée et reliée par aria-describedby (extension D-62). */
  error?: string
}

// Case carrée de 18 px, libellé cliquable (§7.2).
export function Checkbox(p: CheckboxProps) {
  const id = (p.name ? p.name + '-' : 'gm-case-') + useId().replace(/:/g, '')
  const case_ = (
    <label className={cx('gm-check', p.className)}>
      <input
        type="checkbox"
        name={p.name}
        checked={!!p.checked}
        disabled={p.disabled}
        aria-invalid={p.error ? 'true' : undefined}
        aria-describedby={p.error ? id + '-error' : undefined}
        onChange={(e) => { if (p.onChange) p.onChange(e.target.checked) }}
      />
      <span>{p.label}</span>
    </label>
  )
  if (!p.error) return case_
  return (
    <div className="gm-field">
      {case_}
      <div className="gm-field-error" id={id + '-error'} role="alert">
        <Icon name="octagon-alert" />
        {p.error}
      </div>
    </div>
  )
}
