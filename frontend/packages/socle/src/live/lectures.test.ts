import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { QueryClient } from '@tanstack/react-query'
import { openLiveChannel } from './live'
import { ACCUSE_LECTURE, accuseDe, surAccuseLecture, type AccuseLecture } from './lectures'

// Accusés de lecture (D-88, docs/lot1/accuses-lecture.md §4) : l'événement poussé est remis aux fils abonnés,
// sans invalider la messagerie ni le compteur de non-lus, et garde l'identifiant de reprise.
class FauxFlux {
  static ouverts: FauxFlux[] = []
  onopen: (() => void) | null = null
  onmessage: ((m: { data: string }) => void) | null = null
  onerror: (() => void) | null = null
  url: string
  constructor(url: string) {
    this.url = url
    FauxFlux.ouverts.push(this)
  }
  close() {}
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

describe('accusés de lecture poussés', () => {
  it('accuseDe : seulement le type collaboration.conversation-read.v1 complet', () => {
    expect(accuseDe({ id: '1', type: ACCUSE_LECTURE, subject: 'c-1', topic: 'collaboration', acteur: 'u-2', luJusquA: '2026-10-06T10:00:00Z' }))
      .toEqual({ conversationId: 'c-1', lecteurId: 'u-2', luJusquA: '2026-10-06T10:00:00Z' })
    expect(accuseDe({ id: '1', type: ACCUSE_LECTURE, subject: 'c-1', topic: 'collaboration', acteur: 'u-2' })).toBeNull()
    expect(accuseDe({ id: '1', type: 'collaboration.message-sent.v1', subject: 'c-1', topic: 'collaboration', acteur: 'u-2', luJusquA: 'x' })).toBeNull()
  })

  it('remis aux abonnés, sans aucune invalidation ; reprise depuis son identifiant ; désabonnement', () => {
    const qc = new QueryClient()
    const invalider = vi.spyOn(qc, 'invalidateQueries')
    const recus: AccuseLecture[] = []
    const fin = surAccuseLecture((a) => recus.push(a))
    openLiveChannel('des', ['collaboration'], qc, () => undefined, { baseMs: 1000, maxMs: 8000 })
    const f = FauxFlux.ouverts[0]
    f.onmessage?.({ data: JSON.stringify({ id: 'e-9', type: ACCUSE_LECTURE, subject: 'c-1', topic: 'collaboration', acteur: 'u-2', luJusquA: '2026-10-06T10:00:00Z' }) })
    // Enveloppe incomplète : ignorée, sans invalidation non plus.
    f.onmessage?.({ data: JSON.stringify({ id: 'e-10', type: ACCUSE_LECTURE, subject: 'c-1', topic: 'collaboration' }) })
    expect(recus).toEqual([{ conversationId: 'c-1', lecteurId: 'u-2', luJusquA: '2026-10-06T10:00:00Z' }])
    expect(invalider).not.toHaveBeenCalled()
    fin()
    f.onmessage?.({ data: JSON.stringify({ id: 'e-11', type: ACCUSE_LECTURE, subject: 'c-1', topic: 'collaboration', acteur: 'u-3', luJusquA: '2026-10-06T11:00:00Z' }) })
    expect(recus).toHaveLength(1)
    f.onerror?.()
    vi.advanceTimersByTime(1000)
    expect(FauxFlux.ouverts[1].url).toContain('depuis=e-11')
  })
})
