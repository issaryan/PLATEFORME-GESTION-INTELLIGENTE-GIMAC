import { Button, PageHeader, Tag } from '@gimac/design-system'

export default function Apercu() {
  return (
    <PageHeader
      overline="Homologation"
      title="Liaisons et parcours"
      meta={<><span>Bienvenue, Aïcha Mahamat Nour</span><Tag>Conducteur de projet</Tag></>}
      actions={<Button variant="primary" icon="plus">Ouvrir une liaison</Button>}
    />
  )
}
