import type { ChangeEvent, HTMLAttributes } from 'react'
import { cx } from '../../lib/cx'

// Propriétés communes des champs (§7.2) ; placeholder interdit : l'étiquette est permanente.
export interface FieldProps<T = string> {
  id?: string
  name?: string
  label: string
  value?: T
  onChange?: (v: T) => void
  required?: boolean
  help?: string
  error?: string
  disabled?: boolean
  readOnly?: boolean
  autoComplete?: string
  inputMode?: HTMLAttributes<HTMLInputElement>['inputMode']
  maxLength?: number
  mono?: boolean
  className?: string
  placeholder?: never
}

// Attributs de l'élément de saisie, repris à l'identique de inputAttrs() du bundle d'origine.
export function inputAttrs(p: FieldProps<string>, id: string) {
  const by: string[] = []
  if (p.help && !p.error) by.push(id + '-help')
  if (p.error) by.push(id + '-error')
  return {
    id,
    name: p.name,
    className: cx('gm-input', p.mono && 'gm-mono'),
    value: p.value,
    disabled: p.disabled,
    readOnly: p.readOnly,
    required: p.required,
    autoComplete: p.autoComplete,
    inputMode: p.inputMode,
    maxLength: p.maxLength,
    onChange: (e: ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
      if (p.onChange) p.onChange(e.target.value)
    },
    'aria-invalid': p.error ? ('true' as const) : undefined,
    'aria-describedby': by.length ? by.join(' ') : undefined,
  }
}
