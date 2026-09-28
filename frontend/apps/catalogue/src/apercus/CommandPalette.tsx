import { CommandPalette } from '@gimac/design-system'
import { Scene } from '../outils/Scene'

export default function Apercu() {
  return (
    <Scene hauteur={480}>
      <div className="gm-overlay gm-overlay-inline" style={{ alignItems: 'flex-start', paddingTop: 'var(--gm-space-6)' }}>
        <CommandPalette
          query="LIA-2026"
          active={0}
          results={[
            { key: 1, group: 'objets', kind: 'liaison', code: 'LIA-2026-017', label: 'Établissement de paiement A, émetteur', status: 'info', statusLabel: 'mise en service' },
            { key: 2, group: 'objets', kind: 'liaison', code: 'LIA-2026-021', label: 'Banque D, acquéreur', status: 'crit', statusLabel: 'suspendue' },
            { key: 3, group: 'objets', kind: 'ticket', code: 'TCK-000318', label: 'Liaison LIA-2026-017 injoignable', status: 'info', statusLabel: 'en cours' },
            { key: 4, group: 'ecrans', kind: 'ecran', label: 'Liaisons et parcours' },
            { key: 5, group: 'ecrans', kind: 'ecran', label: 'Liaisons supervisées' },
            { key: 6, group: 'actions', kind: 'action', label: 'Ouvrir un ticket sur LIA-2026-017', shortcut: 'Ctrl+T' },
          ]}
        />
      </div>
    </Scene>
  )
}
