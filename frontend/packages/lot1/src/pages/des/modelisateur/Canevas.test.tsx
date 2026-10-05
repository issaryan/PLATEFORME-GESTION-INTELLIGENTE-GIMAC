import { afterEach, describe, expect, it, vi } from 'vitest'
import { act, cleanup, fireEvent, screen, waitFor } from '@testing-library/react'
import type { ReactNode } from 'react'
import { rendrePage, sessionTest } from '../../../test/rendu'
import { servicesSimules } from '../../../services/simule'
import ReferentielsOperationsPage from '../ReferentielsOperationsPage'
import { DELAI_POSITIONS } from './Canevas'

// Gestes du canevas (D-77, §6) : la bibliothèque est remplacée par un relais qui expose les rappels que
// @xyflow/react déclenche au glisser (onConnect, onEdgesDelete, onNodeDragStop, onBeforeDelete), pour vérifier
// ce que le modélisateur en fait : PUT /arcs (présent ou retiré), refus de cycle restitué, PUT /positions regroupé.
type Rappels = {
  nodes: { id: string; type?: string; position: { x: number; y: number } }[]
  edges: { id: string; source: string; target: string }[]
  onConnect: (c: { source: string; target: string; sourceHandle: null; targetHandle: null }) => void
  isValidConnection: (c: { source: string; target: string }) => boolean
  onEdgesDelete: (e: { id: string; source: string; target: string }[]) => void
  onBeforeDelete: (x: { nodes: unknown[]; edges: unknown[] }) => Promise<{ nodes: unknown[]; edges: unknown[] }>
  onNodeDragStop: (e: unknown, n: unknown, l: { id: string; type?: string; position: { x: number; y: number } }[]) => void
  onNodeDoubleClick: (e: unknown, n: { id: string; type?: string }) => void
  children?: ReactNode
}
const relais: { p?: Rappels } = {}
vi.mock('@xyflow/react', async (original) => {
  const reel = await original<typeof import('@xyflow/react')>()
  return {
    ...reel,
    ReactFlow: (p: Rappels) => {
      relais.p = p
      return <div data-testid="relais">{p.nodes.filter((n) => n.type === 'operation').length} cartes, {p.edges.length} arcs</div>
    },
  }
})

afterEach(() => {
  cleanup()
  vi.restoreAllMocks()
  vi.useRealTimers()
})
const MOTIF = '/des/referentiels/operations'
const EDITION = ['HOM_REFERENTIEL_READ', 'HOM_REFERENTIEL_EDIT', 'HOM_REFERENTIEL_PUBLISH']
const ouvrir = (version = 'REF-CI-4') => {
  const services = servicesSimules({ latenceMs: 0 })
  rendrePage(<ReferentielsOperationsPage />, { chemin: `${MOTIF}?version=${version}`, motif: MOTIF, services, session: sessionTest(EDITION) })
  return services
}
const rappels = () => relais.p as Rappels

describe('DES-12 : gestes du canevas', () => {
  it('tirer d\'une carte à l\'autre crée l\'arc (PUT /arcs present=true) ; supprimer un arc le retire', async () => {
    const services = ouvrir()
    const arc = vi.spyOn(services.homologation, 'arcReferentiel')
    expect((await screen.findByTestId('relais')).textContent).toMatch(/^4\d cartes/)
    expect(rappels().isValidConnection({ source: 'C1', target: 'C1' })).toBe(false)
    expect(rappels().isValidConnection({ source: 'C1', target: 'C2' })).toBe(true)
    act(() => rappels().onConnect({ source: 'C1', target: 'C11', sourceHandle: null, targetHandle: null }))
    await waitFor(() => expect(arc).toHaveBeenCalledWith('REF-CI-4', { de: 'C1', vers: 'C11', present: true }))
    await waitFor(() => expect(rappels().edges.some((e) => e.id === 'C1>C11')).toBe(true))
    // Suppr sur une carte : seules les arêtes sélectionnées partent, jamais une opération.
    expect(await rappels().onBeforeDelete({ nodes: [{ id: '3' }], edges: [{ id: 'C1>C11' }] })).toEqual({ nodes: [], edges: [{ id: 'C1>C11' }] })
    act(() => rappels().onEdgesDelete([{ id: 'C1>C11', source: 'C1', target: 'C11' }]))
    await waitFor(() => expect(arc).toHaveBeenCalledWith('REF-CI-4', { de: 'C1', vers: 'C11', present: false }))
    await waitFor(() => expect(rappels().edges.some((e) => e.id === 'C1>C11')).toBe(false))
  })

  it('un arc qui fermerait un cycle est refusé par le service et expliqué', async () => {
    ouvrir()
    await screen.findByTestId('relais')
    act(() => rappels().onConnect({ source: '35', target: '3', sourceHandle: null, targetHandle: null }))
    const alerte = await screen.findByText('Cet arc fermerait un cycle : une opération dépendrait d\'elle-même.')
    expect(alerte).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Fermer' }))
    await waitFor(() => expect(screen.queryByText(/fermerait un cycle/)).toBeNull())
  })

  it('déplacements regroupés : un seul PUT /positions après le délai', async () => {
    const services = ouvrir()
    const placer = vi.spyOn(services.homologation, 'placerOperations')
    await screen.findByTestId('relais')
    vi.useFakeTimers()
    act(() => {
      rappels().onNodeDragStop(null, null, [{ id: 'C1', type: 'operation', position: { x: 10.4, y: 20.6 } }])
      rappels().onNodeDragStop(null, null, [{ id: 'C2', type: 'operation', position: { x: 300, y: 40 } }, { id: 'etape-TESTS', type: 'couloir', position: { x: 0, y: 0 } }])
    })
    expect(placer).not.toHaveBeenCalled()
    act(() => vi.advanceTimersByTime(DELAI_POSITIONS + 10))
    vi.useRealTimers()
    await waitFor(() => expect(placer).toHaveBeenCalledTimes(1))
    expect(placer).toHaveBeenCalledWith('REF-CI-4', [{ code: 'C1', x: 10, y: 21 }, { code: 'C2', x: 300, y: 40 }])
  })

  it('double-clic sur une carte : modale ; sur un couloir : rien', async () => {
    ouvrir()
    await screen.findByTestId('relais')
    act(() => rappels().onNodeDoubleClick(null, { id: 'etape-TESTS', type: 'couloir' }))
    expect(screen.queryByRole('dialog')).toBeNull()
    act(() => rappels().onNodeDoubleClick(null, { id: 'C2', type: 'operation' }))
    expect(await screen.findByRole('dialog', { name: 'Opération C2' })).toBeTruthy()
  })
})
