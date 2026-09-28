import { DecisionToTaskRow } from '@gimac/design-system'

export default function Apercu() {
  return (
    <div className="gm-col" style={{ gap: 'var(--gm-space-2)' }}>
      <DecisionToTaskRow decisionId="DEC-2026-042" decision="Relancer les trois institutions à régulariser avant le comité du 15/10." taskId="TSK-2026-0412" task="Relances documentaires, lot 3" owner="Conformité" due="10/10/2026" status="info" statusLabel="en cours" />
      <DecisionToTaskRow decisionId="DEC-2026-043" decision="Réviser le seuil de rejet de la sonde LIA-2025-102 à 4 %." owner="Chef du DES" due="02/10/2026" status="warn" statusLabel="tâche non créée" late={false} />
    </div>
  )
}
