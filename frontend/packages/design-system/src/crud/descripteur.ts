// Descripteur de ressource (DS-GIMAC-01 §8.2) : chaque agrégat métier est décrit une seule fois ;
// les quatre composants universels du CRUD le lisent et ne connaissent rien de la ressource.
import type { ReactNode } from 'react'
import type { ApiError, Option, Status, TimelineEvent } from '../lib/types'
import type { IconName } from '../icones/icons'

export type FieldKind =
  | 'text' | 'mono' | 'textarea' | 'number' | 'money' | 'date' | 'datetime' | 'daterange'
  | 'select' | 'searchSelect' | 'multiSelect' | 'checkbox' | 'radio' | 'switch' | 'file' | 'status' | 'custom'

export interface FieldDescriptor<T = Record<string, unknown>> {
  name: keyof T & string
  label: string
  kind: FieldKind
  required?: boolean
  help?: string
  options?: Option[] | (() => Promise<Option[]>)
  inList?: boolean
  inDetail?: boolean
  inForm?: boolean | 'create' | 'edit'
  /** Largeur de colonne en liste ; 2 élargit le champ dans le formulaire. */
  width?: number
  sortable?: boolean
  filterable?: boolean
  readOnly?: boolean
  /** Permission requise pour voir le champ. */
  permission?: string
  unit?: string
  accept?: string
  maxSize?: string
  render?: (value: unknown, row: T) => ReactNode
}

export interface ActionContext<T = Record<string, unknown>> {
  row?: T | null
  descriptor: ResourceDescriptor<T>
}

export interface ResourceAction<T = Record<string, unknown>> {
  key: string
  label: string
  icon?: IconName
  variant?: 'primary' | 'secondary' | 'danger'
  scope: 'row' | 'bulk' | 'page'
  permission: string
  /** ConfirmDialog (« confirm ») ou SignDialog (« sign »). */
  confirm?: 'none' | 'confirm' | 'sign'
  /** Motif obligatoire. */
  reason?: boolean
  consequences?: string[]
  /** Reflète les transitions permises renvoyées par le service. */
  isAllowed?: (row: T) => boolean
  run?: (ctx: ActionContext<T>) => Promise<void> | void
}

export interface ResourceDescriptor<T = Record<string, unknown>> {
  key: string
  label: { singular: string; plural: string; gender: 'm' | 'f' }
  idField: keyof T & string
  titleField: keyof T & string
  statusField?: keyof T & string
  endpoints: { list: string; get: string; create?: string; update?: string; delete?: string; archive?: string }
  fields: FieldDescriptor<T>[]
  actions: ResourceAction<T>[]
  /** Permission requise pour voir l'onglet (et déclencher ses lectures). */
  tabs?: { key: string; label: string; permission?: string; render?: (row: T) => ReactNode }[]
  timeline?: (row: T) => string
  statusDictionary?: Record<string, { label: string; status: Status }>
  permissions: { list: string; read: string; create?: string; update?: string; delete?: string }
  /** Sujet du canal poussé rafraîchissant les listes. */
  liveTopic?: string
}

export interface ListQuery {
  page: number
  pageSize: number
  q?: string
  sort?: { key: string; dir: 'asc' | 'desc' }
  filters?: Record<string, string>
}

export type ResourceState = 'loading' | 'empty' | 'error' | 'ok'
export type { ApiError, TimelineEvent }
