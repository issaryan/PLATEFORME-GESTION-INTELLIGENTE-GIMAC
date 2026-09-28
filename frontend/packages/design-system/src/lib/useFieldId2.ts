import { useRef } from 'react'

// Compteur propre à la saisie avancée : dans le système publié, il démarre à 1000
// (identifiants gm-1001, gm-1002…) pour ne jamais croiser ceux des champs de base.
let compteur = 1000

export function useFieldId2(p: { id?: string; name?: string }): string {
  const ref = useRef<string | null>(null)
  if (ref.current === null) ref.current = p.id || p.name || `gm-${++compteur}`
  return ref.current
}
