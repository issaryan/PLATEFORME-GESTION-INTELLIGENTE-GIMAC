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
