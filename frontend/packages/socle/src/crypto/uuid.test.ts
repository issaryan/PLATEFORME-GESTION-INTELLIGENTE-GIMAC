import { afterEach, describe, expect, it, vi } from 'vitest'
import { installerRandomUUID, uuidV4 } from './uuid'

const V4 = /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/

describe('uuidV4', () => {
  afterEach(() => vi.restoreAllMocks())

  it('randomUUID du navigateur quand il existe (contexte sécurisé)', () => {
    expect(uuidV4({ randomUUID: () => 'natif' })).toBe('natif')
    expect(uuidV4()).toMatch(V4)
  })

  it('contexte non sécurisé : seize octets de getRandomValues, version 4 et variante RFC posées', () => {
    const tirage = (t: Uint8Array<ArrayBuffer>) => t.fill(0xff)
    expect(uuidV4({ getRandomValues: tirage })).toBe('ffffffff-ffff-4fff-bfff-ffffffffffff')
    expect(uuidV4({ getRandomValues: (t) => t.fill(0) })).toBe('00000000-0000-4000-8000-000000000000')
  })

  it('sans Web Crypto : Math.random, toujours au format v4', () => {
    vi.spyOn(Math, 'random').mockReturnValue(0.5)
    expect(uuidV4({})).toBe('80808080-8080-4080-8080-808080808080')
    const original = Object.getOwnPropertyDescriptor(globalThis, 'crypto')
    Object.defineProperty(globalThis, 'crypto', { value: undefined, configurable: true })
    try {
      expect(uuidV4()).toBe('80808080-8080-4080-8080-808080808080')
    } finally {
      if (original) Object.defineProperty(globalThis, 'crypto', original)
    }
  })
})

describe('installerRandomUUID', () => {
  it('pose randomUUID absent, à partir de getRandomValues de la même source', () => {
    const c: { randomUUID?: () => string; getRandomValues: (t: Uint8Array<ArrayBuffer>) => Uint8Array<ArrayBuffer> } = { getRandomValues: (t) => t.fill(0x11) }
    installerRandomUUID(c)
    expect(c.randomUUID?.()).toBe('11111111-1111-4111-9111-111111111111')
  })

  it('garde randomUUID existant ; sans Web Crypto, rien à poser', () => {
    const natif = () => 'natif'
    const c = { randomUUID: natif }
    installerRandomUUID(c)
    expect(c.randomUUID).toBe(natif)
    expect(() => installerRandomUUID(undefined)).not.toThrow()
    const nu: { randomUUID?: () => string } = {}
    installerRandomUUID(nu)
    expect(nu.randomUUID?.()).toMatch(V4)
  })

  it("module '@gimac/socle/polyfill' : installé sur crypto global au chargement", async () => {
    const original = Object.getOwnPropertyDescriptor(globalThis, 'crypto')
    const reel = globalThis.crypto
    const c: { randomUUID?: () => string; getRandomValues: (t: Uint8Array<ArrayBuffer>) => Uint8Array<ArrayBuffer> } = { getRandomValues: (t) => reel.getRandomValues(t) }
    Object.defineProperty(globalThis, 'crypto', { value: c, configurable: true })
    try {
      vi.resetModules()
      await import('./polyfill')
      expect(c.randomUUID?.()).toMatch(V4)
    } finally {
      if (original) Object.defineProperty(globalThis, 'crypto', original)
    }
  })
})
