import { useContext } from 'react'
import { ContexteServices } from './contexte'
import type { ServicesConsole } from './ports'

/** Services injectés de la console ; seuls les hooks de données et l'exécution des actes les emploient. */
export function useServicesConsole(): ServicesConsole {
  const s = useContext(ContexteServices)
  if (!s) throw new Error('useServicesConsole doit être employé sous ServicesConsoleProvider')
  return s
}
