import { CoverageBar, ProgressRing } from '@gimac/design-system'

export default function Apercu() {
  return (
    <div className="gm-row" style={{ gap: 'var(--gm-space-6)', alignItems: 'flex-start' }}>
      <ProgressRing label="Certification" value={100} status="ok" statusLabel="certifiée" />
      <ProgressRing label="Mise en service" value={60} />
      <ProgressRing label="Couverture des tests" value={35} size={56} />
      <div className="gm-col" style={{ gap: 'var(--gm-space-2)', minWidth: 480 }}>
        <CoverageBar service="Retrait" level={4} />
        <CoverageBar service="Paiement" level={3} />
        <CoverageBar service="Transfert" level={1} />
        <CoverageBar service="Commerce électronique" level={0} />
      </div>
    </div>
  )
}
