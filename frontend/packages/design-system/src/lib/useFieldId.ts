import { useRef } from 'react'

let compteur = 0

// Identifiant stable d'un champ : id fourni, sinon name, sinon identifiant généré.
export function useFieldId(p: { id?: string; name?: string }): string {
  const ref = useRef<string | null>(null)
  if (ref.current === null) ref.current = p.id || p.name || `gm-${++compteur}`
  return ref.current
}
