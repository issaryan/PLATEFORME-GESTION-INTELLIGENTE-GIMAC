import { useState, type KeyboardEvent } from 'react'
import { cx } from '../../lib/cx'
import { FieldWrapper } from '../FieldWrapper/FieldWrapper'
import { Icon } from '../../icones/Icon/Icon'
import { useFieldId2 } from '../../lib/useFieldId2'
import type { BaseProps, Option } from '../../lib/types'
import { useLibelle } from '../../i18n/LangueProvider'

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
  /** Liste imposée ouverte ou fermée ; à défaut, elle s'ouvre pendant la saisie et se ferme au choix. */
  open?: boolean
  loading?: boolean
}

// Liste avec recherche (dès 10 options), affichage code + libellé, création interdite (§7.2).
// Choix à la souris ou au clavier (flèches, Entrée), fermeture par Échap ; option active annoncée.
export function SearchSelect(p: SearchSelectProps) {
  const t = useLibelle()
  const id = useFieldId2(p)
  const opts = p.options || []
  const [ouverte, setOuverte] = useState(false)
  const [active, setActive] = useState(0)
  const visible = p.open ?? ouverte
  const choisir = (o: SearchOption) => {
    p.onChange?.(o.value)
    setOuverte(false)
  }
  const touche = (e: KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'ArrowDown') {
      e.preventDefault()
      setOuverte(true)
      setActive((a) => Math.min(a + 1, Math.max(opts.length - 1, 0)))
    } else if (e.key === 'ArrowUp') {
      e.preventDefault()
      setActive((a) => Math.max(a - 1, 0))
    } else if (e.key === 'Enter' && visible && opts[active]) {
      e.preventDefault()
      choisir(opts[active])
    } else if (e.key === 'Escape') {
      setOuverte(false)
    }
  }
  return (
    <FieldWrapper id={id} label={p.label} required={p.required} help={p.help} error={p.error} className={p.className}>
      <div className="gm-input-wrap">
        <input
          id={id}
          type="text"
          role="combobox"
          aria-expanded={visible ? 'true' : 'false'}
          aria-controls={id + '-list'}
          aria-autocomplete="list"
          aria-activedescendant={visible && opts[active] ? `${id}-opt-${active}` : undefined}
          className="gm-input"
          value={p.query || ''}
          onChange={(e) => {
            p.onQuery?.(e.target.value)
            setOuverte(true)
            setActive(0)
          }}
          onKeyDown={touche}
          onBlur={() => setOuverte(false)}
        />
        <span className="gm-input-suffix">{p.loading ? <span className="gm-spinner" style={{ width: 14, height: 14 }} /> : <Icon name="search" />}</span>
      </div>
      {visible ? (
        <ul id={id + '-list'} role="listbox" className="gm-listbox">
          {opts.length ? (
            opts.map((o, i) => (
              <li
                key={o.value}
                id={`${id}-opt-${i}`}
                role="option"
                aria-selected={o.value === p.value ? 'true' : 'false'}
                className={cx('gm-option', (o.value === p.value || i === active) && 'gm-option-sel')}
                // Choix avant la perte du focus du champ.
                onMouseDown={(e) => {
                  e.preventDefault()
                  choisir(o)
                }}
              >
                <span className="gm-mono">{o.code}</span>
                <span>{o.label}</span>
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
