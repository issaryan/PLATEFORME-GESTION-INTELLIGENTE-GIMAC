import type { Option } from '../../lib/types'
import { CheckboxGroup } from '../CheckboxGroup/CheckboxGroup'
import { useLibelle } from '../../i18n/LangueProvider'

export interface PermissionGroup {
  key: string
  label: string
  options: Option[]
}

export interface PermissionPickerProps {
  label: string
  groups: PermissionGroup[]
  value?: string[]
  onChange?: (v: string[]) => void
  /** Lecture seule : cases désactivées. */
  readOnly?: boolean
  error?: string
}

// Composition d'un profil (console CON-01, D-62) : un CheckboxGroup par domaine de permissions, chacun
// avec « Tout sélectionner » ; les valeurs des autres domaines sont conservées à chaque changement.
export function PermissionPicker(p: PermissionPickerProps) {
  const t = useLibelle()
  const val = p.value || []
  const total = p.groups.reduce((n, g) => n + g.options.length, 0)
  return (
    <div className="gm-col" role="group" aria-label={p.label}>
      <div className="gm-row" style={{ justifyContent: 'space-between' }}>
        <span className="gm-field-label">{p.label}</span>
        <span className="gm-field-help gm-num" role="status">{t('permissions.selection', { n: val.length, total })}</span>
      </div>
      <div className="gm-row" style={{ alignItems: 'flex-start', gap: 'var(--gm-space-6)' }}>
        {p.groups.map((g, i) => (
          <CheckboxGroup
            key={g.key}
            label={g.label}
            options={g.options}
            value={val.filter((v) => g.options.some((o) => o.value === v))}
            disabled={p.readOnly}
            error={i === 0 ? p.error : undefined}
            onChange={(choix) => p.onChange && p.onChange(val.filter((v) => !g.options.some((o) => o.value === v)).concat(choix))}
          />
        ))}
      </div>
    </div>
  )
}
