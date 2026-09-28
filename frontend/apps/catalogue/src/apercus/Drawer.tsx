import {
  Button, CorrelationIdChip, Drawer, EmptyState, KeyValueList, SeverityBadge, SlaClock, TicketStateBar, Timeline,
} from '@gimac/design-system'
import { Scene } from '../outils/Scene'

function corps(tab: string) {
  if (tab === 'chronologie')
    return (
      <Timeline
        events={[
          { event: 'Passage en cours', at: '28/09/2026 14:05', actor: 'Chef de quart', correlationId: 'RQ-2026-0928-14' },
          { event: 'Qualification S1 (impact 4, urgence 5)', at: '28/09/2026 13:52', actor: 'Opérateur' },
          { event: "Ticket créé depuis l'alerte ALR-2026-04417", at: '28/09/2026 13:49', actor: 'Système' },
        ]}
      />
    )
  if (tab === 'pieces')
    return <EmptyState icon="file-text" title="Aucune pièce jointe" text="Les pièces déposées sur ce ticket apparaîtront ici." action={<Button variant="secondary" icon="upload">Déposer une pièce</Button>} />
  if (tab === 'audit')
    return (
      <div className="gm-col" style={{ gap: 'var(--gm-space-2)' }}>
        <span className="gm-field-help">Dernières entrées du registre</span>
        <CorrelationIdChip id="RQ-2026-0928-14" />
        <CorrelationIdChip id="RQ-2026-0928-09" />
      </div>
    )
  return (
    <div className="gm-col">
      <TicketStateBar current="EN_COURS" allowed={['EN_ATTENTE_TIERS', 'RESOLU']} />
      <SlaClock elapsedMin={135} targetMin={240} />
      <KeyValueList
        items={[
          { key: 'Institution', value: 'Établissement de paiement A' },
          { key: 'Liaison', value: 'LIA-2026-017', mono: true },
          { key: 'Sévérité', value: <SeverityBadge level="S1" label="critique" /> },
          { key: 'Responsable', value: 'Chef de quart' },
        ]}
      />
    </div>
  )
}

export default function Apercu() {
  return (
    <Scene hauteur={580}>
      <Drawer
        inline
        type="Ticket, incident"
        reference="TCK-000318"
        status="info"
        statusLabel="en cours"
        title="Liaison LIA-2026-017 injoignable"
        render={corps}
        actions={[
          <Button key={1} variant="secondary">Mettre en attente de tiers</Button>,
          <Button key={2} variant="primary" icon="pen-line">Résoudre l'incident</Button>,
        ]}
      />
    </Scene>
  )
}
