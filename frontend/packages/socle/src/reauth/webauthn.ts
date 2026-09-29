import { http } from '../http/client'
import type { Couche } from '../http/prefixes'

// Réauthentification FIDO2 (IAM-07) : défi obtenu auprès de MS-IAM, assertion sérialisée pour X-Reauth-Assertion.
export type SignPurpose = 'OPERATION_VALIDATE' | 'QUITUS_SIGN' | 'STATUS_APPROVE'
type Challenge = { challengeId: string; challenge: string; rpId: string; allowCredentials: string[]; timeoutMs: number }

const b64u = (b: ArrayBuffer) => btoa(String.fromCharCode(...new Uint8Array(b))).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')
const fromB64u = (s: string) => Uint8Array.from(atob(s.replace(/-/g, '+').replace(/_/g, '/')), (c) => c.charCodeAt(0))

/** L'utilisateur a annulé ou laissé expirer la demande de sa clé de sécurité. */
export class ReauthCancelled extends Error {
  constructor() {
    super('Réauthentification annulée')
    this.name = 'ReauthCancelled'
  }
}

export async function getReauthAssertion(layer: Couche, purpose: SignPurpose): Promise<string> {
  const ch = await http.post<Challenge>(`/bff/${layer}/iam/reauthentification/defi`, { body: { purpose } })
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
    // Annulation, délai dépassé ou clé absente : le navigateur lève NotAllowedError.
    if (e instanceof DOMException && (e.name === 'NotAllowedError' || e.name === 'AbortError')) throw new ReauthCancelled()
    throw e
  }
  if (!(cred instanceof PublicKeyCredential)) throw new ReauthCancelled()
  const r = cred.response as AuthenticatorAssertionResponse
  return btoa(
    JSON.stringify({
      challengeId: ch.challengeId,
      id: cred.id,
      clientDataJSON: b64u(r.clientDataJSON),
      authenticatorData: b64u(r.authenticatorData),
      signature: b64u(r.signature),
      userHandle: r.userHandle ? b64u(r.userHandle) : null,
    }),
  )
}
