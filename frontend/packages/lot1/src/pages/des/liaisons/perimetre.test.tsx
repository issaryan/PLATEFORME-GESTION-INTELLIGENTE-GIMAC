import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, screen, waitFor, within } from '@testing-library/react'
import { ApiError } from '@gimac/socle'
import LiaisonPage from '../LiaisonPage'
import { rendrePage } from '../../../test/rendu'
import { servicesSimules } from '../../../services/simule'
import type { Services } from '../../../services/ports'

// D-133 : onglet « Périmètre » de la liaison, modification motivée par l'ayant droit, historique tracé, lecture seule
// pour les autres et refus du service affiché.
afterEach(() => {
  cleanup()
  vi.restoreAllMocks()
})
const page = (services: Services) =>
  rendrePage(<LiaisonPage />, { chemin: '/des/liaisons/LIA-2026-030?onglet=perimetre', motif: '/des/liaisons/:id', services })

describe('D-133 : périmètre du projet de certification', () => {
  it('le gestionnaire modifie le périmètre avec un motif ; l’historique le trace', async () => {
    page(servicesSimules({ latenceMs: 0 }))
    expect(await screen.findByText('Périmètre du projet de certification')).toBeTruthy()
    fireEvent.click(await screen.findByRole('button', { name: 'Modifier le périmètre' }))
    const d = await screen.findByRole('dialog')
    // Groupes par interface et par famille, nom usuel en clair, code en infobulle seulement.
    expect(within(d).getByText('Mobile, protocole propriétaire')).toBeTruthy()
    expect(within(d).getByText('Transferts et paiement marchand (2 sur 2)')).toBeTruthy()
    expect(within(d).getByText('Paiement de facture').closest('label')?.getAttribute('title')).toBe('Code technique : FACTURE')
    expect(within(d).queryByText('FACTURE')).toBeNull()
    expect(within(d).getByText('Aucun changement pour le moment.')).toBeTruthy()
    expect((within(d).getByRole('button', { name: 'Enregistrer le périmètre' }) as HTMLButtonElement).disabled).toBe(true)
    // Recherche, case « tout le groupe » et compteur.
    fireEvent.change(within(d).getByRole('searchbox'), { target: { value: 'facture' } })
    expect(within(d).queryByText('Transfert')).toBeNull()
    fireEvent.change(within(d).getByRole('searchbox'), { target: { value: '' } })
    fireEvent.click(within(d).getByLabelText('Tout le groupe « Autres services »'))
    expect(within(d).getByText('4 service(s) choisi(s) sur 4')).toBeTruthy()
    fireEvent.click(within(d).getByLabelText('Tout le groupe « Autres services »'))
    fireEvent.click(within(d).getByLabelText('Transfert'))
    expect(within(d).getByText('Retirés : Transfert')).toBeTruthy()
    fireEvent.click(within(d).getByLabelText('Paiement de facture'))
    expect(within(d).getByText('Ajoutés : Paiement de facture')).toBeTruthy()
    fireEvent.click(within(d).getByLabelText('Paiement de facture'))
    fireEvent.click(within(d).getByRole('button', { name: 'Enregistrer le périmètre' }))
    expect(await within(d).findByText('Indiquez le motif de la modification.')).toBeTruthy()
    fireEvent.change(within(d).getByLabelText(/Motif/), { target: { value: 'Lot 1 limité au paiement' } })
    fireEvent.click(within(d).getByRole('button', { name: 'Enregistrer le périmètre' }))
    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull())
    expect(await screen.findByText('Lot 1 limité au paiement')).toBeTruthy()
    expect(screen.getByText('Transfert (hors périmètre)')).toBeTruthy()
  })
  it('lecture seule sans droit, refus du service affiché, fermeture sans enregistrer', async () => {
    page(servicesSimules({ latenceMs: 0, utilisateur: { id: 'U-SNA', nom: 'Agent SNA', profils: ['ROLE_SNA'] } }))
    expect(await screen.findByText(/Seuls le gestionnaire du projet/)).toBeTruthy()
    expect(screen.queryByRole('button', { name: 'Modifier le périmètre' })).toBeNull()
    cleanup()
    const s = servicesSimules({ latenceMs: 0 })
    const modifier = vi.fn(async () => {
      throw new ApiError(403, 'HOM_PERIMETRE_GESTIONNAIRE_SEUL', 'refus', 'c-1')
    })
    page({ ...s, homologation: { ...s.homologation, modifierPerimetre: modifier } })
    fireEvent.click(await screen.findByRole('button', { name: 'Modifier le périmètre' }))
    let d = await screen.findByRole('dialog')
    fireEvent.click(within(d).getByLabelText('Transfert'))
    fireEvent.change(within(d).getByLabelText(/Motif/), { target: { value: 'Essai' } })
    fireEvent.click(within(d).getByRole('button', { name: 'Enregistrer le périmètre' }))
    expect(await within(d).findByText(/modifient le périmètre/)).toBeTruthy()
    fireEvent.click(within(d).getByRole('button', { name: 'Annuler' }))
    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull())
    fireEvent.click(await screen.findByRole('button', { name: 'Modifier le périmètre' }))
    d = await screen.findByRole('dialog')
    expect(d).toBeTruthy()
  })
})
