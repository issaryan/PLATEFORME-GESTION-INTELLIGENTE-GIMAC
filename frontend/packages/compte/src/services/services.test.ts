import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError, MOT_DE_PASSE_DEMO, ReauthCancelled, connexionSimulee, simulationConnexion, type Session } from '@gimac/socle'
import { catalogue } from '../i18n'
import { actionsFournisseur, etapesAssistant } from '../modele'
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
    await s.changerMotDePasse({ actuel: 'a', nouveau: 'b' })
    await s.enrolerTotp()
    await s.confirmerTotp({ enrolementId: 'e-1', code: '123456', libelle: 'Téléphone' })
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
      'POST /bff/portail/iam/moi/mot-de-passe',
      'POST /bff/portail/iam/moi/second-facteur/totp/enrolement',
      'POST /bff/portail/iam/moi/second-facteur/totp',
      'GET /bff/portail/iam/moi/sessions',
      'DELETE /bff/portail/iam/moi/sessions/s%2F1',
      'DELETE /bff/portail/iam/moi/sessions',
      'GET /bff/portail/iam/moi/cles-securite',
      'DELETE /bff/portail/iam/moi/cles-securite/k-1',
    ])
    expect(JSON.parse(String(spy.mock.calls[3][1]?.body))).toEqual({ actuel: 'a', nouveau: 'b' })
    expect(JSON.parse(String(spy.mock.calls[5][1]?.body))).toEqual({ enrolementId: 'e-1', code: '123456', libelle: 'Téléphone' })
  })

  it('code TOTP hors premier accès : 428 puis réauthentification ACCOUNT_SENSITIVE (X-Reauth-Assertion) ; autre refus propagé', async () => {
    Object.defineProperty(navigator, 'credentials', {
      value: { get: async () => ({ id: 'k', response: { clientDataJSON: new Uint8Array([1]).buffer, authenticatorData: new Uint8Array([2]).buffer, signature: new Uint8Array([3]).buffer, userHandle: null } }) },
      configurable: true,
    })
    const spy = vi.spyOn(globalThis, 'fetch')
      .mockResolvedValueOnce(json({ code: 'IAM_REAUTH_REQUIRED' }, 428))
      .mockResolvedValueOnce(json({ challengeId: 'c', challenge: 'AQ', rpId: 'r', allowCredentials: [], timeoutMs: 1 }))
      .mockResolvedValueOnce(json(null, 204))
      .mockResolvedValueOnce(json({ code: 'IAM_TOTP_CODE_INVALID' }, 400))
    const s = monCompteHttp('des')
    await s.confirmerTotp({ enrolementId: 'e', code: '000000' })
    expect(JSON.parse(String(spy.mock.calls[1][1]?.body))).toEqual({ purpose: 'ACCOUNT_SENSITIVE' })
    expect(new Headers(spy.mock.calls[2][1]?.headers).get('X-Reauth-Assertion')).toBeTruthy()
    await expect(s.confirmerTotp({ enrolementId: 'e', code: '000000' })).rejects.toMatchObject({ code: 'IAM_TOTP_CODE_INVALID' })
  })

  it('enrôlement d’une clé par l’assistant WebAuthn du socle', async () => {
    vi.spyOn(globalThis, 'fetch')
      .mockResolvedValueOnce(json({ challengeId: 'c', challenge: 'AQ', rp: { id: 'r', name: 'n' }, user: { id: 'dQ', name: 'u', displayName: 'U' }, pubKeyCredParams: [{ type: 'public-key', alg: -7 }], timeoutMs: 1, excludeCredentials: [] }))
      .mockResolvedValueOnce(json({ id: 'k-9', nom: 'Clé', creeeLe: '2026-09-29T00:00:00Z', derniereUtilisation: null }))
    Object.defineProperty(navigator, 'credentials', { value: { create: async () => ({ id: 'x', response: { clientDataJSON: new Uint8Array([1]).buffer, attestationObject: new Uint8Array([2]).buffer } }) }, configurable: true })
    expect((await monCompteHttp('des').enrolerCle('Clé')).id).toBe('k-9')
  })
})

