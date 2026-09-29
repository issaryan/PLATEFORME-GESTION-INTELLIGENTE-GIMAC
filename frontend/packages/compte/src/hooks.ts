import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { ApiError, CleSecurite } from '@gimac/socle'
import { useServiceCompte } from './contexte'
import type { ActionLibreService, FichePersonnelle, Moi, PremiereConfiguration, SessionCompte } from './modele'

// Hooks de données de « Mon compte » : seuls points d'appel du port. Clés [compte, ressource].
export const clesCompte = {
  tout: ['compte'] as const,
  moi: ['compte', 'moi'] as const,
  sessions: ['compte', 'sessions'] as const,
  cles: ['compte', 'cles'] as const,
}

export const useMoi = () => {
  const s = useServiceCompte()
  return useQuery<Moi, ApiError>({ queryKey: clesCompte.moi, queryFn: () => s.moi() })
}
export const useMesSessions = () => {
  const s = useServiceCompte()
  return useQuery<SessionCompte[], ApiError>({ queryKey: clesCompte.sessions, queryFn: () => s.sessions() })
}
export const useMesCles = () => {
  const s = useServiceCompte()
  return useQuery<CleSecurite[], ApiError>({ queryKey: clesCompte.cles, queryFn: () => s.cles() })
}

/** Action à effet : service puis invalidation de tout « Mon compte ». */
function useEffetCompte<V, R>(agir: (v: V) => Promise<R>) {
  const qc = useQueryClient()
  return useMutation<R, ApiError | Error, V>({ mutationFn: agir, onSuccess: () => qc.invalidateQueries({ queryKey: clesCompte.tout }) })
}

export const usePremiereConfiguration = () => {
  const s = useServiceCompte()
  return useEffetCompte((p: PremiereConfiguration) => s.premiereConfiguration(p))
}
export const useModifierFiche = () => {
  const s = useServiceCompte()
  return useEffetCompte((f: FichePersonnelle) => s.modifier(f))
}
/** Inscrit l'action (mot de passe, second facteur) puis reconnecte : le fournisseur d'identité l'exécute. */
export const useActionLibreService = () => {
  const s = useServiceCompte()
  return useEffetCompte(async (a: ActionLibreService) => {
    await s.demanderAction(a)
    await s.reconnecter()
  })
}
export const useReconnecter = () => {
  const s = useServiceCompte()
  return useEffetCompte(() => s.reconnecter())
}
export const useFermerSession = () => {
  const s = useServiceCompte()
  return useEffetCompte((id: string) => s.fermerSession(id))
}
export const useFermerSessions = () => {
  const s = useServiceCompte()
  return useEffetCompte(() => s.fermerSessions())
}
export const useEnrolerCle = () => {
  const s = useServiceCompte()
  return useEffetCompte((nom: string) => s.enrolerCle(nom))
}
export const useSupprimerCle = () => {
  const s = useServiceCompte()
  return useEffetCompte((id: string) => s.supprimerCle(id))
}
