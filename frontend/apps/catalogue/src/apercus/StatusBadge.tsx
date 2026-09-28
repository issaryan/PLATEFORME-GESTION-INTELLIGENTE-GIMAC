import { StatusBadge } from '@gimac/design-system'

export default function Apercu() {
  return (
    <div className="gm-row">
      <StatusBadge status="ok" label="conforme" />
      <StatusBadge status="ok" label="en service" />
      <StatusBadge status="warn" label="à régulariser" />
      <StatusBadge status="warn" label="en attente de tiers" />
      <StatusBadge status="crit" label="non conforme" />
      <StatusBadge status="crit" label="seuil franchi" />
      <StatusBadge status="info" label="en homologation" />
      <StatusBadge status="neutral" label="brouillon" />
      <StatusBadge status="neutral" label="archivé" icon="lock" />
    </div>
  )
}
