export interface StateMachineBarProps {
  label?: string
  states?: { key: string; label: string }[]
  current?: string
  allowed?: string[]
}

// Barre d'états : états parcourus, état courant, transitions permises (§7.6).
export function StateMachineBar(p: StateMachineBarProps) {
  const states = p.states || []
  const idx = states.findIndex((s) => s.key === p.current)
  return (
    <ol className="gm-states" aria-label={p.label || 'États'}>
      {states.map((s, i) => {
        const cls = i < idx ? 'gm-state-done' : i === idx ? 'gm-state-current' : p.allowed && p.allowed.indexOf(s.key) >= 0 ? 'gm-state-next' : ''
        return <li key={s.key} className={cls} aria-current={i === idx ? 'step' : undefined}>{s.label}</li>
      })}
    </ol>
  )
}
