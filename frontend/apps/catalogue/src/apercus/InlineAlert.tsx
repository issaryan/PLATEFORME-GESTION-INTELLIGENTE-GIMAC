import { Button, InlineAlert } from '@gimac/design-system'

export default function Apercu() {
  return (
    <InlineAlert level="crit" title="La pièce a été refusée." correlationId="RQ-2026-0928-27" action={<Button size="sm" variant="secondary">Déposer à nouveau</Button>}>
      Elle contient un numéro de carte non masqué. Masquez le numéro puis déposez à nouveau.
    </InlineAlert>
  )
}
