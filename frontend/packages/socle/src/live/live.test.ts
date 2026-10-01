import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { QueryClient } from '@tanstack/react-query'
import { openLiveChannel, type EtatCanal } from './live'

// Canal poussé : connexion par couche et par sujets, invalidation des requêtes du sujet (et du parcours pour
// l'homologation et la certification), reprise depuis le dernier identifiant, reconnexion à délai croissant.
class FauxFlux {
  static ouverts: FauxFlux[] = []
  onopen: (() => void) | null = null
  onmessage: ((m: { data: string }) => void) | null = null
  onerror: (() => void) | null = null
  ferme = false
  url: string
  init?: EventSourceInit
  constructor(url: string, init?: EventSourceInit) {
    this.url = url
    this.init = init
    FauxFlux.ouverts.push(this)
  }
  close() {
    this.ferme = true
  }
}

beforeEach(() => {
  FauxFlux.ouverts = []
  vi.useFakeTimers()
  vi.stubGlobal('EventSource', FauxFlux)
})
afterEach(() => {
  vi.useRealTimers()
  vi.unstubAllGlobals()
})

describe('openLiveChannel', () => {
  it('ouvre le flux de la couche avec ses sujets et les témoins de session ; état connecté', () => {
    const etats: EtatCanal[] = []
    openLiveChannel('des', ['homologation', 'collaboration'], new QueryClient(), (e) => etats.push(e), { baseMs: 1000, maxMs: 8000 })
    const f = FauxFlux.ouverts[0]
    expect(f.url).toBe('/bff/des/live?sujets=homologation%2Ccollaboration')
    expect(f.init).toEqual({ withCredentials: true })
    f.onopen?.()
    expect(etats).toEqual(['connected'])
  })
  it('un événement invalide son sujet, et le parcours pour l’homologation et la certification', () => {
    const qc = new QueryClient()
    const invalider = vi.spyOn(qc, 'invalidateQueries').mockResolvedValue()
    openLiveChannel('des', ['homologation', 'certification', 'collaboration'], qc, () => undefined, { baseMs: 1000, maxMs: 8000 })
    const f = FauxFlux.ouverts[0]
    f.onmessage?.({ data: JSON.stringify({ id: 'e1', type: 'homologation.liaison-updated.v1', subject: 'L1', topic: 'homologation' }) })
    expect(invalider.mock.calls.map((c) => c[0])).toEqual([{ queryKey: ['homologation'] }, { queryKey: ['parcours'] }])
    invalider.mockClear()
    f.onmessage?.({ data: JSON.stringify({ id: 'e2', type: 'collaboration.message-sent.v1', subject: 'C1', topic: 'collaboration' }) })
    expect(invalider.mock.calls.map((c) => c[0])).toEqual([{ queryKey: ['collaboration'] }])
  })
  it('perte du flux : état perdu, reconnexion à délai croissant plafonné, reprise depuis le dernier identifiant', () => {
    const etats: EtatCanal[] = []
    openLiveChannel('portail', ['certification'], new QueryClient(), (e) => etats.push(e), { baseMs: 1000, maxMs: 3000 })
    FauxFlux.ouverts[0].onmessage?.({ data: JSON.stringify({ id: 'e7', type: 't', subject: 's', topic: 'certification' }) })
    FauxFlux.ouverts[0].onerror?.()
    expect(etats).toEqual(['lost'])
    expect(FauxFlux.ouverts[0].ferme).toBe(true)
    vi.advanceTimersByTime(999)
    expect(FauxFlux.ouverts).toHaveLength(1)
    vi.advanceTimersByTime(1)
    expect(FauxFlux.ouverts[1].url).toBe('/bff/portail/live?sujets=certification&depuis=e7')
    FauxFlux.ouverts[1].onerror?.()
    vi.advanceTimersByTime(2000)
    expect(FauxFlux.ouverts).toHaveLength(3)
    FauxFlux.ouverts[2].onerror?.()
    // 4 s calculées, plafonnées à 3 s.
    vi.advanceTimersByTime(3000)
    expect(FauxFlux.ouverts).toHaveLength(4)
    // Une connexion rétablie remet le délai à sa base.
    FauxFlux.ouverts[3].onopen?.()
    FauxFlux.ouverts[3].onerror?.()
    vi.advanceTimersByTime(1000)
    expect(FauxFlux.ouverts).toHaveLength(5)
  })
  it('fermeture : plus de reconnexion', () => {
    const fermer = openLiveChannel('console', ['homologation'], new QueryClient(), () => undefined, { baseMs: 1000, maxMs: 8000 })
    FauxFlux.ouverts[0].onerror?.()
    fermer()
    vi.advanceTimersByTime(10_000)
    expect(FauxFlux.ouverts).toHaveLength(1)
    const fermer2 = openLiveChannel('console', ['homologation'], new QueryClient(), () => undefined, { baseMs: 1000, maxMs: 8000 })
    fermer2()
    expect(FauxFlux.ouverts[1].ferme).toBe(true)
    FauxFlux.ouverts[1].onerror?.()
    vi.advanceTimersByTime(10_000)
    expect(FauxFlux.ouverts).toHaveLength(2)
  })
})
