import { useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import type { ApiError } from '@gimac/socle'
import { useServices } from '../services/ServicesProvider'
import type { CreationFonction, CreationTypeProjet, GrapheReferentiel, ModificationFonction, ModificationTypeProjet, SaisieEtape, SaisieOperation } from '../modele/homologation'
import type { Telechargement } from '../modele/certification'
import { useEffet, useLecture } from './base'
import { TYPES_PROJET_REPRIS } from '../modele/commun'
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

// Types de projet et catalogue des fonctions (D-91) : lecture HOM_REFERENTIEL_READ, écriture HOM_REFERENTIEL_EDIT.
const TYPES = ['homologation', 'types-projet'] as const
const FONCTIONS = ['homologation', 'fonctions'] as const
export const useTypesProjet = (actif?: boolean, active = true) =>
  useLecture([...TYPES, actif ?? 'tous'], (s) => s.homologation.typesProjet(actif), { enabled: active, staleTime: 60_000 })
export const useCreerTypeProjet = () => useEffet((s, c: CreationTypeProjet) => s.homologation.creerTypeProjet(c), [['homologation']])
export const useModifierTypeProjet = () => useEffet((s, v: { code: string; m: ModificationTypeProjet }) => s.homologation.modifierTypeProjet(v.code, v.m), [['homologation']])
export const useFonctionsReferentiel = (actif?: boolean, active = true) =>
  useLecture([...FONCTIONS, actif ?? 'tous'], (s) => s.homologation.fonctionsReferentiel(actif), { enabled: active, staleTime: 60_000 })
export const useCreerFonction = () => useEffet((s, c: CreationFonction) => s.homologation.creerFonction(c), [['homologation']])
export const useModifierFonction = () => useEffet((s, v: { code: string; m: ModificationFonction }) => s.homologation.modifierFonction(v.code, v.m), [['homologation']])

/**
 * Fonctions du catalogue (D-91) pour l'affichage : libellé servi (une fonction créée n'a pas de clé de traduction) et
 * fonctions externes (tenues par le participant ou une institution liée). Repli sur la traduction tant que le
 * catalogue n'est pas lu.
 */
export function useFonctionsCatalogue() {
  const { t } = useTranslation()
  const c = useCatalogueReferentiels()
  return useMemo(() => {
    const l = c.data?.fonctions ?? []
    return {
      libelle: (code: string) => t(`parcours.fonction.${code}`, { defaultValue: l.find((f) => f.code === code)?.libelle ?? code }),
      externes: l.filter((f) => f.externe).map((f) => f.code),
      internes: l.filter((f) => !f.externe && f.actif !== false),
      pret: c.isSuccess,
    }
  }, [c.data, c.isSuccess, t])
}

/**
 * Types de projet proposés (D-91) : lus au catalogue servi (`proposables` : actifs dont le référentiel est publié, pour
 * le cadrage d'une liaison) ; libellé traduit d'un type repris, libellé servi sinon. Tant que la liste n'est pas servie
 * (service antérieur à D-91), les trois types repris restent proposés.
 */
export function useOptionsTypesProjet(proposables = false) {
  const { t } = useTranslation()
  const q = useTypesProjet(proposables ? true : undefined)
  // CER-05 : sans la liste des types (droit de lecture des référentiels), libellé lu au catalogue du modélisateur.
  const c = useCatalogueReferentiels()
  return useMemo(() => {
    const l = q.data ? q.data.filter((x) => !proposables || x.proposable).map((x) => ({ code: x.code, libelle: x.libelle })) : q.isError ? TYPES_PROJET_REPRIS.map((code) => ({ code, libelle: code })) : []
    return {
      options: l.map((x) => ({ value: x.code, label: t(`parcours.typeProjet.${x.code}`, { defaultValue: x.libelle }) })),
      libelle: (code: string) => t(`parcours.typeProjet.${code}`, { defaultValue: q.data?.find((x) => x.code === code)?.libelle ?? c.data?.typesProjet?.find((x) => x.code === code)?.libelle ?? code }),
      pret: q.isSuccess || q.isError,
    }
  }, [q.data, q.isError, q.isSuccess, proposables, t, c.data])
}
