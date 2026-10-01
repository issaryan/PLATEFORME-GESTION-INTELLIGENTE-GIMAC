import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, screen, waitFor, within } from '@testing-library/react'
import { ApiError, MOT_DE_PASSE_DEMO, ReauthCancelled } from '@gimac/socle'
import MonComptePage from './MonComptePage'
import { monCompteSimule } from '../services/simule'
import { avec, rendre } from '../test/rendu'

// D-62, D-65 : toute modification de « Mon compte » se fait dans une fenêtre modale, sur la plateforme.
afterEach(cleanup)
const confirmation = () => screen.getByRole('alertdialog')
const fenetre = () => screen.getByRole('dialog')
const saisir = (nom: string, valeur: string) => fireEvent.change(document.querySelector(`[name="${nom}"]`)!, { target: { value: valeur } })

describe('Mon compte', () => {
  it('fiche : lecture, modification dans une fenêtre modale, enregistrement annoncé, erreur de téléphone sur le champ', async () => {
    const base = monCompteSimule({ latenceMs: 0 })
    const modifier = vi.fn(base.modifier)
    rendre(<MonComptePage />, { service: avec({ modifier }, base) })
    expect(await screen.findByRole('heading', { level: 1, name: 'Mon compte' })).toBeTruthy()
    expect(await screen.findByText('demo')).toBeTruthy()
    expect(screen.getByText('Chef de la DDRM')).toBeTruthy()
    expect(document.querySelector('input[name="fonction"]')).toBeNull()
    fireEvent.click(screen.getByRole('button', { name: 'Modifier ma fiche' }))
    expect(fenetre().getAttribute('aria-modal')).toBe('true')
    saisir('fonction', 'Cheffe de la DDRM')
    saisir('langue', 'es')
    fireEvent.click(within(fenetre()).getByRole('button', { name: 'Enregistrer ma fiche' }))
    expect(await screen.findByText('Votre fiche a été enregistrée.')).toBeTruthy()
    expect(screen.queryByRole('dialog')).toBeNull()
    expect(modifier).toHaveBeenCalledWith(expect.objectContaining({ fonction: 'Cheffe de la DDRM', langue: 'es', prenom: 'Aïssatou' }))
    expect(await screen.findByText('Cheffe de la DDRM')).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Modifier ma fiche' }))
    saisir('telephone', 'abc')
    fireEvent.click(within(fenetre()).getByRole('button', { name: 'Enregistrer ma fiche' }))
    expect(await screen.findByText(/Numéro de téléphone invalide/)).toBeTruthy()
    expect(document.querySelector('input[name="telephone"]')!.getAttribute('aria-invalid')).toBe('true')
    // Saisie modifiée : l'abandon est confirmé.
    fireEvent.click(within(fenetre()).getByRole('button', { name: 'Annuler' }))
    expect(within(fenetre()).getByText('Abandonner la saisie ?')).toBeTruthy()
    fireEvent.click(within(fenetre()).getByRole('button', { name: 'Reprendre la saisie' }))
    fireEvent.click(within(fenetre()).getByRole('button', { name: 'Annuler' }))
    fireEvent.click(within(fenetre()).getByRole('button', { name: 'Abandonner les modifications' }))
    expect(screen.queryByRole('dialog')).toBeNull()
  })

  it('fiche : langue refusée et erreur générale avec corrélation', async () => {
    let code = 'IAM_LANGUAGE_INVALID'
    rendre(<MonComptePage />, { service: avec({ modifier: async () => { throw new ApiError(400, code, 'x', 'RQ-7') } }) })
    fireEvent.click(await screen.findByRole('button', { name: 'Modifier ma fiche' }))
    fireEvent.click(within(fenetre()).getByRole('button', { name: 'Enregistrer ma fiche' }))
    expect(await screen.findByText(/Langue invalide/)).toBeTruthy()
    code = 'IAM_IDP_UNAVAILABLE'
    fireEvent.click(within(fenetre()).getByRole('button', { name: 'Enregistrer ma fiche' }))
    expect(await screen.findByText('RQ-7')).toBeTruthy()
  })

  it('mot de passe : fenêtre modale, politique, violations, changement annoncé', async () => {
    const base = monCompteSimule({ latenceMs: 0 })
    const changer = vi.fn(base.changerMotDePasse)
    rendre(<MonComptePage />, { service: avec({ changerMotDePasse: changer }, base) })
    fireEvent.click(await screen.findByRole('button', { name: 'Changer mon mot de passe' }))
    expect(within(fenetre()).getByText(/Au moins 12 caractères/)).toBeTruthy()
    expect(within(fenetre()).getByLabelText(/^Mot de passe actuel/)).toBeTruthy()
    saisir('motDePasseActuel', MOT_DE_PASSE_DEMO)
    saisir('nouveauMotDePasse', 'motdepasse-simple')
    saisir('confirmationMotDePasse', 'motdepasse-simple')
    fireEvent.click(within(fenetre()).getByRole('button', { name: 'Enregistrer le mot de passe' }))
    expect(await screen.findByText(/au moins une majuscule/)).toBeTruthy()
    expect(screen.getByText(/au moins un chiffre/)).toBeTruthy()
    saisir('motDePasseActuel', MOT_DE_PASSE_DEMO)
    saisir('nouveauMotDePasse', 'Nouveau-Mdp-2026!')
    saisir('confirmationMotDePasse', 'Nouveau-Mdp-2026!')
    fireEvent.click(within(fenetre()).getByRole('button', { name: 'Enregistrer le mot de passe' }))
    expect(await screen.findByText('Votre mot de passe a été changé.')).toBeTruthy()
    expect(screen.queryByRole('dialog')).toBeNull()
    expect(changer).toHaveBeenLastCalledWith({ actuel: MOT_DE_PASSE_DEMO, nouveau: 'Nouveau-Mdp-2026!' })
    fireEvent.click(screen.getByRole('button', { name: 'Changer mon mot de passe' }))
    fireEvent.click(within(fenetre()).getByRole('button', { name: 'Annuler' }))
    expect(screen.queryByRole('dialog')).toBeNull()
  })

  it('code TOTP : fenêtre modale, remplacement annoncé, code QR, activation', async () => {
    rendre(<MonComptePage />)
    fireEvent.click(await screen.findByRole('button', { name: 'Reconfigurer mon code TOTP' }))
    expect(within(fenetre()).getByText(/sera remplacé/)).toBeTruthy()
    expect(await within(fenetre()).findByRole('img', { name: /Code QR/ })).toBeTruthy()
    saisir('codeTotp', '424242')
    fireEvent.click(within(fenetre()).getByRole('button', { name: 'Vérifier et activer' }))
    expect(await screen.findByText(/Votre application d’authentification est associée/)).toBeTruthy()
    expect(screen.queryByRole('dialog')).toBeNull()
    fireEvent.click(screen.getByRole('button', { name: 'Reconfigurer mon code TOTP' }))
    fireEvent.click(within(fenetre()).getByRole('button', { name: 'Fermer' }))
    expect(screen.queryByRole('dialog')).toBeNull()
  })

  it('clés de sécurité : liste, enrôlement dans une fenêtre modale, nom obligatoire, annulation, suppression', async () => {
    let annuler = false
    const base = monCompteSimule({ latenceMs: 0 })
    rendre(<MonComptePage />, { service: avec({ enrolerCle: async (n) => { if (annuler) throw new ReauthCancelled(); return base.enrolerCle(n) } }, base) })
    expect(await screen.findByText('YubiKey 5 NFC (bureau)')).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Enrôler une clé' }))
    saisir('nomNouvelleCle', '  ')
    fireEvent.click(within(fenetre()).getByRole('button', { name: 'Enrôler une clé' }))
    expect(await screen.findByText('Donnez un nom à la clé avant de l’enrôler.')).toBeTruthy()
    saisir('nomNouvelleCle', 'Clé de secours')
    fireEvent.click(within(fenetre()).getByRole('button', { name: 'Enrôler une clé' }))
    expect(await screen.findByText('La clé « Clé de secours » est enrôlée.')).toBeTruthy()
    expect(await screen.findByText('Clé de secours')).toBeTruthy()
    expect(screen.getAllByText('jamais').length).toBeGreaterThan(0)
    annuler = true
    fireEvent.click(screen.getByRole('button', { name: 'Enrôler une clé' }))
    fireEvent.click(within(fenetre()).getByRole('button', { name: 'Enrôler une clé' }))
    expect(await screen.findByText(/a été annulée/)).toBeTruthy()
    fireEvent.click(within(fenetre()).getByRole('button', { name: 'Annuler' }))
    fireEvent.click(screen.getAllByRole('button', { name: 'Supprimer la clé' })[0])
    fireEvent.click(within(confirmation()).getByLabelText('Je confirme : cette action est irréversible.'))
    fireEvent.click(within(confirmation()).getByRole('button', { name: 'Supprimer la clé' }))
    expect(await screen.findByText('La clé « YubiKey 5 NFC (bureau) » a été supprimée.')).toBeTruthy()
  })

  it('suppression refusée : erreur avec corrélation', async () => {
    rendre(<MonComptePage />, { service: avec({ supprimerCle: async () => { throw new ApiError(404, 'IAM_KEY_NOT_FOUND', 'x', 'RQ-9') } }) })
    fireEvent.click((await screen.findAllByRole('button', { name: 'Supprimer la clé' }))[0])
    fireEvent.click(within(confirmation()).getByLabelText('Je confirme : cette action est irréversible.'))
    fireEvent.click(within(confirmation()).getByRole('button', { name: 'Supprimer la clé' }))
    expect(await screen.findByText('RQ-9')).toBeTruthy()
  })

  it('appareils connectés : session courante marquée, fermer une session puis les autres, refus avec corrélation', async () => {
    const base = monCompteSimule({ latenceMs: 0 })
    let echec = false
    rendre(<MonComptePage />, { service: avec({ fermerAppareil: async (id) => { if (echec) throw new ApiError(409, 'CNX_SESSION_COURANTE', 'x', 'RQ-10'); return base.fermerAppareil(id) } }, base) })
    const carte = (await screen.findByRole('heading', { name: 'Appareils connectés' })).closest('section')!
    expect(await within(carte).findByText('Firefox 131')).toBeTruthy()
    const courante = within(carte).getByText('Cet appareil').closest('tr')!
    expect(within(courante).getByText('10.20.1.15')).toBeTruthy()
    expect(within(courante).queryByRole('button')).toBeNull()
    expect(within(carte).getByText('10.20.4.•')).toBeTruthy()
    fireEvent.click(within(within(carte).getByText('Firefox 131').closest('tr')!).getByRole('button', { name: 'Fermer la session' }))
    expect(within(confirmation()).getByText(/Ordinateur · Firefox 131 · Ubuntu 24\.04/)).toBeTruthy()
    fireEvent.click(within(confirmation()).getByRole('button', { name: 'Fermer la session' }))
    expect(await screen.findByText('La session a été fermée : l’appareil est déconnecté.')).toBeTruthy()
    await waitFor(() => expect(within(carte).queryByText('Firefox 131')).toBeNull())
    echec = true
    fireEvent.click(within(carte).getAllByRole('button', { name: 'Fermer la session' })[0])
    fireEvent.click(within(confirmation()).getByRole('button', { name: 'Fermer la session' }))
    expect(await screen.findByText('RQ-10')).toBeTruthy()
    expect(screen.getByText(/utilisez « Se déconnecter »/)).toBeTruthy()
    fireEvent.click(within(carte).getByRole('button', { name: 'Fermer les autres sessions' }))
    expect(within(confirmation()).getByText('3 autres sessions seront fermées. Cet appareil reste connecté.')).toBeTruthy()
    fireEvent.click(within(confirmation()).getByRole('button', { name: 'Annuler' }))
    fireEvent.click(within(carte).getByRole('button', { name: 'Fermer les autres sessions' }))
    fireEvent.click(within(confirmation()).getByRole('button', { name: 'Fermer les autres sessions' }))
    expect(await screen.findByText('Vos autres sessions ont été fermées. Cet appareil reste connecté.')).toBeTruthy()
    await waitFor(() => expect(within(carte).getAllByRole('row')).toHaveLength(2))
    expect((within(carte).getByRole('button', { name: 'Fermer les autres sessions' }) as HTMLButtonElement).disabled).toBe(true)
  })

  it('compte sans clé ni second facteur : états signalés ; lecture impossible : corrélation', async () => {
    rendre(<MonComptePage />, { service: monCompteSimule({ latenceMs: 0, premierAcces: true, compte: { secondFacteur: false, langue: null, prenom: null } }) })
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
    expect(container.textContent).not.toMatch(/\b(compte|reauth|commun|motDePasse|totp)\.[a-zA-Z]/)
    expect(container.textContent).not.toContain('Mon compte')
  })
})
