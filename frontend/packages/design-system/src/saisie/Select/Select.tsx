import type { Option } from '../../lib/types'
import { useFieldId } from '../../lib/useFieldId'
import { FieldWrapper } from '../FieldWrapper/FieldWrapper'
import { inputAttrs, type FieldProps } from '../FieldWrapper/inputAttrs'

export interface SelectProps extends FieldProps<string> {
  options?: Option[]
}

/** Options consécutives d'un même groupe réunies (optgroup) ; une option sans groupe reste seule. */
function grouper(options: Option[]): { group?: string; options: Option[] }[] {
  const l: { group?: string; options: Option[] }[] = []
  for (const o of options) {
    const dernier = l.at(-1)
    if (dernier && o.group && dernier.group === o.group) dernier.options.push(o)
    else l.push({ group: o.group, options: [o] })
  }
  return l
}

// Liste fermée jusqu'à 10 options (§7.2) ; options groupées sous un titre (optgroup) si elles portent un groupe.
export function Select(p: SelectProps) {
  const id = useFieldId(p)
  const { inputMode: _inputMode, maxLength: _maxLength, ...attrs } = inputAttrs(p, id)
  const option = (o: Option) => <option key={o.value} value={o.value}>{o.label}</option>
  return (
    <FieldWrapper {...p} id={id}>
      <select {...attrs}>
        {grouper(p.options || []).map((g, i) => (g.group ? <optgroup key={`${g.group}-${i}`} label={g.group}>{g.options.map(option)}</optgroup> : g.options.map(option)))}
      </select>
    </FieldWrapper>
  )
}
