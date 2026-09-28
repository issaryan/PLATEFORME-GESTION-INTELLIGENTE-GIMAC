import type { Status } from '../lib/types'
import type { IconName } from './icons'

// Pictogrammes réservés aux statuts (§5.5) : ne pas réaffecter.
export const STATUS_ICON: Record<Status, IconName | null> = {
  ok: 'circle-check',
  warn: 'triangle-alert',
  crit: 'octagon-alert',
  info: 'info',
  neutral: null,
}
