import { createContext, useContext, useMemo, type ReactNode } from 'react'
import type { Couche } from '../http/prefixes'
import { connexionHttp } from './http'
import type { ServiceConnexion } from './modele'

// Injection du service de connexion et de la couche : l'application choisit l'implémentation (HTTP ou
// simulée) ; l'écran de connexion et la reconnexion de fin de premier accès la reçoivent par ce contexte.
type ContexteConnexion = { service: ServiceConnexion; couche: Couche }
const Contexte = createContext<ContexteConnexion | null>(null)

export function ConnexionProvider({ service, couche, children }: { service: ServiceConnexion; couche: Couche; children: ReactNode }) {
  const valeur = useMemo(() => ({ service, couche }), [service, couche])
  return <Contexte.Provider value={valeur}>{children}</Contexte.Provider>
}

const defaut: { valeur?: ContexteConnexion } = {}

/** Service de connexion et couche ; à défaut de fournisseur, la passerelle réelle et la couche « des ». */
export function useConnexion(): ContexteConnexion {
  const c = useContext(Contexte)
  if (c) return c
  defaut.valeur ??= { service: connexionHttp(), couche: 'des' }
  return defaut.valeur
}
