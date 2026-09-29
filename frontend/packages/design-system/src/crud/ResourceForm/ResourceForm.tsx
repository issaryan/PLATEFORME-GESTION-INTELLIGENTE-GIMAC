import { useState, type ReactNode } from 'react'
import { cx } from '../../lib/cx'
import type { ApiError, Option } from '../../lib/types'
import { Button } from '../../actions/Button/Button'
import { Link } from '../../actions/Link/Link'
import { TextField } from '../../saisie/TextField/TextField'
import { TextArea } from '../../saisie/TextArea/TextArea'
import { NumberField } from '../../saisie/NumberField/NumberField'
import { DateField, DateTimeField, DateRangeField } from '../../saisie/DateField/DateField'
import { Select } from '../../saisie/Select/Select'
import { SearchSelect } from '../../saisie/SearchSelect/SearchSelect'
import { MultiSelect } from '../../saisie/MultiSelect/MultiSelect'
import { Checkbox } from '../../saisie/Checkbox/Checkbox'
import { RadioGroup } from '../../saisie/RadioGroup/RadioGroup'
import { Switch } from '../../saisie/Switch/Switch'
import { FileDropzone } from '../../saisie/FileDropzone/FileDropzone'
import { Stepper } from '../../navigation/Stepper/Stepper'
import { InlineAlert } from '../../conteneurs/InlineAlert/InlineAlert'
import type { FieldDescriptor, ResourceDescriptor } from '../descripteur'
import { optionsOf } from '../outils'
import { useLibelle } from '../../i18n/LangueProvider'

export interface ResourceFormProps<T> {
  descriptor: ResourceDescriptor<T>
  mode?: 'create' | 'edit'
  /** Valeurs initiales ; les clés « <champ>_q » portent la saisie des recherches. */
  initial?: Record<string, unknown>
  /** Étapes (Stepper) quand le formulaire dépasse 8 champs. */
  steps?: { label: string; fields: string[] }[]
  step?: number
  /** Erreurs métier renvoyées par le service, par champ. */
  errors?: Record<string, string>
  serverError?: ApiError
  onSubmit?: (values: Record<string, unknown>) => void
  onCancel?: () => void
  submitLabel?: string
  confirm?: 'none' | 'confirm' | 'sign'
  embedded?: boolean
  title?: string
  /** Texte d'introduction sous le titre (effets de la soumission, rappel d'une règle du service). */
  intro?: ReactNode
}

