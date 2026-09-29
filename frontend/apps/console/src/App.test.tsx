import { afterEach, beforeAll, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import i18next from 'i18next'
import { initI18n } from '@gimac/socle'
import { catalogueCompte } from '@gimac/compte'
import App from './App'
import { catalogues } from './i18n'
import { fr } from './i18n/fr'
import { en } from './i18n/en'
import { es } from './i18n/es'

// Application console : session de démonstration par profil, navigation élaguée selon les permissions,
// premier accès avant tout écran, écrans réservés, session réelle par la passerelle.
beforeAll(() => {
  if (!i18next.isInitialized) initI18n([catalogueCompte, ...catalogues], 'fr')
  Object.defineProperty(window, 'innerWidth', { value: 1400, configurable: true })
})
afterEach(() => {
  cleanup()
  localStorage.clear()
  vi.unstubAllEnvs()
  vi.restoreAllMocks()
})
const monter = (chemin = '/') => render(<MemoryRouter initialEntries={[chemin]}><App /></MemoryRouter>)

describe('Console', () => {
  it('super administrateur (profil par défaut) : tableau de bord et navigation complète', async () => {
    monter()
    expect(await screen.findByRole('heading', { name: 'Tableau de bord du super administrateur' }, { timeout: 8000 })).toBeTruthy()
    for (const n of ['Comptes', 'Profils et permissions', 'Journal d’administration', 'Politique de sécurité', 'Messagerie', 'Configuration', 'Journalisation', 'Mon compte', 'Se déconnecter']) {
      expect(screen.getByRole('link', { name: n })).toBeTruthy()
    }
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

  it('services réels : sans session, invitation à se connecter ; déconnexion par la passerelle', async () => {
    vi.stubEnv('VITE_SERVICES', 'http')
    vi.spyOn(globalThis, 'fetch').mockImplementation(async () => new Response(JSON.stringify({}), { status: 401, headers: { 'Content-Type': 'application/json' } }))
    monter('/console/tableau-de-bord')
    expect(await screen.findByText(/session a expiré/, {}, { timeout: 8000 })).toBeTruthy()
    cleanup()
    monter('/connexion')
    expect(await screen.findByRole('button', { name: 'Se connecter avec le fournisseur d\'identité' })).toBeTruthy()
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
    await waitFor(() => expect(assign).toHaveBeenCalledWith('/fin'), { timeout: 8000 })
  })
})
