import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, screen, waitFor } from '@testing-library/react'
import { ApiError } from '@gimac/socle'
import { rendrePage } from '../../test/rendu'
import { servicesSimules } from '../../services/simule'
import ReferentielsOperationsPage from './ReferentielsOperationsPage'

afterEach(cleanup)
const MOTIF = '/des/referentiels/operations'

describe('DES-12 Référentiels d\'opérations', () => {
  it('DES-12 : chargement, arbre puis état vide tant qu\'aucune version n\'est choisie', async () => {
    const { container } = rendrePage(<ReferentielsOperationsPage />, { chemin: MOTIF, motif: MOTIF })
    expect(container.querySelector('[aria-busy="true"]')).not.toBeNull()
    expect(await screen.findByRole('tree')).toBeTruthy()
    expect(screen.getByText('Aucune version sélectionnée')).toBeTruthy()
  })

  it('DES-12 : liste vide et erreur avec identifiant de corrélation', async () => {
    const vide = servicesSimules({ latenceMs: 0 })
    vide.homologation.listerReferentiels = async () => []
    rendrePage(<ReferentielsOperationsPage />, { chemin: MOTIF, motif: MOTIF, services: vide })
    expect(await screen.findByText('Aucun référentiel')).toBeTruthy()
    cleanup()
    const ko = servicesSimules({ latenceMs: 0 })
    ko.homologation.listerReferentiels = vi.fn().mockRejectedValue(new ApiError(503, 'HTTP_503', 'Service indisponible.', 'cid-503'))
    rendrePage(<ReferentielsOperationsPage />, { chemin: MOTIF, motif: MOTIF, services: ko })
    expect((await screen.findByRole('alert')).textContent).toContain('cid-503')
  })

  it('DES-12 : publication absente sur une version publiée, présente sur un brouillon', async () => {
    rendrePage(<ReferentielsOperationsPage />, { chemin: `${MOTIF}?version=REF-CI-3`, motif: MOTIF })
    await screen.findByRole('heading', { name: 'Carte interbancaire, version 3' })
    expect(screen.queryByRole('button', { name: 'Publier la version' })).toBeNull()
    cleanup()
    rendrePage(<ReferentielsOperationsPage />, { chemin: `${MOTIF}?version=REF-CI-4`, motif: MOTIF })
    expect(await screen.findByRole('button', { name: 'Publier la version' })).toBeTruthy()
  })

  it('DES-12 : un cycle de trois opérations est restitué dans l\'ordre du service', async () => {
    const s = servicesSimules({ latenceMs: 0 })
    s.homologation.controlerReferentiel = async () => ({ acyclique: false, ordreTopologique: [], cycle: ['OP-A', 'OP-B', 'OP-C'], incoherences: [], manquants: [] })
    rendrePage(<ReferentielsOperationsPage />, { chemin: `${MOTIF}?version=REF-CI-4&onglet=controle`, motif: MOTIF, services: s })
    expect((await screen.findByTestId('cycle')).textContent).toBe('Cycle détecté : OP-A → OP-B → OP-C → OP-A')
  })

  it('DES-12 : la comparaison montre les opérations retirées et ajoutées', async () => {
    rendrePage(<ReferentielsOperationsPage />, { chemin: `${MOTIF}?version=REF-CI-4&onglet=comparaison`, motif: MOTIF })
    await waitFor(() => expect(document.querySelector('.gm-diff-del')).not.toBeNull())
    expect(document.querySelector('.gm-diff-add')).not.toBeNull()
  })

  it('DES-12 : la simulation exclut les liaisons au circuit de quitus ouvert', async () => {
    rendrePage(<ReferentielsOperationsPage />, { chemin: `${MOTIF}?version=REF-CI-4&onglet=migration`, motif: MOTIF })
    expect(await screen.findByText('Exclue : circuit de quitus ouvert')).toBeTruthy()
    expect(screen.queryByLabelText('LIA-2025-102')).toBeNull()
    fireEvent.click(screen.getByLabelText('LIA-2026-017'))
    expect(screen.getByRole('button', { name: 'Migrer les liaisons retenues (1)' })).toBeTruthy()
  })

  it.each(['en', 'es'] as const)('DES-12 : rendu en %s sans clé brute', async (langue) => {
    const { container } = rendrePage(<ReferentielsOperationsPage />, { chemin: `${MOTIF}?version=REF-CI-4&vue=liste`, motif: MOTIF, langue })
    await screen.findByRole('tree')
    await screen.findAllByRole('table')
    expect(container.textContent).not.toMatch(/\b(modelisateur|parcours)\.[a-zA-Z.]+/)
    expect(container.textContent).not.toMatch(/\b(referentiel|segment|partie|typeProjet|commun)\.[a-zA-Z.]+/)
    expect(container.textContent).not.toContain('Référentiels d\'opérations')
  })
})
