import { afterEach, beforeAll, describe, expect, it, vi } from 'vitest'
import { cleanup, render, screen, waitFor } from '@testing-library/react'
import { initI18n } from '../i18n/i18n'
import { ApiError } from '../http/errors'
import { adresseConnexion, deconnecter, navigation, reconnecter, retourSur, surNonAutorise } from './deconnexion'
import { PageDeconnexion } from './PageDeconnexion'

// D-62 : déconnexion et reconnexion (actions requises) par la passerelle.
const json = (corps: object) => new Response(JSON.stringify(corps), { status: 200, headers: { 'Content-Type': 'application/json' } })
beforeAll(() => {
  initI18n([], 'fr')
})
afterEach(() => {
  cleanup()
  vi.restoreAllMocks()
})

describe('déconnexion', () => {
  it('déconnexion : la passerelle ferme la session, retour à la connexion sans page du fournisseur d’identité', async () => {
    const vers = vi.spyOn(navigation, 'vers').mockImplementation(() => undefined)
    const spy = vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce(json({ logoutUrl: 'https://idp/logout?x=1' }))
    sessionStorage.setItem('gm.sessionDemo', '{"identifiant":"x"}')
    await deconnecter()
    expect(String(spy.mock.calls[0][0])).toContain('/api/logout')
    expect(spy.mock.calls[0][1]?.method).toBe('POST')
    expect(vers).toHaveBeenCalledWith('/connexion?motif=deconnexion')
    expect(sessionStorage.getItem('gm.sessionDemo')).toBeNull()
  })
  it('session déjà close, inactivité : retour à la connexion avec le motif, la durée et la page', async () => {
    const vers = vi.spyOn(navigation, 'vers').mockImplementation(() => undefined)
    vi.spyOn(globalThis, 'fetch').mockRejectedValueOnce(new TypeError('hors ligne'))
    window.history.pushState({}, '', '/des/liaisons?page=2')
    await deconnecter('inactivite', 30)
    expect(vers).toHaveBeenCalledWith('/connexion?motif=inactivite&minutes=30&retour=%2Fdes%2Fliaisons%3Fpage%3D2')
    window.history.pushState({}, '', '/')
  })
  it('reconnexion : session close puis écran de connexion', async () => {
    const vers = vi.spyOn(navigation, 'vers').mockImplementation(() => undefined)
    vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce(json({}))
    await reconnecter()
    expect(vers).toHaveBeenCalledWith('/connexion?motif=deconnexion')
  })
  it('401 : CNX_SESSION_EXPIREE ramène à la connexion avec le motif ; autre 401 sans motif', async () => {
    const vers = vi.spyOn(navigation, 'vers').mockImplementation(() => undefined)
    window.history.pushState({}, '', '/console/comptes')
    expect(await surNonAutorise(new ApiError(401, 'CNX_SESSION_EXPIREE', '', ''))).toBe(false)
    expect(vers).toHaveBeenLastCalledWith('/connexion?motif=expiree&retour=%2Fconsole%2Fcomptes')
    await surNonAutorise()
    expect(vers).toHaveBeenLastCalledWith('/connexion?retour=%2Fconsole%2Fcomptes')
    window.history.pushState({}, '', '/connexion?motif=x')
    await surNonAutorise()
    expect(vers).toHaveBeenLastCalledWith('/connexion')
    window.history.pushState({}, '', '/')
  })
  it('retour : chemins internes seulement (pas de redirection ouverte), jamais l’écran de connexion', () => {
    expect(retourSur('/des/x')).toBe('/des/x')
    expect(retourSur('//evil.example')).toBeUndefined()
    expect(retourSur('/\\evil')).toBeUndefined()
    expect(retourSur('https://evil.example')).toBeUndefined()
    expect(retourSur('/connexion/mot-de-passe-oublie')).toBeUndefined()
    expect(retourSur(null)).toBeUndefined()
    expect(adresseConnexion()).toBe('/connexion')
  })
  it('navigation par défaut : window.location.assign', () => {
    const assign = vi.fn()
    const origine = window.location
    Object.defineProperty(window, 'location', { value: { ...origine, assign }, configurable: true })
    navigation.vers('/x')
    expect(assign).toHaveBeenCalledWith('/x')
    Object.defineProperty(window, 'location', { value: origine, configurable: true })
  })
  it('page de déconnexion : action lancée une seule fois, état annoncé', async () => {
    const agir = vi.fn(async () => undefined)
    const { rerender } = render(<PageDeconnexion agir={agir} />)
    rerender(<PageDeconnexion agir={agir} />)
    expect(screen.getByRole('status').textContent).toContain('Déconnexion en cours')
    await waitFor(() => expect(agir).toHaveBeenCalledTimes(1))
  })
})
