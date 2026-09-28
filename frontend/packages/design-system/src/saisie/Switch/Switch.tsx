import { cx } from '../../lib/cx'

export interface SwitchProps {
  label: string
  checked?: boolean
  onChange?: (v: boolean) => void
  disabled?: boolean
  className?: string
}

// Préférence binaire à effet immédiat (§7.2), jamais pour une action à effet.
export function Switch(p: SwitchProps) {
  return (
    <label className={cx('gm-switch', p.className)}>
      <input type="checkbox" role="switch" aria-checked={!!p.checked} checked={!!p.checked} disabled={p.disabled} onChange={(e) => { if (p.onChange) p.onChange(e.target.checked) }} />
      <span>{p.label}</span>
    </label>
  )
}
