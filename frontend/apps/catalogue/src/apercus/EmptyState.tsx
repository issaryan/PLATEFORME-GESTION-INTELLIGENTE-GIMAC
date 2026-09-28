import { Button, EmptyState } from '@gimac/design-system'

export default function Apercu() {
  return (
    <EmptyState icon="landmark" title="Aucune institution ne correspond aux filtres" text="Élargissez la période ou retirez un filtre pour afficher des institutions membres." action={<Button variant="secondary">Réinitialiser les filtres</Button>} />
  )
}
