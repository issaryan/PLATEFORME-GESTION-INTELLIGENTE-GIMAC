import { useFieldId } from '../../lib/useFieldId'
import { FieldWrapper } from '../FieldWrapper/FieldWrapper'
import { inputAttrs, type FieldProps } from '../FieldWrapper/inputAttrs'

export interface TextFieldProps extends FieldProps<string> {
  type?: 'text' | 'email' | 'tel'
}

// Texte court (§7.2) ; variante mono pour les identifiants.
export function TextField(p: TextFieldProps) {
  const id = useFieldId(p)
  return (
    <FieldWrapper {...p} id={id}>
      <input {...inputAttrs(p, id)} type={p.type || 'text'} />
    </FieldWrapper>
  )
}
