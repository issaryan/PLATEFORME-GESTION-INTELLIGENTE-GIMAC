import { createContext } from 'react'
import type { ServicesConsole } from './ports'

// Contexte d'injection des services de la console (implémentation HTTP ou simulée).
export const ContexteServices = createContext<ServicesConsole | null>(null)
