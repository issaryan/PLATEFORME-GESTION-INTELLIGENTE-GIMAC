import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, screen, waitFor, within } from '@testing-library/react'
import { rendrePage } from '../../test/rendu'
import { servicesSimules } from '../../services/simule'
import ReferentielsOperationsPage from './ReferentielsOperationsPage'

// DES-12 : actions sur les versions (création d'un brouillon, dérivation, contrôle, publication,
// simulation de migration) ; le service décide, l'écran affiche.
afterEach(() => {
  cleanup()
  vi.restoreAllMocks()
})
const MOTIF = '/des/referentiels/operations'
const ouvrir = (suffixe = '', services = servicesSimules({ latenceMs: 0 })) => ({ ...rendrePage(<ReferentielsOperationsPage />, { chemin: `${MOTIF}${suffixe}`, motif: MOTIF, services }), services })

describe('DES-12 : actions sur les versions', () => {
  it('création d’un brouillon : champs requis, puis version créée et sélectionnée', async () => {
    const { container, services } = ouvrir()
    const creer = vi.spyOn(services.homologation, 'creerBrouillon')
    fireEvent.click(await screen.findByRole('button', { name: 'Créer un brouillon' }))
    const f = await waitFor(() => { const x = container.querySelector('form'); if (!x) throw new Error('absent'); return x })
    fireEvent.submit(f)
    expect((await screen.findAllByText(/obligatoire/i)).length).toBeGreaterThan(0)
    expect(creer).not.toHaveBeenCalled()
    fireEvent.change(container.querySelector('select[name="typeProjet"]')!, { target: { value: 'PAIEMENT_MOBILE' } })
    fireEvent.change(container.querySelector('input[name="libelle"]')!, { target: { value: 'Paiement mobile, brouillon 3' } })
    fireEvent.submit(f)
    await waitFor(() => expect(creer).toHaveBeenCalledWith({ typeProjet: 'PAIEMENT_MOBILE', libelle: 'Paiement mobile, brouillon 3' }))
    expect(await screen.findByRole('heading', { name: 'Paiement mobile, brouillon 3' })).toBeTruthy()
  })
  it('création annulée : retour à l’état vide', async () => {
    ouvrir()
    fireEvent.click(await screen.findByRole('button', { name: 'Créer un brouillon' }))
    fireEvent.click(await screen.findByRole('button', { name: 'Annuler' }))
    expect(await screen.findByText('Aucune version sélectionnée')).toBeTruthy()
  })
  it('dérivation d’un brouillon depuis une version publiée', async () => {
    const { services } = ouvrir('?version=REF-CI-3')
    const deriver = vi.spyOn(services.homologation, 'deriverBrouillon')
    fireEvent.click(await screen.findByRole('button', { name: 'Dériver un brouillon' }))
    const d = await screen.findByRole('alertdialog')
    fireEvent.click(within(d).getByRole('button', { name: 'Dériver un brouillon' }))
    await waitFor(() => expect(deriver).toHaveBeenCalledWith('REF-CI-3'))
  })
  it('contrôle, simulation de migration et publication d’un brouillon', async () => {
    const { services } = ouvrir('?version=REF-CI-4')
    const publier = vi.spyOn(services.homologation, 'publierReferentiel')
    fireEvent.click(await screen.findByRole('button', { name: 'Contrôler le graphe' }))
    expect(await screen.findByText('Attributs manquants par opération')).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Publier la version' }))
    const d = await screen.findByRole('alertdialog')
    fireEvent.click(within(d).getByRole('button', { name: 'Publier la version' }))
    await waitFor(() => expect(publier).toHaveBeenCalledWith('REF-CI-4'))
  })
  it('choix d’une version dans l’arbre', async () => {
    ouvrir()
    const arbre = await screen.findByRole('tree')
    fireEvent.click(within(arbre).getByText('Paiement mobile'))
    fireEvent.click(within(arbre).getAllByText(/Version 2/i).at(-1)!)
    expect(await screen.findByRole('heading', { name: 'Paiement mobile, version 2' })).toBeTruthy()
  })
})
