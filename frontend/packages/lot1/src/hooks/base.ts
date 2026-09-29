import { useMutation, useQuery, useQueryClient, type QueryKey, type UseQueryOptions } from '@tanstack/react-query'
import type { ApiError } from '@gimac/socle'
import { useServices } from '../services/ServicesProvider'
import type { Services } from '../services/ports'

// Briques des hooks de données : seules à appeler les services injectés. Clés de requête
// [sujet, ressource, …] : le canal poussé invalide toutes les requêtes d'un sujet.

export function useLecture<R>(cle: QueryKey, lire: (s: Services) => Promise<R>, options?: Omit<UseQueryOptions<R, ApiError>, 'queryKey' | 'queryFn'>) {
  const s = useServices()
  return useQuery<R, ApiError>({ queryKey: cle, queryFn: () => lire(s), ...options })
}

/** Action à effet : exécute le service puis invalide les clés indiquées (par défaut tout le sujet). */
export function useEffet<V, R = unknown>(agir: (s: Services, v: V) => Promise<R>, invalider: QueryKey[]) {
  const s = useServices()
  const qc = useQueryClient()
  return useMutation<R, ApiError, V>({
    mutationFn: (v) => agir(s, v),
    onSuccess: () => Promise.all(invalider.map((queryKey) => qc.invalidateQueries({ queryKey }))),
  })
}
