import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, screen, waitFor, within } from '@testing-library/react'
import { ApiError } from '@gimac/socle'
import MaJourneePage from './MaJourneePage'
import { rendrePage, sessionTest } from '../../../test/rendu'
import { servicesSimules } from '../../../services/simule'
import type { Services } from '../../../services/ports'
import { PROFILS } from '../../../permissions'
import { ajouterJours, aujourdhui, versSaisie } from '../../../modele/jours'

// Ma journée (D-76, /des/activite) : activités du jour (ajout, modification, statut, report, ordre,
// suppression), rendez-vous et tâches du jour, rapport du jour rédigé et soumis ; lecture de la journée
// d'un membre de l'équipe ; refus du service expliqués.
afterEach(() => {
  cleanup()
  vi.restoreAllMocks()
})

const COLLAB = { id: 'u-collab', nom: 'Nadège Mvondo Assiga' }
const RESP = { id: 'u-resp-info', nom: 'Gaston Ndzana Ebode' }
const services = (u = COLLAB) => servicesSimules({ latenceMs: 0, utilisateur: u })
const session = (u = COLLAB, role = 'ROLE_COLLABORATEUR') => ({ ...sessionTest(PROFILS[role], [role]), sujet: u.id, nom: u.nom })
const page = (chemin = '/des/activite', o: { s?: Services; u?: typeof COLLAB; role?: string } = {}) =>
  rendrePage(<MaJourneePage />, { chemin, motif: '/des/activite', services: o.s ?? services(o.u), session: session(o.u, o.role) })
const dialogue = (nom: string | RegExp, alerte = false) => screen.findByRole(alerte ? 'alertdialog' : 'dialog', { name: nom })
const tableau = async () => (await screen.findAllByRole('table'))[0]

