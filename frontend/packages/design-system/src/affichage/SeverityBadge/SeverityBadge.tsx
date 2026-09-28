import type { Severity } from '../../lib/types'

export interface SeverityBadgeProps {
  level: Severity
  label?: string
}

// Sévérité S1 à S4, puce à bordure (§7.3).
export function SeverityBadge(p: SeverityBadgeProps) {
  return (
    <span className={'gm-severity gm-severity-' + p.level}>
      {p.level}
      {p.label ? ' · ' + p.label : null}
    </span>
  )
}
