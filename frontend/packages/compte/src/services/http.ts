import { APPAREILS, HISTORIQUE_CONNEXIONS, enrolerCleSecurite, getReauthAssertion, http, prefixeIam, refusReauth, type Couche } from '@gimac/socle'
import type { ServiceMonCompte } from './ports'

// Implémentation HTTP de « Mon compte » : /bff/<couche>/iam/moi, que la passerelle réécrit vers
// /api/v1/moi de MS-IAM (MonCompteResource). Disponible dans les trois couches, et pendant la session
// restreinte du premier accès pour /moi, /moi/premiere-configuration, /moi/mot-de-passe et
// /moi/second-facteur/** (D-65).
const nomImage = (type: string) => `photo.${type === 'image/png' ? 'png' : type === 'image/jpeg' ? 'jpg' : 'webp'}`

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
    // Photo de profil (mon-compte.md §1) : multipart, champ « image » ; MS-IAM vérifie le type réel par les octets.
    changerPhoto: (image) => {
      const corps = new FormData()
      corps.append('image', image, nomImage(image.type))
      return http.put(`${M}/avatar`, { body: corps })
    },
    retirerPhoto: () => http.delete(`${M}/avatar`),
    // Appareils connectés et historique (mon-compte.md §3) : sessions portées par la passerelle.
    appareils: () => http.get(APPAREILS),
    fermerAppareil: (id) => http.delete(`${APPAREILS}/${encodeURIComponent(id)}`),
    fermerAutresAppareils: () => http.delete(APPAREILS),
    historique: (page, taille) => http.get(HISTORIQUE_CONNEXIONS, { query: { page, taille } }),
    // Rattachement (D-76 §1.3) : servi à la couche interne seulement, comme « Mon activité ».
    ...(couche === 'des' ? { organisation: () => http.get('/bff/des/iam/organisation/moi') } : {}),
  }
}
