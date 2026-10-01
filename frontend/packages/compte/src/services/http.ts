import { enrolerCleSecurite, getReauthAssertion, http, prefixeIam, refusReauth, type Couche } from '@gimac/socle'
import type { ServiceMonCompte } from './ports'

// Implémentation HTTP de « Mon compte » : /bff/<couche>/iam/moi, que la passerelle réécrit vers
// /api/v1/moi de MS-IAM (MonCompteResource). Disponible dans les trois couches, et pendant la session
// restreinte du premier accès pour /moi, /moi/premiere-configuration, /moi/mot-de-passe et
// /moi/second-facteur/** (D-65).
export function monCompteHttp(couche: Couche): ServiceMonCompte {
  const M = `${prefixeIam(couche)}/moi`
  return {
    moi: () => http.get(M),
    premiereConfiguration: (p) => http.post(`${M}/premiere-configuration`, { body: p }),
    modifier: (f) => http.put(M, { body: f }),
    changerMotDePasse: (c) => http.post(`${M}/mot-de-passe`, { body: c }),
    enrolerTotp: () => http.post(`${M}/second-facteur/totp/enrolement`),
    // Hors premier accès, MS-IAM exige la clé de sécurité si l'une est enrôlée (428, ACCOUNT_SENSITIVE).
    confirmerTotp: async (c) => {
      try {
        await http.post(`${M}/second-facteur/totp`, { body: c })
      } catch (e) {
        if (refusReauth(e) !== 'REQUISE') throw e
        const reauthAssertion = await getReauthAssertion(couche, 'ACCOUNT_SENSITIVE')
        await http.post(`${M}/second-facteur/totp`, { body: c, reauthAssertion })
      }
    },
    sessions: () => http.get(`${M}/sessions`),
    fermerSession: (id) => http.delete(`${M}/sessions/${encodeURIComponent(id)}`),
    fermerSessions: () => http.delete(`${M}/sessions`),
    cles: () => http.get(`${M}/cles-securite`),
    enrolerCle: (nom) => enrolerCleSecurite(couche, nom),
    supprimerCle: (id) => http.delete(`${M}/cles-securite/${encodeURIComponent(id)}`),
  }
}
