import { OperationNode } from '@gimac/design-system'

export default function Apercu() {
  return (
    <div className="gm-row" style={{ alignItems: 'stretch' }}>
      <OperationNode code="OP-2026-1186" label="Tests de bout en bout retrait" owner="Institution" state="closed" elements={{ owner: true, proof: true, timestamp: true, validation: true }} />
      <OperationNode code="OP-2026-1187" label="Recette du service paiement" owner="Institution" state="eligible" critical elements={{ owner: true, proof: true, timestamp: true, validation: false }} />
      <OperationNode code="OP-2026-1190" label="Simulation des scénarios de litige" owner="GIMAC" state="late" elements={{ owner: true, proof: false, timestamp: false, validation: false }} />
      <OperationNode code="OP-2026-1195" label="Bascule en production" owner="GIMAC" state="locked" elements={{}} />
    </div>
  )
}
