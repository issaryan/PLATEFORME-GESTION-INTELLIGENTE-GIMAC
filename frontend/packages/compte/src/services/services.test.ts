import { afterEach, describe, expect, it, vi } from 'vitest'
import { ApiError, ReauthCancelled, navigationDocument } from '@gimac/socle'
import { catalogue } from '../i18n'
import { actionsFournisseur } from '../modele'
import { monCompteHttp } from './http'
import { monCompteSimule } from './simule'

const json = (corps: unknown, status = 200) =>
  status === 204 ? new Response(null, { status }) : new Response(JSON.stringify(corps), { status, headers: { 'Content-Type': 'application/json' } })
afterEach(() => vi.restoreAllMocks())

describe('Mon compte : adaptateur HTTP', () => {
  it('chaque opération appelle la route /bff/<couche>/iam/moi attendue', async () => {
    const spy = vi.spyOn(globalThis, 'fetch').mockImplementation(async (_u, init) => json({ ok: true }, init?.method === 'DELETE' ? 204 : 200))
    const s = monCompteHttp('portail')
    await s.moi()
    await s.premiereConfiguration({ telephone: '+237 699 00 00 00', langue: 'fr', charteAcceptee: true })
    await s.modifier({ telephone: '+237 699 00 00 01' })
    await s.demanderAction('UPDATE_PASSWORD')
    await s.sessions()
    await s.fermerSession('s/1')
    await s.fermerSessions()
    await s.cles()
    await s.supprimerCle('k-1')
    const appels = spy.mock.calls.map(([u, i]) => `${i?.method} ${new URL(String(u)).pathname}`)
    expect(appels).toEqual([
      'GET /bff/portail/iam/moi',
      'POST /bff/portail/iam/moi/premiere-configuration',
      'PUT /bff/portail/iam/moi',
      'POST /bff/portail/iam/moi/actions',
      'GET /bff/portail/iam/moi/sessions',
      'DELETE /bff/portail/iam/moi/sessions/s%2F1',
      'DELETE /bff/portail/iam/moi/sessions',
      'GET /bff/portail/iam/moi/cles-securite',
      'DELETE /bff/portail/iam/moi/cles-securite/k-1',
    ])
    expect(JSON.parse(String(spy.mock.calls[3][1]?.body))).toEqual({ action: 'UPDATE_PASSWORD' })
  })

  it('enrôlement par l’assistant WebAuthn du socle ; reconnexion par la passerelle', async () => {
    const vers = vi.spyOn(navigationDocument, 'vers').mockImplementation(() => undefined)
    vi.spyOn(globalThis, 'fetch')
      .mockResolvedValueOnce(json({ challengeId: 'c', challenge: 'AQ', rp: { id: 'r', name: 'n' }, user: { id: 'dQ', name: 'u', displayName: 'U' }, pubKeyCredParams: [{ type: 'public-key', alg: -7 }], timeoutMs: 1, excludeCredentials: [] }))
      .mockResolvedValueOnce(json({ id: 'k-9', nom: 'Clé', creeeLe: '2026-09-29T00:00:00Z', derniereUtilisation: null }))
      .mockResolvedValueOnce(json({}))
    Object.defineProperty(navigator, 'credentials', { value: { create: async () => ({ id: 'x', response: { clientDataJSON: new Uint8Array([1]).buffer, attestationObject: new Uint8Array([2]).buffer } }) }, configurable: true })
    const s = monCompteHttp('des')
    expect((await s.enrolerCle('Clé')).id).toBe('k-9')
    await s.reconnecter()
    expect(vers).toHaveBeenCalledWith('/oauth2/authorization/oidc')
  })
})

