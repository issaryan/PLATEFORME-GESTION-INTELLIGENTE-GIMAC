import { createContext } from 'react'
import type { ServicesConsole } from './ports'

// Contexte d'injection des services de l'administration (implémentation HTTP ou simulée).
export const ContexteServices = createContext<ServicesConsole | null>(null)

/**
 * Chemins des écrans de l'administration (D-69, D-79) : la console les sert sous /console, l'application interne
 * sous /des/administration. « espace » est la racine de la couche (« Mon compte » : {espace}/mon-compte) ;
 * « plateforme » celle des écrans de la plateforme (supervision, infrastructure, configuration, journalisation).
 */
export type CheminsAdministration = { base: string; espace: string; plateforme?: string }
export const CHEMINS_CONSOLE: CheminsAdministration = { base: '/console', espace: '/console', plateforme: '/console/plateforme' }
export const ContexteChemins = createContext<CheminsAdministration>(CHEMINS_CONSOLE)