describe('Ma journée', () => {
  it('activités, rendez-vous et rapport du jour ; ajout validé puis enregistré', async () => {
    page()
    expect(await screen.findByRole('heading', { level: 1, name: 'Ma journée' })).toBeTruthy()
    expect(await screen.findByText('Configurer le VPN du participant')).toBeTruthy()
    expect(screen.getByRole('heading', { name: 'Activités du jour (3)' })).toBeTruthy()
    expect(await screen.findByRole('link', { name: /Point téléphonique avec le correspondant/ })).toBeTruthy()
    expect(await screen.findByText('Votre rapport du jour n’est pas encore rédigé.')).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Ajouter une activité' }))
    const d = await dialogue('Ajouter une activité')
    fireEvent.change(within(d).getByLabelText(/Temps passé/), { target: { value: '2000' } })
    fireEvent.click(within(d).getByRole('button', { name: 'Ajouter une activité' }))
    expect(await within(d).findByText('Indiquez l’intitulé de l’activité.')).toBeTruthy()
    expect(within(d).getByText('Durée entière de 0 à 1440 minutes.')).toBeTruthy()
    fireEvent.change(within(d).getByLabelText(/^Intitulé/), { target: { value: 'Tester la liaison de secours' } })
    fireEvent.change(within(d).getByLabelText(/Temps passé/), { target: { value: '45' } })
    fireEvent.change(within(d).getByLabelText(/^Jour/), { target: { value: '31/02/2026' } })
    fireEvent.click(within(d).getByRole('button', { name: 'Ajouter une activité' }))
    expect(await within(d).findByText('Date attendue au format JJ/MM/AAAA.')).toBeTruthy()
    fireEvent.change(within(d).getByLabelText(/^Jour/), { target: { value: versSaisie(aujourdhui()) } })
    fireEvent.click(within(d).getByRole('button', { name: 'Ajouter une activité' }))
    expect(await screen.findByText('Activité ajoutée.')).toBeTruthy()
    expect(await screen.findByText('Tester la liaison de secours')).toBeTruthy()
    expect(screen.getByText('45 min')).toBeTruthy()
  })

  it('statut, ordre, modification, report et suppression d’une activité', async () => {
    page()
    const t = await tableau()
    await within(t).findByText('Configurer le VPN du participant')
    fireEvent.change(within(t).getByRole('combobox', { name: 'Statut de « Configurer le VPN du participant »' }), { target: { value: 'FAITE' } })
    expect(await screen.findByText('Statut changé : Faite.')).toBeTruthy()
    // Ordre : la deuxième activité monte en tête.
    fireEvent.click(within(t).getByRole('button', { name: 'Monter « Configurer le VPN du participant »' }))
    await waitFor(() => expect(within(t).getAllByRole('row')[1].textContent).toContain('Configurer le VPN du participant'))
    fireEvent.click(within(t).getByRole('button', { name: 'Descendre « Configurer le VPN du participant »' }))
    await waitFor(() => expect(within(t).getAllByRole('row')[2].textContent).toContain('Configurer le VPN du participant'))
    // Modification.
    fireEvent.click(within(t).getByRole('button', { name: 'Modifier « Classer les comptes rendus de la semaine »' }))
    const m = await dialogue('Modifier l’activité')
    fireEvent.change(within(m).getByLabelText(/^Intitulé/), { target: { value: 'Classer les comptes rendus' } })
    fireEvent.click(within(m).getByRole('button', { name: 'Enregistrer l’activité' }))
    expect(await screen.findByText('Activité modifiée.')).toBeTruthy()
    // Report : jour antérieur refusé, puis jour ouvré suivant.
    fireEvent.click(await within(t).findByRole('button', { name: 'Reporter « Classer les comptes rendus »' }))
    const r = await dialogue('Reporter l’activité', true)
    fireEvent.change(within(r).getByLabelText(/Reporter au/), { target: { value: versSaisie(ajouterJours(aujourdhui(), -3)) } })
    fireEvent.click(within(r).getByRole('button', { name: 'Reporter l’activité' }))
    expect(await within(r).findByText('Choisissez un jour postérieur à celui de l’activité.')).toBeTruthy()
    fireEvent.change(within(r).getByLabelText(/Reporter au/), { target: { value: versSaisie(ajouterJours(aujourdhui(), 1)) } })
    fireEvent.click(within(r).getByRole('button', { name: 'Reporter l’activité' }))
    expect(await screen.findByText(/^Activité reportée au /)).toBeTruthy()
    expect(await within(t).findByText(/^Reportée au /)).toBeTruthy()
    // Suppression confirmée.
    fireEvent.click(within(t).getByRole('button', { name: 'Supprimer « Point de situation avec l’équipe »' }))
    const s = await dialogue('Supprimer l’activité', true)
    fireEvent.click(within(s).getByRole('button', { name: 'Supprimer l’activité' }))
    expect(await screen.findByText('Activité supprimée.')).toBeTruthy()
    await waitFor(() => expect(within(t).queryByText('Point de situation avec l’équipe')).toBeNull())
  })

  it('rédiger le rapport du jour : prérempli, soumis au responsable, puis ouvert', async () => {
    page()
    const rediger = await screen.findByRole('button', { name: 'Rédiger mon rapport' })
    await waitFor(() => expect((rediger as HTMLButtonElement).disabled).toBe(false))
    fireEvent.click(rediger)
    const d = await dialogue('Nouveau rapport')
    expect(await within(d).findByText('Le rapport soumis sera remis à Gaston Ndzana Ebode, votre responsable.')).toBeTruthy()
    fireEvent.click(within(d).getByRole('button', { name: 'Enregistrer et soumettre' }))
    expect(await within(d).findByText('Le rapport est vide : préremplissez-le ou rédigez-le.')).toBeTruthy()
    fireEvent.click(within(d).getByRole('button', { name: 'Préremplir à partir de mes activités' }))
    await waitFor(() => expect((within(d).getByLabelText(/Contenu du rapport/) as HTMLTextAreaElement).value).toContain('## Réalisé'))
    fireEvent.click(within(d).getByRole('button', { name: 'Enregistrer et soumettre' }))
    expect(await screen.findByText(/^Rapport remis à .+ \(votre n\+1\)\.$/)).toBeTruthy()
    expect(await screen.findByText('Votre rapport est soumis à votre responsable.')).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Ouvrir mon rapport' }))
    const c = await dialogue(/Rapport quotidien de Nadège Mvondo Assiga/)
    expect(await within(c).findByText('Gaston Ndzana Ebode (votre n+1)')).toBeTruthy()
    expect(within(c).getByRole('heading', { name: 'Réalisé' })).toBeTruthy()
  })

  it('journée d’un membre de l’équipe : lecture seule ; jour précédent et retour à aujourd’hui', async () => {
    page(`/des/activite?collaborateur=u-res`, { u: RESP, role: 'ROLE_RESPONSABLE_SERVICE' })
    expect(await screen.findByRole('heading', { level: 1, name: 'Journée de Idriss Mahamat Saleh' })).toBeTruthy()
    expect(await screen.findByText('Configurer le VPN du participant')).toBeTruthy()
    expect(screen.queryByRole('button', { name: 'Ajouter une activité' })).toBeNull()
    expect(screen.queryByRole('combobox')).toBeNull()
    expect(screen.queryByText('Rapport du jour')).toBeNull()
    fireEvent.click(screen.getByRole('button', { name: 'Jour précédent' }))
    expect(await screen.findByRole('button', { name: 'Aujourd’hui' })).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Jour suivant' }))
    await waitFor(() => expect(screen.queryByRole('button', { name: 'Aujourd’hui' })).toBeNull())
  })

  it('refus du service : journée hors équipe, changement de statut refusé', async () => {
    page(`/des/activite?collaborateur=U-001`, { u: RESP, role: 'ROLE_RESPONSABLE_SERVICE' })
    expect(await screen.findByText('Cette personne ou cette unité est hors de votre portée selon la matrice des pouvoirs.', {}, { timeout: 4000 })).toBeTruthy()
    cleanup()
    const s = services()
    vi.spyOn(s.pilotage, 'changerStatut').mockRejectedValue(new ApiError(409, 'PIL_TRANSITION_INTERDITE', 'x', 'cid-1'))
    page('/des/activite', { s })
    const t = await tableau()
    fireEvent.change(await within(t).findByRole('combobox', { name: 'Statut de « Configurer le VPN du participant »' }), { target: { value: 'ANNULEE' } })
    expect(await screen.findByText('Ce changement d’état n’est pas permis.')).toBeTruthy()
  })
})
