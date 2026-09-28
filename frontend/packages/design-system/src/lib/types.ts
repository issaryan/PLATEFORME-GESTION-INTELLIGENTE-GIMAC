// Types transverses du catalogue (DS-GIMAC-01 chapitre 7).
export type Status = 'ok' | 'warn' | 'crit' | 'info' | 'neutral'
export type Severity = 'S1' | 'S2' | 'S3' | 'S4'

export interface Option {
  value: string
  label: string
}

// Propriétés communes exposées par tous les composants (§7 : className et data-testid).
export interface BaseProps {
  className?: string
  'data-testid'?: string
}

export interface ApiError {
  code?: string
  message: string
  correlationId?: string
}

export interface KeyValueItem {
  key: string
  value: import('react').ReactNode
  mono?: boolean
}

export interface TimelineEvent {
  event: string
  at: string
  actor?: string
  correlationId?: string
}
