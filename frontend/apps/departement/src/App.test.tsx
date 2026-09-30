import { afterEach, beforeAll, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import i18next from 'i18next'
import { initI18n } from '@gimac/socle'
import { catalogues } from '@gimac/lot1'
import { catalogueCompte } from '@gimac/compte'
import App from './App'
import { CLE_PROFIL_DEMO, PROFILS_DEMO, roleDemo, sessionDemo } from './profils'

// Application du département : session de démonstration par profil (D-57, D-58), navigation élaguée
// selon les permissions, premier accès, session réelle par la passerelle, déconnexion.
beforeAll(() => {
  if (!i18next.isInitialized) initI18n([...catalogues, catalogueCompte], 'fr')
  Object.defineProperty(window, 'innerWidth', { value: 1400, configurable: true })
})
afterEach(() => {
  cleanup()
  localStorage.clear()
  vi.unstubAllEnvs()
  vi.unstubAllGlobals()
  vi.restoreAllMocks()
})
/** Canal poussé (SSE) : absent de jsdom, simulé sans événement. */
class FluxMuet {
  onopen: (() => void) | null = null
  onerror: (() => void) | null = null
  onmessage: (() => void) | null = null
  addEventListener() {}
  removeEventListener() {}
  close() {}
}
const monter = (chemin = '/') => render(<MemoryRouter initialEntries={[chemin]}><App /></MemoryRouter>)
const navigation = () => screen.getByRole('navigation', { name: 'Navigation principale' })

describe('profils de démonstration', () => {
  it('chefs du DES et de la DDRM : ouverture et désignation ; ingénieur : ni l’une ni l’autre ; conducteur : lecture', () => {
    for (const r of ['ROLE_CHEF_DES', 'ROLE_CHEF_DDRM']) expect(PROFILS_DEMO[r].permissions).toEqual(expect.arrayContaining(['HOM_LIAISON_OPEN', 'HOM_PROJECT_DESIGNATE']))
    expect(PROFILS_DEMO.ROLE_INGENIEUR_HOMOLOGATION.permissions).not.toContain('HOM_LIAISON_OPEN')
    expect(PROFILS_DEMO.ROLE_CONDUCTEUR_PROJET.permissions).not.toContain('HOM_PROJECT_DESIGNATE')
    expect(PROFILS_DEMO.ROLE_CONDUCTEUR_PROJET.permissions).toContain('HOM_LIAISON_READ')
    expect(PROFILS_DEMO.ROLE_CHARGE_DDRM.permissions).not.toContain('PAR_TRANSITION_APPROVE')
  })
  it('profil choisi par la préférence locale, chef de la DDRM à défaut', () => {
    expect(roleDemo()).toBe('ROLE_CHEF_DDRM')
    localStorage.setItem(CLE_PROFIL_DEMO, 'ROLE_CONDUCTEUR_PROJET')
    expect(sessionDemo()).toMatchObject({ sujet: 'U-201', nom: 'Aïcha Mahamat Nour', roles: ['ROLE_CONDUCTEUR_PROJET'], doubleFacteur: true })
    expect(roleDemo('ROLE_INCONNU')).toBe('ROLE_CHEF_DDRM')
  })
})

describe('Application du département', () => {
  it('chef de la DDRM (défaut) : tableau de bord, « Mon travail », registre, messagerie et Mon compte à son nom', async () => {
    monter()
    expect(await screen.findByRole('heading', { level: 1, name: 'Tableau de bord' }, { timeout: 8000 })).toBeTruthy()
    expect(await screen.findByRole('region', { name: 'Mon travail' })).toBeTruthy()
    // Messages non lus en pastille sur l'entrée « Messagerie » (D-64).
    expect(await screen.findByLabelText(/non lus/)).toBeTruthy()
    expect(screen.getByRole('region', { name: 'Relations avec les membres (DDRM)' })).toBeTruthy()
    for (const n of ['Institutions membres', 'Liaisons et parcours', 'Mon compte', 'Se déconnecter']) expect(screen.getAllByRole('link', { name: n }).length).toBeGreaterThan(0)
    expect(document.querySelector('.gm-footer')).toBeTruthy()
    fireEvent.click(screen.getByRole('link', { name: 'Mon compte' }))
    expect(await screen.findByRole('heading', { level: 1, name: 'Mon compte' })).toBeTruthy()
    expect(await screen.findByDisplayValue('Bertrand')).toBeTruthy()
  })
  it('conducteur de projet : « Mes projets », sans registre des institutions en écriture', async () => {
    localStorage.setItem(CLE_PROFIL_DEMO, 'ROLE_CONDUCTEUR_PROJET')
    monter('/des/liaisons')
    expect(await screen.findByRole('switch', { name: 'Mes projets' }, { timeout: 8000 })).toBeTruthy()
    fireEvent.click(screen.getAllByRole('link', { name: 'Tableau de bord' })[0])
    expect(await screen.findByRole('heading', { name: /^Mes projets \(/ })).toBeTruthy()
  })
  it.each([
    ['/des/institutions', 'Institutions membres'],
    ['/des/institutions/INST-0105', 'Mobile Money Sanaga'],
    ['/des/liaisons/LIA-2026-030', 'Établissement de microfinance C'],
    ['/des/liaisons/LIA-2025-102/mise-en-service', 'Banque B'],
    ['/des/liaisons/LIA-2026-017/operations/OPI-017-3/cloture', /OP-E04/],
    ['/des/referentiels/operations', 'Référentiels d\'opérations'],
    ['/des/messagerie?conversation=CONV-0001', 'Messagerie'],
  ])('route %s : écran chargé à la demande', async (chemin, titre) => {
    monter(chemin)
    expect(await screen.findByRole('heading', { level: 1, name: titre }, { timeout: 8000 })).toBeTruthy()
  })
  it('adresse inconnue : retour au tableau de bord', async () => {
    monter('/des/inconnue')
    expect(await screen.findByRole('heading', { level: 1, name: 'Tableau de bord' }, { timeout: 8000 })).toBeTruthy()
    cleanup()
    monter('/ailleurs')
    expect(await screen.findByRole('heading', { level: 1, name: 'Tableau de bord' }, { timeout: 8000 })).toBeTruthy()
  })
  it('premier accès : l’assistant remplace tous les écrans, sans navigation', async () => {
    localStorage.setItem('gm.premierAccesDemo', '1')
    monter('/des/institutions')
    expect(await screen.findByRole('heading', { name: 'Premier accès à la plateforme' }, { timeout: 8000 })).toBeTruthy()
    expect(navigation().querySelectorAll('a[href="/des/institutions"]')).toHaveLength(0)
  })
  it('services réels : session de la passerelle, Mon compte de la couche des', async () => {
    vi.stubEnv('VITE_SERVICES', 'http')
    vi.stubGlobal('EventSource', FluxMuet)
    const reponse = (corps: unknown, status = 200) => new Response(JSON.stringify(corps), { status, headers: { 'Content-Type': 'application/json' } })
    const spy = vi.spyOn(globalThis, 'fetch').mockImplementation(async (u) => {
      const chemin = new URL(String(u)).pathname
      if (chemin === '/api/session') return reponse({ sujet: 'u-7', nom: 'Agent SNA', identifiant: 'sna', roles: ['ROLE_SNA'], permissions: ['HOM_LIAISON_LIST'], institutionId: '', acr: '2', doubleFacteur: true })
      if (chemin === '/bff/des/iam/moi') return reponse({ compte: { id: 'u-7', nomAffiche: 'Agent SNA', profils: ['ROLE_SNA'], actionsRequises: [], profilComplet: true }, clesSecurite: 1, sessions: 1, etapesRestantes: [] })
      if (chemin.endsWith('/liaisons')) return reponse({ contenu: [], total: 0 })
      return reponse({}, 404)
    })
    monter('/des/liaisons')
    expect(await screen.findByRole('heading', { level: 1, name: 'Liaisons et parcours' }, { timeout: 8000 })).toBeTruthy()
    await waitFor(() => expect(spy.mock.calls.map(([u]) => new URL(String(u)).pathname)).toContain('/bff/des/homologation/liaisons'))
    expect(spy.mock.calls.map(([u]) => new URL(String(u)).pathname)).toContain('/bff/des/iam/moi')
  })
  it('services réels : sans session, invitation à se connecter ; écran de connexion', async () => {
    vi.stubEnv('VITE_SERVICES', 'http')
    vi.stubGlobal('EventSource', FluxMuet)
    const assign = vi.fn()
    Object.defineProperty(window, 'location', { value: { ...window.location, assign }, configurable: true })
    vi.spyOn(globalThis, 'fetch').mockImplementation(async () => new Response(JSON.stringify({}), { status: 401, headers: { 'Content-Type': 'application/json' } }))
    monter('/des/tableau-de-bord')
    expect(await screen.findByText(/session a expiré/, {}, { timeout: 8000 })).toBeTruthy()
    await waitFor(() => expect(assign).toHaveBeenCalledWith('/oauth2/authorization/oidc'))
    cleanup()
    monter('/connexion')
    expect(await screen.findByRole('button', { name: 'Se connecter avec le fournisseur d\'identité' })).toBeTruthy()
  })
  it('déconnexion : la passerelle clôt la session', async () => {
    const assign = vi.fn()
    Object.defineProperty(window, 'location', { value: { ...window.location, assign }, configurable: true })
    vi.spyOn(globalThis, 'fetch').mockImplementation(async () => new Response(JSON.stringify({ logoutUrl: '/fin' }), { status: 200, headers: { 'Content-Type': 'application/json' } }))
    monter('/des/deconnexion')
    await waitFor(() => expect(assign).toHaveBeenCalledWith('/fin'), { timeout: 8000 })
  })
})
