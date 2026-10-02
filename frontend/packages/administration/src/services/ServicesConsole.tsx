import type { ReactNode } from 'react'
import { CHEMINS_CONSOLE, ContexteChemins, ContexteServices, type CheminsAdministration } from './contexte'
import type { ServicesConsole } from './ports'

// Injection des services de l'administration : l'application choisit l'implémentation (HTTP ou simulée)
// et les chemins de ses écrans (console ou département, D-69).
export function ServicesConsoleProvider({ services, chemins = CHEMINS_CONSOLE, children }: { services: ServicesConsole; chemins?: CheminsAdministration; children: ReactNode }) {
  return (
    <ContexteServices.Provider value={services}>
      <ContexteChemins.Provider value={chemins}>{children}</ContexteChemins.Provider>
    </ContexteServices.Provider>
  )
}
