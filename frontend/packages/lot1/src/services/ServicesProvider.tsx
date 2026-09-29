import { createContext, useContext, type ReactNode } from 'react'
import type { Services } from './ports'

// Injection des services : l'application choisit l'implémentation (HTTP ou simulée) ; les hooks de
// données la reçoivent par ce contexte. Aucun composant ni aucune page n'importe une implémentation.
const Contexte = createContext<Services | null>(null)

export function ServicesProvider({ services, children }: { services: Services; children: ReactNode }) {
  return <Contexte.Provider value={services}>{children}</Contexte.Provider>
}

export function useServices(): Services {
  const s = useContext(Contexte)
  if (!s) throw new Error('useServices doit être employé sous ServicesProvider')
  return s
}