describe('Mon compte : service simulé', () => {
  beforeEach(() => simulationConnexion.reinitialiser())

  it('premier accès : mot de passe, code TOTP, fiche et charte ; fin transmise à la connexion simulée', async () => {
    const s = monCompteSimule({ latenceMs: 0, premierAcces: true, compte: { identifiant: 'nouvel.agent' } })
    const m = await s.moi()
    expect(m.etapesRestantes).toEqual(['MOT_DE_PASSE', 'SECOND_FACTEUR', 'FICHE', 'CHARTE'])
    expect(m.compte.secondFacteur).toBe(false)
    expect(m.clesSecurite).toBe(0)
    await expect(s.changerMotDePasse({ actuel: 'faux', nouveau: 'Nouveau-Mdp-2026!' })).rejects.toMatchObject({ code: 'IAM_PASSWORD_INVALID' })
    await expect(s.changerMotDePasse({ actuel: MOT_DE_PASSE_DEMO, nouveau: MOT_DE_PASSE_DEMO })).rejects.toMatchObject({ code: 'IAM_PASSWORD_REUSED' })
    await expect(s.changerMotDePasse({ actuel: MOT_DE_PASSE_DEMO, nouveau: 'court' })).rejects.toMatchObject({ code: 'IAM_PASSWORD_POLICY', details: { violations: ['LONGUEUR', 'MAJUSCULE', 'CHIFFRE', 'SPECIAL'] } })
    await s.changerMotDePasse({ actuel: MOT_DE_PASSE_DEMO, nouveau: 'Nouveau-Mdp-2026!' })
    const e = await s.enrolerTotp()
    expect(e.secret).toMatch(/^[A-Z2-7]{32}$/)
    expect(e.uri).toBe(`otpauth://totp/GIMAC%20DES%3Anouvel.agent?secret=${e.secret}&issuer=GIMAC%20DES&digits=6&period=30&algorithm=SHA1`)
    await expect(s.confirmerTotp({ enrolementId: e.enrolementId, code: '12' })).rejects.toMatchObject({ code: 'IAM_TOTP_CODE_INVALID' })
    await expect(s.confirmerTotp({ enrolementId: 'inconnu', code: '123456' })).rejects.toMatchObject({ code: 'IAM_TOTP_ENROLMENT_EXPIRED' })
    await s.confirmerTotp({ enrolementId: e.enrolementId, code: '000000' })
    await expect(s.premiereConfiguration({ telephone: '+237 699', langue: 'fr', charteAcceptee: true })).rejects.toMatchObject({ code: 'IAM_PHONE_INVALID' })
    await expect(s.premiereConfiguration({ telephone: '', langue: 'fr', charteAcceptee: true })).rejects.toMatchObject({ code: 'IAM_PHONE_REQUIRED' })
    await expect(s.premiereConfiguration({ telephone: '+237 699 00 00 00', langue: 'de' as 'fr', charteAcceptee: true })).rejects.toMatchObject({ code: 'IAM_LANGUAGE_INVALID' })
    await expect(s.premiereConfiguration({ telephone: '+237 699 00 00 00', langue: 'fr', charteAcceptee: false })).rejects.toMatchObject({ code: 'IAM_CHARTER_REQUIRED' })
    const apres = await s.premiereConfiguration({ telephone: '+237 699 00 00 00', langue: 'en', fonction: 'RSSI adjoint', charteAcceptee: true })
    expect(apres.etapesRestantes).toEqual([])
    expect(apres.compte.langue).toBe('en')
    const cnx = connexionSimulee({ latenceMs: 0, comptes: [{ identifiant: 'nouvel.agent', couches: ['des'], etat: 'PREMIER_ACCES', session: {} as Session }] })
    expect(await cnx.connecter({ identifiant: 'nouvel.agent', motDePasse: 'Nouveau-Mdp-2026!', couche: 'des', code: '000000' })).toMatchObject({ etat: 'CONNECTE' })
  })

  it('étapes choisies ; hors premier accès, fiche incomplète et second facteur absent restent à faire', async () => {
    expect((await monCompteSimule({ latenceMs: 0, premierAcces: true, etapes: ['CHARTE'] }).moi()).etapesRestantes).toEqual(['CHARTE'])
    expect((await monCompteSimule({ latenceMs: 0, compte: { profilComplet: false, secondFacteur: false } }).moi()).etapesRestantes).toEqual(['FICHE', 'SECOND_FACTEUR'])
  })

  it('fiche, mot de passe, clés et sessions', async () => {
    const s = monCompteSimule({ latenceMs: 0, motDePasse: 'Actuel-Mdp-2026!' })
    await s.changerMotDePasse({ actuel: 'Actuel-Mdp-2026!', nouveau: 'Suivant-Mdp-2026!' })
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
    await expect(s.enrolerCle('x')).resolves.toBeTruthy()
    expect(ApiError).toBeTruthy()
  })

  it('étapes de l’assistant dans l’ordre, noms historiques compris ; actions de l’administration', () => {
    expect(etapesAssistant(['CHARTE', 'VERIFIER_COURRIEL', 'UPDATE_PASSWORD', 'CONFIGURE_TOTP', 'UPDATE_PROFILE'])).toEqual(['MOT_DE_PASSE', 'SECOND_FACTEUR', 'FICHE', 'CHARTE'])
    expect(actionsFournisseur(['CHARTE', 'VERIFIER_COURRIEL', 'MOT_DE_PASSE'])).toEqual(['MOT_DE_PASSE', 'VERIFIER_COURRIEL'])
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
