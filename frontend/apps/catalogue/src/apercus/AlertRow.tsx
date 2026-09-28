import { AlertRow } from '@gimac/design-system'

export default function Apercu() {
  return (
    <div className="gm-col" style={{ gap: 'var(--gm-space-2)' }} role="list">
      <AlertRow id="ALR-2026-04417" severity="S1" liaison="LIA-2026-017 · Établissement de paiement A" message="Sonde de disponibilité : aucune réponse depuis 4 min" at="28/09/2026 14:01" remainingMin={4} />
      <AlertRow id="ALR-2026-04418" severity="S2" liaison="LIA-2025-102 · Banque B" message="Taux de rejet 6,2 % sur 15 min, seuil 5 %" at="28/09/2026 13:52" remainingMin={11} />
    </div>
  )
}
