import { InlineAlert } from '@gimac/design-system'
import type { MessageErreur } from '../outils'

// Résultat d'une action de « Mon compte » : InlineAlert du catalogue, annoncé (role status ou alert).
export type Retour = { niveau: 'info' | 'crit' | 'warn'; message: MessageErreur } | null

export function Annonce({ retour }: { retour: Retour }) {
  return retour ? <InlineAlert level={retour.niveau} title={retour.message.texte} correlationId={retour.message.correlationId} /> : null
}
