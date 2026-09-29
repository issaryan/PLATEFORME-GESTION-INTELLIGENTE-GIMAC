import { StateMachineBar } from '../../donnees/StateMachineBar/StateMachineBar'
import { useLibelle } from '../../i18n/LangueProvider'
import type { CleLibelle } from '../../i18n/libelles'

export type TicketState = 'NOUVEAU' | 'QUALIFIE' | 'EN_COURS' | 'EN_ATTENTE_TIERS' | 'RESOLU' | 'CLOS' | 'ROUVERT'

const TICKET_STATES: { key: TicketState; cle: CleLibelle }[] = [
  { key: 'NOUVEAU', cle: 'ticket.nouveau' },
  { key: 'QUALIFIE', cle: 'ticket.qualifie' },
  { key: 'EN_COURS', cle: 'ticket.enCours' },
  { key: 'EN_ATTENTE_TIERS', cle: 'ticket.attenteTiers' },
  { key: 'RESOLU', cle: 'ticket.resolu' },
  { key: 'CLOS', cle: 'ticket.clos' },
  { key: 'ROUVERT', cle: 'ticket.rouvert' },
]

export interface TicketStateBarProps {
  current: TicketState
  /** Transitions permises renvoyées par le service. */
  allowed?: TicketState[]
}

// Automate à sept états d'un ticket (§7.7).
export function TicketStateBar(p: TicketStateBarProps) {
  const t = useLibelle()
  return <StateMachineBar label={t('ticket.etat')} states={TICKET_STATES.map((s) => ({ key: s.key, label: t(s.cle) }))} current={p.current} allowed={p.allowed} />
}
