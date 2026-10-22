// D-118 : habillage d'un produit hébergé sur le socle (Sandbox GIMAC) ; sans habillage, rendu de la plateforme
// inchangé (les suites existantes le prouvent), une vérification par propriété.
import { afterEach, beforeAll, beforeEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { ThemeProvider } from '@gimac/design-system'
import { initI18n } from './i18n/i18n'
import type { Couche } from './http/prefixes'
import type { Session } from './session/session'
import { Aiguillage, reglageAiguillage } from './session/Aiguillage'
import { navigation } from './session/deconnexion'
import { ConnexionProvider } from './connexion/contexte'
import { PageConnexion } from './connexion/PageConnexion'
import { MOT_DE_PASSE_DEMO, connexionSimulee, simulationConnexion } from './connexion/simule'
import { Enveloppe } from './enveloppe/Enveloppe'
import { AIGUILLAGE_PLATEFORME, type HabillageConnexion, type ReglesAiguillage } from './habillage'

beforeAll(() => {
  initI18n([], 'fr')
  Object.defineProperty(window, 'innerWidth', { value: 1400, configurable: true })
})
beforeEach(() => {
  sessionStorage.clear()
  simulationConnexion.reinitialiser()
  reglageAiguillage.delaiMs = 0
})
afterEach(() => {
  cleanup()
  vi.restoreAllMocks()
})

const LOGO = { clair: '/logo-noir.png', sombre: '/logo-blanc.png', alt: 'GIMAC Sandbox' }
const session = (o: Partial<Session> = {}): Session => ({ sujet: 'agent', nom: 'Agent', identifiant: 'agent', roles: [], permissions: [], institutionId: '', acr: '2', doubleFacteur: true, ...o })

/** Règle d'un produit hébergé : la console garde tout compte interne, le portail les institutions. */
const REGLES: ReglesAiguillage = {
  applicationDe: (s) => (s.institutionId ? 'portail' : 'console'),
  apresRefus: (c) => (c === 'portail' ? 'console' : 'portail'),
  adresse: (c) => `http://produit/${c}/tableau-de-bord`,
  adresseConnexion: (c, o) => `http://produit/${c}/connexion?identifiant=${o.identifiant}`,
}

function connexion(o: { habillage?: HabillageConnexion; aiguillage?: ReglesAiguillage; couche?: Couche; s?: Session; couches?: Couche[]; theme?: 'light' | 'dark' } = {}) {
  const couche = o.couche ?? 'console'
  const service = connexionSimulee({ comptes: [{ identifiant: 'agent', couches: o.couches ?? ['des', 'console'], session: o.s ?? session() }], latenceMs: 0 })
  return render(
    <MemoryRouter initialEntries={['/connexion']}>
      <ThemeProvider theme={o.theme ?? 'light'}>
        <ConnexionProvider service={service} couche={couche}>
          <PageConnexion couche={couche} habillage={o.habillage} aiguillage={o.aiguillage} />
        </ConnexionProvider>
      </ThemeProvider>
    </MemoryRouter>,
  )
}
const saisir = (nom: string, valeur: string) => fireEvent.change(document.querySelector(`input[name="${nom}"]`)!, { target: { value: valeur } })

describe('habillage de la connexion', () => {
  it('sans habillage : cernes, logo, nom Moabi, mention de la couche', () => {
    const { container } = connexion()
    expect(container.querySelector('.gm-acces-cernes')).toBeTruthy()
    expect(container.querySelector('.gm-acces-marque')).toBeTruthy()
    expect(container.querySelector('.gm-acces-centre')).toBeNull()
    expect(document.title).toBe('Moabi')
    expect(screen.getByText(/Console d'administration et d'audit · Usage interne/)).toBeTruthy()
  })

  it('fond à la place des cernes', () => {
    const { container } = connexion({ habillage: { fond: <div data-testid="fond" /> } })
    expect(screen.getByTestId('fond')).toBeTruthy()
    expect(container.querySelector('.gm-acces-cernes')).toBeNull()
  })

  it('marque remplacée, ou retirée et carte centrée', () => {
    connexion({ habillage: { marque: <p>Marque du produit</p> } })
    expect(screen.getByText('Marque du produit')).toBeTruthy()
    cleanup()
    const { container } = connexion({ habillage: { marque: null } })
    expect(container.querySelector('.gm-acces-marque')).toBeNull()
    expect(container.querySelector('.gm-acces-centre')).toBeTruthy()
  })

  it('logo clair et sombre, texte de remplacement', () => {
    connexion({ habillage: { logo: LOGO } })
    expect(screen.getByAltText('GIMAC Sandbox').getAttribute('src')).toBe('/logo-noir.png')
    cleanup()
    connexion({ habillage: { logo: { clair: '/a.png', sombre: '/b.png' } }, theme: 'dark' })
    expect(screen.getByAltText('GIMAC').getAttribute('src')).toBe('/b.png')
  })

  it('nom de l’onglet', () => {
    connexion({ habillage: { nom: 'Moabi · Sandbox GIMAC' } })
    expect(document.title).toBe('Moabi · Sandbox GIMAC')
  })

  it('mention du pied remplacée, ou retirée : l’heure seule reste', () => {
    connexion({ habillage: { mention: 'Sandbox GIMAC · environnement de test' } })
    expect(screen.getByText('Sandbox GIMAC · environnement de test')).toBeTruthy()
    cleanup()
    connexion({ habillage: { mention: '' } })
    expect(screen.queryByText(/Usage interne/)).toBeNull()
    expect(screen.getByRole('timer')).toBeTruthy()
  })
})

describe('règle d’aiguillage d’un produit hébergé', () => {
  const connecter = async () => {
    saisir('identifiant', 'agent')
    saisir('motDePasse', MOT_DE_PASSE_DEMO)
    fireEvent.click(screen.getByRole('button', { name: 'Se connecter' }))
    await screen.findByLabelText(/^Code/)
    saisir('code', '000000')
    fireEvent.click(screen.getByRole('button', { name: 'Vérifier le code' }))
  }

  it('compte interne sans permission de console de la plateforme : reste sur la console du produit', async () => {
    const vers = vi.spyOn(navigation, 'vers').mockImplementation(() => undefined)
    connexion({ aiguillage: REGLES })
    await connecter()
    await waitFor(() => expect(vers).toHaveBeenCalledWith('/console/tableau-de-bord'))
  })

  it('compte d’institution : conduit vers l’adresse du produit', async () => {
    const vers = vi.spyOn(navigation, 'vers').mockImplementation(() => undefined)
    connexion({ aiguillage: REGLES, s: session({ institutionId: 'INST-99001' }) })
    await connecter()
    await waitFor(() => expect(vers).toHaveBeenCalledWith('http://produit/portail/tableau-de-bord'))
  })

  it('couche refusée : écran de connexion de l’autre application du produit', async () => {
    const vers = vi.spyOn(navigation, 'vers').mockImplementation(() => undefined)
    connexion({ aiguillage: REGLES, couche: 'portail', couches: ['console'] })
    saisir('identifiant', 'agent')
    saisir('motDePasse', MOT_DE_PASSE_DEMO)
    fireEvent.click(screen.getByRole('button', { name: 'Se connecter' }))
    await waitFor(() => expect(vers).toHaveBeenCalledWith('http://produit/console/connexion?identifiant=agent'))
  })

  it('garde d’aiguillage : règle du produit ; règle de la plateforme exposée', async () => {
    const vers = vi.spyOn(navigation, 'vers').mockImplementation(() => undefined)
    render(<ThemeProvider theme="light"><Aiguillage session={session()} couche="console" regles={REGLES}><p>contenu</p></Aiguillage></ThemeProvider>)
    expect(screen.getByText('contenu')).toBeTruthy()
    cleanup()
    render(<ThemeProvider theme="light"><Aiguillage session={session({ institutionId: 'INST-99001' })} couche="console" regles={REGLES}><p>contenu</p></Aiguillage></ThemeProvider>)
    await waitFor(() => expect(vers).toHaveBeenCalledWith('http://produit/portail/tableau-de-bord'))
    expect(AIGUILLAGE_PLATEFORME.applicationDe(session(), 'console')).toBe('des')
    expect(AIGUILLAGE_PLATEFORME.apresRefus('portail')).toBe('des')
    expect(AIGUILLAGE_PLATEFORME.adresse('portail', null)).toBe('http://localhost:4201/portail/tableau-de-bord')
    expect(AIGUILLAGE_PLATEFORME.adresseConnexion('des', { depuis: 'portail' })).toBe('http://localhost:4200/connexion?motif=aiguillage&depuis=portail')
  })
})

describe('habillage de l’enveloppe', () => {
  const monter = (habillage?: Parameters<typeof Enveloppe>[0]['habillage'], theme: 'light' | 'dark' = 'light') =>
    render(
      <MemoryRouter initialEntries={['/console/tableau-de-bord']}>
        <ThemeProvider theme={theme}>
          <Enveloppe couche="console" version="0.1.0" navigation={[{ entrees: [{ cle: 'tb', libelleKey: 'x.tb', icone: 'gauge', chemin: '/console/tableau-de-bord' }] }]} habillage={habillage}>
            <p>contenu</p>
          </Enveloppe>
        </ThemeProvider>
      </MemoryRouter>,
    )
  const sources = () => [...document.querySelectorAll('img')].map((i) => i.getAttribute('src'))

  it('sans habillage : logos et pied de la plateforme, aucun bandeau', () => {
    monter()
    expect(sources().some((s) => s?.includes('gimac-logo-blanc'))).toBe(true)
    expect(screen.getByText('Plateforme de suivi des activités du GIMAC, version 0.1.0')).toBeTruthy()
    expect(screen.getByText('Usage interne, confidentiel')).toBeTruthy()
  })

  it('logo : sombre dans la navigation latérale, clair ou sombre dans la barre selon le thème', () => {
    monter({ logo: LOGO })
    expect(sources()).toContain('/logo-blanc.png')
    expect(sources().some((s) => s?.includes('gimac-logo'))).toBe(false)
    cleanup()
    monter({ logo: LOGO }, 'dark')
    expect(sources().every((s) => s === '/logo-blanc.png')).toBe(true)
  })

  it('bandeau sous la barre supérieure', () => {
    monter({ bandeau: <div data-testid="bandeau">SANDBOX GIMAC</div> })
    expect(screen.getByTestId('bandeau').closest('.gm-shell-top')).toBeTruthy()
  })

  it('pied : désignation et classification du produit', () => {
    monter({ pied: { plateforme: 'Sandbox GIMAC, version 0.1.0', classification: '' } })
    expect(screen.getByText('Sandbox GIMAC, version 0.1.0')).toBeTruthy()
    expect(screen.queryByText('Usage interne, confidentiel')).toBeNull()
  })
})
