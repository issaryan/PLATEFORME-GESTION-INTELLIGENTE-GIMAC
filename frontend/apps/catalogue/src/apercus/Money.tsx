import { DateTime, Money } from '@gimac/design-system'

export default function Apercu() {
  return (
    <div className="gm-row" style={{ gap: 'var(--gm-space-6)' }}>
      <Money amount={1250000} />
      <Money amount={48500} currency="XAF" />
      <DateTime local="28/09/2026 14:05" utc="28/09/2026 13:05" iso="2026-09-28T13:05:00Z" />
      <DateTime local="28/09/2026 14:01" relative="il y a 4 min" utc="28/09/2026 13:01" iso="2026-09-28T13:01:00Z" />
    </div>
  )
}
