import { http } from '../http/client'
import { prefixeIam, type Couche } from '../http/prefixes'
import { b64u, CleSecuriteIndisponible, cleSecuriteDisponible, estAnnulation, fromB64u, ReauthCancelled } from './webauthn'

// Enrôlement d'une clé de sécurité FIDO2 depuis « Mon compte » (D-62, IAM-07) : options obtenues auprès
// de MS-IAM, création de la clé par navigator.credentials.create, attestation renvoyée en base64url.

/** Options d'enrôlement de MS-IAM (ReauthentificationService.OptionsEnrolement). */
export type OptionsEnrolement = {
  challengeId: string
  challenge: string
  rp: { id: string; name: string }
  user: { id: string; name: string; displayName: string }
  pubKeyCredParams: { type: string; alg: number }[]
  timeoutMs: number
  excludeCredentials: string[]
}

/** Clé de sécurité enrôlée (identifiant, nom, dates). */
export type CleSecurite = { id: string; nom: string; creeeLe: string; derniereUtilisation: string | null }

/** Options converties pour navigator.credentials.create (défi et identifiant d'utilisateur décodés). */
export function optionsCreation(o: OptionsEnrolement): PublicKeyCredentialCreationOptions {
  return {
    challenge: fromB64u(o.challenge),
    rp: { id: o.rp.id, name: o.rp.name },
    user: { id: fromB64u(o.user.id), name: o.user.name, displayName: o.user.displayName },
    pubKeyCredParams: o.pubKeyCredParams.map((p) => ({ type: 'public-key' as const, alg: p.alg })),
    timeout: o.timeoutMs,
    excludeCredentials: (o.excludeCredentials ?? []).map((id) => ({ type: 'public-key' as const, id: fromB64u(id) })),
    authenticatorSelection: { userVerification: 'required', residentKey: 'discouraged' },
    attestation: 'none',
  }
}

/**
 * Enrôle une clé : défi, création par l'authentificateur, enregistrement de l'attestation.
 * Lève ReauthCancelled si l'utilisateur annule ou laisse expirer la demande.
 */
export async function enrolerCleSecurite(couche: Couche, nom: string): Promise<CleSecurite> {
  if (!cleSecuriteDisponible()) throw new CleSecuriteIndisponible()
  const base = `${prefixeIam(couche)}/moi/cles-securite`
  const options = await http.post<OptionsEnrolement>(`${base}/defi`)
  let cred: Credential | null
  try {
    cred = await navigator.credentials.create({ publicKey: optionsCreation(options) })
  } catch (e) {
    if (estAnnulation(e)) throw new ReauthCancelled()
    throw e
  }
  if (!cred || !('response' in cred)) throw new ReauthCancelled()
  const r = (cred as PublicKeyCredential).response as AuthenticatorAttestationResponse
  return http.post<CleSecurite>(base, {
    body: { challengeId: options.challengeId, nom: nom.trim(), clientDataJSON: b64u(r.clientDataJSON), attestationObject: b64u(r.attestationObject) },
  })
}
