import { afterEach, describe, expect, it, vi } from 'vitest'
import { nouvelleCle } from './hooks'

// Clé d'idempotence hors contexte sécurisé : sans crypto.randomUUID, UUID v4 tiré de getRandomValues ou, à défaut, de Math.random.
describe('nouvelleCle', () => {
  afterEach(() => vi.unstubAllGlobals())

  it('UUID v4 sans randomUUID, avec ou sans getRandomValues', () => {
    const forme = /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/
    vi.stubGlobal('crypto', { getRandomValues: (t: Uint8Array) => t.fill(7) })
    expect(nouvelleCle()).toMatch(forme)
    vi.stubGlobal('crypto', undefined)
    expect(nouvelleCle()).toMatch(forme)
  })
})
