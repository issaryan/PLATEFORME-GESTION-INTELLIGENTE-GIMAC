import type { ListQuery } from '../modele/commun'
import type { FiltreInstitutions } from '../modele/participant'

// Clés de requête du lot 1 : [sujet, ressource, identifiant, sous-ressource].
export const cles = {
  participant: ['participant'] as const,
  institutions: (f?: FiltreInstitutions) => ['participant', 'institutions', ...(f ? [f] : [])] as const,
  institution: (id: string, sous?: string) => ['participant', 'institution', id, ...(sous ? [sous] : [])] as const,
  indicateursDdrm: () => ['participant', 'indicateurs'] as const,
  homologation: ['homologation'] as const,
  certification: ['certification'] as const,
  referentiels: () => ['homologation', 'referentiels'] as const,
  referentiel: (id: string, sous?: string) => ['homologation', 'referentiel', id, ...(sous ? [sous] : [])] as const,
  liaisons: (q?: ListQuery) => ['homologation', 'liaisons', ...(q ? [q] : [])] as const,
  pilotage: (q?: ListQuery) => ['homologation', 'pilotage', ...(q ? [q] : [])] as const,
  liaison: (id: string, sous?: string) => ['homologation', 'liaison', id, ...(sous ? [sous] : [])] as const,
  operation: (id: string, sous?: string) => ['homologation', 'operation', id, ...(sous ? [sous] : [])] as const,
  reprise: () => ['homologation', 'reprise'] as const,
  conducteurs: () => ['homologation', 'conducteurs'] as const,
  dossier: (id: string, sous?: string) => ['certification', 'dossier', id, ...(sous ? [sous] : [])] as const,
  dossierLiaison: (liaisonId: string) => ['certification', 'dossier-liaison', liaisonId] as const,
  circuit: (id: string) => ['certification', 'circuit', id] as const,
  attestation: (numero: string) => ['certification', 'attestation', numero] as const,
  registre: (q?: ListQuery) => ['certification', 'registre', ...(q ? [q] : [])] as const,
  portail: (sous: string, id?: string) => ['homologation', 'portail', sous, ...(id ? [id] : [])] as const,
  portailCertifications: () => ['certification', 'portail'] as const,
}
