import { QueryClient } from '@tanstack/react-query'

/** Client de requêtes de la console : sans persistance (DS-GIMAC-01 §8.6), un réessai. */
export function creerClientRequetes() {
  return new QueryClient({ defaultOptions: { queries: { retry: 1, staleTime: 15_000, refetchOnWindowFocus: false } } })
}
