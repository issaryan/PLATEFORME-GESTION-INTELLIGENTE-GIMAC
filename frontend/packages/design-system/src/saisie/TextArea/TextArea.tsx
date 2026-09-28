import { useFieldId } from '../../lib/useFieldId'
import { FieldWrapper } from '../FieldWrapper/FieldWrapper'
import { inputAttrs, type FieldProps } from '../FieldWrapper/inputAttrs'

export interface TextAreaProps extends FieldProps<string> {
  rows?: number
}

// Texte long (§7.2).
export function TextArea(p: TextAreaProps) {
  const id = useFieldId(p)
  const { inputMode: _inputMode, ...attrs } = inputAttrs(p, id)
  return (
    <FieldWrapper {...p} id={id}>
      <textarea {...attrs} rows={p.rows || 3} />
    </FieldWrapper>
  )
}
