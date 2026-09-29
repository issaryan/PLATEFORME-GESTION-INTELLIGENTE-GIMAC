import type { CleLibelle } from '../i18n/libelles'
import type { IconName } from '../icones/icons'

// Règle des quatre éléments (responsable, preuve, horodatage, validation), partagée par
// OperationNode et FourElementsChecklist.
export interface FourElements {
  owner?: boolean | string | 'pending'
  proof?: boolean | string | 'pending'
  timestamp?: boolean | string | 'pending'
  validation?: boolean | string | 'pending'
}

export type FourElementKey = keyof FourElements

export const ELEMENTS: ReadonlyArray<readonly [FourElementKey, CleLibelle, IconName]> = [
  ['owner', 'elements.responsable', 'user'],
  ['proof', 'elements.preuve', 'file-text'],
  ['timestamp', 'elements.horodatage', 'clock'],
  ['validation', 'elements.validation', 'shield-check'],
]

/** Un élément est réuni s'il est vrai ou décrit ; « pending » (en attente) ne l'est pas encore. */
export const reuni = (v: FourElements[FourElementKey]): boolean => !!v && v !== 'pending'
