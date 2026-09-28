import { DagView } from '@gimac/design-system'

export default function Apercu() {
  return (
    <DagView
      phases={[{ key: 'p1', label: 'Raccordement' }, { key: 'p2', label: 'Certification' }, { key: 'p3', label: 'Mise en service' }]}
      nodes={[
        { code: 'OP-2026-1180', label: 'Recette technique de raccordement', owner: 'GIMAC', phase: 'p1', state: 'closed', critical: true, elements: { owner: true, proof: true, timestamp: true, validation: true } },
        { code: 'OP-2026-1181', label: 'Personnalisation et clés', owner: 'Institution', phase: 'p1', state: 'closed', elements: { owner: true, proof: true, timestamp: true, validation: true } },
        { code: 'OP-2026-1186', label: 'Tests de bout en bout retrait', owner: 'Institution', phase: 'p2', state: 'closed', critical: true, deps: ['OP-2026-1180'], elements: { owner: true, proof: true, timestamp: true, validation: true } },
        { code: 'OP-2026-1187', label: 'Recette du service paiement', owner: 'Institution', phase: 'p2', state: 'eligible', critical: true, deps: ['OP-2026-1180', 'OP-2026-1181'], elements: { owner: true, proof: true, timestamp: true, validation: false } },
        { code: 'OP-2026-1190', label: 'Simulation des scénarios de litige', owner: 'GIMAC', phase: 'p2', state: 'late', deps: ['OP-2026-1181'], elements: { owner: true, proof: false, timestamp: false, validation: false } },
        { code: 'OP-2026-1195', label: 'Bascule en production', owner: 'GIMAC', phase: 'p3', state: 'locked', critical: true, deps: ['OP-2026-1186', 'OP-2026-1187', 'OP-2026-1190'], elements: {} },
      ]}
    />
  )
}
