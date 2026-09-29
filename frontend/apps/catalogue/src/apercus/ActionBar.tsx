import { ActionBar, Button } from '@gimac/design-system'

export default function Apercu() {
  return (
    <ActionBar label="Dépôt d'une pièce">
      <Button variant="secondary">Annuler</Button>
      <Button variant="primary" icon="upload">Déposer la pièce</Button>
    </ActionBar>
  )
}
