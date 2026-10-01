import { http } from '../http/client'
import { API_CONNEXION, SESSION } from '../http/prefixes'
import type { ServiceConnexion } from './modele'

// Implémentation HTTP de la connexion : routes publiques /api/connexion/** de MS-GATEWAY, jeton anti-CSRF
// recopié par le client comme pour toute mutation. Un 401 y est une réponse du formulaire (anonyme).

/** Témoin anti-CSRF publié par la passerelle (CookieServerCsrfTokenRepository). */
const TEMOIN_CSRF = 'XSRF-TOKEN'

/**
 * Première visite : la passerelle ne pose le témoin anti-CSRF qu'en réponse à une requête. Sans lui, le
 * premier POST serait refusé (403) ; une lecture anonyme de la session (401 attendu) le fait poser.
 */
export async function assurerJetonCsrf(): Promise<void> {
  if (typeof document === 'undefined' || document.cookie.split('; ').some((c) => c.startsWith(`${TEMOIN_CSRF}=`))) return
  try {
    await http.get(SESSION, { anonyme: true, fond: true })
  } catch {
    // 401 sans session : seul le témoin posé importe.
  }
}

export function connexionHttp(): ServiceConnexion {
  return {
    connecter: async (d) => {
      await assurerJetonCsrf()
      return http.post(API_CONNEXION.connexion, { body: d, anonyme: true })
    },
    motDePasseOublie: async (identifiant) => {
      await assurerJetonCsrf()
      await http.post(API_CONNEXION.motDePasseOublie, { body: { identifiant }, anonyme: true })
    },
    nouveauMotDePasse: async (jeton, motDePasse) => {
      await assurerJetonCsrf()
      await http.post(API_CONNEXION.nouveauMotDePasse, { body: { jeton, motDePasse }, anonyme: true })
    },
  }
}