describe('Mon compte : service simulé', () => {
  it('premier accès : fiche, puis actions requises exécutées à la reconnexion', async () => {
    const s = monCompteSimule({ latenceMs: 0, premierAcces: true })
    const m = await s.moi()
    expect(m.etapesRestantes).toEqual(['UPDATE_PASSWORD', 'VERIFY_EMAIL', 'FICHE'])
    expect(m.clesSecurite).toBe(0)
    await expect(s.premiereConfiguration({ telephone: '+237 699', langue: 'fr', charteAcceptee: true })).rejects.toMatchObject({ code: 'IAM_PHONE_INVALID' })
    await expect(s.premiereConfiguration({ telephone: '', langue: 'fr', charteAcceptee: true })).rejects.toMatchObject({ code: 'IAM_PHONE_REQUIRED' })
    await expect(s.premiereConfiguration({ telephone: '+237 699 00 00 00', langue: 'de' as 'fr', charteAcceptee: true })).rejects.toMatchObject({ code: 'IAM_LANGUAGE_INVALID' })
    await expect(s.premiereConfiguration({ telephone: '+237 699 00 00 00', langue: 'fr', charteAcceptee: false })).rejects.toMatchObject({ code: 'IAM_CHARTER_REQUIRED' })
    const apres = await s.premiereConfiguration({ telephone: '+237 699 00 00 00', langue: 'en', fonction: 'RSSI adjoint', charteAcceptee: true })
    expect(apres.etapesRestantes).toEqual(['UPDATE_PASSWORD', 'VERIFY_EMAIL'])
    expect(apres.compte.langue).toBe('en')
    await s.reconnecter()
    expect((await s.moi()).etapesRestantes).toEqual([])
  })

  it('libre-service : mot de passe, second facteur, action refusée', async () => {
    const s = monCompteSimule({ latenceMs: 0 })
    expect((await s.demanderAction('UPDATE_PASSWORD')).etapesRestantes).toEqual(['UPDATE_PASSWORD'])
    expect((await s.demanderAction('CONFIGURE_TOTP')).etapesRestantes).toEqual(['UPDATE_PASSWORD', 'CONFIGURE_TOTP'])
    await expect(s.demanderAction('VERIFY_EMAIL' as 'UPDATE_PASSWORD')).rejects.toBeInstanceOf(ApiError)
  })

  it('fiche, clés et sessions', async () => {
    const s = monCompteSimule({ latenceMs: 0 })
    await expect(s.modifier({ telephone: 'abc' })).rejects.toMatchObject({ code: 'IAM_PHONE_INVALID' })
    await expect(s.modifier({ langue: 'it' as 'fr' })).rejects.toMatchObject({ code: 'IAM_LANGUAGE_INVALID' })
    expect((await s.modifier({ prenom: 'Awa', nom: 'Diallo', langue: 'es' })).compte.nomAffiche).toBe('Awa Diallo')
    expect((await s.modifier({ prenom: '', nom: '' })).compte.nomAffiche).toBe('demo')
    await expect(s.enrolerCle('  ')).rejects.toMatchObject({ code: 'IAM_KEY_NAME_INVALID' })
    const k = await s.enrolerCle('Clé de secours')
    expect((await s.cles()).map((c) => c.nom)).toContain('Clé de secours')
    await s.supprimerCle(k.id)
    await expect(s.supprimerCle(k.id)).rejects.toMatchObject({ code: 'IAM_KEY_NOT_FOUND' })
    expect(await s.sessions()).toHaveLength(2)
    await s.fermerSession('s-ancienne')
    expect(await s.sessions()).toHaveLength(1)
    await s.fermerSessions()
    expect(await s.sessions()).toHaveLength(0)
    await expect(monCompteSimule({ latenceMs: 0, annulerEnrolement: true }).enrolerCle('x')).rejects.toBeInstanceOf(ReauthCancelled)
    expect((await monCompteSimule({ latenceMs: 0, compte: { secondFacteur: false } }).moi()).etapesRestantes).toEqual(['CONFIGURE_TOTP'])
  })

  it('actions du fournisseur d’identité dans l’ordre d’exécution', () => {
    expect(actionsFournisseur(['FICHE', 'VERIFY_EMAIL', 'UPDATE_PASSWORD'])).toEqual(['UPDATE_PASSWORD', 'VERIFY_EMAIL'])
  })
})

describe('catalogues de Mon compte', () => {
  it('FR, EN et ES portent les mêmes clés, sans traduction vide', () => {
    const cles = (c: Record<string, string>) => Object.keys(c).sort()
    expect(cles(catalogue.en)).toEqual(cles(catalogue.fr))
    expect(cles(catalogue.es)).toEqual(cles(catalogue.fr))
    for (const c of [catalogue.fr, catalogue.en, catalogue.es]) expect(Object.values(c).filter((v) => !v.trim())).toEqual([])
  })
})
