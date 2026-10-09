import { afterEach, beforeAll, beforeEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { ThemeProvider } from '@gimac/design-system'
import { ApiError } from '../http/errors'
import type { Couche } from '../http/prefixes'
import { reglageAiguillage } from '../session/Aiguillage'
import { initI18n } from '../i18n/i18n'
import type { Session } from '../session/session'
import { lireSessionDemo } from '../session/demo'
import { navigation } from '../session/deconnexion'
import { ConnexionProvider, useConnexion } from './contexte'
import { connexionHttp } from './http'
import type { ServiceConnexion } from './modele'
import { PageConnexion } from './PageConnexion'
import { violationsDe, violationsMotDePasse } from './politique'
import { MOT_DE_PASSE_DEMO, connexionSimulee, simulationConnexion, type CompteConnexionDemo } from './simule'

// D-65 : connexion sur la plateforme (identifiant et mot de passe, puis code), mot de passe oublié,
// nouveau mot de passe ; messages par code d'erreur ; aucune page du fournisseur d'identité.

beforeAll(() => {
  initI18n([], 'fr')
})
beforeEach(() => {
  sessionStorage.clear()
  simulationConnexion.reinitialiser()
})
afterEach(() => {
  cleanup()
  vi.restoreAllMocks()
})

const session = (identifiant: string): Session => ({ sujet: identifiant, nom: identifiant, identifiant, roles: [], permissions: [], institutionId: '', acr: '2', doubleFacteur: true })
const COMPTES: CompteConnexionDemo[] = [
  { identifiant: 'c.ateba', couches: ['des', 'console'], role: 'ROLE_CHEF_DDRM', session: session('c.ateba') },
  { identifiant: 'nouveau', couches: ['des'], etat: 'PREMIER_ACCES', role: 'ROLE_SNA', session: session('nouveau') },
  { identifiant: 'ancien', couches: ['des'], etat: 'INACTIF', session: session('ancien') },
  { identifiant: 'correspondant-a', motDePasse: 'Propre-Mdp-2026!', couches: ['portail'], session: session('correspondant-a') },
]
const sim = () => connexionSimulee({ comptes: COMPTES, latenceMs: 0 })

function monter(adresse = '/connexion', service: ServiceConnexion = sim(), o: { destination?: string; erreur?: { message: string; correlationId?: string }; couche?: Couche } = {}) {
  const vers = vi.spyOn(navigation, 'vers').mockImplementation(() => undefined)
  const couche = o.couche ?? 'des'
  render(
    <MemoryRouter initialEntries={[adresse]}>
      <ThemeProvider theme="light">
        <ConnexionProvider service={service} couche={couche}>
          <PageConnexion couche={couche} destination={o.destination} erreur={o.erreur} />
        </ConnexionProvider>
      </ThemeProvider>
    </MemoryRouter>,
  )
  return vers
}
const saisir = (nom: string, valeur: string) => fireEvent.change(document.querySelector(`input[name="${nom}"]`)!, { target: { value: valeur } })
const cliquer = (nom: string | RegExp) => fireEvent.click(screen.getByRole('button', { name: nom }))

describe('PageConnexion : identifiant, mot de passe, puis code', () => {
  it('carte d’accès, formulaire de la plateforme, aucun renvoi vers le fournisseur d’identité', () => {
    monter()
    expect(screen.getByRole('heading', { level: 1 }).textContent).toBe('Connexion')
    expect(screen.getByRole('img', { name: 'GIMAC' })).toBeTruthy()
    expect(screen.getByLabelText(/^Identifiant/)).toBeTruthy()
    expect((screen.getByLabelText(/^Mot de passe/) as HTMLInputElement).type).toBe('password')
    expect(screen.getByRole('link', { name: 'Mot de passe oublié ?' }).getAttribute('href')).toBe('/connexion/mot-de-passe-oublie')
    expect(document.body.textContent).not.toMatch(/fournisseur d'identité|oauth2/i)
    expect(document.title).toBe('Moabi')
  })

  it('champs obligatoires signalés sans appel', async () => {
    const service = { ...sim(), connecter: vi.fn() }
    monter('/connexion', service)
    cliquer('Se connecter')
    expect(await screen.findByText('Saisissez votre identifiant.')).toBeTruthy()
    expect(screen.getByText('Saisissez votre mot de passe.')).toBeTruthy()
    expect(service.connecter).not.toHaveBeenCalled()
  })

  it('CODE_REQUIS puis CONNECTE : code à six chiffres, mot de passe effacé, retour à la page demandée', async () => {
    const vers = monter('/connexion?retour=%2Fdes%2Fliaisons')
    saisir('identifiant', 'c.ateba')
    saisir('motDePasse', MOT_DE_PASSE_DEMO)
    cliquer('Se connecter')
    expect(await screen.findByLabelText(/^Code à six chiffres/)).toBeTruthy()
    expect(screen.getByText(/Mot de passe vérifié pour c.ateba/)).toBeTruthy()
    expect(document.querySelector('input[name="motDePasse"]')).toBeNull()
    saisir('code', '12a34')
    expect((document.querySelector('input[name="code"]') as HTMLInputElement).value).toBe('1234')
    cliquer('Vérifier le code')
    expect(await screen.findByText('Le code compte exactement six chiffres.')).toBeTruthy()
    saisir('code', '000000')
    cliquer('Vérifier le code')
    await waitFor(() => expect(vers).toHaveBeenCalledWith('/des/liaisons'))
    expect(lireSessionDemo()).toEqual({ identifiant: 'c.ateba', role: 'ROLE_CHEF_DDRM', premierAcces: false })
  })

  it('code refusé : message dédié, code effacé ; « Changer d’identifiant » efface le mot de passe', async () => {
    const service = sim()
    const connecter = vi.spyOn(service, 'connecter')
    monter('/connexion', service)
    saisir('identifiant', 'c.ateba')
    saisir('motDePasse', MOT_DE_PASSE_DEMO)
    cliquer('Se connecter')
    await screen.findByLabelText(/^Code à six chiffres/)
    connecter.mockRejectedValueOnce(new ApiError(401, 'CNX_IDENTIFIANTS_INVALIDES', '', 'cid-code'))
    saisir('code', '123456')
    cliquer('Vérifier le code')
    expect(await screen.findByText(/Code incorrect ou expiré/)).toBeTruthy()
    expect(screen.getByText('cid-code')).toBeTruthy()
    expect((document.querySelector('input[name="code"]') as HTMLInputElement).value).toBe('')
    connecter.mockResolvedValueOnce({ etat: 'CODE_REQUIS' })
    saisir('code', '123456')
    cliquer('Vérifier le code')
    expect(await screen.findByText(/Code incorrect ou expiré/)).toBeTruthy()
    cliquer("Changer d'identifiant")
    expect((document.querySelector('input[name="motDePasse"]') as HTMLInputElement).value).toBe('')
    expect((document.querySelector('input[name="identifiant"]') as HTMLInputElement).value).toBe('c.ateba')
  })

  it('PREMIER_ACCES : session restreinte ouverte, tableau de bord de la couche (l’assistant s’y ouvre)', async () => {
    const vers = monter()
    saisir('identifiant', 'nouveau')
    saisir('motDePasse', MOT_DE_PASSE_DEMO)
    cliquer('Se connecter')
    await waitFor(() => expect(vers).toHaveBeenCalledWith('/des/tableau-de-bord'))
    expect(lireSessionDemo()).toMatchObject({ identifiant: 'nouveau', premierAcces: true, etapes: ['MOT_DE_PASSE', 'SECOND_FACTEUR', 'FICHE', 'CHARTE'] })
  })

  it.each([
    ['c.ateba', 'mauvais', 'Identifiant ou mot de passe incorrect.'],
    ['inconnu', MOT_DE_PASSE_DEMO, 'Identifiant ou mot de passe incorrect.'],
    ['ancien', MOT_DE_PASSE_DEMO, /Ce compte est désactivé/],
  ])('refus : %s → message par code', async (identifiant, mdp, attendu) => {
    monter()
    saisir('identifiant', identifiant)
    saisir('motDePasse', mdp)
    cliquer('Se connecter')
    expect(await screen.findByText(attendu)).toBeTruthy()
    expect((document.querySelector('input[name="motDePasse"]') as HTMLInputElement).value).toBe('')
  })

  it('trop de tentatives (429) après cinq échecs en une minute', async () => {
    monter()
    for (let i = 0; i < 5; i++) {
      saisir('identifiant', 'c.ateba')
      saisir('motDePasse', 'faux')
      cliquer('Se connecter')
      await screen.findByText('Identifiant ou mot de passe incorrect.')
    }
    saisir('motDePasse', MOT_DE_PASSE_DEMO)
    cliquer('Se connecter')
    expect(await screen.findByText(/Trop de tentatives/)).toBeTruthy()
  })

  it('erreur inconnue ou non normalisée : message générique ; erreur de l’application affichée', async () => {
    const service = { ...sim(), connecter: vi.fn().mockRejectedValueOnce(new ApiError(503, 'HTTP_503', '', '')).mockRejectedValueOnce(new Error('x')) }
    monter('/connexion', service, { erreur: { message: 'Passerelle en maintenance', correlationId: 'cid-app' } })
    expect(screen.getByText('Passerelle en maintenance')).toBeTruthy()
    saisir('identifiant', 'a')
    saisir('motDePasse', 'b')
    cliquer('Se connecter')
    expect(await screen.findByText('La connexion a échoué.')).toBeTruthy()
    cliquer('Se connecter')
    saisir('motDePasse', 'b')
    cliquer('Se connecter')
    await waitFor(() => expect(service.connecter).toHaveBeenCalledTimes(2))
    expect(screen.getByText('La connexion a échoué.')).toBeTruthy()
  })

  it.each([
    ['inactivite', 'Session fermée après 30 minutes d’inactivité.'.replace('’', "'")],
    ['expiree', 'Votre session a expiré. Reconnectez-vous pour continuer.'],
    ['deconnexion', 'Vous êtes déconnecté.'],
  ])('motif %s affiché', (motif, texte) => {
    monter(`/connexion?motif=${motif}&minutes=30`)
    expect(screen.getByText(texte)).toBeTruthy()
  })

  it('destination imposée ; retour hors de l’application ignoré', async () => {
    const vers = monter('/connexion?retour=https%3A%2F%2Fevil.example', sim(), { destination: '/des/parcours' })
    saisir('identifiant', 'c.ateba')
    saisir('motDePasse', MOT_DE_PASSE_DEMO)
    cliquer('Se connecter')
    await screen.findByLabelText(/^Code à six chiffres/)
    saisir('code', '654321')
    cliquer('Vérifier le code')
    await waitFor(() => expect(vers).toHaveBeenCalledWith('/des/parcours'))
  })

  it('écran Moabi (D-67) : carte, nom guilloché ou en contour selon le thème, langue en menu, heures en pied', () => {
    monter()
    expect(document.title).toBe('Moabi')
    expect(screen.getByRole('heading', { level: 1, name: 'Connexion' })).toBeTruthy()
    expect(screen.getByRole('img', { name: 'Moabi' }).getAttribute('data-style')).toBe('guilloche')
    expect(screen.getByText('GIMAC · Afrique centrale')).toBeTruthy()
    // Décor de l'arbre moabi : cernes derrière la carte, arbre derrière le nom, feuille sur le i ; décoratif.
    const carte = screen.getByRole('main')
    expect(carte.querySelector('.gm-acces-cernes')?.getAttribute('aria-hidden')).toBe('true')
    expect(carte.querySelectorAll('.gm-acces-cernes path').length).toBeGreaterThan(50)
    const nom = screen.getByRole('img', { name: 'Moabi' })
    expect(nom.querySelector('.gm-marque-arbre')?.getAttribute('aria-hidden')).toBe('true')
    expect(nom.querySelectorAll('.gm-arbre-feuille').length).toBeGreaterThan(100)
    expect(nom.querySelector('.gm-marque-feuille')).toBeTruthy()
    expect(screen.getByRole('timer', { name: 'Heure locale et heure UTC' }).textContent).toMatch(/^Douala \d{2}:\d{2}:\d{2} · UTC \d{2}:\d{2}:\d{2}$/)
    expect(screen.getByText(/Application du département · Usage interne, confidentiel/)).toBeTruthy()
    // Thème : bascule directe ; le nom passe en contour.
    fireEvent.click(screen.getByRole('button', { name: 'Mode sombre' }))
    expect(screen.getByRole('img', { name: 'Moabi' }).getAttribute('data-style')).toBe('contour')
    fireEvent.click(screen.getByRole('button', { name: 'Clair' }))
    expect(screen.getByRole('img', { name: 'Moabi' }).getAttribute('data-style')).toBe('guilloche')
    // Langue : menu surgissant du globe, fermé par Échap ou un clic à l'extérieur.
    const globe = screen.getByRole('button', { name: 'Préférences' })
    fireEvent.click(globe)
    expect(globe.getAttribute('aria-expanded')).toBe('true')
    fireEvent.keyDown(document, { key: 'Escape' })
    expect(globe.getAttribute('aria-expanded')).toBe('false')
    fireEvent.click(globe)
    fireEvent.change(screen.getByRole('combobox', { name: 'Langue' }), { target: { value: 'en' } })
    expect(screen.getByRole('button', { name: 'Sign in' })).toBeTruthy()
    fireEvent.mouseDown(document.body)
    expect(screen.queryByRole('dialog', { name: 'Preferences' })).toBeNull()
    fireEvent.click(screen.getByRole('button', { name: 'Preferences' }))
    fireEvent.change(screen.getByRole('combobox', { name: 'Language' }), { target: { value: 'fr' } })
  })
})

describe('Mot de passe oublié', () => {
  it('toujours la même réponse ; identifiant obligatoire ; retour à la connexion', async () => {
    const service = sim()
    const oubli = vi.spyOn(service, 'motDePasseOublie')
    monter('/connexion/mot-de-passe-oublie', service)
    expect(screen.getByRole('heading', { name: 'Mot de passe oublié' })).toBeTruthy()
    cliquer('Recevoir un lien de réinitialisation')
    expect(await screen.findByText('Saisissez votre identifiant ou votre adresse électronique.')).toBeTruthy()
    saisir('identifiant', 'personne@nulle-part')
    cliquer('Recevoir un lien de réinitialisation')
    expect(await screen.findByText(/Si un compte actif correspond/)).toBeTruthy()
    expect(oubli).toHaveBeenCalledWith('personne@nulle-part')
    expect(screen.getByRole('link', { name: 'Retour à la connexion' }).getAttribute('href')).toBe('/connexion')
  })
  it('trop de demandes : message par code', async () => {
    const service = { ...sim(), motDePasseOublie: vi.fn().mockRejectedValue(new ApiError(429, 'CNX_TROP_DE_TENTATIVES', '', '')) }
    monter('/connexion/mot-de-passe-oublie', service)
    saisir('identifiant', 'c.ateba')
    cliquer('Recevoir un lien de réinitialisation')
    expect(await screen.findByText(/Trop de tentatives/)).toBeTruthy()
  })
})

describe('Nouveau mot de passe (lien du courriel)', () => {
  const JETON = 'Zm9vYmFyYmF6cXV4MTIzNDU2'
  it('sans jeton : lien incomplet, nouvelle demande proposée', () => {
    monter('/connexion/nouveau-mot-de-passe')
    expect(screen.getByText(/Ce lien est inconnu, expiré ou déjà utilisé/)).toBeTruthy()
    expect(screen.getByRole('link', { name: 'Demander un nouveau lien' })).toBeTruthy()
  })
  it('politique rappelée ; confirmation différente ; violations IAM_PASSWORD_POLICY ; succès ; jeton déjà utilisé', async () => {
    monter(`/connexion/nouveau-mot-de-passe?jeton=${JETON}`)
    expect(screen.getByText(/Au moins 12 caractères/)).toBeTruthy()
    cliquer('Enregistrer le mot de passe')
    expect(await screen.findByText('Saisissez votre mot de passe.')).toBeTruthy()
    saisir('nouveauMotDePasse', 'court')
    saisir('confirmationMotDePasse', 'autre')
    cliquer('Enregistrer le mot de passe')
    expect(await screen.findByText('Les deux saisies diffèrent.')).toBeTruthy()
    saisir('nouveauMotDePasse', 'court')
    saisir('confirmationMotDePasse', 'court')
    cliquer('Enregistrer le mot de passe')
    expect(await screen.findByText('Ce mot de passe ne respecte pas la politique de sécurité.')).toBeTruthy()
    expect(screen.getByText('au moins 12 caractères')).toBeTruthy()
    expect(screen.getByText('au moins une majuscule')).toBeTruthy()
    saisir('nouveauMotDePasse', 'Nouveau-Mdp-2026!')
    saisir('confirmationMotDePasse', 'Nouveau-Mdp-2026!')
    cliquer('Enregistrer le mot de passe')
    expect(await screen.findByText(/Votre mot de passe a été enregistré/)).toBeTruthy()
    expect(screen.getByRole('link', { name: 'Se connecter' }).getAttribute('href')).toBe('/connexion')
  })
  it('jeton invalide : message et nouvelle demande', async () => {
    monter('/connexion/nouveau-mot-de-passe?jeton=expire')
    saisir('nouveauMotDePasse', 'Nouveau-Mdp-2026!')
    saisir('confirmationMotDePasse', 'Nouveau-Mdp-2026!')
    cliquer('Enregistrer le mot de passe')
    expect(await screen.findByText(/Ce lien est inconnu, expiré ou déjà utilisé/)).toBeTruthy()
    expect(screen.getByRole('link', { name: 'Demander un nouveau lien' })).toBeTruthy()
  })
})

describe('Services de connexion', () => {
  const json = (status: number, corps?: object) =>
    new Response(corps ? JSON.stringify(corps) : null, { status, headers: corps ? { 'Content-Type': 'application/json' } : {} })
  it('HTTP : POST /api/connexion (corps du contrat, jeton anti-CSRF), routes du mot de passe oublié', async () => {
    document.cookie = 'XSRF-TOKEN=jeton-csrf'
    const spy = vi.spyOn(globalThis, 'fetch')
      .mockResolvedValueOnce(json(200, { etat: 'CODE_REQUIS' }))
      .mockResolvedValueOnce(json(202))
      .mockResolvedValueOnce(json(204))
    const s = connexionHttp()
    expect(await s.connecter({ identifiant: 'a', motDePasse: 'b', couche: 'console' })).toEqual({ etat: 'CODE_REQUIS' })
    await s.motDePasseOublie('a')
    await s.nouveauMotDePasse('j', 'm')
    expect(String(spy.mock.calls[0][0])).toContain('/api/connexion')
    expect(JSON.parse(String(spy.mock.calls[0][1]?.body))).toEqual({ identifiant: 'a', motDePasse: 'b', couche: 'console' })
    expect(new Headers(spy.mock.calls[0][1]?.headers).get('X-XSRF-TOKEN')).toBe('jeton-csrf')
    expect(String(spy.mock.calls[1][0])).toContain('/api/connexion/mot-de-passe-oublie')
    expect(JSON.parse(String(spy.mock.calls[1][1]?.body))).toEqual({ identifiant: 'a' })
    expect(String(spy.mock.calls[2][0])).toContain('/api/connexion/nouveau-mot-de-passe')
    expect(JSON.parse(String(spy.mock.calls[2][1]?.body))).toEqual({ jeton: 'j', motDePasse: 'm' })
    document.cookie = 'XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 GMT'
  })

  it('HTTP : première visite sans témoin anti-CSRF, lecture anonyme de la session avant le POST', async () => {
    document.cookie = 'XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 GMT'
    const spy = vi.spyOn(globalThis, 'fetch')
      .mockImplementationOnce(async () => {
        document.cookie = 'XSRF-TOKEN=pose-par-la-passerelle'
        return json(401, { code: 'NON_AUTHENTIFIE' })
      })
      .mockResolvedValueOnce(json(200, { etat: 'CODE_REQUIS' }))
    expect(await connexionHttp().connecter({ identifiant: 'a', motDePasse: 'b', couche: 'des' })).toEqual({ etat: 'CODE_REQUIS' })
    expect(String(spy.mock.calls[0][0])).toContain('/api/session')
    expect(new Headers(spy.mock.calls[0][1]?.headers).get('X-Gimac-Fond')).toBe('1')
    expect(new Headers(spy.mock.calls[1][1]?.headers).get('X-XSRF-TOKEN')).toBe('pose-par-la-passerelle')
    document.cookie = 'XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 GMT'
  })

  it('simulé : premier accès terminé et nouveau mot de passe gardés en mémoire de la page', async () => {
    const s = sim()
    simulationConnexion.definirMotDePasse('nouveau', 'Choisi-Mdp-2026!')
    await expect(s.connecter({ identifiant: 'nouveau', motDePasse: MOT_DE_PASSE_DEMO, couche: 'des' })).rejects.toMatchObject({ code: 'CNX_IDENTIFIANTS_INVALIDES' })
    expect(await s.connecter({ identifiant: 'nouveau', motDePasse: 'Choisi-Mdp-2026!', couche: 'des' })).toMatchObject({ etat: 'PREMIER_ACCES' })
    simulationConnexion.terminerPremierAcces('nouveau')
    expect(await s.connecter({ identifiant: 'nouveau', motDePasse: 'Choisi-Mdp-2026!', couche: 'des', code: '000000' })).toMatchObject({ etat: 'CONNECTE' })
    const partiel = connexionSimulee({ comptes: [{ identifiant: 'x', couches: ['des'], etat: 'PREMIER_ACCES', etapes: ['CHARTE'], session: session('x') }] })
    const r = partiel.connecter({ identifiant: 'x', motDePasse: MOT_DE_PASSE_DEMO, couche: 'des' })
    await expect(r).resolves.toEqual({ etat: 'PREMIER_ACCES', etapes: ['CHARTE'] })
  })

  it('politique : violations locales et violations portées par une erreur', () => {
    expect(violationsMotDePasse('abc')).toEqual(['LONGUEUR', 'MAJUSCULE', 'CHIFFRE', 'SPECIAL'])
    expect(violationsMotDePasse('Ateba-2026-Secret', 'ateba')).toEqual(['IDENTIFIANT'])
    expect(violationsDe(new ApiError(400, 'IAM_PASSWORD_POLICY', '', '', { violations: ['invalidPasswordMinLengthMessage'] }))).toEqual(['invalidPasswordMinLengthMessage'])
    expect(violationsDe(new ApiError(400, 'IAM_PASSWORD_POLICY', '', ''))).toEqual([])
    expect(violationsDe(new Error('x'))).toEqual([])
  })

  it('contexte : la passerelle réelle et la couche « des » à défaut de fournisseur', () => {
    let vu: ReturnType<typeof useConnexion> | undefined
    function Sonde() {
      vu = useConnexion()
      return null
    }
    render(<Sonde />)
    expect(vu?.couche).toBe('des')
    expect(typeof vu?.service.connecter).toBe('function')
  })
})

// D-79 : aiguillage des comptes entre les applications, à la connexion.
describe('PageConnexion : aiguillage vers l’application du compte', () => {
  beforeEach(() => {
    reglageAiguillage.delaiMs = 0
  })
  const interne = (permissions: string[]): Session => ({ ...session('agent'), permissions })
  const comptes = (s: Session, couches: Couche[] = ['des', 'console']): ServiceConnexion =>
    connexionSimulee({ comptes: [{ identifiant: 'agent', couches, role: 'ROLE_COLLABORATEUR', session: s }], latenceMs: 0 })
  const connecter = async () => {
    saisir('identifiant', 'agent')
    saisir('motDePasse', MOT_DE_PASSE_DEMO)
    cliquer('Se connecter')
    await screen.findByLabelText(/^Code/)
    saisir('code', '000000')
    cliquer('Vérifier le code')
  }

  it('compte interne sans permission de console sur la console : message bref, puis application interne avec la session de démonstration', async () => {
    const vers = monter('/connexion', comptes(interne(['COL_MESSAGE_READ'])), { couche: 'console' })
    await connecter()
    expect(await screen.findByText(/n'ouvre pas la console d'administration/)).toBeTruthy()
    const lien = screen.getByRole('link', { name: 'Continuer maintenant' }).getAttribute('href')!
    expect(lien).toMatch(/^http:\/\/localhost:4200\/des\/tableau-de-bord#gm-session-demo=/)
    await waitFor(() => expect(vers).toHaveBeenCalledWith(lien))
  })

  it('compte interne portant une permission de console : reste sur la console', async () => {
    const vers = monter('/connexion', comptes(interne(['PLATFORM_OPS_READ'])), { couche: 'console' })
    await connecter()
    await waitFor(() => expect(vers).toHaveBeenCalledWith('/console/tableau-de-bord'))
  })

  it.each([
    ['portail' as Couche, ['des', 'console'] as Couche[], /compte interne : il s'ouvre dans l'application interne/, 'http://localhost:4200/connexion?motif=aiguillage&depuis=portail&identifiant=agent'],
    ['des' as Couche, ['portail'] as Couche[], /compte d'institution : il s'ouvre dans le portail/, 'http://localhost:4201/connexion?motif=aiguillage&depuis=des&identifiant=agent'],
    ['console' as Couche, ['portail'] as Couche[], /compte d'institution : il s'ouvre dans le portail/, 'http://localhost:4201/connexion?motif=aiguillage&depuis=console&identifiant=agent'],
  ])('couche refusée sur %s : écran de connexion de l’autre application, identifiant prérempli', async (couche, couches, texte, adresse) => {
    const vers = monter('/connexion', comptes(interne([]), couches), { couche })
    saisir('identifiant', 'agent')
    saisir('motDePasse', MOT_DE_PASSE_DEMO)
    cliquer('Se connecter')
    expect(await screen.findByText(texte)).toBeTruthy()
    await waitFor(() => expect(vers).toHaveBeenCalledWith(adresse))
  })

  it('arrivée par aiguillage : motif affiché et identifiant prérempli', () => {
    monter('/connexion?motif=aiguillage&depuis=portail&identifiant=p.mbarga')
    expect(screen.getByText(/Le portail des participants vous a conduit ici/)).toBeTruthy()
    expect((document.querySelector('input[name="identifiant"]') as HTMLInputElement).value).toBe('p.mbarga')
  })
})
