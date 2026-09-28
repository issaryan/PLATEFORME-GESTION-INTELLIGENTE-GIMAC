import { cx } from '../../lib/cx'
import { Icon } from '../../icones/Icon/Icon'

export type StepState = 'todo' | 'current' | 'done' | 'error'

export interface StepperProps {
  steps?: { label: string; state?: StepState }[]
}

// Assistant en étapes (§7.4).
export function Stepper(p: StepperProps) {
  return (
    <ol className="gm-stepper">
      {(p.steps || []).map((s, i) => {
        const state = s.state || 'todo'
        return (
          <li key={i} className={cx('gm-step', 'gm-step-' + state)} aria-current={state === 'current' ? 'step' : undefined}>
            <span className="gm-step-num">{state === 'done' ? <Icon name="check" size={14} /> : i + 1}</span>
            <span>{s.label}</span>
          </li>
        )
      })}
    </ol>
  )
}
