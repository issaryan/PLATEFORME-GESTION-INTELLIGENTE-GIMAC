import { createContext, useContext, type ReactNode } from 'react'
import type { ServicesSandbox } from './ports'

// Injection des services de l'espace sandbox (D-122) : l'application choisit l'implémentation (HTTP par la passerelle,
// ou simulée sur demande explicite VITE_SERVICES=simule). Aucune page n'importe une implémentation.
const Contexte = createContext<ServicesSandbox | null>(null)

export function ServicesSandboxProvider({ services, children }: { services: ServicesSandbox; children: ReactNode }) {
  return <Contexte.Provider value={services}>{children}</Contexte.Provider>
}

export function useServicesSandbox(): ServicesSandbox {
  const s = useContext(Contexte)
  if (!s) throw new Error('useServicesSandbox doit être employé sous ServicesSandboxProvider')
  return s
}
