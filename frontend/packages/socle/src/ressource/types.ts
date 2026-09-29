import type { ReactNode } from 'react'
import type { FieldDescriptor, IconName, ResourceAction, ResourceDescriptor, Status } from '@gimac/design-system'
import type { SignPurpose } from '../reauth/webauthn'

// Types communs des ressources du lot 1 (F0 §A4 et §A5), corrigés : les extensions remplacent les
// propriétés homonymes du design system au lieu de s'y croiser (Omit), et le contexte d'action porte
// les services injectés, jamais le client HTTP.

export type ViewState = 'loading' | 'empty' | 'error' | 'ok'
export type Tone = Status
export type StatusDictionary = Record<string, { label: string; status: Tone }>

/** Transitions et actions permises, toujours calculées par le service. */
export type Permitted = { transitionsPermises: string[]; actionsPermises: string[] }

/** Fabrique d'un dictionnaire d'états : la clé i18n est « <préfixe>.<ÉTAT> ». */
export const dictionnaire = (prefixe: string, etats: Record<string, Tone>): StatusDictionary =>
  Object.fromEntries(Object.entries(etats).map(([k, status]) => [k, { label: `${prefixe}.${k}`, status }]))

export type ActionContext<T, S> = {
  row: T
  selection: T[]
  motif?: string
  assertion?: string
  services: S
  navigate: (to: string) => void
  openForm: (mode: 'create' | 'edit') => void
  openTab: (key: string) => void
  invalidate: () => void
}

export type ResourceActionLot1<T, S> = Omit<ResourceAction<T>, 'consequences' | 'run' | 'icon'> & {
  icon?: IconName | (string & {})
  /** Corps du ConfirmDialog. */
  consequences?: (row: T) => ReactNode
  /** SignDialog : objet de la réauthentification et empreinte affichée. */
  sign?: { purpose: SignPurpose; empreinte: (row: T) => string }
  run?: (ctx: ActionContext<T, S>) => Promise<unknown> | void
}

export type FormStep<T> = { key: string; labelKey: string; fields: (keyof T & string)[]; action?: string }

export type ResourceDescriptorLot1<T, S> = Omit<ResourceDescriptor<T>, 'actions' | 'fields'> & {
  fields: FieldDescriptor<T>[]
  actions: ResourceActionLot1<T, S>[]
  steps?: FormStep<T>[]
}
