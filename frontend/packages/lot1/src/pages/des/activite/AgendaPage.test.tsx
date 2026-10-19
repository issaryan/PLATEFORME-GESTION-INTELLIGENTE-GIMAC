import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, screen, waitFor, within } from '@testing-library/react'
import { ApiError } from '@gimac/socle'
import AgendaPage, { COULEUR_CATEGORIE, joursDeVue, voisin } from './AgendaPage'
import { rendrePage, sessionTest } from '../../../test/rendu'
import { servicesSimules } from '../../../services/simule'
import type { Services } from '../../../services/ports'
import { PROFILS } from '../../../permissions'
import { aujourdhui, versSaisie } from '../../../modele/jours'

// Agenda (D-76, /des/agenda) : semaine (jour sur petit écran), invitations, fiche d'un événement, création et
// modification en fenêtre modale (horaires, journée entière, participants de l'annuaire), suppression,
// agenda d'un membre de l'équipe en lecture réduite.
afterEach(() => {
  cleanup()
  vi.restoreAllMocks()
  Object.defineProperty(window, 'innerWidth', { value: 1024, configurable: true })
})

const COLLAB = { id: 'u-collab', nom: 'Nadège Mvondo Assiga' }
const RESP = { id: 'u-resp-info', nom: 'Gaston Ndzana Ebode' }
const services = (u = COLLAB) => servicesSimules({ latenceMs: 0, utilisateur: u })
const session = (u = COLLAB, role = 'ROLE_COLLABORATEUR') => ({ ...sessionTest(PROFILS[role], [role]), sujet: u.id, nom: u.nom })
const page = (chemin = '/des/agenda', o: { s?: Services; u?: typeof COLLAB; role?: string } = {}) =>
  rendrePage(<AgendaPage />, { chemin, motif: '/des/agenda', services: o.s ?? services(o.u), session: session(o.u, o.role) })

