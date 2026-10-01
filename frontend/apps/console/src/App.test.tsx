import { afterEach, beforeAll, beforeEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import i18next from 'i18next'
import { MOT_DE_PASSE_DEMO, initI18n } from '@gimac/socle'
import { catalogueCompte } from '@gimac/compte'
import { catalogueMessagerie } from '@gimac/lot1'
import App from './App'
import { catalogues } from './i18n'
import { fr } from './i18n/fr'
import { en } from './i18n/en'
import { es } from './i18n/es'

// Application console : session de démonstration par profil, navigation élaguée selon les permissions,
// premier accès avant tout écran, écrans réservés, session réelle par la passerelle.
beforeAll(() => {
  if (!i18next.isInitialized) initI18n([catalogueCompte, catalogueMessagerie, ...catalogues], 'fr')
  Object.defineProperty(window, 'innerWidth', { value: 1400, configurable: true })
})
// Session de démonstration ouverte (connexion simulée, D-65) ; retirée par les scénarios de connexion.
beforeEach(() => sessionStorage.setItem('gm.sessionDemo', JSON.stringify({ identifiant: 'test' })))
afterEach(() => {
  cleanup()
  localStorage.clear()
  sessionStorage.clear()
  vi.unstubAllEnvs()
  vi.restoreAllMocks()
})
const monter = (chemin = '/') => render(<MemoryRouter initialEntries={[chemin]}><App /></MemoryRouter>)

describe('Console', () => {
  it('super administrateur (profil par défaut) : tableau de bord et navigation complète', async () => {
    monter()
    expect(await screen.findByRole('heading', { name: 'Tableau de bord du super administrateur' }, { timeout: 8000 })).toBeTruthy()
    for (const n of ['Comptes', 'Profils et permissions', 'Journal d’administration', 'Politique de sécurité', 'Courriels de la plateforme', 'Configuration', 'Journalisation', 'Mon compte', 'Se déconnecter']) {
      expect(screen.getByRole('link', { name: n })).toBeTruthy()
    }
    // Messagerie instantanée (D-64) : ouverte au super administrateur, pastille des non-lus de l'annuaire simulé.
    const messagerie = screen.getByRole('link', { name: /^Messagerie/ })
    expect(messagerie.getAttribute('href')).toBe('/console/messagerie')
    expect(await within(messagerie).findByLabelText('3 non lus')).toBeTruthy()
    fireEvent.click(messagerie)
    expect(await screen.findByRole('heading', { level: 1, name: 'Messagerie' }, { timeout: 8000 })).toBeTruthy()
    expect(await screen.findByRole('list', { name: 'Conversations' })).toBeTruthy()
    expect(document.querySelector('.gm-footer')).toBeTruthy()
    fireEvent.click(screen.getByRole('link', { name: 'Mon compte' }))
    expect(await screen.findByRole('heading', { level: 1, name: 'Mon compte' })).toBeTruthy()
  })

  it('RSSI (gm.profilDemo) : son tableau de bord ; un écran non permis est refusé', async () => {
    localStorage.setItem('gm.profilDemo', 'ROLE_RSSI')
    monter('/console/plateforme/journalisation')
    expect(await screen.findByRole('heading', { name: 'Journalisation des services' }, { timeout: 8000 })).toBeTruthy()
    fireEvent.click(screen.getByRole('link', { name: 'Tableau de bord' }))
    expect(await screen.findByRole('heading', { name: 'Tableau de bord de la sécurité des SI' })).toBeTruthy()
    // Le RSSI, membre interne, dispose de la messagerie instantanée (D-64).
    expect(screen.getByRole('link', { name: /^Messagerie/ }).getAttribute('href')).toBe('/console/messagerie')
    cleanup()
    localStorage.setItem('gm.profilDemo', 'ROLE_INCONNU')
    monter('/console/inconnue')
    expect(await screen.findByRole('heading', { name: 'Tableau de bord du super administrateur' }, { timeout: 8000 })).toBeTruthy()
  })

  it('services réels : session de la passerelle, « Mon compte » de la couche console, écran réservé refusé', async () => {
    vi.stubEnv('VITE_SERVICES', 'http')
    const reponse = (corps: unknown) => new Response(JSON.stringify(corps), { status: 200, headers: { 'Content-Type': 'application/json' } })
    const spy = vi.spyOn(globalThis, 'fetch').mockImplementation(async (u) => {
      const chemin = new URL(String(u)).pathname
      if (chemin === '/api/session') return reponse({ sujet: 'u-9', nom: 'Auditeur', identifiant: 'auditeur', roles: [], permissions: ['IAM_ACCOUNT_READ'], institutionId: '', acr: '2', doubleFacteur: true })
      if (chemin === '/bff/console/iam/moi') return reponse({ compte: { id: 'u-9', nomAffiche: 'Auditeur', profils: [], actionsRequises: [] }, clesSecurite: 1, sessions: 1, etapesRestantes: [] })
      return reponse({})
    })
    monter('/console/comptes/nouveau')
    expect(await screen.findByText('Accès refusé', {}, { timeout: 8000 })).toBeTruthy()
    expect(screen.queryByRole('link', { name: 'Journalisation' })).toBeNull()
    expect(spy.mock.calls.map(([u]) => new URL(String(u)).pathname)).toContain('/bff/console/iam/moi')
  })

  it('premier accès : l’assistant remplace tous les écrans, sans navigation', async () => {
    localStorage.setItem('gm.premierAccesDemo', '1')
    monter('/console/comptes')
    expect(await screen.findByRole('heading', { name: 'Premier accès à la plateforme' }, { timeout: 8000 })).toBeTruthy()
    expect(screen.queryByRole('link', { name: 'Comptes' })).toBeNull()
    expect(screen.getByRole('link', { name: 'Se déconnecter' })).toBeTruthy()
  })

  it('services réels : sans session, retour à l’écran de connexion de la plateforme (D-65)', async () => {
    vi.stubEnv('VITE_SERVICES', 'http')
    const assign = vi.fn()
    Object.defineProperty(window, 'location', { value: { ...window.location, assign }, configurable: true })
    vi.spyOn(globalThis, 'fetch').mockImplementation(async () => new Response(JSON.stringify({}), { status: 401, headers: { 'Content-Type': 'application/json' } }))
    monter('/console/tableau-de-bord')
    expect(await screen.findByText(/session a expiré/, {}, { timeout: 8000 })).toBeTruthy()
    await waitFor(() => expect(assign).toHaveBeenCalledWith(expect.stringMatching(/^\/connexion(\?|$)/)))
    cleanup()
    monter('/connexion')
    expect(await screen.findByRole('button', { name: 'Se connecter' })).toBeTruthy()
  })

  it('services simulés : sans session, écran de connexion ; RSSI connecté avec son code', async () => {
    sessionStorage.clear()
    const assign = vi.fn()
    Object.defineProperty(window, 'location', { value: { ...window.location, assign }, configurable: true })
    monter('/console/comptes')
    expect(await screen.findByRole('button', { name: 'Se connecter' }, { timeout: 8000 })).toBeTruthy()
    fireEvent.change(document.querySelector('input[name="identifiant"]')!, { target: { value: 'rssi' } })
    fireEvent.change(document.querySelector('input[name="motDePasse"]')!, { target: { value: MOT_DE_PASSE_DEMO } })
    fireEvent.click(screen.getByRole('button', { name: 'Se connecter' }))
    fireEvent.change(await screen.findByLabelText(/^Code à six chiffres/, {}, { timeout: 8000 }), { target: { value: '000000' } })
    fireEvent.click(screen.getByRole('button', { name: 'Vérifier le code' }))
    await waitFor(() => expect(assign).toHaveBeenCalledWith('/console/comptes'), { timeout: 8000 })
    expect(JSON.parse(sessionStorage.getItem('gm.sessionDemo')!)).toMatchObject({ identifiant: 'rssi', role: 'ROLE_RSSI' })
  })
})

describe('catalogues de la console', () => {
  it('FR, EN et ES portent les mêmes clés, sans traduction vide', () => {
    const cles = (c: Record<string, string>) => Object.keys(c).sort()
    expect(cles(en)).toEqual(cles(fr))
    expect(cles(es)).toEqual(cles(fr))
    for (const c of [fr, en, es]) expect(Object.values(c).filter((v) => !v.trim())).toEqual([])
  })
})

describe('déconnexion', () => {
  it('route /console/deconnexion : la passerelle clôt la session', async () => {
    const assign = vi.fn()
    Object.defineProperty(window, 'location', { value: { ...window.location, assign }, configurable: true })
    vi.spyOn(globalThis, 'fetch').mockImplementation(async () => new Response(JSON.stringify({ logoutUrl: '/fin' }), { status: 200, headers: { 'Content-Type': 'application/json' } }))
    monter('/console/deconnexion')
    await waitFor(() => expect(assign).toHaveBeenCalledWith('/connexion?motif=deconnexion'), { timeout: 8000 })
  })
})
