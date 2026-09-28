import { TreeView } from '@gimac/design-system'

export default function Apercu() {
  return (
    <div style={{ maxWidth: 520 }}>
      <TreeView
        label="Référentiels d'opérations"
        expanded={['tronc', 'emetteur']}
        selected="em-2"
        nodes={[
          { key: 'tronc', label: 'Tronc commun', icon: 'route', badge: { status: 'ok', label: 'v3.2 active' }, children: [
            { key: 't-1', code: 'OP-T01', label: 'Recette technique de raccordement', count: 4 },
            { key: 't-2', code: 'OP-T02', label: 'Certification sécurité', count: 6 },
            { key: 't-3', code: 'OP-T03', label: 'Bascule en production', count: 2 }] },
          { key: 'emetteur', label: 'Branche « émetteur »', icon: 'route', badge: { status: 'info', label: 'v3.3 brouillon' }, children: [
            { key: 'em-1', code: 'OP-E01', label: 'Personnalisation et clés', count: 3 },
            { key: 'em-2', code: 'OP-E02', label: 'Scénarios de litige émetteur', count: 5 },
            { key: 'em-3', code: 'OP-E03', label: 'Simulation des autorisations', count: 4 }] },
          { key: 'acquereur', label: 'Branche « acquéreur »', icon: 'route', badge: { status: 'ok', label: 'v3.2 active' }, children: [{ key: 'ac-1', code: 'OP-A01', label: 'Terminaux et TPE', count: 3 }] },
        ]}
      />
    </div>
  )
}
