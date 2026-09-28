import { FieldWrapper } from '../FieldWrapper/FieldWrapper'
import { Tag } from '../../affichage/Tag/Tag'
import { useFieldId2 } from '../../lib/useFieldId2'
import type { BaseProps, Option } from '../../lib/types'

export interface MultiSelectProps extends BaseProps {
  id?: string
  name?: string
  label: string
  required?: boolean
  help?: string
  error?: string
  value?: string[]
  onChange?: (v: string[]) => void
  options?: Option[]
  query?: string
  onQuery?: (q: string) => void
}

// Sélection multiple par jetons, retrait au clavier (Retour arrière) et à la souris (§7.2).
export function MultiSelect(p: MultiSelectProps) {
  const id = useFieldId2(p)
  const value = p.value || []
  return (
    <FieldWrapper id={id} label={p.label} required={p.required} help={p.help} error={p.error} className={p.className}>
      <div className="gm-multi" onClick={() => {}}>
        {value.map((v) => {
          const o = (p.options || []).filter((x) => x.value === v)[0]
          return (
            <Tag key={v} onRemove={() => p.onChange && p.onChange(value.filter((x) => x !== v))}>
              {o ? o.label : v}
            </Tag>
          )
        })}
        <input
          id={id}
          type="text"
          className="gm-multi-input"
          aria-label={p.label}
          value={p.query || ''}
          onChange={(e) => p.onQuery && p.onQuery(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === 'Backspace' && !e.currentTarget.value && value.length) p.onChange && p.onChange(value.slice(0, -1))
          }}
        />
      </div>
    </FieldWrapper>
  )
}
