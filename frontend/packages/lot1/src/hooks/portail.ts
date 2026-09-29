import { useEffet, useLecture } from './base'
import { cles } from './cles'

// Hooks de données du portail (POR-03) : seuls points d'appel du port ServicePortail. L'institution
// vient du jeton côté service ; aucune requête ne la porte.
export const useMesLiaisons = () => useLecture(cles.portail('liaisons'), (s) => s.portail.mesLiaisons())
export const useMaLiaison = (id: string) => useLecture(cles.portail('liaison', id), (s) => s.portail.maLiaison(id))
export const useAFaire = (liaisonId: string) => useLecture(cles.portail('a-faire', liaisonId), (s) => s.portail.aFaire(liaisonId))
export const useOperationAFaire = (liaisonId: string, id: string) =>
  useLecture(cles.portail('operation', `${liaisonId}/${id}`), (s) => s.portail.operationAFaire(liaisonId, id))
export const useCertificationsPortail = () => useLecture(cles.portailCertifications(), (s) => s.portail.certifications())
export const useTelechargerAttestationPortail = () => useEffet((s, certificationId: string) => s.portail.telechargerAttestation(certificationId), [])
