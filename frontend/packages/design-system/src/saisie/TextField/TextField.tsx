import { useFieldId } from '../../lib/useFieldId'
import { FieldWrapper } from '../FieldWrapper/FieldWrapper'
import { inputAttrs, type FieldProps } from '../FieldWrapper/inputAttrs'

export interface TextFieldProps extends FieldProps<string> {
  /** « password » : saisie masquée d'un secret en écriture seule, jamais préremplie (extension D-62). */
  type?: 'text' | 'email' | 'tel' | 'password'
}

// Texte court (§7.2) ; variante mono pour les identifiants.
export function TextField(p: TextFieldProps) {
  const id = useFieldId(p)
  return (
    <FieldWrapper {...p} id={id}>
      <input {...inputAttrs(p, id)} type={p.type || 'text'} autoComplete={p.type === 'password' ? p.autoComplete || 'new-password' : p.autoComplete} />
    </FieldWrapper>
  )
}
