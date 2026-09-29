import { afterEach, describe, expect, it, vi } from 'vitest'
import { enrolerCleSecurite, optionsCreation, type OptionsEnrolement } from './enrolement'
import { ReauthCancelled } from './webauthn'

// D-62 : enrôlement d'une clé de sécurité depuis « Mon compte » (navigator.credentials.create simulé).
const json = (corps: object, status = 200) => new Response(JSON.stringify(corps), { status, headers: { 'Content-Type': 'application/json' } })
const OPTIONS: OptionsEnrolement = {
  challengeId: 'enr-1', challenge: 'AQID', rp: { id: 'des.gimac.local', name: 'GIMAC DES' },
  user: { id: 'dS0x', name: 'u-1', displayName: 'Super Administrateur' },
  pubKeyCredParams: [{ type: 'public-key', alg: -7 }, { type: 'public-key', alg: -257 }], timeoutMs: 120000, excludeCredentials: ['BAU'],
}
const credentials = (create: () => Promise<unknown>) => Object.defineProperty(navigator, 'credentials', { value: { create }, configurable: true })

afterEach(() => vi.restoreAllMocks())

describe('enrôlement d’une clé de sécurité', () => {
  it('options : défi, identifiant d’utilisateur et clés exclues décodés, vérification exigée', () => {
    const o = optionsCreation(OPTIONS)
    expect(Array.from(new Uint8Array(o.challenge as ArrayBuffer))).toEqual([1, 2, 3])
    expect(new TextDecoder().decode(o.user.id as Uint8Array)).toBe('u-1')
    expect(o.rp).toEqual({ id: 'des.gimac.local', name: 'GIMAC DES' })
    expect(o.pubKeyCredParams.map((p) => p.alg)).toEqual([-7, -257])
    expect(o.excludeCredentials).toHaveLength(1)
    expect(o.timeout).toBe(120000)
    expect(o.authenticatorSelection?.userVerification).toBe('required')
    expect(optionsCreation({ ...OPTIONS, excludeCredentials: undefined as unknown as string[] }).excludeCredentials).toEqual([])
  })

  it('défi, création puis enregistrement de l’attestation en base64url', async () => {
    const spy = vi.spyOn(globalThis, 'fetch')
      .mockResolvedValueOnce(json(OPTIONS))
      .mockResolvedValueOnce(json({ id: 'k-1', nom: 'YubiKey bureau', creeeLe: '2026-09-29T08:00:00Z', derniereUtilisation: null }))
    const create = vi.fn(async () => ({ id: 'cle', response: { clientDataJSON: new Uint8Array([1]).buffer, attestationObject: new Uint8Array([2, 3]).buffer } }))
    credentials(create)
    const cle = await enrolerCleSecurite('des', '  YubiKey bureau ')
    expect(cle.id).toBe('k-1')
    expect(String(spy.mock.calls[0][0])).toContain('/bff/des/iam/moi/cles-securite/defi')
    expect(String(spy.mock.calls[1][0])).toMatch(/\/bff\/des\/iam\/moi\/cles-securite$/)
    expect(JSON.parse(String(spy.mock.calls[1][1]?.body))).toEqual({ challengeId: 'enr-1', nom: 'YubiKey bureau', clientDataJSON: 'AQ', attestationObject: 'AgM' })
    expect(create).toHaveBeenCalledTimes(1)
  })

  it('annulation : ReauthCancelled, aucune attestation envoyée', async () => {
    const spy = vi.spyOn(globalThis, 'fetch').mockImplementation(async () => json(OPTIONS))
    credentials(async () => { throw new DOMException('annulé', 'NotAllowedError') })
    await expect(enrolerCleSecurite('console', 'Clé')).rejects.toBeInstanceOf(ReauthCancelled)
    credentials(async () => null)
    await expect(enrolerCleSecurite('console', 'Clé')).rejects.toBeInstanceOf(ReauthCancelled)
    credentials(async () => { throw new TypeError('non pris en charge') })
    await expect(enrolerCleSecurite('console', 'Clé')).rejects.toBeInstanceOf(TypeError)
    expect(spy).toHaveBeenCalledTimes(3)
  })
})
