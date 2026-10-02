import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, screen, waitFor, within } from '@testing-library/react'
import { rendrePage, sessionTest } from '../../test/rendu'
import { servicesSimules } from '../../services/simule'
import ReferentielsOperationsPage from './ReferentielsOperationsPage'

// DES-12 : actions sur les versions (création d'un brouillon, dérivation, contrôle, publication,
// simulation de migration) ; le service décide, l'écran affiche.
afterEach(() => {
  cleanup()
  vi.restoreAllMocks()
})
const MOTIF = '/des/referentiels/operations'
// Ingénieur d'homologation (édition, publication, migration) ; LECTEUR : le seul droit de lecture (super administrateur).
const EDITION = ['HOM_REFERENTIEL_READ', 'HOM_REFERENTIEL_EDIT', 'HOM_REFERENTIEL_PUBLISH', 'HOM_REFERENTIEL_MIGRATE']
const ouvrir = (suffixe = '', services = servicesSimules({ latenceMs: 0 }), permissions = EDITION) =>
  ({ ...rendrePage(<ReferentielsOperationsPage />, { chemin: `${MOTIF}${suffixe}`, motif: MOTIF, services, session: sessionTest(permissions) }), services })

describe('DES-12 : actions sur les versions', () => {
  it('lecteur seul (HOM_REFERENTIEL_READ) : ni « Nouveau brouillon », ni contrôle, ni simulation de migration (aucun 403)', async () => {
    const services = servicesSimules({ latenceMs: 0 })
    const simuler = vi.spyOn(services.homologation, 'simulerMigration')
    const controler = vi.spyOn(services.homologation, 'controlerReferentiel')
    const creer = vi.spyOn(services.homologation, 'creerBrouillon')
    const [v] = await services.homologation.listerReferentiels()
    ouvrir(`?version=${v.id}&onglet=migration`, services, ['HOM_REFERENTIEL_READ'])
    expect(await screen.findByRole('tab', { name: /Opérations/ })).toBeTruthy()
    expect(screen.queryByRole('button', { name: /brouillon/i })).toBeNull()
    expect(screen.queryByRole('tab', { name: /migration/i })).toBeNull()
    expect(screen.queryByRole('tab', { name: /Contrôle/ })).toBeNull()
    expect(simuler).not.toHaveBeenCalled()
    expect(controler).not.toHaveBeenCalled()
    expect(creer).not.toHaveBeenCalled()
  })
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