// Création ou modification : champs inForm, erreurs mappées, étapes, brouillon (§8.3, gabarit G4).
export function ResourceForm<T extends object>(p: ResourceFormProps<T>) {
  const t = useLibelle()
  const desc = p.descriptor
  const mode = p.mode || 'create'
  const fields = desc.fields.filter((f) => f.inForm === true || f.inForm === mode)
  const [vals, setVals] = useState<Record<string, unknown>>(p.initial || {})
  const set = (n: string, v: unknown) => {
    const o = Object.assign({}, vals)
    o[n] = v
    setVals(o)
  }
  const errors = p.errors || {}
  const steps = p.steps
  const [step, setStep] = useState(p.step || 0)
  const stepFields = steps ? fields.filter((f) => (steps[step].fields || []).indexOf(f.name) >= 0) : fields
  const texte = (n: string) => (vals[n] as string | undefined) || ''

  function control(f: FieldDescriptor<T>): ReactNode {
    const value = vals[f.name] !== undefined ? vals[f.name] : f.kind === 'multiSelect' ? [] : f.kind === 'checkbox' || f.kind === 'switch' ? false : ''
    const common = {
      name: f.name,
      label: f.label,
      required: f.required,
      help: f.help,
      error: errors[f.name],
      readOnly: f.readOnly,
      onChange: (v: unknown) => set(f.name, v),
    }
    const s = value as string
    const opts: Option[] = optionsOf(f)
    switch (f.kind) {
      case 'textarea':
        return <TextArea {...common} value={s} />
      case 'number':
      case 'money':
        return <NumberField {...common} value={s} unit={f.kind === 'money' ? 'XAF' : f.unit} />
      case 'date':
        return <DateField {...common} value={s} />
      case 'datetime':
        return (
          <DateTimeField
            {...common}
            date={texte(f.name + '_date')}
            time={texte(f.name + '_time')}
            onChange={(v: { date: string; time: string }) => {
              set(f.name + '_date', v.date)
              set(f.name + '_time', v.time)
            }}
          />
        )
      case 'daterange': {
        const plage = (vals[f.name] || {}) as { from?: string; to?: string }
        return <DateRangeField {...common} from={plage.from || ''} to={plage.to || ''} />
      }
      case 'select':
        return <Select {...common} value={s} options={[{ value: '', label: t('crud.choisir') }].concat(opts)} />
      case 'searchSelect':
        return (
          <SearchSelect
            {...common}
            value={s}
            options={opts.map((o) => Object.assign({ code: o.value }, o))}
            query={texte(f.name + '_q')}
            onQuery={(q: string) => set(f.name + '_q', q)}
            open={!!vals[f.name + '_q']}
          />
        )
      case 'multiSelect':
        return <MultiSelect {...common} value={value as string[]} options={opts} />
      case 'checkbox':
        return <Checkbox label={f.label} checked={!!vals[f.name]} onChange={(v: boolean) => set(f.name, v)} />
      case 'radio':
        return <RadioGroup name={f.name} label={f.label} value={texte(f.name)} options={opts} onChange={(v: string) => set(f.name, v)} />
      case 'switch':
        return <Switch label={f.label} checked={!!vals[f.name]} onChange={(v: boolean) => set(f.name, v)} />
      case 'file':
        return <FileDropzone {...common} accept={f.accept} maxSize={f.maxSize} />
      case 'mono':
        return <TextField {...common} value={s} mono />
      default:
        return <TextField {...common} value={s} />
    }
  }

  const g = desc.label.gender === 'f' ? 'f' : 'm'
  const title = t(mode === 'create' ? (g === 'f' ? 'crud.enregistrerUne' : 'crud.enregistrerUn') : g === 'f' ? 'crud.modifierUne' : 'crud.modifierUn', { objet: desc.label.singular })
  const elision = /^[aeiouyhàâéèêîôû]/i.test(desc.label.singular)
  const soumettre = t(elision ? 'crud.enregistrerElision' : g === 'f' ? 'crud.enregistrerLa' : 'crud.enregistrerLe', { objet: desc.label.singular })
  const errList = Object.keys(errors)
  return (
    <form
      className="gm-resource gm-form"
      onSubmit={(e) => {
        e.preventDefault()
        if (p.onSubmit) p.onSubmit(vals)
      }}
      aria-labelledby="gm-form-title"
    >
      {p.embedded ? null : <h1 id="gm-form-title" className="gm-heading-display">{p.title || title}</h1>}
      {p.intro ? p.intro : null}
      {steps ? (
        <div style={{ margin: 'var(--gm-space-4) 0' }}>
          <Stepper steps={steps.map((st, i) => ({ label: st.label, state: i < step ? 'done' : i === step ? 'current' : 'todo' }))} />
        </div>
      ) : null}
      {errList.length ? (
        <InlineAlert level="crit" title={t(errList.length > 1 ? 'crud.champsACorriger' : 'crud.champACorriger', { n: errList.length })}>
          <ul style={{ margin: '4px 0 0', paddingLeft: 20 }}>
            {errList.map((k) => {
              const f = desc.fields.filter((x) => x.name === k)[0]
              return (
                <li key={k}>
                  <Link href={'#' + k}>{f ? f.label : k}</Link> : {errors[k]}
                </li>
              )
            })}
          </ul>
        </InlineAlert>
      ) : null}
      {p.serverError ? <InlineAlert level="crit" title={p.serverError.message} correlationId={p.serverError.correlationId} /> : null}
      <div className="gm-form-grid">
        {stepFields.map((f) => (
          <div key={f.name} className={cx('gm-form-field', (f.kind === 'textarea' || f.kind === 'file' || f.kind === 'multiSelect' || f.width === 2) && 'gm-form-wide')}>
            {control(f)}
          </div>
        ))}
      </div>
      <div className="gm-field-help">{t('crud.legende')}</div>
      <div className="gm-form-actions">
        <Button variant="ghost" type="button" onClick={p.onCancel}>{t('commun.annuler')}</Button>
        <span style={{ flex: 1 }} />
        {steps && step > 0 ? <Button variant="secondary" type="button" onClick={() => setStep(step - 1)}>{t('crud.etapePrecedente')}</Button> : null}
        {steps && step < steps.length - 1 ? (
          <Button variant="primary" type="button" icon="arrow-right" onClick={() => setStep(step + 1)}>{t('crud.etapeSuivante')}</Button>
        ) : (
          <Button variant="primary" type="submit" icon={p.confirm === 'sign' ? 'pen-line' : undefined}>
            {p.submitLabel || (mode === 'create' ? soumettre : t('crud.enregistrerModifs'))}
          </Button>
        )}
      </div>
    </form>
  )
}
