import { StateMachineBar } from '../../donnees/StateMachineBar/StateMachineBar'

export type TicketState = 'NOUVEAU' | 'QUALIFIE' | 'EN_COURS' | 'EN_ATTENTE_TIERS' | 'RESOLU' | 'CLOS' | 'ROUVERT'

const TICKET_STATES: { key: TicketState; label: string }[] = [
  { key: 'NOUVEAU', label: 'nouveau' },
  { key: 'QUALIFIE', label: 'qualifié' },
  { key: 'EN_COURS', label: 'en cours' },
  { key: 'EN_ATTENTE_TIERS', label: 'en attente de tiers' },
  { key: 'RESOLU', label: 'résolu' },
  { key: 'CLOS', label: 'clos' },
  { key: 'ROUVERT', label: 'rouvert' },
]

export interface TicketStateBarProps {
  current: TicketState
  /** Transitions permises renvoyées par le service. */
  allowed?: TicketState[]
}

// Automate à sept états d'un ticket (§7.7).
export function TicketStateBar(p: TicketStateBarProps) {
  return <StateMachineBar label="État du ticket" states={TICKET_STATES} current={p.current} allowed={p.allowed} />
}
