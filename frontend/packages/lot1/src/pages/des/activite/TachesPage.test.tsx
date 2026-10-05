import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, screen, waitFor, within } from '@testing-library/react'
import { ApiError } from '@gimac/socle'
import TachesPage from './TachesPage'
import { rendrePage, sessionTest } from '../../../test/rendu'
import { servicesSimules } from '../../../services/simule'
import type { Services } from '../../../services/ports'
import { PROFILS } from '../../../permissions'

// Tâches (D-76, /des/taches) : onglets selon les permissions, filtre d'état, fiche (origine, historique),
// transitions motivées, création pour soi ou pour un membre de l'équipe, modification, refus expliqués.
afterEach(() => {
  cleanup()
  vi.restoreAllMocks()
})

const COLLAB = { id: 'u-collab', nom: 'Nadège Mvondo Assiga' }
const RESP = { id: 'u-resp-info', nom: 'Gaston Ndzana Ebode' }
const services = (u = COLLAB) => servicesSimules({ latenceMs: 0, utilisateur: u })
const session = (u = COLLAB, role = 'ROLE_COLLABORATEUR') => ({ ...sessionTest(PROFILS[role], [role]), sujet: u.id, nom: u.nom })
const page = (chemin = '/des/taches', o: { s?: Services; u?: typeof COLLAB; role?: string } = {}) =>
  rendrePage(<TachesPage />, { chemin, motif: '/des/taches', services: o.s ?? services(o.u), session: session(o.u, o.role) })