describe('Agenda', () => {
  it('semaine, invitation acceptée, fiche d’un événement reçu', async () => {
    page()
    expect(await screen.findByRole('heading', { level: 1, name: 'Agenda' })).toBeTruthy()
    // Grille horaire de la semaine (D-113) : sept colonnes de jours.
    const semaine = await waitFor(() => { const g = document.querySelector<HTMLElement>('.gm-agenda'); expect(g?.getAttribute('aria-label')).toMatch(/^Semaine \d+ : du /); return g! })
    expect(semaine.querySelectorAll('.gm-agenda-colonne')).toHaveLength(7)
    const invitations = await screen.findByRole('heading', { name: 'Invitation en attente (1)' })
    expect(invitations).toBeTruthy()
    fireEvent.click(screen.getAllByRole('button', { name: 'Accepter' })[0])
    expect(await screen.findByText('Invitation acceptée.')).toBeTruthy()
    await waitFor(() => expect(screen.queryByRole('heading', { name: /Invitation en attente/ })).toBeNull())
    fireEvent.click(within(semaine).getByRole('button', { name: /Comité de pilotage des liaisons/ }))
    const fiche = await screen.findByRole('dialog', { name: 'Comité de pilotage des liaisons' })
    expect(within(fiche).getByText('Salle du conseil')).toBeTruthy()
    expect(within(fiche).getByText('Accepté')).toBeTruthy()
    fireEvent.click(within(fiche).getByRole('button', { name: 'Décliner' }))
    expect(await screen.findByText('Invitation déclinée.')).toBeTruthy()
  })

  it('nouvel événement : validations, participants de l’annuaire, création ; modification puis suppression', async () => {
    page()
    fireEvent.click(await screen.findByRole('button', { name: 'Nouvel événement' }))
    const d = await screen.findByRole('dialog', { name: 'Nouvel événement' })
    fireEvent.change(within(d).getByLabelText(/Début/), { target: { value: '9h' } })
    fireEvent.click(within(d).getByRole('button', { name: 'Créer l’événement' }))
    expect(await within(d).findByText('Indiquez le titre de l’événement.')).toBeTruthy()
    expect(within(d).getByText('Heures attendues au format HH:MM.')).toBeTruthy()
    fireEvent.change(within(d).getByLabelText(/^Titre/), { target: { value: 'Point sur la recette' } })
    fireEvent.change(within(d).getByLabelText(/Début/), { target: { value: '15:00' } })
    fireEvent.change(within(d).getByLabelText(/Fin \(HH:MM\)/), { target: { value: '14:00' } })
    fireEvent.click(within(d).getByRole('button', { name: 'Créer l’événement' }))
    expect(await within(d).findByText('La fin doit suivre le début.')).toBeTruthy()
    fireEvent.change(within(d).getByLabelText(/Fin \(HH:MM\)/), { target: { value: '16:00' } })
    fireEvent.change(within(d).getByLabelText('Lieu'), { target: { value: 'Salle 4' } })
    fireEvent.change(within(d).getByLabelText('Catégorie'), { target: { value: 'ACTIVITE' } })
    fireEvent.click(await within(d).findByRole('button', { name: /Idriss Mahamat Saleh/ }))
    // QUO-17 : la personne choisie n'apparaît qu'une fois, dans l'annuaire.
    expect(within(d).getByText('1 personne choisie').parentElement!.textContent).toContain('Idriss Mahamat Saleh')
    expect(within(d).getAllByText('Idriss Mahamat Saleh', { selector: '.gm-tag, .gm-tag *' })).toHaveLength(1)
    fireEvent.click(within(d).getByRole('button', { name: 'Créer l’événement' }))
    expect(await screen.findByText('Événement créé ; les participants sont invités.')).toBeTruthy()
    fireEvent.click(await screen.findByRole('button', { name: /Point sur la recette/ }))
    const fiche = await screen.findByRole('dialog', { name: 'Point sur la recette' })
    expect(within(fiche).getByText('En attente')).toBeTruthy()
    fireEvent.click(within(fiche).getByRole('button', { name: 'Modifier l’événement' }))
    const m = await screen.findByRole('dialog', { name: 'Modifier l’événement' })
    fireEvent.click(within(m).getByLabelText('Journée entière'))
    fireEvent.change(within(m).getByLabelText(/^Au/), { target: { value: 'demain' } })
    fireEvent.click(within(m).getByRole('button', { name: 'Enregistrer l’événement' }))
    expect(await within(m).findByText('Date attendue au format JJ/MM/AAAA.')).toBeTruthy()
    fireEvent.change(within(m).getByLabelText(/^Au/), { target: { value: versSaisie(aujourdhui()) } })
    fireEvent.click(within(m).getByRole('button', { name: 'Enregistrer l’événement' }))
    expect(await screen.findByText('Événement modifié.')).toBeTruthy()
    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull())
    fireEvent.click((await screen.findAllByRole('button', { name: /Point sur la recette/ }))[0])
    const fiche2 = await screen.findByRole('dialog', { name: 'Point sur la recette' })
    expect(within(fiche2).getByText(/toute la journée/)).toBeTruthy()
    fireEvent.click(within(fiche2).getByRole('button', { name: 'Supprimer l’événement' }))
    const conf = await screen.findByRole('alertdialog', { name: 'Supprimer l’événement' })
    fireEvent.click(within(conf).getByRole('checkbox'))
    fireEvent.click(within(conf).getByRole('button', { name: 'Supprimer l’événement' }))
    expect(await screen.findByText('Événement supprimé.')).toBeTruthy()
  })

  it('petit écran : un jour à la fois ; navigation d’un jour à l’autre', async () => {
    Object.defineProperty(window, 'innerWidth', { value: 480, configurable: true })
    page()
    const jour = await waitFor(() => { const g = document.querySelector<HTMLElement>('.gm-agenda'); expect(g?.getAttribute('aria-label')).toMatch(/\d{4}$/); return g! })
    expect(jour.querySelectorAll('.gm-agenda-colonne')).toHaveLength(1)
    fireEvent.click(screen.getByRole('button', { name: 'Jour suivant' }))
    fireEvent.click(screen.getByRole('button', { name: 'Jour précédent' }))
    fireEvent.click(screen.getByRole('button', { name: 'Aujourd’hui' }))
    expect(screen.queryByRole('button', { name: 'Semaine suivante' })).toBeNull()
  })

  it('responsable : agenda d’un membre de l’équipe, lecture réduite et sans création', async () => {
    page('/des/agenda', { u: RESP, role: 'ROLE_RESPONSABLE_SERVICE' })
    const choix = await screen.findByLabelText('Agenda affiché')
    await waitFor(() => expect(within(choix).getAllByRole('option').length).toBe(4))
    fireEvent.change(choix, { target: { value: 'u-sys' } })
    await waitFor(() => expect(screen.queryByRole('button', { name: 'Nouvel événement' })).toBeNull())
    fireEvent.click((await screen.findAllByRole('button', { name: /Congé annuel/ }))[0])
    const fiche = await screen.findByRole('dialog', { name: 'Congé annuel' })
    expect(within(fiche).getByText('Agenda d’un membre de votre équipe : catégorie, titre et horaires seulement.')).toBeTruthy()
    expect(within(fiche).queryByRole('button', { name: 'Modifier l’événement' })).toBeNull()
    fireEvent.click(within(fiche).getAllByRole('button', { name: 'Fermer' }).at(-1)!)
    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull())
  })

  it('refus du service : agenda indisponible, réponse refusée', async () => {
    const s = services()
    vi.spyOn(s.pilotage, 'repondre').mockRejectedValue(new ApiError(404, 'PIL_EVENEMENT_INTROUVABLE', 'Événement introuvable.', 'cid-2'))
    page('/des/agenda', { s })
    fireEvent.click((await screen.findAllByRole('button', { name: 'Décliner' }))[0])
    expect(await screen.findByText('Événement introuvable.')).toBeTruthy()
    cleanup()
    const ko = services()
    vi.spyOn(ko.pilotage, 'agenda').mockRejectedValue(new ApiError(503, 'HTTP_503', '', 'cid-3'))
    page('/des/agenda', { s: ko })
    expect(await screen.findByText('Le service n’a pas pu traiter la demande.')).toBeTruthy()
  })

  it('vues jour et mois, filtre des catégories, création depuis une case de la grille (D-113)', async () => {
    page()
    expect(await screen.findAllByRole('button', { name: /Comité de pilotage des liaisons/ })).toBeTruthy()
    fireEvent.click(screen.getByLabelText('Réunion'))
    await waitFor(() => expect(screen.queryByRole('button', { name: /Comité de pilotage des liaisons/ })).toBeNull())
    fireEvent.click(screen.getByLabelText('Réunion'))
    expect(await screen.findAllByRole('button', { name: /Comité de pilotage des liaisons/ })).toBeTruthy()
    fireEvent.click(screen.getByRole('tab', { name: 'Jour' }))
    await waitFor(() => expect(document.querySelectorAll('.gm-agenda-colonne')).toHaveLength(1))
    fireEvent.click(screen.getByRole('button', { name: /Créer un événement le .* à 10:00/ }))
    const d = await screen.findByRole('dialog', { name: 'Nouvel événement' })
    expect((within(d).getByLabelText(/Début \(HH:MM\)/) as HTMLInputElement).value).toBe('10:00')
    fireEvent.keyDown(d, { key: 'Escape' })
    fireEvent.click(screen.getByRole('tab', { name: 'Mois' }))
    const mois = await waitFor(() => { const g = document.querySelector<HTMLElement>('.gm-agenda-mois'); expect(g).toBeTruthy(); return g! })
    expect(mois.querySelectorAll('.gm-agenda-mois-case')).toHaveLength(42)
    fireEvent.click(screen.getByRole('button', { name: 'Mois suivant' }))
    fireEvent.click(screen.getByRole('button', { name: 'Mois précédent' }))
  })

  it('calcule les jours affichés et la période voisine', () => {
    expect(joursDeVue('jour', '2026-10-19')).toEqual(['2026-10-19'])
    expect(joursDeVue('semaine', '2026-10-21')[0]).toBe('2026-10-19')
    const m = joursDeVue('mois', '2026-10-19')
    expect([m.length, m[0], m[41]]).toEqual([42, '2026-09-28', '2026-11-08'])
    expect(voisin('jour', '2026-10-19', 1)).toBe('2026-10-20')
    expect(voisin('semaine', '2026-10-19', -1)).toBe('2026-10-12')
    expect(voisin('mois', '2026-12-19', 1)).toBe('2027-01-01')
    expect(new Set(Object.values(COULEUR_CATEGORIE)).size).toBe(6)
  })
})
