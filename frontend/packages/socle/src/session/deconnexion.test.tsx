import { afterEach, beforeAll, describe, expect, it, vi } from 'vitest'
import { cleanup, render, screen, waitFor } from '@testing-library/react'
import { initI18n } from '../i18n/i18n'
import { deconnecter, navigation, reconnecter } from './deconnexion'
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
  it('déconnexion : adresse de fin de session du fournisseur d’identité', async () => {
    const vers = vi.spyOn(navigation, 'vers').mockImplementation(() => undefined)
    const spy = vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce(json({ logoutUrl: 'https://idp/logout?x=1' }))
    await deconnecter()
    expect(String(spy.mock.calls[0][0])).toContain('/api/logout')
    expect(spy.mock.calls[0][1]?.method).toBe('POST')
    expect(vers).toHaveBeenCalledWith('https://idp/logout?x=1')
  })
  it('déconnexion sans adresse ou session déjà close : retour à la connexion', async () => {
    const vers = vi.spyOn(navigation, 'vers').mockImplementation(() => undefined)
    vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce(json({})).mockRejectedValueOnce(new TypeError('hors ligne'))
    await deconnecter()
    await deconnecter()
    expect(vers).toHaveBeenNthCalledWith(1, '/oauth2/authorization/oidc')
    expect(vers).toHaveBeenNthCalledWith(2, '/oauth2/authorization/oidc')
  })
  it('reconnexion : session close puis nouvelle authentification (actions requises)', async () => {
    const vers = vi.spyOn(navigation, 'vers').mockImplementation(() => undefined)
    vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce(json({ logoutUrl: 'ignoree' })).mockRejectedValueOnce(new TypeError('hors ligne'))
    await reconnecter()
    await reconnecter()
    expect(vers).toHaveBeenCalledTimes(2)
    expect(vers).toHaveBeenCalledWith('/oauth2/authorization/oidc')
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
