import type { LiveEvent } from './live'

// Accusés de lecture de la messagerie (D-88, docs/lot1/accuses-lecture.md §2 et §4) : la passerelle remet
// `collaboration.conversation-read.v1` aux autres membres connectés, enveloppe
// { id, type, subject: conversationId, topic: "collaboration", acteur: lecteurId, luJusquA }. L'événement ne
// relit ni la liste ni le compteur de non-lus : il est remis aux fils ouverts qui s'y sont abonnés, qui mettent
// à jour leurs coches. La messagerie simulée diffuse les siens par le même chemin.

export const ACCUSE_LECTURE = 'collaboration.conversation-read.v1'

/** Le lecteur a lu tous les messages de la conversation envoyés jusqu'à `luJusquA` inclus. */
export type AccuseLecture = { conversationId: string; lecteurId: string; luJusquA: string }

const abonnes = new Set<(a: AccuseLecture) => void>()

/** Abonnement aux accusés de lecture reçus ; rend la fonction de désabonnement. */
export function surAccuseLecture(rappel: (a: AccuseLecture) => void): () => void {
  abonnes.add(rappel)
  return () => {
    abonnes.delete(rappel)
  }
}

export function diffuserAccuseLecture(a: AccuseLecture): void {
  abonnes.forEach((f) => f(a))
}

/** Accusé porté par un événement du canal, null pour tout autre type ou une enveloppe incomplète. */
export function accuseDe(ev: LiveEvent): AccuseLecture | null {
  if (ev.type !== ACCUSE_LECTURE || !ev.subject || !ev.acteur || !ev.luJusquA) return null
  return { conversationId: ev.subject, lecteurId: ev.acteur, luJusquA: ev.luJusquA }
}
