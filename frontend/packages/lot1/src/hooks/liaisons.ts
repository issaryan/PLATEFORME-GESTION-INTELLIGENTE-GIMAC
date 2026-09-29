import type { ListQuery } from '../modele/commun'
import type { OuvertureLiaison } from '../modele/homologation'
import { useEffet, useLecture } from './base'
import { cles } from './cles'

// Hooks de données des écrans DES-08 et DES-09 : seuls points d'appel du port ServiceHomologation
// pour les liaisons, leurs opérations et les résultats du simulateur.

export const useLiaisons = (q: ListQuery, actif = true) => useLecture(cles.liaisons(q), (s) => s.homologation.listerLiaisons(q), { enabled: actif })
export const usePilotageLiaisons = (q: ListQuery) => useLecture(cles.pilotage(q), (s) => s.homologation.tableauDeBordLiaisons(q))
export const useLiaison = (id: string) => useLecture(cles.liaison(id), (s) => s.homologation.lireLiaison(id))
export const useHistoriqueLiaison = (id: string) => useLecture(cles.liaison(id, 'historique'), (s) => s.homologation.historiqueLiaison(id))
export const useGrapheLiaison = (id: string) => useLecture(cles.liaison(id, 'graphe'), (s) => s.homologation.grapheLiaison(id))
export const useCouverturesLiaison = (id: string) => useLecture(cles.liaison(id, 'couvertures'), (s) => s.homologation.couverturesLiaison(id))
export const useDernierExportPreuves = (id: string) => useLecture(cles.liaison(id, 'export'), (s) => s.homologation.dernierExportPreuves(id))
export const useEtatReprise = () => useLecture(cles.reprise(), (s) => s.homologation.etatReprise())

export const useOperationsLiaison = (liaisonId: string, filtres?: Record<string, string | boolean | undefined>) =>
  useLecture(cles.liaison(liaisonId, 'operations' + (filtres ? JSON.stringify(filtres) : '')), (s) => s.homologation.listerOperations(liaisonId, filtres))
export const useCampagnesLiaison = (liaisonId: string) => useLecture(cles.liaison(liaisonId, 'campagnes'), (s) => s.homologation.campagnesLiaison(liaisonId))
export const useScenariosInconnus = (liaisonId: string) => useLecture(cles.liaison(liaisonId, 'scenarios'), (s) => s.homologation.scenariosInconnus(liaisonId))

/** Options du champ de recherche des collaborateurs habilités (MS-IAM). */
export const useOptionsCollaborateurs = () => useLecture(['homologation', 'options', 'collaborateurs'], (s) => s.homologation.rechercherCollaborateurs(''))
/** Conducteurs de projet habilités (ROLE_CONDUCTEUR_PROJET), seuls désignables à la tête d'une liaison. */
export const useConducteurs = (actif = true) => useLecture(cles.conducteurs(), (s) => s.homologation.conducteurs(), { enabled: actif })

/** Ouverture au cadrage : la fiche et le registre de l'institution sont aussi relus (son statut change). */
export const useOuvrirLiaison = () => useEffet((s, e: OuvertureLiaison) => s.homologation.ouvrirLiaison(e), [cles.homologation, cles.participant])
export const useDesignerChefDeProjet = () =>
  useEffet((s, v: { id: string; chefDeProjetId: string; motif?: string }) => s.homologation.designerChefDeProjet(v.id, v.chefDeProjetId, v.motif), [cles.homologation])
export const useAttribuerOperation = () =>
  useEffet((s, v: { id: string; responsableId: string; motif: string }) => s.homologation.attribuerOperation(v.id, v.responsableId, v.motif), [cles.homologation])
