import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, screen, waitFor } from '@testing-library/react'
import { rendrePage, sessionTest } from '../../../../test/rendu'
import { servicesSimules } from '../../../../services/simule'
import type { OperationParcours } from '../../../../modele/parcours'
import { DetailPreuve, typeMimeDe } from './DetailPreuve'

afterEach(cleanup)

const op = { id: 'op-3', rang: '3', libelle: 'Arrêt du périmètre des services', etat: 'FAITE' } as unknown as OperationParcours
const livrable = { code: 'L1', libelle: 'Fiche de modalités du périmètre', nature: 'LIVRABLE', deposant: null, typesPieces: ['application/pdf'], obligatoire: true, peutDeposer: false, consigne: 'PDF daté et signé' } as never

describe('DetailPreuve (demande du 19/10/2026)', () => {
  it('déduit le type de la pièce de son nom', () => {
    expect(typeMimeDe('preuve.pdf')).toBe('application/pdf')
    expect(typeMimeDe('capture.PNG')).toBe('image/png')
    expect(typeMimeDe('sans-extension')).toBe('application/octet-stream')
  })
  it('montre de quoi il s’agit, le dépôt, et ouvre la pièce dans la visionneuse', async () => {
    const services = servicesSimules()
    const telecharger = vi.spyOn(services.parcours, 'telechargerPiece').mockResolvedValue({ blob: new Blob(['x'], { type: 'text/plain' }), empreinte: null })
    const fermer = vi.fn()
    rendrePage(<DetailPreuve preuve={{ op, livrable, etat: 'VALIDEE', depot: { documentRef: 'DOC-1', nom: 'piece.txt', deposeLe: '2026-08-03', deposePar: 'Personne fictive' } }} onFermer={fermer} />, { chemin: '/des/parcours/L', motif: '/des/parcours/:id', services, session: sessionTest([]) })
    expect(screen.getByText('PDF daté et signé')).toBeTruthy()
    expect(screen.getByText('application/pdf')).toBeTruthy()
    expect(screen.getByText('piece.txt')).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Voir la pièce' }))
    await waitFor(() => expect(telecharger).toHaveBeenCalledWith('op-3', 'DOC-1'))
    fireEvent.click(screen.getAllByRole('button', { name: 'Fermer' })[0])
  })
  it('motive un « sans objet » et signale l’absence de dépôt', () => {
    rendrePage(<DetailPreuve preuve={{ op, livrable, service: 'W2W', etat: 'SANS_OBJET', depot: { sansObjet: true, motif: 'participant non porté', deposeLe: '2026-08-03', deposePar: 'Personne fictive' } }} onFermer={() => {}} />, { chemin: '/des/parcours/L', motif: '/des/parcours/:id', services: servicesSimules(), session: sessionTest([]) })
    expect(screen.getByText('participant non porté')).toBeTruthy()
    expect(screen.queryByRole('button', { name: 'Voir la pièce' })).toBeNull()
    cleanup()
    rendrePage(<DetailPreuve preuve={{ op, livrable, etat: 'ATTENDUE' }} onFermer={() => {}} />, { chemin: '/des/parcours/L', motif: '/des/parcours/:id', services: servicesSimules(), session: sessionTest([]) })
    expect(screen.getByText('Aucune pièce déposée pour le moment.')).toBeTruthy()
  })
})
