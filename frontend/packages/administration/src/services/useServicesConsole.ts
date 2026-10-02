import { useContext } from 'react'
import { ContexteChemins, ContexteServices, type CheminsAdministration } from './contexte'
import type { ServicesConsole } from './ports'

/** Services injectés de la console ; seuls les hooks de données et l'exécution des actes les emploient. */
export function useServicesConsole(): ServicesConsole {
  const s = useContext(ContexteServices)
  if (!s) throw new Error('useServicesConsole doit être employé sous ServicesConsoleProvider')
  return s
}

/** Chemins des écrans de l'administration dans l'application courante (console ou département). */
export function useChemins(): CheminsAdministration {
  return useContext(ContexteChemins)
}
