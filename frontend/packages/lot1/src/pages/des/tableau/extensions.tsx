import { createContext, useContext, type ComponentType, type ReactNode } from 'react'

/**
 * Sections du tableau de bord fournies par l'application (D-69) : l'application du département y place
 * le tableau d'administration des comptes de @gimac/administration (indicateurs, actions, derniers actes),
 * que le lot 1 ne connaît pas. Sans extension, le tableau de bord garde sa section d'actions.
 */
export type ExtensionsTableauDeBord = { administration?: ComponentType }

const Contexte = createContext<ExtensionsTableauDeBord>({})

export function ExtensionsTableauDeBordProvider({ valeur, children }: { valeur: ExtensionsTableauDeBord; children: ReactNode }) {
  return <Contexte.Provider value={valeur}>{children}</Contexte.Provider>
}

export const useExtensionsTableauDeBord = () => useContext(Contexte)
