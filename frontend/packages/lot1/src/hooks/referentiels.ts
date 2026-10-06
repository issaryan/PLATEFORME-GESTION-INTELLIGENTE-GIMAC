import { useMutation, useQueryClient } from '@tanstack/react-query'
import type { ApiError } from '@gimac/socle'
import { useServices } from '../services/ServicesProvider'
import type { GrapheReferentiel, SaisieEtape, SaisieOperation } from '../modele/homologation'
import type { Telechargement } from '../modele/certification'
import { useEffet, useLecture } from './base'
import { cles } from './cles'

// Hooks de données de DES-12 (HOM-01 à HOM-03). Contrôle à blanc et simulation sont des lectures sans effet.
export const useReferentiels = () => useLecture(cles.referentiels(), (s) => s.homologation.listerReferentiels())
export const useReferentiel = (id: string) => useLecture(cles.referentiel(id), (s) => s.homologation.lireReferentiel(id), { enabled: !!id })
export const useOperationsReferentiel = (id: string | null) =>
  useLecture(cles.referentiel(id ?? '', 'operations'), (s) => s.homologation.operationsReferentiel(id ?? ''), { enabled: !!id })
export const useControleReferentiel = (id: string) => useLecture(cles.referentiel(id, 'controle'), (s) => s.homologation.controlerReferentiel(id))
export const useSimulationMigration = (id: string) => useLecture(cles.referentiel(id, 'simulation'), (s) => s.homologation.simulerMigration(id))
export const useLiaisonsImpactees = (versionId: string) =>
  useLecture(cles.referentiel(versionId, 'impact'), (s) => s.homologation.liaisonsImpactees(versionId))
export const useHistoriqueReferentiel = (id: string) => useLecture(cles.referentiel(id, 'historique'), (s) => s.homologation.historiqueReferentiel(id))

/** Migration des liaisons retenues (HOM-03), après confirmation dans l'onglet de simulation. */
export const useMigrerLiaisons = (id: string) =>
  useEffet((s, liaisons: string[]) => s.homologation.migrerLiaisons(id, liaisons), [['homologation']])

// Modélisateur de processus (D-77, §6) : graphe étendu, catalogue et écritures du brouillon. Chaque écriture
// invalide la version (graphe, opérations, contrôle) et l'arbre des versions ; le service décide de tout.
export const useGrapheReferentiel = (id: string) => useLecture(cles.referentiel(id, 'graphe'), (s) => s.homologation.grapheReferentiel(id), { enabled: !!id })
/** Catalogue du modélisateur ; avec la version (D-89), ses étapes en plus (relues à chaque écriture de la version). */
export const useCatalogueReferentiels = (versionId?: string) => useLecture(versionId ? cles.referentiel(versionId, 'catalogue') : cles.catalogueReferentiels(),
  (s) => s.homologation.catalogueReferentiels(versionId), { staleTime: versionId ? 0 : 5 * 60_000 })

const versionEtArbre = (id: string) => [cles.referentiel(id), cles.referentiels()]
/** Export CSV (D-89) : lecture, sans invalidation. */
export function useExporterReferentiel() {
  const s = useServices()
  return useMutation<Telechargement & { nom: string }, ApiError, string>({ mutationFn: (id) => s.homologation.exporterReferentiel(id) })
}
// Étapes configurables (D-89 §2) : chaque écriture relit la version (graphe, catalogue de la version) et l'arbre.
export const useAjouterEtape = (id: string) => useEffet((s, e: SaisieEtape & { code: string }) => s.homologation.ajouterEtape(id, e), versionEtArbre(id))
export const useModifierEtape = (id: string) => useEffet((s, v: { code: string; etape: SaisieEtape }) => s.homologation.modifierEtape(id, v.code, v.etape), versionEtArbre(id))
export const useSupprimerEtape = (id: string) => useEffet((s, code: string) => s.homologation.supprimerEtape(id, code), versionEtArbre(id))
export const useOrdonnerEtapes = (id: string) => useEffet((s, codes: string[]) => s.homologation.ordonnerEtapes(id, codes), versionEtArbre(id))
export const useCreerOperation = (id: string) =>
  useEffet((s, op: SaisieOperation & { code: string }) => s.homologation.creerOperation(id, op), versionEtArbre(id))
export const useEnregistrerOperation = (id: string) =>
  useEffet((s, v: { code: string; operation: SaisieOperation }) => s.homologation.enregistrerOperation(id, v.code, v.operation), versionEtArbre(id))
export const useSupprimerOperation = (id: string) => useEffet((s, code: string) => s.homologation.supprimerOperation(id, code), versionEtArbre(id))
export const useArcReferentiel = (id: string) =>
  useEffet((s, arc: { de: string; vers: string; present: boolean }) => s.homologation.arcReferentiel(id, arc), versionEtArbre(id))
export const useDesignerPilote = (id: string) => useEffet((s, compteId: string) => s.homologation.designerPilote(id, compteId), versionEtArbre(id))
/** Positions : le graphe en cache est mis à jour sur place, sans relecture (le canevas les montre déjà). */
export function usePlacerOperations(id: string) {
  const s = useServices()
  const qc = useQueryClient()
  return useMutation<void, ApiError, { code: string; x: number; y: number }[]>({
    mutationFn: (positions) => s.homologation.placerOperations(id, positions),
    onSuccess: (_, positions) => {
      qc.setQueryData<GrapheReferentiel>(cles.referentiel(id, 'graphe'), (g) =>
        g ? { ...g, noeuds: g.noeuds.map((n) => { const p = positions.find((x) => x.code === n.code); return p ? { ...n, position: { x: p.x, y: p.y } } : n }) } : g)
    },
  })
}
