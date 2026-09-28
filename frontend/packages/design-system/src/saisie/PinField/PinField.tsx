import { cx } from '../../lib/cx'
import { Icon } from '../../icones/Icon/Icon'

export interface PinFieldProps {
  label?: string
  value: string
  /** Nombre de cases (6 par défaut). */
  length?: number
  help?: string
  error?: string
  onChange?: (v: string) => void
}

// Saisie d'un code court à usage unique, une case par chiffre (§7.2).
export function PinField(p: PinFieldProps) {
  const n = p.length || 6
  const v = (p.value || '').split('')
  const cells: number[] = []
  for (let i = 0; i < n; i++) cells.push(i)
  return (
    <div className="gm-field">
      <span className="gm-field-label" id="pin-label">{p.label || 'Code court de vérification'}</span>
      <div className="gm-pin" role="group" aria-labelledby="pin-label">
        {cells.map((i) => (
          <input
            key={i}
            type="text"
            inputMode="numeric"
            maxLength={1}
            className={cx('gm-input gm-pin-cell', p.error && 'gm-pin-err')}
            aria-label={'Chiffre ' + (i + 1) + ' sur ' + n}
            value={v[i] || ''}
            autoComplete={i === 0 ? 'one-time-code' : 'off'}
            onChange={() => {}}
          />
        ))}
      </div>
      {p.help && !p.error ? <div className="gm-field-help">{p.help}</div> : null}
      {p.error ? (
        <div className="gm-field-error" role="alert">
          <Icon name="octagon-alert" />
          {p.error}
        </div>
      ) : null}
    </div>
  )
}
