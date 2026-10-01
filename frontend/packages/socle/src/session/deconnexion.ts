import { http } from '../http/client'
import type { ApiError } from '../http/errors'
import { CONNEXION, DECONNEXION } from '../http/prefixes'
import { fermerSessionDemo } from './demo'

// Déconnexion et retour à l'écran de connexion de la plateforme (D-62, D-65). Aucune page du fournisseur
// d'identité n'est jamais ouverte : la passerelle ferme la session de Keycloak côté serveur.

/** Navigation du document ; remplaçable dans les tests. */
export const navigation = { vers: (adresse: string) => window.location.assign(adresse) }

/** Motif affiché par l'écran de connexion. */
export type MotifConnexion = 'deconnexion' | 'inactivite' | 'expiree'

/** Chemin interne sûr (un seul « / » en tête, hors écran de connexion), sinon undefined : pas de redirection ouverte. */
export function retourSur(chemin: string | null | undefined): string | undefined {
  if (!chemin || !chemin.startsWith('/') || chemin.startsWith('//') || chemin.startsWith('/\\')) return undefined
  if (chemin === CONNEXION || chemin.startsWith(`${CONNEXION}/`) || chemin.startsWith(`${CONNEXION}?`)) return undefined
  return chemin
}

/** Page courante, pour y revenir après la connexion. */
export const retourCourant = (): string | undefined => retourSur(window.location.pathname + window.location.search)

/** Adresse de l'écran de connexion, avec son motif et la page de retour. */
export function adresseConnexion(o: { motif?: MotifConnexion; minutes?: number; retour?: string } = {}): string {
  const q = new URLSearchParams()
  if (o.motif) q.set('motif', o.motif)
  if (o.minutes) q.set('minutes', String(o.minutes))
  const retour = retourSur(o.retour)
  if (retour) q.set('retour', retour)
  const s = q.toString()
  return s ? `${CONNEXION}?${s}` : CONNEXION
}

/** Déconnexion complète : la passerelle ferme ses sessions (et celle de Keycloak), puis retour à la connexion. */
export async function deconnecter(motif: MotifConnexion = 'deconnexion', minutes?: number): Promise<void> {
  try {
    await http.post(DECONNEXION, { anonyme: true })
  } catch {
    /* session déjà close : retour à la connexion */
  }
  fermerSessionDemo()
  navigation.vers(adresseConnexion({ motif, minutes, retour: motif === 'deconnexion' ? undefined : retourCourant() }))
}

/** Nouvelle authentification sur l'écran de la plateforme : la session courante est close. */
export async function reconnecter(): Promise<void> {
  await deconnecter('deconnexion')
}

/**
 * Réponse 401 d'une route protégée (crochet onUnauthorized du client) : session absente, ou close par
 * la passerelle (CNX_SESSION_EXPIREE, inactivité ou durée maximale). Retour à la connexion, puis à la page.
 */
export async function surNonAutorise(e?: ApiError): Promise<boolean> {
  fermerSessionDemo()
  navigation.vers(adresseConnexion({ motif: e?.code === 'CNX_SESSION_EXPIREE' ? 'expiree' : undefined, retour: retourCourant() }))
  return false
}
