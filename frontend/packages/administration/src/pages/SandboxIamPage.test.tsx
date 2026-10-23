import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, screen, waitFor, within } from '@testing-library/react'
import { ApiError } from '@gimac/socle'
import SandboxIamPage from './SandboxIamPage'
import { avec, rendrePage, simules } from '../test/rendu'
import { paginer, sandboxSimulee } from '../services/simule/sandbox'
import { sandboxHttp } from '../services/http/sandbox'

// D-119, US-EXI-06 : la sandbox dans la console, lectures de MS-IAM (périmètres suspendus, comptes protégés).
beforeEach(() => {
  vi.useFakeTimers({ toFake: ['Date'] })
  vi.setSystemTime(new Date('2026-10-22T08:00:00Z'))
})
afterEach(() => {
  cleanup()
  vi.useRealTimers()
  vi.restoreAllMocks()
})
const page = (chemin = '/console/sandbox', services = simules()) => rendrePage(<SandboxIamPage />, { chemin, motif: '/console/sandbox', permissions: ['IAM_ACCOUNT_READ'], services })

describe('Sandbox GIMAC dans la console', () => {
  it('US-EXI-06 scénario 1 : le périmètre suspendu de 99001 est affiché avec son motif et son auteur', async () => {
    page()
    expect(await screen.findByRole('heading', { name: 'Sandbox GIMAC' })).toBeTruthy()
    const ligne = (await screen.findByText('99001')).closest('tr') as HTMLElement
    expect(within(ligne).getByText('Suspendu')).toBeTruthy()
    expect(within(ligne).getByText('Statut SUSPENDU au registre des participants')).toBeTruthy()
    expect(within(ligne).getByText('service-account-sbx-espace')).toBeTruthy()
    expect(screen.getByText(/sans modifier l’état des comptes/)).toBeTruthy()
  })

  it('US-EXI-06 scénario 3 : onglet des comptes protégés, tenu dans l’adresse', async () => {
    page()
    fireEvent.click(await screen.findByRole('tab', { name: 'Comptes protégés' }))
    expect(await screen.findByText('Awa Ndiaye')).toBeTruthy()
    expect(screen.getAllByText('Dernier administrateur')).toHaveLength(2)
    expect(screen.getByTestId('emplacement').textContent).toBe('/console/sandbox?onglet=protections')
  })

  it('onglet inconnu : périmètres ; listes vides et erreurs rendues', async () => {
    page('/console/sandbox?onglet=x', avec('sandbox', { perimetres: async () => paginer([], 0, 20) }))
    expect(await screen.findByText('Aucun périmètre d’institution n’est suspendu.')).toBeTruthy()
    cleanup()
    page('/console/sandbox?onglet=protections', avec('sandbox', { protections: async () => paginer([], 0, 20) }))
    expect(await screen.findByText('Aucun compte protégé.')).toBeTruthy()
    cleanup()
    page('/console/sandbox', avec('sandbox', { perimetres: async () => { throw new ApiError(503, 'IAM_INDISPONIBLE', 'MS-IAM indisponible', 'c-1') } }))
    expect(await screen.findByText('MS-IAM indisponible')).toBeTruthy()
  })

  it('pagination : taille changée, retour à la première page', async () => {
    const lus: [number, number][] = []
    const base = simules()
    page('/console/sandbox?onglet=protections', avec('sandbox', { protections: async (p, t) => { lus.push([p, t]); return base.sandbox.protections(p, t) } }, base))
    expect(await screen.findByText('Jean Mbarga')).toBeTruthy()
    expect(lus[0]).toEqual([0, 20])
  })
})

describe('Sandbox GIMAC : services', () => {
  it('simulés : pages de 100 au plus, code invalide refusé, état d’un périmètre', async () => {
    const s = simules().sandbox
    expect(paginer([1, 2, 3], -1, 500)).toEqual({ contenu: [1, 2, 3], total: 3, page: 0, taille: 100 })
    expect(paginer([1, 2, 3], 1, 0)).toEqual({ contenu: [2], total: 3, page: 1, taille: 1 })
    expect((await s.perimetre('99001')).suspendu).toBe(true)
    expect(await s.perimetre('99002')).toEqual({ code: '99002', suspendu: false, depuis: null, sessionsRevoquees: 0, rejouee: false })
    await expect(s.perimetre('99 001')).rejects.toMatchObject({ code: 'IAM_SBX_PERIMETRE_INVALIDE' })
    const tri = sandboxSimulee({ attendre: async (v) => (typeof v === 'function' ? (v as () => unknown)() : v) as never, refus: () => new Error('x') as never }, [
      { code: 'A', depuis: '2026-10-01T00:00:00Z', acteurId: 's', motif: null },
      { code: 'B', depuis: '2026-10-02T00:00:00Z', acteurId: 's', motif: null },
    ])
    expect((await tri.perimetres(0, 20)).contenu.map((p) => p.code)).toEqual(['B', 'A'])
  })

  it('HTTP : /bff/console/iam/sandbox, pages à partir de 0', async () => {
    const spy = vi.spyOn(globalThis, 'fetch').mockImplementation(async () => new Response('{}', { status: 200, headers: { 'Content-Type': 'application/json' } }))
    const s = sandboxHttp()
    await s.perimetres(0, 20)
    await s.perimetre('99/01')
    await s.protections(1, 10)
    await waitFor(() => expect(spy).toHaveBeenCalledTimes(3))
    expect(spy.mock.calls.map(([u, i]) => `${(i as RequestInit).method} ${new URL(String(u)).pathname}${new URL(String(u)).search}`)).toEqual([
      'GET /bff/console/iam/sandbox/perimetres?page=0&taille=20',
      'GET /bff/console/iam/sandbox/perimetres/99%2F01',
      'GET /bff/console/iam/sandbox/protections?page=1&taille=10',
    ])
  })
})
