import { Button, Card, KeyValueList, Panel, StatusBadge, Tag } from '@gimac/design-system'

export default function Apercu() {
  return (
    <div className="gm-row" style={{ alignItems: 'stretch' }}>
      <Card
        title="Dossier de mise en service"
        actions={<Button size="sm" variant="ghost" icon="download">Télécharger l'attestation</Button>}
        footer={[<Button key={1} variant="secondary">Refuser avec motif</Button>, <Button key={2} variant="primary" icon="pen-line">Signer le quitus</Button>]}
      >
        <KeyValueList items={[{ key: 'Liaison', value: 'LIA-2026-017', mono: true }, { key: 'Quitus technique', value: <StatusBadge status="ok" label="signé" /> }, { key: 'Quitus sécurité et conformité', value: <StatusBadge status="warn" label="en attente" /> }]} />
      </Card>
      <Panel>
        <div className="gm-col" style={{ gap: 'var(--gm-space-2)' }}>
          <strong>Filtres actifs</strong>
          <div className="gm-row">
            <Tag onRemove={() => {}}>Statut : actif</Tag>
            <Tag onRemove={() => {}}>Pays : Cameroun</Tag>
          </div>
        </div>
      </Panel>
    </div>
  )
}
