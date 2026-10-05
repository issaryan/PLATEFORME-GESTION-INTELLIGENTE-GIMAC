import { createContext, useContext, type ComponentType, type ReactNode } from 'react'

/**
 * Widgets du tableau de bord fournis par l'application (D-69, D-79) : l'application interne y place les widgets
 * de @gimac/administration, que le lot 1 ne connaît pas : tableau d'administration des comptes (indicateurs,
 * actions, derniers actes), résumé de la supervision (services, alertes) et charges de travail en erreur.
 * Sans extension, le tableau de bord garde une section d'actions ou un raccourci vers l'écran.
 */
export type ExtensionsTableauDeBord = { administration?: ComponentType; supervision?: ComponentType; infrastructure?: ComponentType }

const Contexte = createContext<ExtensionsTableauDeBord>({})

export function ExtensionsTableauDeBordProvider({ valeur, children }: { valeur: ExtensionsTableauDeBord; children: ReactNode }) {
  return <Contexte.Provider value={valeur}>{children}</Contexte.Provider>
}

export const useExtensionsTableauDeBord = () => useContext(Contexte)
