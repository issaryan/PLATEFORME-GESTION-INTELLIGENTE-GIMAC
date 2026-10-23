import { keepPreviousData } from '@tanstack/react-query'
import { useEffet, useLecture } from './index'
import type { DemandePrestataire, TypeDelegation } from '../modele/sandbox'

// Hooks de la Sandbox GIMAC dans la console (D-119) : pages comptées à partir de 1 dans les écrans, à partir de 0 chez
// MS-IAM.
export const clesSandbox = {
  sandbox: ['iam', 'sandbox'] as const,
  perimetres: (page: number, taille: number) => ['iam', 'sandbox', 'perimetres', page, taille] as const,
  protections: (page: number, taille: number) => ['iam', 'sandbox', 'protections', page, taille] as const,
  delegations: (type: TypeDelegation, page: number, taille: number) => ['iam', 'sandbox', 'delegations', type, page, taille] as const,
}

export const usePerimetresSuspendus = (page: number, taille: number) =>
  useLecture(clesSandbox.perimetres(page, taille), (s) => s.sandbox.perimetres(page - 1, taille), { placeholderData: keepPreviousData })

export const useDelegations = (type: TypeDelegation, page: number, taille: number) =>
  useLecture(clesSandbox.delegations(type, page, taille), (s) => s.sandbox.delegations(type, page - 1, taille), { placeholderData: keepPreviousData })

export const useComptesProteges =(page: number, taille: number) =>
  useLecture(clesSandbox.protections(page, taille), (s) => s.sandbox.protections(page - 1, taille), { placeholderData: keepPreviousData })

// US-EXI-24 : accès de prestataire de maintenance, demandé puis approuvé par le RSSI ; chaque geste relit les délégations.
export const useDemanderPrestataire = () => useEffet((s, d: DemandePrestataire) => s.sandbox.demanderPrestataire(d), [clesSandbox.sandbox])
export const useApprouverPrestataire = () => useEffet((s, id: string) => s.sandbox.approuverPrestataire(id), [clesSandbox.sandbox])
export const useRefuserPrestataire = () => useEffet((s, v: { id: string; motif: string }) => s.sandbox.refuserPrestataire(v.id, v.motif), [clesSandbox.sandbox])
export const useRevoquerPrestataire = () => useEffet((s, v: { id: string; motif: string }) => s.sandbox.revoquerPrestataire(v.id, v.motif), [clesSandbox.sandbox])

// US-EXI-10 : registre d'audit de la sandbox (MS-AUDIT) et rebut des enveloppes non conformes.
export const useRegistreSandbox = (institution: string, page: number, taille: number) =>
  useLecture(['audit', 'sandbox', 'registre', institution, page, taille], (s) => s.sandbox.registre(institution || undefined, page - 1, taille), { placeholderData: keepPreviousData })
export const useRebutSandbox = (page: number, taille: number) =>
  useLecture(['audit', 'sandbox', 'rebut', page, taille], (s) => s.sandbox.rebut(page - 1, taille), { placeholderData: keepPreviousData })
