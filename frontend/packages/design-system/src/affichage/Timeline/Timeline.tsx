import type { TimelineEvent } from '../../lib/types'

export interface TimelineProps {
  events?: TimelineEvent[]
}

// Chronologie verticale (§7.3).
export function Timeline(p: TimelineProps) {
  return (
    <ol className="gm-timeline">
      {(p.events || []).map((e, i) => (
        <li key={i}>
          <div>{e.event}</div>
          <div className="gm-timeline-meta">
            <span>{e.at}</span>
            {e.actor ? <span>{e.actor}</span> : null}
            {e.correlationId ? <span className="gm-mono">{e.correlationId}</span> : null}
          </div>
        </li>
      ))}
    </ol>
  )
}
