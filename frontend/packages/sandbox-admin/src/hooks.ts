import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { uuidV4 } from '@gimac/socle'
import type { ActeEspace, DemandeOuverture } from './modele'
import { useServicesSandbox } from './services/contexte'

// Hooks de l'espace sandbox (D-122) : clés sous ['sandbox', 'espaces', …] ; chaque acte invalide les lectures qu'il change.
const K = ['sandbox', 'espaces'] as const

export function useEspaces(apres: string | undefined, taille: number) {
  const { espaces } = useServicesSandbox()
  return useQuery({ queryKey: [...K, 'liste', apres ?? '', taille], queryFn: () => espaces.lister({ apres, taille }) })
}

export function useEspace(code: string) {
  const { espaces } = useServicesSandbox()
  return useQuery({ queryKey: [...K, code], queryFn: () => espaces.consulter(code), enabled: !!code })
}

export function useOuvrirEspace() {
  const { espaces } = useServicesSandbox()
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (v: { demande: DemandeOuverture; cle: string }) => espaces.ouvrir(v.demande, v.cle),
    onSuccess: () => qc.invalidateQueries({ queryKey: K }),
  })
}

export function useActeEspace() {
  const { espaces } = useServicesSandbox()
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (v: { code: string; acte: ActeEspace; cle: string }) => espaces.agir(v.code, v.acte, v.cle),
    onSuccess: () => qc.invalidateQueries({ queryKey: K }),
  })
}

const KS = ['sandbox', 'supervision'] as const

export function useEtatSante() {
  const { supervision } = useServicesSandbox()
  return useQuery({ queryKey: [...KS, 'sante'], queryFn: supervision.etatSante })
}

export function useElementsSupervises(page: number, taille: number) {
  const { supervision } = useServicesSandbox()
  return useQuery({ queryKey: [...KS, 'elements', page, taille], queryFn: () => supervision.elements({ page, taille }) })
}

export function useAlertesSandbox(page: number, taille: number) {
  const { supervision } = useServicesSandbox()
  return useQuery({ queryKey: [...KS, 'alertes', page, taille], queryFn: () => supervision.alertes({ page, taille }) })
}

export function useDisponibilites(page: number, taille: number) {
  const { supervision } = useServicesSandbox()
  return useQuery({ queryKey: [...KS, 'disponibilites', page, taille], queryFn: () => supervision.disponibilites({ page, taille }) })
}

/** Clé d'idempotence d'une intention : tirée à l'ouverture du dialogue, gardée pour ses nouvelles tentatives. */
export const nouvelleCle = (): string => uuidV4()
export const nouvelleCle = (): string =>
  typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function'
    ? crypto.randomUUID()
    : '10000000-1000-4000-8000-100000000000'.replace(/[018]/g, (c: string) => {
        const n = Number(c)
        const r = typeof crypto !== 'undefined' && crypto.getRandomValues
          ? crypto.getRandomValues(new Uint8Array(1))[0]
          : Math.floor(Math.random() * 256)
        return (n ^ (r & (15 >> (n / 4)))).toString(16)
      })

/** Mesures par service : relues toutes les 30 secondes, au rythme des cycles de SBX-TRACE. */
export function useServicesSupervises(page: number, taille: number) {
  const { supervision } = useServicesSandbox()
  return useQuery({ queryKey: [...KS, 'services', page, taille], queryFn: () => supervision.services({ page, taille }), refetchInterval: 30_000 })
}
