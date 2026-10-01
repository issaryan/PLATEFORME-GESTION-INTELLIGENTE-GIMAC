import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, screen, waitFor } from '@testing-library/react'
import { ApiError, MOT_DE_PASSE_DEMO, connexionSimulee, navigationDocument, simulationConnexion, type Session } from '@gimac/socle'
import { GardePremierAcces } from './GardePremierAcces'
import { monCompteSimule } from './services/simule'
import { avec, rendre } from './test/rendu'

afterEach(() => {
  cleanup()
  vi.restoreAllMocks()
})
beforeEach(() => {
  simulationConnexion.reinitialiser()
  sessionStorage.clear()
})

// D-62, D-65 : aucun tableau de bord avant la fin du premier accès, fait entièrement sur la plateforme.
const garde = () => (
  <GardePremierAcces>{(assistant) => (assistant ? <div data-testid="assistant">{assistant}</div> : <h1>Tableau de bord</h1>)}</GardePremierAcces>
)
const restreinte: Session = { sujet: 'u', nom: 'Nouvel agent', identifiant: 'nouvel.agent', roles: [], permissions: [], institutionId: '', acr: '1', doubleFacteur: false, restreinte: true }
const saisir = (nom: string, valeur: string) => fireEvent.change(document.querySelector(`[name="${nom}"]`)!, { target: { value: valeur } })
const cliquer = (nom: string | RegExp) => fireEvent.click(screen.getByRole('button', { name: nom }))
const NOUVEAU = 'Nouveau-Mdp-2026!'

async function motDePasse() {
  expect(await screen.findByRole('heading', { name: 'Choisir votre mot de passe' })).toBeTruthy()
  saisir('motDePasseActuel', MOT_DE_PASSE_DEMO)
  saisir('nouveauMotDePasse', NOUVEAU)
  saisir('confirmationMotDePasse', NOUVEAU)
  cliquer('Enregistrer le mot de passe')
}
async function totp() {
  expect(await screen.findByRole('heading', { name: 'Associer votre application d’authentification' })).toBeTruthy()
  expect(await screen.findByRole('img', { name: 'Code QR de configuration de l’application d’authentification' })).toBeTruthy()
  saisir('codeTotp', '123456')
  cliquer('Vérifier et activer')
}

