import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { useServicesSandbox } from '../services/contexte'
import type { FiltreActivite, Granularite, TriCampagnes } from './modele'

// Lectures de l'écran « Activité des tests » (D-121) : clés sous ['sandbox', 'activite', …], la page précédente reste
// affichée pendant un changement de filtre (pas de flash de squelette, règle dataviz).
const K = ['sandbox', 'activite'] as const
const garder = { placeholderData: keepPreviousData }

export function useSynthese(f: FiltreActivite) {
  const { activite } = useServicesSandbox()
  return useQuery({ queryKey: [...K, 'synthese', f], queryFn: () => activite.synthese(f), ...garder })
}

export function useActivite(f: FiltreActivite, g: Granularite) {
  const { activite } = useServicesSandbox()
  return useQuery({ queryKey: [...K, 'points', f, g], queryFn: () => activite.activite(f, g), ...garder })
}

export function useParticipantsActifs(f: FiltreActivite, page: number, taille: number) {
  const { activite } = useServicesSandbox()
  return useQuery({ queryKey: [...K, 'participants', f, page, taille], queryFn: () => activite.participants(f, { page, taille }), ...garder })
}

export function useCampagnesActives(f: FiltreActivite, tri: TriCampagnes, page: number, taille: number) {
  const { activite } = useServicesSandbox()
  return useQuery({ queryKey: [...K, 'campagnes', f, tri, page, taille], queryFn: () => activite.campagnes(f, tri, { page, taille }), ...garder })
}

export function useCas(f: FiltreActivite, page: number, taille: number) {
  const { activite } = useServicesSandbox()
  return useQuery({ queryKey: [...K, 'cas', f, page, taille], queryFn: () => activite.cas(f, { page, taille }), ...garder })
}

export function useCodesRetour(f: FiltreActivite, page: number, taille: number) {
  const { activite } = useServicesSandbox()
  return useQuery({ queryKey: [...K, 'codes', f, page, taille], queryFn: () => activite.codesRetour(f, { page, taille }), ...garder })
}

export function useLiaisonCertification(id: string | null, autorise: boolean) {
  const { activite } = useServicesSandbox()
  return useQuery({ queryKey: [...K, 'liaison', id], queryFn: () => activite.liaison(id as string), enabled: !!id && autorise })
}
