import type { BaseProps, Option } from '../../lib/types'
import { useId } from 'react'
import { Icon } from '../../icones/Icon/Icon'
import { useLibelle } from '../../i18n/LangueProvider'

export interface CheckboxGroupProps extends BaseProps {
  label: string
  value?: string[]
  options: Option[]
  onChange?: (v: string[]) => void
  /** Lecture seule : toutes les cases désactivées (extension D-62). */
  disabled?: boolean
  /** Erreur du groupe (aucun profil choisi…), annoncée sous les cases (extension D-62). */
  error?: string
  /** Identifiant du groupe, base de l'identifiant du message d'erreur. */
  id?: string
}

// Groupe de cases avec « Tout sélectionner » à l'état indéterminé (§7.2).
export function CheckboxGroup(p: CheckboxGroupProps) {
  const t = useLibelle()
  const genere = 'gm-groupe-' + useId().replace(/:/g, '')
  const id = p.id || genere
  const val = p.value || []
  const all = !!p.options.length && p.options.every((o) => val.indexOf(o.value) >= 0)
  const some = val.some((v) => p.options.some((o) => o.value === v)) && !all
  const erreur = p.error ? id + '-error' : undefined
  return (
    <fieldset className="gm-field" style={{ border: 0, padding: 0, margin: 0 }} aria-describedby={erreur} aria-invalid={p.error ? 'true' : undefined} data-testid={p['data-testid']}>
      <legend className="gm-field-label">{p.label}</legend>
      <label className="gm-check">
        <input
          type="checkbox"
          checked={all}
          disabled={p.disabled}
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
              disabled={p.disabled}
              onChange={(e) => p.onChange && p.onChange(e.target.checked ? val.concat([o.value]) : val.filter((x) => x !== o.value))}
            />
            <span>{o.label}</span>
          </label>
        ))}
      </div>
      {p.error ? (
        <div className="gm-field-error" id={erreur} role="alert">
          <Icon name="octagon-alert" />
          {p.error}
        </div>
      ) : null}
    </fieldset>
  )
}
