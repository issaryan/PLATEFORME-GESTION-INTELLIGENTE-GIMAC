import { useFieldId } from '../../lib/useFieldId'
import { FieldWrapper } from '../FieldWrapper/FieldWrapper'
import { inputAttrs, type FieldProps } from '../FieldWrapper/inputAttrs'

export interface NumberFieldProps extends FieldProps<string> {
  /** Unité en suffixe (XAF, %, min). */
  unit?: string
  min?: number
  max?: number
  step?: number
}

// Nombre avec unité en suffixe (§7.2).
export function NumberField(p: NumberFieldProps) {
  const id = useFieldId(p)
  return (
    <FieldWrapper {...p} id={id}>
      <div className="gm-input-wrap">
        <input {...inputAttrs(p, id)} type="text" inputMode="decimal" className="gm-input gm-num" />
        {p.unit ? <span className="gm-input-suffix">{p.unit}</span> : null}
      </div>
    </FieldWrapper>
  )
}
