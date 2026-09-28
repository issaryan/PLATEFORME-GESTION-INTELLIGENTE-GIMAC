import { SeverityBadge } from '@gimac/design-system'

export default function Apercu() {
  return (
    <div className="gm-row">
      <SeverityBadge level="S1" label="critique" />
      <SeverityBadge level="S2" label="majeure" />
      <SeverityBadge level="S3" />
      <SeverityBadge level="S4" />
    </div>
  )
}
