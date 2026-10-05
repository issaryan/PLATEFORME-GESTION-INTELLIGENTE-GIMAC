import { afterEach, beforeAll, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { ThemeProvider } from '@gimac/design-system'
import { initI18n } from '../i18n/i18n'
import type { Session } from './session'
import { navigation } from './deconnexion'
import { lireSessionDemo, ouvrirSessionDemo } from './demo'
import {
  ADRESSES_APPLICATIONS, adresseApplication, adresseConnexionApplication, applicationApresRefus, applicationDe, estCompteInstitution, ouvreConsole, reprendreSessionDemo, satisfait,
} from './aiguillage'
import { Aiguillage, reglageAiguillage } from './Aiguillage'

// D-79 : aiguillage des comptes entre application interne, portail et console, par la session seule.

beforeAll(() => {
  initI18n([], 'fr')
})
afterEach(() => {
  cleanup()
  vi.restoreAllMocks()
  sessionStorage.clear()
})

const s = (o: Partial<Session> = {}): Session => ({ sujet: 'x', nom: 'X', identifiant: 'x', roles: [], permissions: [], institutionId: '', acr: '2', doubleFacteur: true, ...o })

describe('prédicats de permissions', () => {
  it('any : l’une au moins ; all : toutes ; absent ou vide : satisfait', () => {
    expect(satisfait(['A'], undefined)).toBe(true)
    expect(satisfait(undefined, {})).toBe(true)
    expect(satisfait(['A'], { any: ['A', 'B'] })).toBe(true)
    expect(satisfait(['C'], { any: ['A', 'B'] })).toBe(false)
    expect(satisfait(['A', 'B'], { all: ['A', 'B'] })).toBe(true)
    expect(satisfait(['A'], { all: ['A', 'B'] })).toBe(false)
    expect(satisfait(['A', 'C'], { all: ['A'], any: ['B', 'C'] })).toBe(true)
    expect(satisfait(undefined, { any: ['A'] })).toBe(false)
  })
})

describe('application de la session', () => {
  it('compte d’institution : portail, d’où qu’il vienne', () => {
    const c = s({ institutionId: 'INST-0042' })
    expect(estCompteInstitution(c)).toBe(true)
    for (const couche of ['des', 'portail', 'console'] as const) expect(applicationDe(c, couche)).toBe('portail')
  })
  it('compte interne : application interne depuis le portail ; console seulement avec une permission de console', () => {
    expect(applicationDe(s(), 'portail')).toBe('des')
    expect(applicationDe(s(), 'des')).toBe('des')
    expect(applicationDe(s({ permissions: ['COL_MESSAGE_READ'] }), 'console')).toBe('des')
    for (const p of ['IAM_ACCOUNT_READ', 'PLATFORM_SUPERVISION_READ', 'PLATFORM_CONFIG_READ', 'PLATFORM_OPS_READ', 'PLATFORM_SECURITY_POLICY_READ', 'PLATFORM_MESSAGING_EDIT']) {
      expect(ouvreConsole([p])).toBe(true)
      expect(applicationDe(s({ permissions: [p] }), 'console')).toBe('console')
    }
    // Premier accès : session restreinte sans permission, l'assistant se déroule là où le compte s'est connecté.
    expect(applicationDe(s({ restreinte: true }), 'console')).toBe('console')
  })
  it('après un refus de couche : application interne depuis le portail, portail sinon', () => {
    expect(applicationApresRefus('portail')).toBe('des')
    expect(applicationApresRefus('des')).toBe('portail')
    expect(applicationApresRefus('console')).toBe('portail')
  })
  it('adresses : par défaut 4200, 4201 et 4202 ; session de démonstration dans le fragment', () => {
    expect(ADRESSES_APPLICATIONS).toEqual({ des: 'http://localhost:4200', portail: 'http://localhost:4201', console: 'http://localhost:4202' })
    expect(adresseApplication('des')).toBe('http://localhost:4200/des/tableau-de-bord')
    expect(adresseApplication('console', '/console/comptes')).toBe('http://localhost:4202/console/comptes')
    expect(adresseApplication('des', undefined, { identifiant: 'a', role: 'R' })).toBe(`http://localhost:4200/des/tableau-de-bord#gm-session-demo=${encodeURIComponent('{"identifiant":"a","role":"R"}')}`)
    expect(adresseConnexionApplication('portail', { depuis: 'des' })).toBe('http://localhost:4201/connexion?motif=aiguillage&depuis=des')
  })
})

describe('reprise de la session de démonstration', () => {
  it('ouvre la session apportée par le fragment et retire le fragment ; fragment absent ou illisible : rien', () => {
    window.history.replaceState(null, '', '/des/tableau-de-bord')
    expect(reprendreSessionDemo()).toBe(false)
    window.history.replaceState(null, '', '/des/tableau-de-bord#gm-session-demo=%7Bmauvais')
    expect(reprendreSessionDemo()).toBe(false)
    expect(window.location.hash).toBe('')
    window.history.replaceState(null, '', `/des/tableau-de-bord?x=1#gm-session-demo=${encodeURIComponent('{"autre":1}')}`)
    expect(reprendreSessionDemo()).toBe(false)
    window.history.replaceState(null, '', `/des/tableau-de-bord?x=1#gm-session-demo=${encodeURIComponent('{"identifiant":"p.mbarga","role":"ROLE_CHEF_DES"}')}`)
    expect(reprendreSessionDemo()).toBe(true)
    expect(lireSessionDemo()).toEqual({ identifiant: 'p.mbarga', role: 'ROLE_CHEF_DES' })
    expect(window.location.pathname + window.location.search + window.location.hash).toBe('/des/tableau-de-bord?x=1')
  })
})

describe('garde d’aiguillage', () => {
  const monter = (session: Session, couche: 'des' | 'portail' | 'console') =>
    render(<ThemeProvider theme="light"><Aiguillage session={session} couche={couche}><p>contenu</p></Aiguillage></ThemeProvider>)

  it('session à sa place : contenu servi', () => {
    monter(s(), 'des')
    expect(screen.getByText('contenu')).toBeTruthy()
  })
  it('compte interne sans permission de console : message bref, bouton, puis application interne', async () => {
    reglageAiguillage.delaiMs = 0
    ouvrirSessionDemo({ identifiant: 'n.mvondo', role: 'ROLE_COLLABORATEUR' })
    const vers = vi.spyOn(navigation, 'vers').mockImplementation(() => undefined)
    monter(s(), 'console')
    expect(screen.queryByText('contenu')).toBeNull()
    expect(screen.getByText(/n'ouvre pas la console d'administration/)).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: "Ouvrir l'application interne" }))
    const attendu = adresseApplication('des', undefined, { identifiant: 'n.mvondo', role: 'ROLE_COLLABORATEUR' })
    expect(vers).toHaveBeenCalledWith(attendu)
    await waitFor(() => expect(vers).toHaveBeenCalledTimes(2))
  })
  it('compte d’institution sur l’application interne : portail', () => {
    reglageAiguillage.delaiMs = 60_000
    monter(s({ institutionId: 'INST-0042' }), 'des')
    expect(screen.getByText(/compte d'institution : vous êtes conduit vers le portail/)).toBeTruthy()
    expect(screen.getByRole('button', { name: 'Ouvrir le portail des participants' })).toBeTruthy()
  })
})
