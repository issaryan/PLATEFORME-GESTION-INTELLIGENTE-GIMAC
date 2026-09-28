import { useEffect, type RefObject } from 'react'

const FOCALISABLES = 'a[href],button:not([disabled]),input:not([disabled]),select:not([disabled]),textarea:not([disabled]),[tabindex]:not([tabindex="-1"])'

// Piège de focus : premier et dernier élément focalisables, Échap ferme, retour au déclencheur à la fermeture.
export function useFocusTrap(ref: RefObject<HTMLElement | null>, open: boolean, onClose?: () => void): void {
  useEffect(() => {
    if (!open || !ref.current) return
    const opener = document.activeElement as HTMLElement | null
    const node = ref.current
    const first = node.querySelector<HTMLElement>(FOCALISABLES)
    if (first) first.focus()
    function key(e: KeyboardEvent) {
      if (e.key === 'Escape') {
        e.stopPropagation()
        if (onClose) onClose()
      }
      if (e.key === 'Tab') {
        const f = node.querySelectorAll<HTMLElement>(FOCALISABLES)
        if (!f.length) return
        const a = f[0]
        const z = f[f.length - 1]
        if (e.shiftKey && document.activeElement === a) {
          e.preventDefault()
          z.focus()
        } else if (!e.shiftKey && document.activeElement === z) {
          e.preventDefault()
          a.focus()
        }
      }
    }
    node.addEventListener('keydown', key)
    return () => {
      node.removeEventListener('keydown', key)
      if (opener && opener.focus) opener.focus()
    }
    // Comme l'original : le piège ne se réarme qu'au changement d'ouverture.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open])
}
