import { afterEach, describe, expect, it, vi } from 'vitest'
import { nouvelleCle } from './hooks'

// Clé d'idempotence : randomUUID d'abord, à défaut un gabarit v4 tiré par getRandomValues, puis Math.random.
const V4 = /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/

describe('nouvelleCle', () => {
  afterEach(() => vi.unstubAllGlobals())

  it('prend randomUUID quand il existe', () => {
    vi.stubGlobal('crypto', { randomUUID: () => 'cle-native-0001' })
    expect(nouvelleCle()).toBe('cle-native-0001')
  })

  it('tire un gabarit v4 par getRandomValues sans randomUUID', () => {
    vi.stubGlobal('crypto', { getRandomValues: (t: Uint8Array) => t.fill(0xab) })
    expect(nouvelleCle()).toMatch(V4)
  })

  it('se replie sur Math.random sans crypto', () => {
    vi.stubGlobal('crypto', undefined)
    vi.spyOn(Math, 'random').mockReturnValue(0.5)
    expect(nouvelleCle()).toMatch(V4)
  })
})
