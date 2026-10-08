import { keepPreviousData, useQueryClient } from '@tanstack/react-query'
import type { EtatInstance, EtatProcessus, InstanceProcessus, Processus, VueInstances } from '../modele/processus'
import type { Services } from '../services/ports'
import { useEffet, useLecture } from './base'
import { clesPilotage } from './pilotage'

// Processus d'unité (D-87) : clés sous le sujet « pilotage », car une étape crée et termine des tâches de « Mon
// activité » ; tout geste relit l'ensemble (catalogue, instances, tâches, journée). La réponse d'un acte sur une
// instance (la représentation complète) remplace aussitôt celle du cache.
export const clesProcessus = {
  tout: ['pilotage', 'processus'] as const,
  liste: (q: object) => ['pilotage', 'processus', 'liste', q] as const,
  definition: (id: string) => ['pilotage', 'processus', 'definition', id] as const,
  instances: (q: object) => ['pilotage', 'processus', 'instances', q] as const,
  instance: (id: string) => ['pilotage', 'processus', 'instance', id] as const,
  lancables: (q: object) => ['pilotage', 'processus', 'lancables', q] as const,
  demandes: (q: object) => ['pilotage', 'processus', 'demandes', q] as const,
}

/**
 * Suivi en temps réel d'une demande (D-98 §4) : relecture périodique de l'instance en cours et de « Mes demandes » (les
 * actes des intervenants changent l'état sans geste de l'initiateur) ; chaque notification relit aussi tout le sujet.
 */
export const SUIVI_DEMANDE_MS = 10_000

export const useCatalogueProcessus = (q: { unite?: string; etat?: EtatProcessus; q?: string }) =>
  useLecture(clesProcessus.liste(q), (s) => s.processus.lister(q), { placeholderData: keepPreviousData })
/** D-92 : portée de l'appelant (tous domaines ou ses unités), servie par MS-PILOTAGE. */
export const usePorteeProcessus = () => useLecture(['pilotage', 'processus', 'portee'], (s) => s.processus.portee(), { staleTime: 60_000 })
export const useProcessus = (id: string | undefined) => useLecture(clesProcessus.definition(id ?? ''), (s) => s.processus.lire(id!), { enabled: !!id })
export const useInstances = (q: { vue: VueInstances; etat?: EtatInstance; page?: number; taille?: number }, actif = true) =>
  useLecture(clesProcessus.instances(q), (s) => s.processus.instances(q), { enabled: actif, placeholderData: keepPreviousData })
/** Candidats d'un intervenant (désignation hors de l'unité) : dans l'unité, puis ailleurs dans le GIMAC. */
export const useCandidatsProcessus = (unite: string | null | undefined, profil: string | null | undefined, actif = true) =>
  useLecture(['pilotage', 'processus', 'candidats', unite ?? '', profil ?? ''], (s) => s.processus.candidats({ unite: unite!, profil }), { enabled: actif && !!unite, staleTime: 60_000 })
/** Candidats à l'exécution d'une étape en cours (réaffectation). */
export const useCandidatsEtape = (instanceId: string, etape: string, actif = true) =>
  useLecture(['pilotage', 'processus', 'candidats', 'etape', instanceId, etape], (s) => s.processus.candidatsEtape(instanceId, etape), { enabled: actif, staleTime: 30_000 })
export const useInstance = (id: string | undefined) => useLecture(clesProcessus.instance(id ?? ''), (s) => s.processus.instance(id!), {
  enabled: !!id,
  // Instance en cours ou suspendue : relue périodiquement (suivi par l'initiateur, les intervenants et le chef).
  refetchInterval: (q) => (q.state.data && (q.state.data.etat === 'EN_COURS' || q.state.data.etat === 'SUSPENDUE') ? SUIVI_DEMANDE_MS : false),
})
/** D-98 : processus que je peux lancer, groupés par unité propriétaire. */
export const useLancables = (q: { q?: string }) => useLecture(clesProcessus.lancables(q), (s) => s.processus.lancables(q), { placeholderData: keepPreviousData })
/** D-98 : « Mes demandes », relues périodiquement. */
export const useDemandes = (q: { etat?: EtatInstance; q?: string; page?: number; taille?: number }) =>
  useLecture(clesProcessus.demandes(q), (s) => s.processus.demandes(q), { placeholderData: keepPreviousData, refetchInterval: SUIVI_DEMANDE_MS })

const TOUT = [clesPilotage.tout]

/** Écriture d'une définition : la réponse remplace la définition en cache, puis tout le sujet est relu. */
export function useEcritureProcessus<V>(agir: (s: Services, v: V) => Promise<Processus>) {
  const qc = useQueryClient()
  return useEffet<V, Processus>(async (s, v) => {
    const p = await agir(s, v)
    qc.setQueryData(clesProcessus.definition(p.id), p)
    return p
  }, TOUT)
}

/** Acte sur une instance : la représentation rendue remplace celle du cache, puis tout le sujet est relu. */
export function useActeInstance<V>(agir: (s: Services, v: V) => Promise<InstanceProcessus>) {
  const qc = useQueryClient()
  return useEffet<V, InstanceProcessus>(async (s, v) => {
    const i = await agir(s, v)
    qc.setQueryData(clesProcessus.instance(i.id), i)
    return i
  }, TOUT)
}

export const useSupprimerProcessus = () => useEffet((s, id: string) => s.processus.supprimer(id), TOUT)
export const useControleProcessus = () => useEffet((s, id: string) => s.processus.controler(id), [])
