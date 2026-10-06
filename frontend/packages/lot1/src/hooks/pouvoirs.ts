import { keepPreviousData, useMutation, useQueryClient } from '@tanstack/react-query'
import type { ApiError } from '@gimac/socle'
import type { EtatDemande, NouvelleDemande } from '../modele/pouvoirs'
import { useServices } from '../services/ServicesProvider'
import type { Services } from '../services/ports'
import { useLecture } from './base'

// Pouvoirs hiérarchiques (D-81) : mes pouvoirs (matrice, périmètre, unités dirigées) et mes demandes à
// l'administration. Un geste relit les pouvoirs, les demandes et « Mon activité » (équipe, tâches).
export const clesPouvoirs = {
  tout: ['pouvoirs'] as const,
  moi: () => ['pouvoirs', 'moi'] as const,
  demandes: (q: object) => ['pouvoirs', 'demandes', q] as const,
  demande: (id: string) => ['pouvoirs', 'demande', id] as const,
}

/** Mes pouvoirs ; lus une minute durant (la matrice change rarement). */
export const usePouvoirs = (actif = true) => useLecture(clesPouvoirs.moi(), (s) => s.pouvoirs.moi(), { enabled: actif, staleTime: 60_000 })
export const useMesDemandes = (q: { etat?: EtatDemande; page?: number; taille?: number }, actif = true) =>
  useLecture(clesPouvoirs.demandes(q), (s) => s.pouvoirs.demandes(q), { enabled: actif, placeholderData: keepPreviousData })
export const useDemande = (id: string | undefined) => useLecture(clesPouvoirs.demande(id ?? ''), (s) => s.pouvoirs.demande(id!), { enabled: !!id })

function useGeste<V, R>(agir: (s: Services, v: V) => Promise<R>) {
  const s = useServices()
  const qc = useQueryClient()
  return useMutation<R, ApiError, V>({
    mutationFn: (v) => agir(s, v),
    onSuccess: () => Promise.all([qc.invalidateQueries({ queryKey: clesPouvoirs.tout }), qc.invalidateQueries({ queryKey: ['pilotage'] }), qc.invalidateQueries({ queryKey: ['organisation'] })]),
  })
}

export const useDemander = () => useGeste((s, d: NouvelleDemande) => s.pouvoirs.demander(d))
export const useAnnulerDemande = () => useGeste((s, id: string) => s.pouvoirs.annuler(id))
export const useRattacherSubordonne = () => useGeste((s, v: { id: string; unite: string | null; responsableId: string | null; motif?: string | null }) => s.pouvoirs.rattacher(v.id, { unite: v.unite, responsableId: v.responsableId, motif: v.motif }))
