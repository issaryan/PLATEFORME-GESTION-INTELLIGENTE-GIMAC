import { afterEach, beforeAll, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { ApiError } from '../http/errors'
import { initI18n } from '../i18n/i18n'
import { navigation } from './deconnexion'
import { GardeSession, SessionActive, SessionProvider, aPermission, chargerSession, usePermissions, useSession, type Session } from './session'

// D-61 : accès par la matrice des rôles d'un jeton issu d'une authentification à deux facteurs.
beforeAll(() => {
  initI18n([], 'fr')
})
afterEach(() => {
  cleanup()
  vi.restoreAllMocks()
})

const session = (s: Partial<Session> = {}): Session => ({
  sujet: 'u-1', nom: 'Chef DDRM', identifiant: 'test-chef-ddrm', roles: ['ROLE_CHEF_DDRM'],
  permissions: ['PAR_INSTITUTION_READ', 'PAR_TRANSITION_APPROVE'], institutionId: '', acr: '2', doubleFacteur: true, ...s,
})

function Sonde() {
  const s = useSession()
  const p = usePermissions()
  return <span>{s ? `${s.nom}:${(p ?? []).join(',')}` : 'hors-session'}</span>
}

describe('Session', () => {
  it('aPermission : aucune exigée, l’une des exigées, aucune session', () => {
    expect(aPermission(undefined)).toBe(true)
    expect(aPermission(['A', 'B'], 'C', 'B')).toBe(true)
    expect(aPermission(['A'], 'B')).toBe(false)
    expect(aPermission(undefined, 'A')).toBe(false)
  })

  it('hors fournisseur : pas de session ni de permissions', () => {
    render(<Sonde />)
    expect(screen.getByText('hors-session')).toBeTruthy()
  })

  it('fournisseur : session et permissions exposées', () => {
    render(<SessionProvider session={session()}><Sonde /></SessionProvider>)
    expect(screen.getByText('Chef DDRM:PAR_INSTITUTION_READ,PAR_TRANSITION_APPROVE')).toBeTruthy()
  })

  it('chargerSession lit /api/session', async () => {
    const spy = vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce(new Response(JSON.stringify(session()), { status: 200, headers: { 'Content-Type': 'application/json' } }))
    expect((await chargerSession()).identifiant).toBe('test-chef-ddrm')
    expect(String(spy.mock.calls[0][0])).toContain('/api/session')
  })
})

describe('GardeSession', () => {
  it('chargement puis contenu protégé avec la session à deux facteurs', async () => {
    const { container } = render(<GardeSession charger={async () => session()}>{(s) => <><span>{s.nom}</span><Sonde /></>}</GardeSession>)
    expect(container.querySelector('.gm-skeleton')).toBeTruthy()
    expect(await screen.findByText('Chef DDRM')).toBeTruthy()
    expect(screen.getByText('Chef DDRM:PAR_INSTITUTION_READ,PAR_TRANSITION_APPROVE')).toBeTruthy()
  })

  it('jeton sans second facteur : accès refusé et reconnexion proposée', async () => {
    render(<GardeSession charger={async () => session({ acr: '1', doubleFacteur: false })}>{() => <span>protégé</span>}</GardeSession>)
    expect(await screen.findByText(/authentification à deux facteurs/)).toBeTruthy()
    expect(screen.queryByText('protégé')).toBeNull()
    expect(screen.getByRole('button', { name: 'Se connecter' })).toBeTruthy()
  })

  it('sans session (401) : renvoi vers l’écran de connexion de la plateforme, avec la page en retour', async () => {
    const vers = vi.spyOn(navigation, 'vers').mockImplementation(() => undefined)
    window.history.pushState({}, '', '/des/liaisons')
    render(<GardeSession charger={async () => { throw new ApiError(401, 'HTTP_401', '', '') }}>{() => <span>protégé</span>}</GardeSession>)
    expect(await screen.findByText(/session a expiré/)).toBeTruthy()
    expect(screen.queryByText('protégé')).toBeNull()
    expect(vers).toHaveBeenCalledWith('/connexion?retour=%2Fdes%2Fliaisons')
    fireEvent.click(screen.getByRole('button', { name: 'Se connecter' }))
    expect(vers).toHaveBeenCalledTimes(2)
    window.history.pushState({}, '', '/')
  })

  it('adresse de connexion imposée', async () => {
    const vers = vi.spyOn(navigation, 'vers').mockImplementation(() => undefined)
    render(<GardeSession connexion="/ailleurs" charger={async () => { throw new ApiError(401, 'HTTP_401', '', '') }}>{() => <span>protégé</span>}</GardeSession>)
    await waitFor(() => expect(vers).toHaveBeenCalledWith('/ailleurs'))
  })

  it('session restreinte du premier accès : servie sans second facteur (assistant de premier accès)', async () => {
    render(<GardeSession charger={async () => session({ acr: '1', doubleFacteur: false, restreinte: true })}>{() => <span>assistant</span>}</GardeSession>)
    expect(await screen.findByText('assistant')).toBeTruthy()
  })

  it('SessionActive sans surveillance : fournit la session seule', () => {
    render(<SessionActive session={session()} surveiller={false}><Sonde /></SessionActive>)
    expect(screen.getByText('Chef DDRM:PAR_INSTITUTION_READ,PAR_TRANSITION_APPROVE')).toBeTruthy()
    expect(screen.queryByRole('alertdialog')).toBeNull()
  })

  it('erreur de la passerelle : identifiant de corrélation affiché', async () => {
    render(<GardeSession charger={async () => { throw new ApiError(503, 'HTTP_503', '', 'cid-503') }}>{() => <span>protégé</span>}</GardeSession>)
    await waitFor(() => expect(document.body.textContent).toContain('cid-503'))
    expect(screen.queryByText('protégé')).toBeNull()
  })
})
