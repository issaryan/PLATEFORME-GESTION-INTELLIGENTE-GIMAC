import { useLibelle } from '../../i18n/LangueProvider'
export interface StateMachineBarProps {
  label?: string
  states?: { key: string; label: string }[]
  current?: string
  allowed?: string[]
  /** États parcourus, fournis par le service ; à défaut, ceux qui précèdent l'état courant dans la liste. */
  traversed?: string[]
}

// Barre d'états : états parcourus, état courant, transitions permises (§7.6).
export function StateMachineBar(p: StateMachineBarProps) {
  const t = useLibelle()
  const states = p.states || []
  const idx = states.findIndex((s) => s.key === p.current)
  return (
    <ol className="gm-states" aria-label={p.label || t('etats.label')}>
      {states.map((s, i) => {
        const fait = p.traversed ? p.traversed.indexOf(s.key) >= 0 && i !== idx : i < idx
        const cls = fait ? 'gm-state-done' : i === idx ? 'gm-state-current' : p.allowed && p.allowed.indexOf(s.key) >= 0 ? 'gm-state-next' : ''
        return <li key={s.key} className={cls} aria-current={i === idx ? 'step' : undefined}>{s.label}</li>
      })}
    </ol>
  )
}
