import { createContext, useContext, type ReactNode } from 'react'
import type { ServiceMonCompte } from './services/ports'

// Injection du service « Mon compte » : l'application choisit l'implémentation (HTTP ou simulée) ;
// les hooks la reçoivent par ce contexte. Aucune page n'importe une implémentation.
const Contexte = createContext<ServiceMonCompte | null>(null)

export function CompteProvider({ service, children }: { service: ServiceMonCompte; children: ReactNode }) {
  return <Contexte.Provider value={service}>{children}</Contexte.Provider>
}

export function useServiceCompte(): ServiceMonCompte {
  const s = useContext(Contexte)
  if (!s) throw new Error('useServiceCompte doit être employé sous CompteProvider')
  return s
}
