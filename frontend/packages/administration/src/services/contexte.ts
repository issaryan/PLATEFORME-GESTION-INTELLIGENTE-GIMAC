import { createContext } from 'react'
import type { ServicesConsole } from './ports'

// Contexte d'injection des services de l'administration (implémentation HTTP ou simulée).
export const ContexteServices = createContext<ServicesConsole | null>(null)

/**
 * Chemins des écrans de l'administration (D-69) : la console les sert sous /console, le département sous
 * /des/administration. « espace » est la racine de la couche (« Mon compte » : {espace}/mon-compte).
 */
export type CheminsAdministration = { base: string; espace: string }
export const CHEMINS_CONSOLE: CheminsAdministration = { base: '/console', espace: '/console' }
export const ContexteChemins = createContext<CheminsAdministration>(CHEMINS_CONSOLE)
