import { Button, PageHeader, StatusBadge } from '@gimac/design-system'

export default function Apercu() {
  return (
    <PageHeader
      title="Liaisons et parcours"
      subtitle="Les liaisons en cours d'homologation et de mise en service, avec leur phase et leurs couvertures."
      user={{ name: 'Aïcha Mahamat Nour', roles: ['Conducteur de projet'] }}
      meta={<StatusBadge status="info" label="en homologation" />}
      actions={<Button variant="primary" icon="plus">Ouvrir une liaison</Button>}
    />
  )
}