describe('Garde de premier accès', () => {
  it('compte complet : les pages sont servies', async () => {
    rendre(garde())
    expect(await screen.findByText('Tableau de bord')).toBeTruthy()
  })

  it('lecture en cours : squelette à la place des pages ; MS-IAM injoignable : mode dégradé', async () => {
    const { container } = rendre(garde(), { service: avec({ moi: () => new Promise(() => undefined) }) })
    expect(container.querySelector('.gm-skeleton')).toBeTruthy()
    expect(screen.queryByText('Tableau de bord')).toBeNull()
    cleanup()
    rendre(garde(), { service: avec({ moi: async () => { throw new ApiError(503, 'IAM_IDP_UNAVAILABLE', 'x', 'c') } }) })
    expect(await screen.findByText('Tableau de bord')).toBeTruthy()
  })

  it('session restreinte : mot de passe, code TOTP, fiche, charte, puis connexion avec le nouveau mot de passe et un code', async () => {
    const vers = vi.spyOn(navigationDocument, 'vers').mockImplementation(() => undefined)
    const base = monCompteSimule({ latenceMs: 0, premierAcces: true, compte: { identifiant: 'nouvel.agent' } })
    const premiere = vi.fn(base.premiereConfiguration)
    const connexion = connexionSimulee({ latenceMs: 0, comptes: [{ identifiant: 'nouvel.agent', couches: ['des'], etat: 'PREMIER_ACCES', role: 'ROLE_SNA', session: restreinte }] })
    const connecter = vi.spyOn(connexion, 'connecter')
    const { container } = rendre(garde(), { service: avec({ premiereConfiguration: premiere }, base), session: restreinte, connexion })
    expect(await screen.findByRole('heading', { name: 'Premier accès à la plateforme' })).toBeTruthy()
    expect(screen.queryByText('Tableau de bord')).toBeNull()
    expect(container.querySelectorAll('.gm-step')).toHaveLength(5)
    expect(container.querySelector('[aria-current="step"]')?.textContent).toContain('Mot de passe')

    // Mot de passe : champs obligatoires, confirmation, politique (violations), puis succès.
    expect(await screen.findByText(/Au moins 12 caractères/)).toBeTruthy()
    cliquer('Enregistrer le mot de passe')
    expect(await screen.findByText('Saisissez votre mot de passe actuel.')).toBeTruthy()
    saisir('motDePasseActuel', MOT_DE_PASSE_DEMO)
    saisir('nouveauMotDePasse', 'court')
    saisir('confirmationMotDePasse', 'court')
    cliquer('Enregistrer le mot de passe')
    expect(await screen.findByText(/au moins une majuscule/)).toBeTruthy()
    expect((document.querySelector('[name="nouveauMotDePasse"]') as HTMLInputElement).value).toBe('')
    await motDePasse()

    // Code TOTP : code QR local, secret sur demande, code de vérification.
    expect(await screen.findByRole('img', { name: /Code QR/ })).toBeTruthy()
    cliquer('Afficher la clé en clair')
    expect(screen.getByTestId('totp-secret').textContent).toMatch(/^([A-Z2-7]{4} ){7}[A-Z2-7]{4}$/)
    cliquer('Masquer la clé')
    expect(screen.queryByTestId('totp-secret')).toBeNull()
    cliquer('Vérifier et activer')
    expect(await screen.findByText('Le code compte exactement six chiffres.')).toBeTruthy()
    saisir('libelleTotp', 'Téléphone professionnel')
    await totp()

    // Fiche : téléphone obligatoire et au bon format, puis charte.
    expect(await screen.findByRole('heading', { name: 'Vos coordonnées professionnelles' })).toBeTruthy()
    cliquer('Continuer')
    expect(await screen.findByText('Le téléphone professionnel est obligatoire.')).toBeTruthy()
    saisir('telephone', '12')
    cliquer('Continuer')
    expect(await screen.findByText(/Numéro de téléphone invalide/)).toBeTruthy()
    saisir('telephone', '+237 699 11 22 33')
    saisir('fonction', 'Analyste SOC')
    cliquer('Continuer')
    expect(await screen.findByRole('heading', { name: 'Charte d’usage de la plateforme DES' })).toBeTruthy()
    cliquer('Valider et continuer')
    expect(await screen.findByText('Vous devez accepter la charte d’usage pour continuer.')).toBeTruthy()
    fireEvent.click(screen.getByLabelText('J’ai lu la charte d’usage et je l’accepte'))
    cliquer('Valider et continuer')

    // Connexion : nouveau mot de passe gardé en mémoire, code saisi.
    expect(await screen.findByRole('heading', { name: 'Vous connecter' })).toBeTruthy()
    expect(premiere).toHaveBeenCalledWith({ telephone: '+237 699 11 22 33', langue: 'fr', fonction: 'Analyste SOC', charteAcceptee: true })
    expect(document.querySelector('[name="motDePasse"]')).toBeNull()
    cliquer('Ouvrir ma session')
    expect(await screen.findByText('Le code compte exactement six chiffres.')).toBeTruthy()
    saisir('code', '000000')
    cliquer('Ouvrir ma session')
    await waitFor(() => expect(vers).toHaveBeenCalledWith('/'))
    expect(connecter).toHaveBeenLastCalledWith({ identifiant: 'nouvel.agent', motDePasse: NOUVEAU, code: '000000', couche: 'des' })
  })

  it('connexion finale sans mot de passe connu : saisi ; refus et étapes restantes signalés', async () => {
    const base = monCompteSimule({ latenceMs: 0, premierAcces: true, etapes: ['CHARTE'], compte: { identifiant: 'nouvel.agent' } })
    const connexion = { ...connexionSimulee({ comptes: [] }), connecter: vi.fn()
      .mockRejectedValueOnce(new ApiError(401, 'CNX_IDENTIFIANTS_INVALIDES', '', 'cid-1'))
      .mockResolvedValueOnce({ etat: 'PREMIER_ACCES', etapes: ['CHARTE'] })
      .mockRejectedValueOnce(new Error('réseau')) }
    rendre(garde(), { service: base, session: restreinte, connexion })
    fireEvent.click(await screen.findByLabelText('J’ai lu la charte d’usage et je l’accepte'))
    cliquer('Valider et continuer')
    expect(await screen.findByRole('heading', { name: 'Vous connecter' })).toBeTruthy()
    saisir('code', '000000')
    cliquer('Ouvrir ma session')
    expect(await screen.findByText('Saisissez votre mot de passe.')).toBeTruthy()
    saisir('motDePasse', NOUVEAU)
    saisir('code', '000000')
    cliquer('Ouvrir ma session')
    expect(await screen.findByText(/Mot de passe ou code incorrect/)).toBeTruthy()
    expect(screen.getByText('cid-1')).toBeTruthy()
    saisir('motDePasse', NOUVEAU)
    saisir('code', '000000')
    cliquer('Ouvrir ma session')
    expect(await screen.findByText(/Des étapes du premier accès restent à faire/)).toBeTruthy()
    saisir('motDePasse', NOUVEAU)
    saisir('code', '000000')
    cliquer('Ouvrir ma session')
    expect(await screen.findByText('La connexion a échoué.')).toBeTruthy()
  })

  it('hors session restreinte, fiche seule : l’assistant se termine après la fiche (charte déjà acceptée)', async () => {
    const service = monCompteSimule({ latenceMs: 0, compte: { profilComplet: false } })
    const { container } = rendre(garde(), { service })
    await screen.findByRole('heading', { name: 'Premier accès à la plateforme' })
    expect(container.querySelectorAll('.gm-step')).toHaveLength(1)
    saisir('telephone', '+237 699 11 22 33')
    cliquer('Valider et continuer')
    expect(await screen.findByText('Tableau de bord')).toBeTruthy()
  })

  it('code TOTP : code refusé, enrôlement expiré puis nouveau code QR, enrôlement impossible', async () => {
    const base = monCompteSimule({ latenceMs: 0, premierAcces: true, etapes: ['SECOND_FACTEUR'] })
    let expire = true
    let panne = true
    const service = avec({
      enrolerTotp: async () => { if (panne) { panne = false; throw new ApiError(503, 'IAM_IDP_UNAVAILABLE', '', 'cid-e') } return base.enrolerTotp() },
      confirmerTotp: async (c) => {
        if (c.code === '111111') throw new ApiError(400, 'IAM_TOTP_CODE_INVALID', '', '')
        if (expire) { expire = false; throw new ApiError(400, 'IAM_TOTP_ENROLMENT_EXPIRED', '', '') }
        return base.confirmerTotp(c)
      },
    }, base)
    rendre(garde(), { service })
    expect(await screen.findByText('cid-e')).toBeTruthy()
    cliquer('Générer un nouveau code QR')
    await screen.findByRole('img', { name: /Code QR/ })
    saisir('codeTotp', '111111')
    cliquer('Vérifier et activer')
    expect(await screen.findByText(/Code incorrect : saisissez le code affiché/)).toBeTruthy()
    saisir('codeTotp', '222222')
    cliquer('Vérifier et activer')
    expect(await screen.findByText(/Ce code QR a expiré/)).toBeTruthy()
    cliquer('Générer un nouveau code QR')
    await screen.findByRole('img', { name: /Code QR/ })
    await totp()
    expect(await screen.findByText('Tableau de bord')).toBeTruthy()
  })

  it('mot de passe : actuel faux et mot de passe réutilisé rattachés aux champs', async () => {
    rendre(garde(), { service: monCompteSimule({ latenceMs: 0, premierAcces: true, etapes: ['MOT_DE_PASSE'] }) })
    await screen.findByRole('heading', { name: 'Choisir votre mot de passe' })
    expect(screen.getByLabelText(/^Mot de passe provisoire/)).toBeTruthy()
    saisir('motDePasseActuel', 'faux')
    saisir('nouveauMotDePasse', NOUVEAU)
    saisir('confirmationMotDePasse', NOUVEAU)
    cliquer('Enregistrer le mot de passe')
    expect(await screen.findByText('Mot de passe actuel incorrect.')).toBeTruthy()
    saisir('motDePasseActuel', MOT_DE_PASSE_DEMO)
    saisir('nouveauMotDePasse', MOT_DE_PASSE_DEMO)
    saisir('confirmationMotDePasse', MOT_DE_PASSE_DEMO)
    cliquer('Enregistrer le mot de passe')
    expect(await screen.findByText(/déjà été utilisé/)).toBeTruthy()
    saisir('motDePasseActuel', MOT_DE_PASSE_DEMO)
    saisir('nouveauMotDePasse', NOUVEAU)
    saisir('confirmationMotDePasse', 'autre')
    cliquer('Enregistrer le mot de passe')
    expect(await screen.findByText('Les deux saisies diffèrent.')).toBeTruthy()
    await motDePasse()
    expect(await screen.findByText('Tableau de bord')).toBeTruthy()
  })

  it('erreur générale de la fiche : identifiant de corrélation', async () => {
    const service = avec({ premiereConfiguration: async () => { throw new ApiError(400, 'IAM_IDP_UNAVAILABLE', 'refus', 'RQ-IAM-1') } }, monCompteSimule({ latenceMs: 0, premierAcces: true, etapes: ['FICHE', 'CHARTE'] }))
    rendre(garde(), { service })
    await screen.findByRole('heading', { name: 'Premier accès à la plateforme' })
    saisir('telephone', '+237 699 11 22 33')
    cliquer('Continuer')
    fireEvent.click(await screen.findByLabelText('J’ai lu la charte d’usage et je l’accepte'))
    cliquer('Valider et continuer')
    expect(await screen.findByText('RQ-IAM-1')).toBeTruthy()
  })

  it.each(['en', 'es'] as const)('assistant en %s sans clé brute', async (langue) => {
    const { container } = rendre(garde(), { service: monCompteSimule({ latenceMs: 0, premierAcces: true }), langue, session: restreinte })
    await screen.findByRole('heading', { name: langue === 'en' ? 'First access to the platform' : 'Primer acceso a la plataforma' })
    expect(container.textContent).not.toMatch(/\b(premierAcces|compte|motDePasse|totp)\.[a-zA-Z]/)
  })
})
