import { afterEach, describe, expect, it } from 'vitest'
import { cleanup, fireEvent, screen, waitFor, within } from '@testing-library/react'
import { ApiError } from '@gimac/socle'
import { OngletReexamens } from './reexamens'
import { LECTEUR, avec, rendrePage, simules } from '../test/rendu'
import { reexamensSimules } from '../services/simule/reexamens'

afterEach(cleanup)

// US-EXI-23 critère 2 : permissions SBX visibles, décisions, clôture et empreinte.
describe('OngletReexamens', () => {
  it('présente les permissions SBX, enregistre les décisions et scelle la campagne', async () => {
    rendrePage(<OngletReexamens />, { chemin: '/console/sandbox?onglet=reexamens', motif: '/console/sandbox' })
    expect(await screen.findByText('Awa Testeuse')).toBeTruthy()
    fireEvent.click(screen.getByText(/3 permissions, dont 2 SBX/))
    expect(screen.getByText('SBX_CER_EXECUTION')).toBeTruthy()
    expect(screen.getByRole('button', { name: 'Clôturer et sceller' })).toHaveProperty('disabled', true)

    const premier = screen.getAllByRole('button', { name: 'Décider' })[0]
    fireEvent.click(premier)
    let dialogue = await screen.findByRole('dialog')
    fireEvent.click(within(dialogue).getByRole('button', { name: 'Enregistrer la décision' }))
    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull())

    fireEvent.click(screen.getByRole('button', { name: 'Décider' }))
    dialogue = await screen.findByRole('dialog')
    fireEvent.change(within(dialogue).getByLabelText('Décision'), { target: { value: 'REVOQUE' } })
    fireEvent.change(within(dialogue).getByLabelText(/Motif de la réduction/), { target: { value: 'Fin de mission' } })
    fireEvent.click(within(dialogue).getByRole('button', { name: 'Enregistrer la décision' }))
    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull())
    expect(screen.getByText('Révoqué')).toBeTruthy()

    fireEvent.click(screen.getByRole('button', { name: 'Clôturer et sceller' }))
    const confirmation = await screen.findByRole('alertdialog')
    fireEvent.click(within(confirmation).getByRole('button', { name: 'Clôturer et sceller' }))
    await waitFor(() => expect(screen.getByText('Close')).toBeTruthy())
    expect(screen.getByText('Empreinte de la campagne scellée')).toBeTruthy()
  })

  it('reste en lecture seule sans IAM_ACCOUNT_ROLES', async () => {
    rendrePage(<OngletReexamens />, { chemin: '/console/sandbox?onglet=reexamens', motif: '/console/sandbox', permissions: LECTEUR })
    expect(await screen.findByText('Awa Testeuse')).toBeTruthy()
    expect(screen.queryByRole('button', { name: 'Décider' })).toBeNull()
    expect(screen.queryByRole('button', { name: 'Clôturer et sceller' })).toBeNull()
  })

  it('exige une réduction stricte et son motif avant de transmettre les profils conservés', async () => {
    const base = simules()
    const services = avec('reexamens', {
      lire: async (id) => {
        const vue = await base.reexamens.lire(id)
        return { ...vue, acces: vue.acces.map((a, i) => i === 0 ? { ...a, profils: ['ROLE_SBX_TESTEUR', 'ROLE_SBX_LECTEUR'] } : a) }
      },
    }, base)
    rendrePage(<OngletReexamens />, { chemin: '/console/sandbox?onglet=reexamens', motif: '/console/sandbox', services })
    fireEvent.click((await screen.findAllByRole('button', { name: 'Décider' }))[0])
    const dialogue = await screen.findByRole('dialog')
    fireEvent.change(within(dialogue).getByLabelText('Décision'), { target: { value: 'REDUIT' } })
    fireEvent.click(within(dialogue).getByRole('button', { name: 'Enregistrer la décision' }))
    expect(within(dialogue).getByText('Le motif est obligatoire.')).toBeTruthy()
    expect(within(dialogue).getByText('Choisissez une réduction stricte et renseignez le motif.')).toBeTruthy()

    fireEvent.click(within(dialogue).getByRole('checkbox', { name: 'ROLE_SBX_TESTEUR' }))
    fireEvent.click(within(dialogue).getByRole('checkbox', { name: 'ROLE_SBX_LECTEUR' }))
    fireEvent.click(within(dialogue).getByRole('button', { name: 'Enregistrer la décision' }))
    expect(within(dialogue).getByText('Choisissez une réduction stricte et renseignez le motif.')).toBeTruthy()
    fireEvent.click(within(dialogue).getByRole('checkbox', { name: 'ROLE_SBX_LECTEUR' }))
    fireEvent.change(within(dialogue).getByLabelText(/Motif de la réduction/), { target: { value: 'Fin de mission' } })
    fireEvent.click(within(dialogue).getByRole('button', { name: 'Enregistrer la décision' }))
    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull())
  })

  it('restitue les erreurs métier et techniques sans fermer la décision', async () => {
    let refus: Error = new ApiError(409, 'IAM_REVIEW_CLOSED', 'Campagne déjà close', 'c-revue')
    const services = avec('reexamens', { decider: async () => { throw refus } })
    rendrePage(<OngletReexamens />, { chemin: '/console/sandbox?onglet=reexamens', motif: '/console/sandbox', services })
    fireEvent.click((await screen.findAllByRole('button', { name: 'Décider' }))[0])
    const dialogue = await screen.findByRole('dialog')
    fireEvent.click(within(dialogue).getByRole('button', { name: 'Enregistrer la décision' }))
    expect(await screen.findByText('Cette campagne est close.')).toBeTruthy()
    refus = new Error('Refus technique')
    fireEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Enregistrer la décision' }))
    expect(await screen.findByText('Refus technique')).toBeTruthy()
  })

  it('affiche une campagne sans compte et expose son ouverture simulée', async () => {
    const base = simules()
    const services = avec('reexamens', { lire: async (id) => ({ ...(await base.reexamens.lire(id)), acces: [] }) }, base)
    rendrePage(<OngletReexamens />, { chemin: '/console/sandbox?onglet=reexamens', motif: '/console/sandbox', services })
    expect(await screen.findByText('Aucun compte dans cette campagne.')).toBeTruthy()
    expect((await reexamensSimules().ouvrir()).id).toBe('2026-t4')
  })

  it('distingue absence de campagne et indisponibilité de la liste', async () => {
    const vide = rendrePage(<OngletReexamens />, {
      chemin: '/console/sandbox?onglet=reexamens', motif: '/console/sandbox', services: avec('reexamens', { lister: async () => [] }),
    })
    expect(await screen.findByText(/Aucune campagne n’est encore ouverte/)).toBeTruthy()
    vide.unmount()
    rendrePage(<OngletReexamens />, {
      chemin: '/console/sandbox?onglet=reexamens', motif: '/console/sandbox',
      services: avec('reexamens', { lister: async () => { throw new ApiError(503, 'IAM_INDISPONIBLE', 'indisponible', 'c-revue') } }),
    })
    expect(await screen.findByText('Le réexamen des accès est momentanément indisponible.')).toBeTruthy()
  })
})
