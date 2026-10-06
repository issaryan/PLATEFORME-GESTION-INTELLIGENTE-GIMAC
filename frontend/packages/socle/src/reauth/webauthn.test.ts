import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../http/errors'
import { b64u, CleSecuriteIndisponible, fromB64u, getReauthAssertion, ReauthCancelled, refusReauth } from './webauthn'

// IAM-07, D-62 : défi sur /bff/<couche>/iam/reauthentification/defi, assertion pour X-Reauth-Assertion.
const json = (corps: object, status = 200) => new Response(JSON.stringify(corps), { status, headers: { 'Content-Type': 'application/json' } })
const octets = (...v: number[]) => new Uint8Array(v).buffer
const credentials = (get: () => Promise<unknown>) => Object.defineProperty(navigator, 'credentials', { value: { get }, configurable: true })

// WebAuthn n'existe que dans un contexte sécurisé : jsdom n'expose pas PublicKeyCredential.
beforeEach(() => {
  vi.stubGlobal('PublicKeyCredential', class {})
  vi.stubGlobal('isSecureContext', true)
})
afterEach(() => {
  vi.restoreAllMocks()
  vi.unstubAllGlobals()
})

describe('réauthentification FIDO2', () => {
  it('base64url : aller-retour sans remplissage', () => {
    const b = new Uint8Array([251, 255, 0, 1])
    expect(b64u(b)).toBe('-_8AAQ')
    expect(Array.from(fromB64u('-_8AAQ'))).toEqual([251, 255, 0, 1])
    expect(b64u(b.buffer)).toBe('-_8AAQ')
  })

  it('ADMIN_SENSITIVE : défi demandé à MS-IAM de la couche, assertion sérialisée', async () => {
    const spy = vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce(json({ challengeId: 'd-1', challenge: 'AQID', rpId: 'des.gimac.local', allowCredentials: ['BAU'], timeoutMs: 60000 }))
    const get = vi.fn(async () => ({ id: 'cle-1', response: { clientDataJSON: octets(1), authenticatorData: octets(2), signature: octets(3), userHandle: octets(4) } }))
    credentials(get)
    const assertion = await getReauthAssertion('console', 'ADMIN_SENSITIVE')
    expect(String(spy.mock.calls[0][0])).toContain('/bff/console/iam/reauthentification/defi')
    expect(JSON.parse(String(spy.mock.calls[0][1]?.body))).toEqual({ purpose: 'ADMIN_SENSITIVE' })
    expect(JSON.parse(atob(assertion))).toEqual({ challengeId: 'd-1', id: 'cle-1', clientDataJSON: 'AQ', authenticatorData: 'Ag', signature: 'Aw', userHandle: 'BA' })
    const options = (get.mock.calls[0] as unknown as [CredentialRequestOptions])[0].publicKey!
    expect(options.rpId).toBe('des.gimac.local')
    expect(options.userVerification).toBe('required')
  })

  it('sans userHandle : null', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce(json({ challengeId: 'd-2', challenge: 'AQ', rpId: 'r', allowCredentials: [], timeoutMs: 1 }))
    credentials(async () => ({ id: 'c', response: { clientDataJSON: octets(1), authenticatorData: octets(1), signature: octets(1), userHandle: null } }))
    expect(JSON.parse(atob(await getReauthAssertion('des', 'QUITUS_SIGN'))).userHandle).toBeNull()
  })

  it('annulation par l’utilisateur ou absence de clé : ReauthCancelled', async () => {
    vi.spyOn(globalThis, 'fetch').mockImplementation(async () => json({ challengeId: 'd', challenge: 'AQ', rpId: 'r', allowCredentials: [], timeoutMs: 1 }))
    credentials(async () => { throw new DOMException('refus', 'NotAllowedError') })
    await expect(getReauthAssertion('des', 'QUITUS_SIGN')).rejects.toBeInstanceOf(ReauthCancelled)
    credentials(async () => null)
    await expect(getReauthAssertion('des', 'QUITUS_SIGN')).rejects.toBeInstanceOf(ReauthCancelled)
    credentials(async () => { throw new TypeError('autre') })
    await expect(getReauthAssertion('des', 'QUITUS_SIGN')).rejects.toBeInstanceOf(TypeError)
  })

  it('aucune clé enrôlée : le 409 IAM_NO_SECURITY_KEY du défi est remonté', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce(json({ code: 'IAM_NO_SECURITY_KEY', message: 'aucune clé' }, 409))
    await expect(getReauthAssertion('portail', 'ADMIN_SENSITIVE')).rejects.toMatchObject({ status: 409, code: 'IAM_NO_SECURITY_KEY' })
  })

  it('refusReauth : annulée, exigée (428), invalide, sans clé, autre', () => {
    expect(refusReauth(new ReauthCancelled())).toBe('ANNULEE')
    expect(refusReauth(new ApiError(428, 'IAM_REAUTH_REQUIRED', '', ''))).toBe('REQUISE')
    expect(refusReauth(new ApiError(428, 'HTTP_428', '', ''))).toBe('REQUISE')
    expect(refusReauth(new ApiError(403, 'IAM_REAUTH_INVALID', '', ''))).toBe('INVALIDE')
    expect(refusReauth(new ApiError(403, 'PIL_PROCESSUS_REAUTH_REFUSEE', '', ''))).toBe('INVALIDE')
    expect(refusReauth(new ApiError(409, 'IAM_NO_SECURITY_KEY', '', ''))).toBe('SANS_CLE')
    expect(refusReauth(new ApiError(409, 'IAM_ACCOUNT_CONFLICT', '', ''))).toBeNull()
    expect(refusReauth(new Error('x'))).toBeNull()
  })

  it('http sans contexte sécurisé : message explicite au lieu d’une propriété absente', async () => {
    const spy = vi.spyOn(globalThis, 'fetch')
    vi.stubGlobal('isSecureContext', false)
    await expect(getReauthAssertion('des', 'OPERATION_VALIDATE')).rejects.toBeInstanceOf(CleSecuriteIndisponible)
    expect(spy).not.toHaveBeenCalled()
  })
})
