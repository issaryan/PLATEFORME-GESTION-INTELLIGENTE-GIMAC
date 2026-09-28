import { Button, Menu } from '@gimac/design-system'

export default function Apercu() {
  return (
    <div className="gm-row" style={{ alignItems: 'flex-start', gap: 'var(--gm-space-6)' }}>
      <Menu
        label="Actions sur l'institution"
        items={[
          { key: 'open', icon: 'eye', label: 'Ouvrir en pleine page', shortcut: 'Entrée' },
          { key: 'transition', icon: 'route', label: 'Demander un changement de statut' },
          { key: 'approve', icon: 'pen-line', label: 'Approuver la transition', disabled: true, disabledReason: 'Vous êtes le demandeur de cette transition' },
          { separator: true },
          { key: 'export', icon: 'download', label: 'Exporter (contrôlé)' },
          { separator: true },
          { key: 'signal', icon: 'octagon-alert', label: 'Signaler une non-conformité', danger: true },
        ]}
      />
      <div className="gm-col" style={{ gap: 'var(--gm-space-2)' }}>
        <span className="gm-field-help">Déclencheur : un bouton « Plus » ou la colonne d'actions</span>
        <Button variant="secondary" icon="menu">Plus</Button>
      </div>
    </div>
  )
}
