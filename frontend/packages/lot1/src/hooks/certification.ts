import type { ListQuery } from '../modele/commun'
import { useEffet, useLecture } from './base'
import { cles } from './cles'

// Hooks de données de MS-CERTIFICATION (DES-11) : seuls points d'appel du port ServiceCertification.
export const useDossierDeLiaison = (liaisonId: string) => useLecture(cles.dossierLiaison(liaisonId), (s) => s.certification.dossierParLiaison(liaisonId))
export const useHistoriqueDossier = (id: string | undefined) =>
  useLecture(cles.dossier(id ?? '', 'historique'), (s) => s.certification.historiqueDossier(id ?? ''), { enabled: !!id })
export const useCircuit = (id: string) => useLecture(cles.circuit(id), (s) => s.certification.circuit(id))
export const useConditions = (id: string) => useLecture(cles.dossier(id, 'conditions'), (s) => s.certification.conditions(id))
export const useFicheDeChangement = (id: string) => useLecture(cles.dossier(id, 'fiche'), (s) => s.certification.ficheDeChangement(id))
export const useProcesVerbal = (id: string) => useLecture(cles.dossier(id, 'proces-verbal'), (s) => s.certification.procesVerbal(id))
export const useHorodatages = (id: string) => useLecture(cles.dossier(id, 'horodatages'), (s) => s.certification.horodatages(id))
export const useAttestation = (numero: string) => useLecture(cles.attestation(numero), (s) => s.certification.lireAttestation(numero))
export const useRegistre = (q: ListQuery) => useLecture(cles.registre(q), (s) => s.certification.registre(q))

export const useLierFiche = () =>
  useEffet((s, v: { dossierId: string; reference: string }) => s.certification.lierFicheDeChangement(v.dossierId, v.reference), [cles.certification])
export const useDeposerProcesVerbal = () =>
  useEffet((s, v: { dossierId: string; fichier: File; heureBascule: string }) => s.certification.deposerProcesVerbal(v.dossierId, v.fichier, v.heureBascule), [cles.certification])
/** Téléchargement journalisé côté service : renvoie le document et l'empreinte de l'en-tête Repr-Digest. */
export const useTelechargerAttestation = () => useEffet((s, numero: string) => s.certification.telechargerAttestation(numero), [])
