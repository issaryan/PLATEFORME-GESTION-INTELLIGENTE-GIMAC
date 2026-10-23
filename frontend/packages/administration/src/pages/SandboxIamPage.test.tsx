import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, screen, waitFor, within } from '@testing-library/react'
import { ApiError } from '@gimac/socle'
import SandboxIamPage from './SandboxIamPage'
import { avec, rendrePage, simules } from '../test/rendu'
import { DELEGATIONS_DEMO, etatEffectif, paginer, sandboxSimulee } from '../services/simule/sandbox'
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

describe('Sandbox GIMAC : listes', () => {
  it('taille de page changée, nouvel essai après une erreur, motif absent', async () => {
    let echec = true
    const base = simules()
    const appels: number[] = []
    page('/console/sandbox', avec('sandbox', {
      perimetres: async (p, t) => {
        appels.push(t)
        if (echec) throw new ApiError(503, 'IAM_INDISPONIBLE', 'MS-IAM indisponible', 'c-2')
        return paginer([{ code: '99009', depuis: '2026-10-21T07:30:00Z', acteurId: 'sa', motif: null }], p, t)
      },
    }, base))
    expect(await screen.findByText('MS-IAM indisponible')).toBeTruthy()
    echec = false
    fireEvent.click(screen.getByRole('button', { name: 'Réessayer' }))
    expect(await screen.findByText('99009')).toBeTruthy()
    expect(screen.getByText('—')).toBeTruthy()
    fireEvent.change(screen.getByLabelText('Taille de page'), { target: { value: '50' } })
    await waitFor(() => expect(appels).toContain(50))
  })
})

describe('Sandbox GIMAC : mandats de partenaire (US-EXI-04)', () => {
  it('US-EXI-04 scénarios 1 à 3 : mandat actif avec ses droits et ses dates, programmé, échu à la date de fin, révoqué', async () => {
    page('/console/sandbox?onglet=mandats')
    const actif = (await screen.findAllByText('Campagnes de certification : exécuter les campagnes'))[0].closest('tr') as HTMLElement
    expect(within(actif).getByText('Active')).toBeTruthy()
    expect(within(actif).getByText('Paul Ebode (Intégrateur Afrique)')).toBeTruthy()
    expect(within(actif).getByText('99002')).toBeTruthy()
    expect(screen.getByText('Programmée')).toBeTruthy()
    expect(screen.getByText('Échue')).toBeTruthy()
    expect(screen.getByText('Révoquée')).toBeTruthy()
    expect(screen.getByText(/l’accès cesse à la date de fin/)).toBeTruthy()
  })

  it('l’échéance suit l’horloge : le mandat actif passe échu à sa date de fin', async () => {
    vi.setSystemTime(new Date('2026-12-31T18:00:00Z'))
    page('/console/sandbox?onglet=mandats')
    expect(await screen.findAllByText('Échue')).toHaveLength(3)
    expect(screen.queryByText('Active')).toBeNull()
  })
})

describe('Sandbox GIMAC : services', () => {
  it('état effectif simulé, comme MS-IAM', () => {
    const d = DELEGATIONS_DEMO[0][0]
    expect(etatEffectif({ ...d, etat: 'DEMANDEE' }, new Date('2026-10-22T08:00:00Z'))).toBe('DEMANDEE')
    expect(etatEffectif({ ...d, etat: 'DEMANDEE' }, new Date('2027-01-01T00:00:00Z'))).toBe('ECHUE')
    expect(etatEffectif({ ...d, etat: 'REFUSEE' })).toBe('REFUSEE')
    expect(etatEffectif(d, new Date('2026-09-01T00:00:00Z'))).toBe('PROGRAMMEE')
  })

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
    await s.delegations('MANDAT', 0, 20)
    await s.delegations(undefined, 2, 5)
    await waitFor(() => expect(spy).toHaveBeenCalledTimes(5))
    expect(spy.mock.calls.map(([u, i]) => `${(i as RequestInit).method} ${new URL(String(u)).pathname}${new URL(String(u)).search}`)).toEqual([
      'GET /bff/console/iam/sandbox/perimetres?page=0&taille=20',
      'GET /bff/console/iam/sandbox/perimetres/99%2F01',
      'GET /bff/console/iam/sandbox/protections?page=1&taille=10',
      'GET /bff/console/iam/sandbox/delegations?type=MANDAT&page=0&taille=20',
      'GET /bff/console/iam/sandbox/delegations?page=2&taille=5',
    ])
    expect((await simules().sandbox.delegations(undefined, 0, 20)).total).toBe(4)
  })
})
