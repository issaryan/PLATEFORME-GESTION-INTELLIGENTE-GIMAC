import { cx } from '../../lib/cx'

export interface CheckboxProps {
  name?: string
  label: string
  checked?: boolean
  onChange?: (v: boolean) => void
  disabled?: boolean
  className?: string
}

// Case carrée de 18 px, libellé cliquable (§7.2).
export function Checkbox(p: CheckboxProps) {
  return (
    <label className={cx('gm-check', p.className)}>
      <input type="checkbox" name={p.name} checked={!!p.checked} disabled={p.disabled} onChange={(e) => { if (p.onChange) p.onChange(e.target.checked) }} />
      <span>{p.label}</span>
    </label>
  )
}
