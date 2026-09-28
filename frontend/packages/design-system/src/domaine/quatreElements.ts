import type { IconName } from '../icones/icons'

// Règle des quatre éléments (responsable, preuve, horodatage, validation), partagée par
// OperationNode et FourElementsChecklist.
export interface FourElements {
  owner?: boolean | string
  proof?: boolean | string
  timestamp?: boolean | string
  validation?: boolean | string
}

export type FourElementKey = keyof FourElements

export const ELEMENTS: ReadonlyArray<readonly [FourElementKey, string, IconName]> = [
  ['owner', 'Responsable', 'user'],
  ['proof', 'Preuve', 'file-text'],
  ['timestamp', 'Horodatage', 'clock'],
  ['validation', 'Validation', 'shield-check'],
]
