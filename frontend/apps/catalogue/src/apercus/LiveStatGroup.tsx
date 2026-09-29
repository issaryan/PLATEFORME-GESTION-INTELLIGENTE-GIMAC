import { LiveStatGroup } from '@gimac/design-system'

export default function Apercu() {
  return (
    <LiveStatGroup
      title="État de la plateforme"
      state="live"
      updatedAt="14:05:32"
      items={[
        { key: 'etat', label: 'État global', value: 'Dégradée', status: 'warn', statusLabel: 'dégradée' },
        { key: 'services', label: 'Services disponibles', value: '7 / 8', status: 'warn', statusLabel: '1 indisponible' },
        { key: 'alertes', label: 'Alertes actives', value: 3, status: 'warn', statusLabel: 'à traiter' },
      ]}
    />
  )
}
