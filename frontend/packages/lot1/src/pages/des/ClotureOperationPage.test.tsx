import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, screen, waitFor, within } from '@testing-library/react'
import { ApiError } from '@gimac/socle'
import { rendrePage } from '../../test/rendu'
import { servicesSimules } from '../../services/simule'
import ClotureOperationPage from './ClotureOperationPage'

afterEach(cleanup)
const MOTIF = '/des/liaisons/:id/operations/:op/cloture'
const chemin = (etape = 'preuve', op = 'OPI-017-3') => `/des/liaisons/LIA-2026-017/operations/${op}/cloture?etape=${etape}`

describe('DES-10 Clore une opération', () => {
  it('DES-10 : chargement puis nominal (étape preuve)', async () => {
    const { container } = rendrePage(<ClotureOperationPage />, { chemin: chemin(), motif: MOTIF })
    expect(container.querySelector('[aria-busy="true"]')).not.toBeNull()
    expect(await screen.findByRole('heading', { level: 1, name: 'Clôture de l\'opération OP-E04' })).toBeTruthy()
    expect(screen.getByText('Dernière version déposée')).toBeTruthy()
  })

  it('DES-10 : erreur avec identifiant de corrélation et réessai ; opération introuvable sans réessai (CER-18)', async () => {
    const s = servicesSimules({ latenceMs: 0 })
    s.homologation.elementsDeCloture = vi.fn().mockRejectedValue(new ApiError(404, 'HOM_OPERATION_NOT_FOUND', 'Opération introuvable.', 'cid-404'))
    const { unmount } = rendrePage(<ClotureOperationPage />, { chemin: chemin(), motif: MOTIF, services: s })
    const alerte = await screen.findByRole('alert')
    expect(alerte.textContent).toContain('cid-404')
    // CER-18 : un refus définitif (introuvable) ne propose pas de réessayer.
    expect(screen.queryByRole('button', { name: 'Réessayer' })).toBeNull()
    unmount()
    s.homologation.elementsDeCloture = vi.fn().mockRejectedValue(new ApiError(503, 'HOM_INDISPONIBLE', 'Service indisponible.', 'cid-503'))
    rendrePage(<ClotureOperationPage />, { chemin: chemin(), motif: MOTIF, services: s })
    expect((await screen.findByRole('alert')).textContent).toContain('cid-503')
    expect(screen.getByRole('button', { name: 'Réessayer' })).toBeTruthy()
  })

  it('DES-10 : quatre éléments restitués tels que le service les renvoie', async () => {
    rendrePage(<ClotureOperationPage />, { chemin: chemin('elements'), motif: MOTIF })
    expect(await screen.findByText('Jeton d\'horodatage en attente de l\'autorité.')).toBeTruthy()
    expect(screen.getByText('Aucune validation par un tiers distinct.')).toBeTruthy()
  })

  it('DES-10 : restriction EST_RESPONSABLE affichée, bouton de validation absent', async () => {
    const s = servicesSimules({ latenceMs: 0 })
    const lire = s.homologation.elementsDeCloture
    s.homologation.elementsDeCloture = async (id) => ({ ...(await lire(id)), restrictionValidation: 'EST_RESPONSABLE', actionsPermises: ['DEPOSER_PREUVE'] })
    rendrePage(<ClotureOperationPage />, { chemin: chemin('signature'), motif: MOTIF, services: s })
    expect(await screen.findByText(/Vous êtes responsable de cette opération/)).toBeTruthy()
    expect(screen.queryByRole('button', { name: 'Valider et clore l\'opération' })).toBeNull()
  })

  it('DES-10 : l\'annulation de la clé de sécurité n\'envoie aucune validation', async () => {
    const s = servicesSimules({ latenceMs: 0, echecs: true })
    const valider = vi.spyOn(s.homologation, 'validerOperation')
    rendrePage(<ClotureOperationPage />, { chemin: chemin('signature'), motif: MOTIF, services: s })
    fireEvent.click(await screen.findByRole('button', { name: 'Valider et clore l\'opération' }))
    const dialogue = await screen.findByRole('dialog')
    fireEvent.change(within(dialogue).getByLabelText(/Motif/), { target: { value: 'Preuve relue et conforme au référentiel' } })
    const cle = within(dialogue).getAllByRole('button').find((b) => /clé/i.test(b.textContent ?? '') && !(b as HTMLButtonElement).disabled)
    expect(cle).toBeTruthy()
    fireEvent.click(cle!)
    expect(await within(dialogue).findByText(/La signature a été annulée/)).toBeTruthy()
    expect(valider).not.toHaveBeenCalled()
  })

  it.each(['en', 'es'] as const)('DES-10 : rendu en %s sans clé brute', async (langue) => {
    const { container } = rendrePage(<ClotureOperationPage />, { chemin: chemin('signature'), motif: MOTIF, langue })
    await screen.findByRole('heading', { level: 1 })
    await waitFor(() => expect(container.textContent).not.toMatch(/\b(cloture|operation|commun)\.[a-zA-Z.]+/))
    expect(container.textContent).not.toContain('Clôture de l\'opération')
  })
})
