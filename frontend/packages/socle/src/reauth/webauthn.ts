import { http } from '../http/client'
import { estApiError } from '../http/errors'
import { prefixeIam, type Couche } from '../http/prefixes'

// Réauthentification FIDO2 (IAM-07) : défi obtenu auprès de MS-IAM (/bff/<couche>/iam/reauthentification/defi),
// assertion sérialisée pour l'en-tête X-Reauth-Assertion (base64 d'un JSON challengeId, id, clientDataJSON,
// authenticatorData, signature, userHandle). ADMIN_SENSITIVE : actes d'administration sensibles (D-62) ;
// ACCOUNT_SENSITIVE : reconfiguration du code TOTP hors premier accès (D-65).
/** SIGNATURE_PROCESSUS (D-87) : signature d'une étape d'un processus d'unité, vérifiée par MS-PILOTAGE auprès de MS-IAM. */
/** SIGNATURE_OPERATION (D-91) : signature attendue d'une opération du parcours, vérifiée par MS-HOMOLOGATION auprès de MS-IAM. */
export type SignPurpose = 'OPERATION_VALIDATE' | 'QUITUS_SIGN' | 'STATUS_APPROVE' | 'ADMIN_SENSITIVE' | 'ACCOUNT_SENSITIVE' | 'SIGNATURE_PROCESSUS' | 'SIGNATURE_OPERATION'
type Challenge = { challengeId: string; challenge: string; rpId: string; allowCredentials: string[]; timeoutMs: number }

/** Encodage base64url sans remplissage (WebAuthn). */
export const b64u = (b: ArrayBuffer | ArrayBufferView): string => {
  const octets = b instanceof ArrayBuffer ? new Uint8Array(b) : new Uint8Array(b.buffer, b.byteOffset, b.byteLength)
  return btoa(String.fromCharCode(...octets)).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')
}
/** Décodage base64url (avec ou sans remplissage). */
export const fromB64u = (s: string): Uint8Array<ArrayBuffer> => Uint8Array.from(atob(s.replace(/-/g, '+').replace(/_/g, '/')), (c) => c.charCodeAt(0))

/**
 * Clé de sécurité inutilisable dans ce navigateur : WebAuthn n'existe que dans un contexte sécurisé
 * (HTTPS ou localhost). Servie en http par une adresse IP, la plateforme ne peut ni enrôler une clé ni
 * signer un acte sensible ; elle le dit au lieu d'échouer sur une propriété absente.
 */
export class CleSecuriteIndisponible extends Error {
  constructor() {
    super('Les clés de sécurité ne fonctionnent que sur une connexion sécurisée (HTTPS). Ouvrez la plateforme par son adresse https, puis recommencez.')
    this.name = 'CleSecuriteIndisponible'
  }
}

/** Vrai si le navigateur peut utiliser une clé de sécurité (contexte sécurisé et WebAuthn présents). */
export const cleSecuriteDisponible = (): boolean =>
  typeof window !== 'undefined' && window.isSecureContext !== false && typeof navigator !== 'undefined' && Boolean(navigator.credentials) && typeof PublicKeyCredential !== 'undefined'

/** L'utilisateur a annulé ou laissé expirer la demande de sa clé de sécurité. */
export class ReauthCancelled extends Error {
  constructor() {
    super('Réauthentification annulée')
    this.name = 'ReauthCancelled'
  }
}

/** Annulation, délai dépassé ou clé absente : le navigateur lève NotAllowedError (ou AbortError). */
export const estAnnulation = (e: unknown): boolean => e instanceof DOMException && (e.name === 'NotAllowedError' || e.name === 'AbortError')

export async function getReauthAssertion(layer: Couche, purpose: SignPurpose): Promise<string> {
  if (!cleSecuriteDisponible()) throw new CleSecuriteIndisponible()
  const ch = await http.post<Challenge>(`${prefixeIam(layer)}/reauthentification/defi`, { body: { purpose } })
  let cred: Credential | null
  try {
    cred = await navigator.credentials.get({
      publicKey: {
        challenge: fromB64u(ch.challenge),
        rpId: ch.rpId,
        timeout: ch.timeoutMs,
        userVerification: 'required',
        allowCredentials: ch.allowCredentials.map((id) => ({ type: 'public-key' as const, id: fromB64u(id) })),
      },
    })
  } catch (e) {
    if (estAnnulation(e)) throw new ReauthCancelled()
    throw e
  }
  if (!cred || !('response' in cred)) throw new ReauthCancelled()
  const c = cred as PublicKeyCredential
  const r = c.response as AuthenticatorAssertionResponse
  return btoa(
    JSON.stringify({
      challengeId: ch.challengeId,
      id: c.id,
      clientDataJSON: b64u(r.clientDataJSON),
      authenticatorData: b64u(r.authenticatorData),
      signature: b64u(r.signature),
      userHandle: r.userHandle ? b64u(r.userHandle) : null,
    }),
  )
}

/**
 * Motif d'un refus lié à la réauthentification forte : annulée par l'utilisateur, exigée (428),
 * invalide (403 IAM_REAUTH_INVALID), aucune clé enrôlée (409 IAM_NO_SECURITY_KEY, à enrôler dans
 * « Mon compte ») ; null pour toute autre erreur.
 */
export type RefusReauth = 'ANNULEE' | 'REQUISE' | 'INVALIDE' | 'SANS_CLE'
export function refusReauth(e: unknown): RefusReauth | null {
  if (e instanceof ReauthCancelled) return 'ANNULEE'
  if (!estApiError(e)) return null
  if (e.status === 428 || e.code === 'IAM_REAUTH_REQUIRED') return 'REQUISE'
  // MS-PILOTAGE (D-87) relaie le refus de MS-IAM pour la signature d'une étape de processus.
  if (e.code === 'IAM_REAUTH_INVALID' || e.code === 'PIL_PROCESSUS_REAUTH_REFUSEE') return 'INVALIDE'
  if (e.code === 'IAM_NO_SECURITY_KEY') return 'SANS_CLE'
  return null
}
