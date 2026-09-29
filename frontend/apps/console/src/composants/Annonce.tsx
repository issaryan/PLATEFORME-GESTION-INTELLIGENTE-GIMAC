import { InlineAlert } from '@gimac/design-system'
import type { Retour } from './actes'

/** Résultat d'un acte : InlineAlert du catalogue, annoncé (role status ou alert). */
export function Annonce({ retour }: { retour: Retour }) {
  return retour ? <InlineAlert level={retour.niveau} title={retour.message.texte} correlationId={retour.message.correlationId} /> : null
}
