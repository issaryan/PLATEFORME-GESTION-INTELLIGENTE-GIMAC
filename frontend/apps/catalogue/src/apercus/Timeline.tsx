import { Timeline } from '@gimac/design-system'

export default function Apercu() {
  return (
    <Timeline
      events={[
        { event: 'Transition approuvée : en homologation vers actif', at: '28/09/2026 14:05', actor: 'Chef du DES', correlationId: 'RQ-2026-0928-14' },
        { event: 'Demande de changement de statut, motif : dossier de mise en service complet', at: '27/09/2026 09:12', actor: 'Homologation', correlationId: 'RQ-2026-0927-3' },
        { event: 'Pièce légale déposée : DOC-2026-7B12', at: '22/09/2026 16:40', actor: 'Correspondant' },
      ]}
    />
  )
}
