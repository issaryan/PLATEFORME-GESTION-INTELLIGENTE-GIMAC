import type { Option } from '../../lib/types'
import { Icon } from '../../icones/Icon/Icon'

export interface RadioGroupProps {
  name: string
  label: string
  value?: string
  options?: Option[]
  onChange?: (v: string) => void
  /** Choix obligatoire : astérisque après la légende, comme les autres champs (DDR-27). */
  required?: boolean
  help?: string
  /** Message sous le groupe, relié aux boutons (DDR-03 : mode d'adhésion manquant). */
  error?: string
}

// Choix exclusif de 2 à 5 options (§7.2).
export function RadioGroup(p: RadioGroupProps) {
  const decrit = p.error ? `${p.name}-error` : p.help ? `${p.name}-help` : undefined
  return (
    <fieldset className="gm-field" style={{ border: 0, padding: 0, margin: 0 }} aria-describedby={decrit} aria-invalid={p.error ? 'true' : undefined}>
      <legend className="gm-field-label">
        {p.label}
        {p.required ? <span className="gm-field-required" aria-hidden="true"> *</span> : null}
      </legend>
      <div className="gm-row" style={{ gap: 'var(--gm-space-4)' }} role="radiogroup" aria-label={p.label} aria-required={p.required ? 'true' : undefined}>
        {(p.options || []).map((o) => (
          <label key={o.value} className="gm-check">
            <input type="radio" name={p.name} value={o.value} checked={p.value === o.value} onChange={() => { if (p.onChange) p.onChange(o.value) }} />
            <span>{o.label}</span>
          </label>
        ))}
      </div>
      {p.help && !p.error ? <div className="gm-field-help" id={`${p.name}-help`}>{p.help}</div> : null}
      {p.error ? (
        <div className="gm-field-error" id={`${p.name}-error`} role="alert">
          <Icon name="octagon-alert" />
          {p.error}
        </div>
      ) : null}
    </fieldset>
  )
}
