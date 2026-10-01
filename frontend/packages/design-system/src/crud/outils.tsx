// Outils internes des composants universels du CRUD (non exportés par le paquet).
import type { ReactNode } from 'react'
import type { Option, Status } from '../lib/types'
import { Button, type ButtonSize } from '../actions/Button/Button'
import { StatusBadge } from '../affichage/StatusBadge/StatusBadge'
import { Money } from '../affichage/Money/Money'
import { DateTime } from '../affichage/DateTime/DateTime'
import { Tag } from '../affichage/Tag/Tag'
import type { FieldDescriptor, ResourceAction, ResourceDescriptor } from './descripteur'
import type { Traduire } from '../i18n/LangueProvider'

export interface Runner<T> {
  run: (a: ResourceAction<T>, row: T | null) => void
}

/** Valeur d'un champ d'une ligne, quelle que soit la forme de la ligne. */
export function champ<T>(row: T | null | undefined, name: string): unknown {
  return row ? (row as Record<string, unknown>)[name] : undefined
}

export function statusOf<T>(desc: ResourceDescriptor<T>, row: T): { label: string; status: Status } {
  const v = desc.statusField ? champ(row, desc.statusField) : undefined
  return (desc.statusDictionary || {})[String(v)] || { label: String(v || '').toLowerCase(), status: 'neutral' }
}

export function optionsOf<T>(f: FieldDescriptor<T>): Option[] {
  return Array.isArray(f.options) ? f.options : []
}

// Horodatages (DS-GIMAC-01 §4.3, règle 5) : heure locale d'Afrique centrale (UTC+1) au format
// 28/09/2026 14:05, l'UTC en infobulle ; une date seule au format 28/09/2026. Une valeur qui n'est pas
// une date ISO est affichée telle quelle.
const DEUX = (n: number) => String(n).padStart(2, '0')
function horodatage(value: unknown, kind: 'date' | 'datetime'): ReactNode {
  if (typeof value !== 'string' || !value) return value as ReactNode
  const jour = value.match(/^(\d{4})-(\d{2})-(\d{2})$/)
  if (jour) return <DateTime local={`${jour[3]}/${jour[2]}/${jour[1]}`} iso={value} />
  const d = new Date(value)
  if (!/^\d{4}-\d{2}-\d{2}T/.test(value) || Number.isNaN(d.getTime())) return <DateTime local={value} />
  const l = new Date(d.getTime() + 3600_000)
  const date = `${DEUX(l.getUTCDate())}/${DEUX(l.getUTCMonth() + 1)}/${l.getUTCFullYear()}`
  const utc = `${DEUX(d.getUTCDate())}/${DEUX(d.getUTCMonth() + 1)}/${d.getUTCFullYear()} ${DEUX(d.getUTCHours())}:${DEUX(d.getUTCMinutes())}`
  return <DateTime local={kind === 'date' ? date : `${date} ${DEUX(l.getUTCHours())}:${DEUX(l.getUTCMinutes())}`} utc={utc} iso={value} />
}

export function renderField<T>(f: FieldDescriptor<T>, value: unknown, row: T, desc: ResourceDescriptor<T>, t: Traduire): ReactNode {
  if (f.render) return f.render(value, row)
  if (f.kind === 'status') {
    const d = (desc.statusDictionary || {})[String(value)] || { label: String(value || '').toLowerCase(), status: 'neutral' as Status }
    return <StatusBadge status={d.status} label={d.label} />
  }
  if (f.kind === 'mono') return <span className="gm-mono">{value as ReactNode}</span>
  if (f.kind === 'money') return <Money amount={value as number} />
  if (f.kind === 'date' || f.kind === 'datetime') return horodatage(value, f.kind)
  if (f.kind === 'select' || f.kind === 'radio' || f.kind === 'searchSelect') {
    const o = optionsOf(f).filter((x) => x.value === value)[0]
    return o ? o.label : (value as ReactNode)
  }
  if (f.kind === 'checkbox' || f.kind === 'switch') return value ? t('crud.oui') : t('crud.non')
  if (f.kind === 'multiSelect') return <span className="gm-row">{((value as string[]) || []).map((v) => <Tag key={v}>{v}</Tag>)}</span>
  return value === undefined || value === null || value === '' ? <span className="gm-field-help">{t('crud.nonRenseigne')}</span> : (value as ReactNode)
}

export function allowed<T>(action: ResourceAction<T>, row: T | null, perms?: string[]): boolean {
  if (perms && perms.indexOf(action.permission) < 0) return false
  if (row && action.isAllowed && !action.isAllowed(row)) return false
  return true
}

export function actionButton<T>(a: ResourceAction<T>, row: T | null, ctx: Runner<T>, size?: ButtonSize) {
  return (
    <Button key={a.key} size={size || 'sm'} variant={a.variant || 'secondary'} icon={a.icon} onClick={() => ctx.run(a, row)}>
      {a.label}
    </Button>
  )
}
