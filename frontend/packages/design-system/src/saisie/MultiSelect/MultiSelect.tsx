import { useState, type KeyboardEvent } from 'react'
import { cx } from '../../lib/cx'
import { FieldWrapper } from '../FieldWrapper/FieldWrapper'
import { Tag } from '../../affichage/Tag/Tag'
import { Avatar } from '../../affichage/Avatar/Avatar'
import { useFieldId2 } from '../../lib/useFieldId2'
import type { BaseProps, Option } from '../../lib/types'
import { useLibelle } from '../../i18n/LangueProvider'

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
  /** Propose les options pendant la saisie (liste déroulante) ; les options déjà choisies n'y figurent pas. */
  suggestions?: boolean
  /** Libellés des valeurs choisies absentes des options courantes (recherche serveur). */
  labels?: Record<string, string>
  /** Avatars des valeurs choisies absentes des options courantes (destinataires). */
  avatars?: Record<string, { name: string; src?: string | null }>
}

// Sélection multiple par jetons, retrait au clavier (Retour arrière) et à la souris (§7.2) ; avec
// suggestions, ajout à la souris ou au clavier (flèches, Entrée), fermeture par Échap.
export function MultiSelect(p: MultiSelectProps) {
  const t = useLibelle()
  const id = useFieldId2(p)
  const value = p.value || []
  const [ouverte, setOuverte] = useState(false)
  const [active, setActive] = useState(0)
  const proposees = (p.options || []).filter((o) => !value.includes(o.value))
  const visible = !!p.suggestions && ouverte
  const libelle = (v: string) => (p.options || []).find((x) => x.value === v)?.label ?? p.labels?.[v] ?? v
  const avatar = (v: string) => (p.options || []).find((x) => x.value === v)?.avatar ?? p.avatars?.[v]
  const ajouter = (o: Option) => {
    p.onChange?.([...value, o.value])
    p.onQuery?.('')
    setActive(0)
  }
  const touche = (e: KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Backspace' && !e.currentTarget.value && value.length) p.onChange?.(value.slice(0, -1))
    if (!p.suggestions) return
    if (e.key === 'ArrowDown') {
      e.preventDefault()
      setOuverte(true)
      setActive((a) => Math.min(a + 1, Math.max(proposees.length - 1, 0)))
    } else if (e.key === 'ArrowUp') {
      e.preventDefault()
      setActive((a) => Math.max(a - 1, 0))
    } else if (e.key === 'Enter' && visible && proposees[active]) {
      e.preventDefault()
      ajouter(proposees[active])
    } else if (e.key === 'Escape') {
      setOuverte(false)
    }
  }
  return (
    <FieldWrapper id={id} label={p.label} required={p.required} help={p.help} error={p.error} className={p.className}>
      <div className="gm-multi">
        {value.map((v) => (
          <Tag key={v} tronque avatar={avatar(v)} onRemove={() => p.onChange?.(value.filter((x) => x !== v))}>
            {libelle(v)}
          </Tag>
        ))}
        <input
          id={id}
          type="text"
          className="gm-multi-input"
          aria-label={p.label}
          role={p.suggestions ? 'combobox' : undefined}
          aria-expanded={p.suggestions ? (visible ? 'true' : 'false') : undefined}
          aria-controls={p.suggestions ? id + '-list' : undefined}
          aria-activedescendant={visible && proposees[active] ? `${id}-opt-${active}` : undefined}
          value={p.query || ''}
          onChange={(e) => {
            p.onQuery?.(e.target.value)
            setOuverte(true)
            setActive(0)
          }}
          onFocus={() => setOuverte(true)}
          onBlur={() => setOuverte(false)}
          onKeyDown={touche}
        />
      </div>
      {visible ? (
        <ul id={id + '-list'} role="listbox" aria-multiselectable="true" className="gm-listbox">
          {proposees.length ? (
            proposees.map((o, i) => (
              <li
                key={o.value}
                id={`${id}-opt-${i}`}
                role="option"
                aria-selected="false"
                className={cx('gm-option', i === active && 'gm-option-sel')}
                onMouseDown={(e) => {
                  e.preventDefault()
                  ajouter(o)
                }}
              >
                {o.avatar ? <Avatar name={o.avatar.name} src={o.avatar.src} size="sm" decorative /> : null}
                {o.label}
              </li>
            ))
          ) : (
            <li className="gm-option gm-option-empty">{t('searchSelect.aucun')}</li>
          )}
        </ul>
      ) : null}
    </FieldWrapper>
  )
}