describe('Tâches', () => {
  it('collaborateur : ses tâches seulement, fiche avec origine et historique, transition motivée', async () => {
    page()
    expect(await screen.findByRole('heading', { level: 1, name: 'Tâches' })).toBeTruthy()
    expect(screen.queryByRole('tab')).toBeNull()
    fireEvent.click(await screen.findByRole('button', { name: 'Mettre à jour la fiche de procédure du service' }))
    const fiche = await screen.findByRole('dialog', { name: 'Mettre à jour la fiche de procédure du service' })
    expect(await within(fiche).findByText('Historique')).toBeTruthy()
    expect(within(fiche).getAllByText(/^Créée/).length).toBeGreaterThan(0)
    fireEvent.click(within(fiche).getByRole('button', { name: 'Signaler un blocage' }))
    const tr = await screen.findByRole('alertdialog', { name: 'Signaler un blocage' })
    fireEvent.change(within(tr).getByLabelText(/Motif/), { target: { value: 'Attente du matériel commandé' } })
    fireEvent.click(within(tr).getByRole('button', { name: 'Signaler un blocage' }))
    expect(await screen.findByText('Tâche passée à l’état « Bloquée ».')).toBeTruthy()
    const fiche2 = await screen.findByRole('dialog', { name: 'Mettre à jour la fiche de procédure du service' })
    expect(await within(fiche2).findByText(/Attente du matériel commandé/)).toBeTruthy()
    fireEvent.click(within(fiche2).getByRole('button', { name: 'Modifier la tâche' }))
    const m = await screen.findByRole('dialog', { name: 'Modifier la tâche' })
    expect(within(m).queryByLabelText('Responsable')).toBeNull()
    fireEvent.change(within(m).getByLabelText(/^Échéance/), { target: { value: '99/99/2026' } })
    fireEvent.click(within(m).getByRole('button', { name: 'Enregistrer la tâche' }))
    expect(await within(m).findByText('Date attendue au format JJ/MM/AAAA.')).toBeTruthy()
    fireEvent.change(within(m).getByLabelText(/^Échéance/), { target: { value: '' } })
    fireEvent.change(within(m).getByLabelText('Priorité'), { target: { value: 'URGENTE' } })
    fireEvent.click(within(m).getByRole('button', { name: 'Enregistrer la tâche' }))
    expect(await screen.findByText('Tâche modifiée.')).toBeTruthy()
  })

  it('tâche issue d’une opération : lien vers son origine ; filtre d’état', async () => {
    page('/des/taches', { u: { id: 'U-201', nom: 'Aïcha Mahamat Nour' }, role: 'ROLE_CONDUCTEUR_PROJET' })
    fireEvent.click(await screen.findByRole('button', { name: 'Compléter les preuves de l’opération OP-E05 de LIA-2026-030' }))
    const fiche = await screen.findByRole('dialog', { name: /Compléter les preuves/ })
    expect((await within(fiche).findByRole('link', { name: 'Ouvrir' })).getAttribute('href')).toBe('/des/parcours/LIA-2026-030')
    expect(within(fiche).getAllByText('En retard').length).toBeGreaterThan(0)
    fireEvent.click(within(fiche).getAllByRole('button', { name: 'Fermer' }).at(-1)!)
    fireEvent.change(screen.getByLabelText('État'), { target: { value: 'TERMINEE' } })
    expect(await screen.findByText('Archiver les pièces du dossier LIA-2026-019')).toBeTruthy()
    await waitFor(() => expect(screen.queryByText('Mettre à jour la fiche de procédure du service')).toBeNull())
  })

  it('responsable : onglets Confiées et Équipe, tâche confiée à un membre ; refus du service', async () => {
    const s = services(RESP)
    page('/des/taches', { s, u: RESP, role: 'ROLE_RESPONSABLE_SERVICE' })
    expect(await screen.findByRole('tab', { name: 'Confiées' })).toBeTruthy()
    fireEvent.click(screen.getByRole('tab', { name: 'Équipe' }))
    expect(await screen.findByRole('columnheader', { name: 'Responsable' })).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Nouvelle tâche' }))
    const d = await screen.findByRole('dialog', { name: 'Nouvelle tâche' })
    fireEvent.click(within(d).getByRole('button', { name: 'Créer la tâche' }))
    expect(await within(d).findByText('Indiquez l’intitulé de la tâche.')).toBeTruthy()
    fireEvent.change(within(d).getByLabelText(/^Intitulé/), { target: { value: 'Vérifier les sauvegardes' } })
    const choix = within(d).getByLabelText('Responsable')
    await waitFor(() => expect(within(choix).getAllByRole('option').length).toBe(4))
    fireEvent.change(choix, { target: { value: 'u-collab' } })
    fireEvent.click(within(d).getByRole('button', { name: 'Créer la tâche' }))
    expect(await screen.findByText('Tâche confiée.')).toBeTruthy()
    fireEvent.click(screen.getByRole('tab', { name: 'Confiées' }))
    expect(await screen.findByRole('button', { name: 'Vérifier les sauvegardes' })).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Vérifier les sauvegardes' }))
    const fiche = await screen.findByRole('dialog', { name: 'Vérifier les sauvegardes' })
    // Créateur sans être responsable : il peut annuler, pas commencer.
    expect(await within(fiche).findByRole('button', { name: 'Annuler la tâche' })).toBeTruthy()
    expect(within(fiche).queryByRole('button', { name: 'Commencer' })).toBeNull()
    vi.spyOn(s.pilotage, 'transition').mockRejectedValueOnce(new ApiError(409, 'PIL_TRANSITION_INTERDITE', 'x', 'cid-9'))
    fireEvent.click(within(fiche).getByRole('button', { name: 'Annuler la tâche' }))
    const tr = await screen.findByRole('alertdialog', { name: 'Annuler la tâche' })
    fireEvent.change(within(tr).getByLabelText(/Motif/), { target: { value: 'Demande devenue sans objet' } })
    fireEvent.click(within(tr).getByRole('button', { name: 'Annuler la tâche' }))
    expect(await within(tr).findByText('Ce changement d’état n’est pas permis.')).toBeTruthy()
  })

  it('liste indisponible : erreur et nouvel essai', async () => {
    const s = services()
    vi.spyOn(s.pilotage, 'taches').mockRejectedValue(new ApiError(503, 'PIL_ORGANISATION_INDISPONIBLE', 'x', 'cid-5'))
    page('/des/taches', { s })
    expect(await screen.findByText('cid-5', { exact: false })).toBeTruthy()
  })
})
