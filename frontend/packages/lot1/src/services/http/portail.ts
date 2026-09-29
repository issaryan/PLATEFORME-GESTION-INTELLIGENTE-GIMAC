import { http, P } from '@gimac/socle'
import type { ServicePortail } from '../ports'

// Implémentation HTTP de ServicePortail (HOM-23, HOM-24, CER-13) : routes /bff/portail/…, réécrites vers
// /api/v1/portail/… ; l'institution vient du jeton côté service, jamais de la requête.
const PH = P.portailHomologation
const PC = P.portailCertification

export const portailHttp: ServicePortail = {
  mesLiaisons: () => http.get(`${PH}/liaisons`),
  maLiaison: (id) => http.get(`${PH}/liaisons/${id}`),
  aFaire: (liaisonId) => http.get(`${PH}/liaisons/${liaisonId}/a-faire`),
  operationAFaire: (liaisonId, id) => http.get(`${PH}/liaisons/${liaisonId}/a-faire/${id}`),
  deposerPreuve: (operationId, fichier, typePiece) => {
    const body = new FormData()
    body.set('fichier', fichier)
    body.set('typePiece', typePiece)
    return http.post(`${PH}/operations/${operationId}/preuves`, { body })
  },
  certifications: () => http.get(`${PC}/certifications`),
  telechargerAttestation: (certificationId) => http.blob(`${PC}/certifications/${certificationId}/attestation`, 'application/pdf'),
}
