import { SideNav } from '@gimac/design-system'
import logo from '@gimac/design-system/assets/logo/gimac-logo-blanc.png'

export default function Apercu() {
  return (
    <div style={{ margin: 'calc(-1 * var(--gm-space-4))' }}>
      <SideNav
        current="institutions"
        height={576}
        logo={logo}
        couche="des"
        groups={[
          { items: [{ key: 'accueil', label: 'Tableau de bord', icon: 'gauge' }] },
          { label: 'Participants', items: [{ key: 'institutions', label: 'Institutions membres', icon: 'landmark' }, { key: 'liaisons', label: 'Liaisons et parcours', icon: 'route' }] },
          { label: 'Exploitation', items: [{ key: 'tickets', label: 'Tickets', icon: 'ticket' }, { key: 'monitoring', label: 'Liaisons supervisées', icon: 'bell-ring' }, { key: 'rapprochement', label: 'Rapprochement', icon: 'scale' }, { key: 'taches', label: 'Tableaux', icon: 'kanban' }] },
          { label: 'Collaboration', items: [{ key: 'salons', label: 'Salons et conférences', icon: 'video' }] },
        ]}
      />
    </div>
  )
}
