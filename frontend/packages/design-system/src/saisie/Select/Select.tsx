import type { Option } from '../../lib/types'
import { useFieldId } from '../../lib/useFieldId'
import { FieldWrapper } from '../FieldWrapper/FieldWrapper'
import { inputAttrs, type FieldProps } from '../FieldWrapper/inputAttrs'

export interface SelectProps extends FieldProps<string> {
  options?: Option[]
}

// Liste fermée jusqu'à 10 options (§7.2).
export function Select(p: SelectProps) {
  const id = useFieldId(p)
  const { inputMode: _inputMode, maxLength: _maxLength, ...attrs } = inputAttrs(p, id)
  return (
    <FieldWrapper {...p} id={id}>
      <select {...attrs}>
        {(p.options || []).map((o) => (
          <option key={o.value} value={o.value}>{o.label}</option>
        ))}
      </select>
    </FieldWrapper>
  )
}
