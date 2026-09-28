import type { Option } from '../../lib/types'

export interface RadioGroupProps {
  name: string
  label: string
  value?: string
  options?: Option[]
  onChange?: (v: string) => void
}

// Choix exclusif de 2 à 5 options (§7.2).
export function RadioGroup(p: RadioGroupProps) {
  return (
    <fieldset className="gm-field" style={{ border: 0, padding: 0, margin: 0 }}>
      <legend className="gm-field-label">{p.label}</legend>
      <div className="gm-row" style={{ gap: 'var(--gm-space-4)' }}>
        {(p.options || []).map((o) => (
          <label key={o.value} className="gm-check">
            <input type="radio" name={p.name} value={o.value} checked={p.value === o.value} onChange={() => { if (p.onChange) p.onChange(o.value) }} />
            <span>{o.label}</span>
          </label>
        ))}
      </div>
    </fieldset>
  )
}
