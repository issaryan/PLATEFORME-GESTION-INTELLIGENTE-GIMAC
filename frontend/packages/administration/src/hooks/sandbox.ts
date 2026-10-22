import { keepPreviousData } from '@tanstack/react-query'
import { useLecture } from './index'

// Hooks de la Sandbox GIMAC dans la console (D-119) : pages comptées à partir de 1 dans les écrans, à partir de 0 chez
// MS-IAM.
export const clesSandbox = {
  sandbox: ['iam', 'sandbox'] as const,
  perimetres: (page: number, taille: number) => ['iam', 'sandbox', 'perimetres', page, taille] as const,
  protections: (page: number, taille: number) => ['iam', 'sandbox', 'protections', page, taille] as const,
}

export const usePerimetresSuspendus = (page: number, taille: number) =>
  useLecture(clesSandbox.perimetres(page, taille), (s) => s.sandbox.perimetres(page - 1, taille), { placeholderData: keepPreviousData })

export const useComptesProteges = (page: number, taille: number) =>
  useLecture(clesSandbox.protections(page, taille), (s) => s.sandbox.protections(page - 1, taille), { placeholderData: keepPreviousData })
