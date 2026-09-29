import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, screen, waitFor } from '@testing-library/react'
import { ApiError, ReauthCancelled } from '@gimac/socle'
import { GardePremierAcces } from './GardePremierAcces'
import { monCompteSimule } from './services/simule'
import { avec, rendre } from './test/rendu'

afterEach(cleanup)

// D-62 : aucun tableau de bord avant la fin du premier accès.
const garde = () => (
  <GardePremierAcces>{(assistant) => (assistant ? <div data-testid="assistant">{assistant}</div> : <h1>Tableau de bord</h1>)}</GardePremierAcces>
)

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

  it('premier accès complet : fiche et charte, clé plus tard, reconnexion pour les actions requises', async () => {
    const base = monCompteSimule({ latenceMs: 0, premierAcces: true })
    const reconnecter = vi.fn(base.reconnecter)
    const premiere = vi.fn(base.premiereConfiguration)
    const { container } = rendre(garde(), { service: avec({ reconnecter, premiereConfiguration: premiere }, base) })
    expect(await screen.findByRole('heading', { name: 'Premier accès à la plateforme' })).toBeTruthy()
    expect(screen.queryByText('Tableau de bord')).toBeNull()
    expect(container.querySelectorAll('.gm-step')).toHaveLength(3)
    expect(container.querySelector('[aria-current="step"]')?.textContent).toContain('Vos coordonnées et la charte')

    // Validation locale : téléphone et charte obligatoires.
    fireEvent.submit(container.querySelector('form')!)
    expect(await screen.findByText('Le téléphone professionnel est obligatoire.')).toBeTruthy()
    expect(screen.getByText('Vous devez accepter la charte d’usage pour continuer.')).toBeTruthy()
    expect(premiere).not.toHaveBeenCalled()

    // Refus du service : erreur rattachée au champ.
    fireEvent.change(container.querySelector('input[name="telephone"]')!, { target: { value: '12' } })
    fireEvent.click(screen.getByLabelText('J’ai lu la charte d’usage et je l’accepte'))
    fireEvent.submit(container.querySelector('form')!)
    expect(await screen.findByText(/Numéro de téléphone invalide/)).toBeTruthy()

    fireEvent.change(container.querySelector('input[name="telephone"]')!, { target: { value: '+237 699 11 22 33' } })
    fireEvent.change(container.querySelector('select[name="langue"]')!, { target: { value: 'fr' } })
    fireEvent.change(container.querySelector('input[name="fonction"]')!, { target: { value: 'Analyste SOC' } })
    fireEvent.submit(container.querySelector('form')!)
    expect(await screen.findByRole('heading', { name: 'Enrôler une clé de sécurité' })).toBeTruthy()
    expect(premiere).toHaveBeenCalledWith({ telephone: '+237 699 11 22 33', langue: 'fr', fonction: 'Analyste SOC', charteAcceptee: true })

    fireEvent.click(screen.getByRole('button', { name: 'Plus tard' }))
    expect(await screen.findByRole('heading', { name: 'Sécuriser votre authentification' })).toBeTruthy()
    expect(screen.getByText('Mot de passe')).toBeTruthy()
    expect(screen.getByText('Adresse électronique')).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Continuer' }))
    expect(await screen.findByText('Tableau de bord')).toBeTruthy()
    expect(reconnecter).toHaveBeenCalledTimes(1)
  })

  it('fiche seule avec clé enrôlée : l’assistant se termine après la fiche', async () => {
    const service = monCompteSimule({ latenceMs: 0, compte: { profilComplet: false } })
    const { container } = rendre(garde(), { service })
    await screen.findByRole('heading', { name: 'Premier accès à la plateforme' })
    expect(container.querySelectorAll('.gm-step')).toHaveLength(1)
    fireEvent.click(screen.getByLabelText('J’ai lu la charte d’usage et je l’accepte'))
    fireEvent.submit(container.querySelector('form')!)
    expect(await screen.findByText('Tableau de bord')).toBeTruthy()
  })

  it('clé de sécurité : enrôlement, annulation signalée, erreur de reconnexion et du service', async () => {
    const base = monCompteSimule({ latenceMs: 0, premierAcces: true, compte: { profilComplet: true, telephone: '+237 699 00 00 00' } })
    let annuler = true
    const service = avec({
      enrolerCle: async (nom) => { if (annuler) throw new ReauthCancelled(); return base.enrolerCle(nom) },
      reconnecter: async () => { throw new ApiError(502, 'HTTP_502', 'passerelle', 'RQ-502') },
    }, base)
    rendre(garde(), { service })
    expect(await screen.findByRole('heading', { name: 'Enrôler une clé de sécurité' })).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Enrôler une clé' }))
    expect(await screen.findByText(/a été annulée/)).toBeTruthy()
    annuler = false
    fireEvent.click(screen.getByRole('button', { name: 'Enrôler une clé' }))
    expect(await screen.findByRole('heading', { name: 'Sécuriser votre authentification' })).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Continuer' }))
    expect(await screen.findByText('RQ-502')).toBeTruthy()
  })

  it('erreur générale de la fiche : identifiant de corrélation ; langue et charte refusées par le service', async () => {
    let code = 'IAM_IDP_UNAVAILABLE'
    const service = avec({ premiereConfiguration: async () => { throw new ApiError(400, code, 'refus', 'RQ-IAM-1') } }, monCompteSimule({ latenceMs: 0, premierAcces: true }))
    const { container } = rendre(garde(), { service })
    await screen.findByRole('heading', { name: 'Premier accès à la plateforme' })
    fireEvent.change(container.querySelector('input[name="telephone"]')!, { target: { value: '+237 699 11 22 33' } })
    fireEvent.click(screen.getByLabelText('J’ai lu la charte d’usage et je l’accepte'))
    fireEvent.submit(container.querySelector('form')!)
    expect(await screen.findByText('RQ-IAM-1')).toBeTruthy()
    code = 'IAM_LANGUAGE_INVALID'
    fireEvent.submit(container.querySelector('form')!)
    expect(await screen.findByText(/Langue invalide/)).toBeTruthy()
    code = 'IAM_CHARTER_REQUIRED'
    fireEvent.submit(container.querySelector('form')!)
    await waitFor(() => expect(screen.getByText('La charte d’usage doit être acceptée.')).toBeTruthy())
  })

  it.each(['en', 'es'] as const)('assistant en %s sans clé brute', async (langue) => {
    const { container } = rendre(garde(), { service: monCompteSimule({ latenceMs: 0, premierAcces: true }), langue })
    await screen.findByRole('heading', { name: langue === 'en' ? 'First access to the platform' : 'Primer acceso a la plataforma' })
    expect(container.textContent).not.toMatch(/\b(premierAcces|compte)\.[a-zA-Z]/)
  })
})
