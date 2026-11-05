import { describe, expect, it } from 'vitest'
import { nouvelleCle } from './hooks'

const V4 = /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/
describe("nouvelleCle (clé d'idempotence)", () => {
  it('contexte sécurisé : randomUUID du navigateur', () => {
    expect(nouvelleCle()).toMatch(V4)
    expect(nouvelleCle()).not.toBe(nouvelleCle())
  })
  it('http sans contexte sécurisé (randomUUID absent) : clé v4 tirée par getRandomValues, jamais deux fois la même', () => {
    const original = Object.getOwnPropertyDescriptor(globalThis, 'crypto')
    const reel = globalThis.crypto
    Object.defineProperty(globalThis, 'crypto', { value: { getRandomValues: (t: Uint8Array<ArrayBuffer>) => reel.getRandomValues(t) }, configurable: true })
    try {
      const a = nouvelleCle()
      expect(a).toMatch(V4)
      expect(nouvelleCle()).not.toBe(a)
    } finally {
      if (original) Object.defineProperty(globalThis, 'crypto', original)
    }
  it('sans générateur cryptographique : erreur explicite, aucune clé prévisible', () => {
    Object.defineProperty(globalThis, 'crypto', { value: {}, configurable: true })
      expect(() => nouvelleCle()).toThrow(/getRandomValues indisponible/)
import { afterEach, describe, expect, it, vi } from 'vitest'
// Clé d'idempotence hors contexte sécurisé : sans crypto.randomUUID, UUID v4 tiré de getRandomValues ou, à défaut, de Math.random.
describe('nouvelleCle', () => {
  afterEach(() => vi.unstubAllGlobals())
  it('UUID v4 sans randomUUID, avec ou sans getRandomValues', () => {
    const forme = /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/
    vi.stubGlobal('crypto', { getRandomValues: (t: Uint8Array) => t.fill(7) })
    expect(nouvelleCle()).toMatch(forme)
    vi.stubGlobal('crypto', undefined)
  })
})
