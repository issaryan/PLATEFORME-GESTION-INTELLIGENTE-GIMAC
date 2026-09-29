import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, screen, waitFor, within } from '@testing-library/react'
import { ApiError, ReauthCancelled } from '@gimac/socle'
import MonComptePage from './MonComptePage'
import { monCompteSimule } from '../services/simule'
import { avec, rendre } from '../test/rendu'

afterEach(cleanup)
const dialogue = () => screen.getByRole('alertdialog')

describe('Mon compte', () => {
  it('fiche : lecture, enregistrement annoncé, erreur de téléphone sur le champ', async () => {
    const base = monCompteSimule({ latenceMs: 0 })
    const modifier = vi.fn(base.modifier)
    const { container } = rendre(<MonComptePage />, { service: avec({ modifier }, base) })
    expect(await screen.findByRole('heading', { level: 1, name: 'Mon compte' })).toBeTruthy()
    expect(await screen.findByText('demo')).toBeTruthy()
    expect(screen.getByText('Chef de la DDRM')).toBeTruthy()
    fireEvent.change(container.querySelector('input[name="fonction"]')!, { target: { value: 'Cheffe de la DDRM' } })
    fireEvent.change(container.querySelector('select[name="langue"]')!, { target: { value: 'es' } })
    fireEvent.click(screen.getByRole('button', { name: 'Enregistrer ma fiche' }))
    expect(await screen.findByText('Votre fiche a été enregistrée.')).toBeTruthy()
    expect(modifier).toHaveBeenCalledWith(expect.objectContaining({ fonction: 'Cheffe de la DDRM', langue: 'es', prenom: 'Aïssatou' }))
    fireEvent.change(container.querySelector('input[name="telephone"]')!, { target: { value: 'abc' } })
    fireEvent.click(screen.getByRole('button', { name: 'Enregistrer ma fiche' }))
    expect(await screen.findByText(/Numéro de téléphone invalide/)).toBeTruthy()
    expect(container.querySelector('input[name="telephone"]')!.getAttribute('aria-invalid')).toBe('true')
  })

  it('fiche : langue refusée et erreur générale avec corrélation', async () => {
    let code = 'IAM_LANGUAGE_INVALID'
    rendre(<MonComptePage />, { service: avec({ modifier: async () => { throw new ApiError(400, code, 'x', 'RQ-7') } }) })
    await screen.findByRole('button', { name: 'Enregistrer ma fiche' })
    fireEvent.click(screen.getByRole('button', { name: 'Enregistrer ma fiche' }))
    expect(await screen.findByText(/Langue invalide/)).toBeTruthy()
    code = 'IAM_IDP_UNAVAILABLE'
    fireEvent.click(screen.getByRole('button', { name: 'Enregistrer ma fiche' }))
    expect(await screen.findByText('RQ-7')).toBeTruthy()
  })

  it('mot de passe et second facteur : action inscrite puis reconnexion', async () => {
    const base = monCompteSimule({ latenceMs: 0 })
    const demanderAction = vi.fn(base.demanderAction)
    const reconnecter = vi.fn(async () => undefined)
    rendre(<MonComptePage />, { service: avec({ demanderAction, reconnecter }, base) })
    fireEvent.click(await screen.findByRole('button', { name: 'Changer mon mot de passe' }))
    fireEvent.click(within(dialogue()).getByRole('button', { name: 'Continuer vers la reconnexion' }))
    expect(await screen.findByText('Reconnexion en cours vers les pages d’authentification du GIMAC.')).toBeTruthy()
    expect(demanderAction).toHaveBeenCalledWith('UPDATE_PASSWORD')
    expect(reconnecter).toHaveBeenCalledTimes(1)
    fireEvent.click(screen.getByRole('button', { name: 'Reconfigurer mon second facteur' }))
    expect(within(dialogue()).getByText(/sera retiré/)).toBeTruthy()
    fireEvent.click(within(dialogue()).getByRole('button', { name: 'Annuler' }))
    expect(screen.queryByRole('alertdialog')).toBeNull()
    fireEvent.click(screen.getByRole('button', { name: 'Reconfigurer mon second facteur' }))
    fireEvent.click(within(dialogue()).getByRole('button', { name: 'Continuer vers la reconnexion' }))
    await waitFor(() => expect(demanderAction).toHaveBeenCalledWith('CONFIGURE_TOTP'))
  })

  it('action refusée par le service : message affiché', async () => {
    rendre(<MonComptePage />, { service: avec({ demanderAction: async () => { throw new ApiError(400, 'IAM_ACTION_NOT_ALLOWED', 'x', 'RQ-8') } }) })
    fireEvent.click(await screen.findByRole('button', { name: 'Changer mon mot de passe' }))
    fireEvent.click(within(dialogue()).getByRole('button', { name: 'Continuer vers la reconnexion' }))
    expect(await screen.findByText('Cette action n’est pas disponible en libre-service.')).toBeTruthy()
  })

  it('clés de sécurité : liste, nom obligatoire, enrôlement, annulation, suppression', async () => {
    let annuler = false
    const base = monCompteSimule({ latenceMs: 0 })
    const { container } = rendre(<MonComptePage />, { service: avec({ enrolerCle: async (n) => { if (annuler) throw new ReauthCancelled(); return base.enrolerCle(n) } }, base) })
    expect(await screen.findByText('YubiKey 5 NFC (bureau)')).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Enrôler une clé' }))
    expect(await screen.findByText('Donnez un nom à la clé avant de l’enrôler.')).toBeTruthy()
    fireEvent.change(container.querySelector('input[name="nomNouvelleCle"]')!, { target: { value: 'Clé de secours' } })
    fireEvent.click(screen.getByRole('button', { name: 'Enrôler une clé' }))
    expect(await screen.findByText('La clé « Clé de secours » est enrôlée.')).toBeTruthy()
    expect(await screen.findByText('Clé de secours')).toBeTruthy()
    expect(screen.getAllByText('jamais').length).toBeGreaterThan(0)
    annuler = true
    fireEvent.change(container.querySelector('input[name="nomNouvelleCle"]')!, { target: { value: 'Autre' } })
    fireEvent.click(screen.getByRole('button', { name: 'Enrôler une clé' }))
    expect(await screen.findByText(/a été annulée/)).toBeTruthy()
    fireEvent.click(screen.getAllByRole('button', { name: 'Supprimer la clé' })[0])
    fireEvent.click(within(dialogue()).getByRole('button', { name: 'Supprimer la clé' }))
    expect(await screen.findByText('La clé « YubiKey 5 NFC (bureau) » a été supprimée.')).toBeTruthy()
  })

  it('suppression refusée : erreur avec corrélation', async () => {
    rendre(<MonComptePage />, { service: avec({ supprimerCle: async () => { throw new ApiError(404, 'IAM_KEY_NOT_FOUND', 'x', 'RQ-9') } }) })
    fireEvent.click((await screen.findAllByRole('button', { name: 'Supprimer la clé' }))[0])
    fireEvent.click(within(dialogue()).getByRole('button', { name: 'Supprimer la clé' }))
    expect(await screen.findByText('RQ-9')).toBeTruthy()
  })

  it('sessions : fermer une session, fermer toutes les sessions, erreur', async () => {
    const base = monCompteSimule({ latenceMs: 0 })
    let echec = false
    rendre(<MonComptePage />, { service: avec({ fermerSession: async (id) => { if (echec) throw new ApiError(503, 'X', 'indisponible', 'RQ-10'); return base.fermerSession(id) } }, base) })
    expect(await screen.findByText('10.20.4.62')).toBeTruthy()
    fireEvent.click(screen.getAllByRole('button', { name: 'Fermer' })[1])
    expect(await screen.findByText('La session a été fermée.')).toBeTruthy()
    await waitFor(() => expect(screen.queryByText('10.20.4.62')).toBeNull())
    echec = true
    fireEvent.click(screen.getAllByRole('button', { name: 'Fermer' })[0])
    expect(await screen.findByText('RQ-10')).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Fermer toutes mes sessions' }))
    expect(within(dialogue()).getByText(/y compris celle-ci/)).toBeTruthy()
    fireEvent.click(within(dialogue()).getByRole('button', { name: 'Annuler' }))
    fireEvent.click(screen.getByRole('button', { name: 'Fermer toutes mes sessions' }))
    fireEvent.click(within(dialogue()).getByRole('button', { name: 'Fermer toutes mes sessions' }))
    expect(await screen.findByText('Toutes vos sessions ont été fermées.')).toBeTruthy()
    expect(await screen.findByText('Aucune session ouverte')).toBeTruthy()
  })

  it('compte sans clé ni second facteur : états signalés ; lecture impossible : corrélation', async () => {
    rendre(<MonComptePage />, { service: monCompteSimule({ latenceMs: 0, premierAcces: true, compte: { secondFacteur: false } }) })
    expect(await screen.findByText('Aucune clé de sécurité enrôlée')).toBeTruthy()
    expect(screen.getByText('non configuré')).toBeTruthy()
    expect(screen.getByText('non vérifiée')).toBeTruthy()
    cleanup()
    rendre(<MonComptePage />, { service: avec({ moi: async () => { throw new ApiError(503, 'X', 'x', 'RQ-11') } }) })
    expect(await screen.findByText('RQ-11')).toBeTruthy()
  })

  it.each(['en', 'es'] as const)('rendu en %s sans clé brute', async (langue) => {
    const { container } = rendre(<MonComptePage />, { langue })
    await screen.findByText('YubiKey 5 NFC (bureau)')
    expect(container.textContent).not.toMatch(/\b(compte|reauth|commun)\.[a-zA-Z]/)
    expect(container.textContent).not.toContain('Mon compte')
  })
})
