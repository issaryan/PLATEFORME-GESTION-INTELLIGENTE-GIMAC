import { createContext, useContext } from 'react'

// Barre latérale de l'application réduite aux pictogrammes (D-84, docs/lot1/messagerie-espace.md) : un écran qui
// a besoin de largeur (la messagerie « élargie ») la réduit le temps de son affichage, puis la rend à l'état
// précédent. Fournie par l'enveloppe ; hors enveloppe ou en navigation en tiroir (moins de 1 280 px), inactive.

export type BarreLaterale = {
  /** Barre affichée en mode icônes. */
  reduite: boolean
  /** Réduit (true) ou rétablit (false) la barre. */
  reduire: (v: boolean) => void
  /** Vrai si la barre est affichée à côté du contenu (pas en tiroir) : le mode icônes a un sens. */
  active: boolean
}

const INACTIVE: BarreLaterale = { reduite: false, reduire: () => undefined, active: false }
export const ContexteBarreLaterale = createContext<BarreLaterale>(INACTIVE)

export const useBarreLaterale = () => useContext(ContexteBarreLaterale)
