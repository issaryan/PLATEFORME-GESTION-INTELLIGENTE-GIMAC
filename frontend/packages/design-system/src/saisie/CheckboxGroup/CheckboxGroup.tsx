import type { BaseProps, Option } from '../../lib/types'
import { useLibelle } from '../../i18n/LangueProvider'

export interface CheckboxGroupProps extends BaseProps {
  label: string
  value?: string[]
  options: Option[]
  onChange?: (v: string[]) => void
}

// Groupe de cases avec « Tout sélectionner » à l'état indéterminé (§7.2).
export function CheckboxGroup(p: CheckboxGroupProps) {
  const t = useLibelle()
  const val = p.value || []
  const all = !!p.options.length && val.length === p.options.length
  const some = val.length > 0 && !all
  return (
    <fieldset className="gm-field" style={{ border: 0, padding: 0, margin: 0 }}>
      <legend className="gm-field-label">{p.label}</legend>
      <label className="gm-check">
        <input
          type="checkbox"
          checked={all}
          ref={(el) => {
            if (el) el.indeterminate = some
          }}
          onChange={() => p.onChange && p.onChange(all ? [] : p.options.map((o) => o.value))}
        />
        <span>{t('checkboxGroup.tout')}</span>
      </label>
      <div className="gm-col" style={{ gap: 'var(--gm-space-2)', paddingLeft: 'var(--gm-space-5)' }}>
        {p.options.map((o) => (
          <label key={o.value} className="gm-check">
            <input
              type="checkbox"
              checked={val.indexOf(o.value) >= 0}
              onChange={(e) => p.onChange && p.onChange(e.target.checked ? val.concat([o.value]) : val.filter((x) => x !== o.value))}
            />
            <span>{o.label}</span>
          </label>
        ))}
      </div>
    </fieldset>
  )
}
