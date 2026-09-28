import { cx } from '../../lib/cx'
import { FieldWrapper } from '../FieldWrapper/FieldWrapper'
import { Icon } from '../../icones/Icon/Icon'
import { useFieldId2 } from '../../lib/useFieldId2'
import type { BaseProps, Option } from '../../lib/types'

export interface SearchOption extends Option {
  code: string
}

export interface SearchSelectProps extends BaseProps {
  id?: string
  name?: string
  label: string
  required?: boolean
  help?: string
  error?: string
  value?: string
  onChange?: (v: string) => void
  options?: SearchOption[]
  query?: string
  onQuery?: (q: string) => void
  open?: boolean
  loading?: boolean
}

// Liste avec recherche (dès 10 options), affichage code + libellé, création interdite (§7.2).
export function SearchSelect(p: SearchSelectProps) {
  const id = useFieldId2(p)
  const opts = p.options || []
  return (
    <FieldWrapper id={id} label={p.label} required={p.required} help={p.help} error={p.error} className={p.className}>
      <div className="gm-input-wrap">
        <input
          id={id}
          type="text"
          role="combobox"
          aria-expanded={p.open ? 'true' : 'false'}
          aria-controls={id + '-list'}
          aria-autocomplete="list"
          className="gm-input"
          value={p.query || ''}
          onChange={(e) => p.onQuery && p.onQuery(e.target.value)}
        />
        <span className="gm-input-suffix">{p.loading ? <span className="gm-spinner" style={{ width: 14, height: 14 }} /> : <Icon name="search" />}</span>
      </div>
      {p.open ? (
        <ul id={id + '-list'} role="listbox" className="gm-listbox">
          {opts.length ? (
            opts.map((o) => (
              <li key={o.value} role="option" aria-selected={o.value === p.value ? 'true' : 'false'} className={cx('gm-option', o.value === p.value && 'gm-option-sel')}>
                <span className="gm-mono">{o.code}</span>
                <span>{o.label}</span>
              </li>
            ))
          ) : (
            <li className="gm-option gm-option-empty">Aucune institution ne correspond. La création n'est pas possible ici.</li>
          )}
        </ul>
      ) : null}
    </FieldWrapper>
  )
}
