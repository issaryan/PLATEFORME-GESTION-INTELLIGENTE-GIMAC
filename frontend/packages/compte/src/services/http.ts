import { enrolerCleSecurite, http, prefixeIam, reconnecter, type Couche } from '@gimac/socle'
import type { ServiceMonCompte } from './ports'

// Implémentation HTTP de « Mon compte » : /bff/<couche>/iam/moi, que la passerelle réécrit vers
// /api/v1/moi de MS-IAM (MonCompteResource). Disponible dans les trois couches.
export function monCompteHttp(couche: Couche): ServiceMonCompte {
  const M = `${prefixeIam(couche)}/moi`
  return {
    moi: () => http.get(M),
    premiereConfiguration: (p) => http.post(`${M}/premiere-configuration`, { body: p }),
    modifier: (f) => http.put(M, { body: f }),
    demanderAction: (action) => http.post(`${M}/actions`, { body: { action } }),
    sessions: () => http.get(`${M}/sessions`),
    fermerSession: (id) => http.delete(`${M}/sessions/${encodeURIComponent(id)}`),
    fermerSessions: () => http.delete(`${M}/sessions`),
    cles: () => http.get(`${M}/cles-securite`),
    enrolerCle: (nom) => enrolerCleSecurite(couche, nom),
    supprimerCle: (id) => http.delete(`${M}/cles-securite/${encodeURIComponent(id)}`),
    reconnecter,
  }
}
