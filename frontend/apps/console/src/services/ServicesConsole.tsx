import type { ReactNode } from 'react'
import { ContexteServices } from './contexte'
import type { ServicesConsole } from './ports'

// Injection des services de la console : l'application choisit l'implémentation (HTTP ou simulée).
export function ServicesConsoleProvider({ services, children }: { services: ServicesConsole; children: ReactNode }) {
  return <ContexteServices.Provider value={services}>{children}</ContexteServices.Provider>
}
