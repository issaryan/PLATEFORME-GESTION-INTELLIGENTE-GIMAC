import { useCallback, useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { keepPreviousData, useMutation, useQuery, useQueryClient, type QueryKey, type UseQueryOptions } from '@tanstack/react-query'
import type { EtatDemande } from '@gimac/lot1'
import { aPermission, usePermissions, type ApiError } from '@gimac/socle'
import { useServicesConsole } from '../services/useServicesConsole'
import type { ServicesConsole } from '../services/ports'
import type { FiltreComptes, FiltreJournal } from '../modele/comptes'
import type { EtatFlux, ImpulsionPlateforme } from '../modele/plateforme'
import type { DemandeGeste, Geste } from '../modele/infrastructure'

// Hooks de données de la console : seuls points d'appel des ports injectés. Clés [sujet, ressource, …].
export const cles = {
  iam: ['iam'] as const,
  comptes: (f?: FiltreComptes) => ['iam', 'comptes', ...(f ? [f] : [])] as const,
  compte: (id: string, sous?: string) => ['iam', 'compte', id, ...(sous ? [sous] : [])] as const,
  indicateurs: () => ['iam', 'indicateurs'] as const,
  journal: (f?: FiltreJournal) => ['iam', 'journal', ...(f ? [f] : [])] as const,
  politique: () => ['iam', 'politique'] as const,
  messagerie: () => ['iam', 'messagerie'] as const,
  profils: () => ['iam', 'profils'] as const,
  permissions: () => ['iam', 'permissions'] as const,
  unites: () => ['iam', 'unites'] as const,
  matrice: () => ['iam', 'matrice'] as const,
  demandes: (q?: object) => ['iam', 'demandes', ...(q ? [q] : [])] as const,
  demande: (id: string) => ['iam', 'demande', id] as const,
  plateforme: ['plateforme'] as const,
  services: () => ['plateforme', 'services'] as const,
  serveurs: () => ['plateforme', 'serveurs'] as const,
  alertes: () => ['plateforme', 'alertes'] as const,
  configuration: () => ['plateforme', 'configuration'] as const,
  journalisation: (service: string, filtre?: string) => ['plateforme', 'journalisation', service, filtre ?? ''] as const,
  infrastructure: ['plateforme', 'infrastructure'] as const,
  charges: () => ['plateforme', 'infrastructure', 'charges'] as const,
  journauxCharge: (nom: string, lignes: number, depuis: string) => ['plateforme', 'infrastructure', 'journaux', nom, lignes, depuis] as const,
  operations: (page: number, taille: number) => ['plateforme', 'infrastructure', 'operations', page, taille] as const,
}

export function useLecture<R>(cle: QueryKey, lire: (s: ServicesConsole) => Promise<R>, options?: Omit<UseQueryOptions<R, ApiError>, 'queryKey' | 'queryFn'>) {
  const s = useServicesConsole()
  return useQuery<R, ApiError>({ queryKey: cle, queryFn: () => lire(s), ...options })
}

/** Action à effet : service puis invalidation des clés indiquées. */
export function useEffet<V, R = unknown>(agir: (s: ServicesConsole, v: V) => Promise<R>, invalider: QueryKey[] = [cles.iam]) {
  const s = useServicesConsole()
  const qc = useQueryClient()
  return useMutation<R, ApiError | Error, V>({
    mutationFn: (v) => agir(s, v),
    onSuccess: () => Promise.all(invalider.map((queryKey) => qc.invalidateQueries({ queryKey }))),
  })
}

export const useComptes = (f: FiltreComptes, actif = true) => useLecture(cles.comptes(f), (s) => s.comptes.lister(f), { enabled: actif })
/** Tous les comptes (pages de 100, la taille maximale de MS-IAM) : organigramme (D-86 §3). */
export const useTousComptes = (actif = true) => useLecture([...cles.comptes(), 'tous'], async (s) => {
  const taille = 100
  const premiere = await s.comptes.lister({ page: 1, taille })
  let l = [...premiere.contenu]
  for (let page = 2; l.length < premiere.total && page <= 50; page++) l = l.concat((await s.comptes.lister({ page, taille })).contenu)
  return l
}, { enabled: actif })
export const useIndicateursComptes = (actif = true) => useLecture(cles.indicateurs(), (s) => s.comptes.indicateurs(), { enabled: actif })
export const useJournal = (f: FiltreJournal, actif = true) => useLecture(cles.journal(f), (s) => s.comptes.journal(f), { enabled: actif })
export const useCompte = (id: string, actif = true) => useLecture(cles.compte(id), (s) => s.comptes.lire(id), { enabled: actif && !!id })
export const useSessionsCompte = (id: string) => useLecture(cles.compte(id, 'sessions'), (s) => s.comptes.sessions(id))
export const useProfils = (actif = true) => useLecture(cles.profils(), (s) => s.configuration.profils(), { enabled: actif })
/**
 * Libellé d'un profil (D-86 §2) : celui du serveur (GET /configuration/profils, lu avec IAM_ACCOUNT_READ), sinon le
 * catalogue de « Mon compte » (compte.profil.<code>), le code à défaut. Un libellé égal au code (attribut gimac_libelle
 * absent) cède aussi la place au catalogue.
 */
export function useLibelleProfil() {
  const { t } = useTranslation()
  const lecture = aPermission(usePermissions(), 'IAM_ACCOUNT_READ')
  const profils = useProfils(lecture)
  const donnees = profils.data
  return useCallback((code: string, serveur?: string | null) => {
    const l = serveur ?? donnees?.find((p) => p.code === code)?.libelle
    return l && l !== code ? l : t(`compte.profil.${code}`, { defaultValue: code })
  }, [donnees, t])
}
export const usePermissionsRealm = (actif = true) => useLecture(cles.permissions(), (s) => s.configuration.permissions(), { enabled: actif })
/** Catalogue des unités organisationnelles (D-76). */
export const useUnitesOrganisation = (actif = true) => useLecture(cles.unites(), (s) => s.organisation.unites(), { enabled: actif })
/** Matrice des pouvoirs (D-81 §3) et demandes des chefs à traiter (§4). */
export const useMatricePouvoirs = (actif = true) => useLecture(cles.matrice(), (s) => s.pouvoirs.matrice(), { enabled: actif })
export const useDemandesATraiter = (q: { etat?: EtatDemande; page: number; taille: number }) => useLecture(cles.demandes(q), (s) => s.pouvoirs.demandes(q), { placeholderData: keepPreviousData })
export const useDemandeATraiter = (id: string | undefined) => useLecture(cles.demande(id ?? ''), (s) => s.pouvoirs.demande(id!), { enabled: !!id })
export const usePolitique = () => useLecture(cles.politique(), (s) => s.configuration.politique())
export const useMessagerie = () => useLecture(cles.messagerie(), (s) => s.configuration.messagerie())
export const useServicesPlateforme = (actif = true) => useLecture(cles.services(), (s) => s.plateforme.services(), { enabled: actif })
export const useServeurs = (actif = true) => useLecture(cles.serveurs(), (s) => s.plateforme.serveurs(), { enabled: actif })
export const useAlertes = (actif = true) => useLecture(cles.alertes(), (s) => s.plateforme.alertes(), { enabled: actif })
export const useConfigurationPlateforme = () => useLecture(cles.configuration(), (s) => s.plateforme.configuration())
export const useJournalisation = (service: string, filtre?: string) =>
  useLecture(cles.journalisation(service, filtre), (s) => s.plateforme.journalisation(service, filtre), { enabled: !!service })

/** Flux temps réel de la plateforme : dernière impulsion et état de la connexion. */
export function useFluxPlateforme(actif = true): { impulsion: ImpulsionPlateforme | null; etat: EtatFlux } {
  const s = useServicesConsole()
  const [impulsion, setImpulsion] = useState<ImpulsionPlateforme | null>(null)
  const [etat, setEtat] = useState<EtatFlux>('connexion')
  useEffect(() => {
    if (!actif) return
    return s.plateforme.flux({ onImpulsion: setImpulsion, onEtat: setEtat })
  }, [s, actif])
  return { impulsion, etat }
}

// Exploitation de l'infrastructure (D-78) : charges, journaux à la demande, historique paginé, gestes.
export const useCharges = (actif = true) => useLecture(cles.charges(), (s) => s.infrastructure.charges(), { enabled: actif })
export const useJournauxCharge = (nom: string, lignes: number, depuis: string) =>
  useLecture(cles.journauxCharge(nom, lignes, depuis), (s) => s.infrastructure.journaux(nom, lignes, depuis), { enabled: !!nom })
export const useOperationsInfrastructure = (page: number, taille: number) => useLecture(cles.operations(page, taille), (s) => s.infrastructure.operations(page, taille))
export const useGesteInfrastructure = () =>
  useEffet((s, v: { nom: string; geste: Geste; demande: DemandeGeste; assertion: string }) => s.infrastructure.executer(v.nom, v.geste, v.demande, v.assertion), [cles.infrastructure])

/** Flux des opérations d'infrastructure : chaque opération poussée rafraîchit charges et historique. */
export function useFluxInfrastructure(actif = true): EtatFlux {
  const s = useServicesConsole()
  const qc = useQueryClient()
  const [etat, setEtat] = useState<EtatFlux>('connexion')
  useEffect(() => {
    if (!actif) return
    return s.infrastructure.flux({ onEtat: setEtat, onOperation: () => void qc.invalidateQueries({ queryKey: cles.infrastructure }) })
  }, [s, qc, actif])
  return etat
}
