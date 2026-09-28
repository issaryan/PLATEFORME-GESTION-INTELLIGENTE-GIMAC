import { Tag } from '@gimac/design-system'

export default function Apercu() {
  return (
    <div className="gm-row">
      <Tag>Banque</Tag>
      <Tag>Adhésion indirecte</Tag>
      <Tag onRemove={() => {}}>Pays : Gabon</Tag>
      <Tag onRemove={() => {}}>Période : ce mois</Tag>
    </div>
  )
}
