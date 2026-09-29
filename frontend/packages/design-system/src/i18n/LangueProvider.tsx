import { createContext, useCallback, useContext, type ReactNode } from 'react'
import { LIBELLES, type CleLibelle } from './libelles'

// Langue des libellés propres aux composants (français, anglais, espagnol : zone CEMAC).
// Sans fournisseur, les composants s'affichent en français, à l'identique du système publié.
// Le système de conception ne dépend d'aucune bibliothèque d'internationalisation : les applications
// alignent ce fournisseur sur la langue choisie par l'utilisateur.
export type LangueDS = 'fr' | 'en' | 'es'

const LangueContext = createContext<LangueDS>('fr')

export function LangueProvider({ langue, children }: { langue: LangueDS; children: ReactNode }) {
  return <LangueContext.Provider value={langue}>{children}</LangueContext.Provider>
}

export function useLangue(): LangueDS {
  return useContext(LangueContext)
}

export type Traduire = (cle: CleLibelle, valeurs?: Record<string, string | number>) => string

export function traduire(langue: LangueDS, cle: CleLibelle, valeurs?: Record<string, string | number>): string {
  const brut = LIBELLES[langue][cle] ?? LIBELLES.fr[cle]
  return valeurs ? brut.replace(/\{(\w+)\}/g, (m, k: string) => (k in valeurs ? String(valeurs[k]) : m)) : brut
}

/** Renvoie t(cle, valeurs) dans la langue courante ; interpolation {nom}. */
export function useLibelle(): Traduire {
  const langue = useLangue()
  return useCallback<Traduire>((cle, valeurs) => traduire(langue, cle, valeurs), [langue])
}
