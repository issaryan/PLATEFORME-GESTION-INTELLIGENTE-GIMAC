import { ReconciliationSummary } from '@gimac/design-system'

export default function Apercu() {
  return (
    <div style={{ maxWidth: 900 }}>
      <ReconciliationSummary
        day="27/09/2026"
        institution="Établissement de paiement A"
        status="warn"
        statusLabel="écarts en cours"
        sources={[
          { name: 'État émetteur (institution)', imported: true, rows: 18422, hash: '3f9a1c7e2b4d8f01a6c5e9b2d7f4a1c83e6b9d2f5a8c1e4b7d0f3a6c9e2b5d8f' },
          { name: 'État acquéreur (institution)', imported: true, rows: 9915, hash: '9b2d7f4a1c83e6b9d2f5a8c1e4b7d0f3a6c9e2b5d8f3f9a1c7e2b4d8f01a6c5e' },
          { name: 'État de la compensation', imported: false },
        ]}
        gaps={[
          { type: 'Retrait non comptabilisé émetteur', count: 3, amount: 375000 },
          { type: 'Paiement en double', count: 1, amount: 48500 },
          { type: 'Écart de montant', count: 0, amount: 0 },
        ]}
      />
    </div>
  )
}
