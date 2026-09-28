import { SuspensCard } from '@gimac/design-system'

export default function Apercu() {
  return (
    <div style={{ maxWidth: 720 }}>
      <SuspensCard
        id="SUS-2026-01187"
        type="Retrait non comptabilisé émetteur"
        ageDays={7}
        status="warn"
        statusLabel="en conciliation"
        day="21/09/2026"
        institution="Établissement de paiement A"
        pivots={['RRN 826412345678', 'STAN 004512', 'TERM 00A1B2']}
        amount={125000}
        owner="Opérateur de rapprochement"
        attestation="attestation-SUS-2026-01187.pdf"
      />
    </div>
  )
}
