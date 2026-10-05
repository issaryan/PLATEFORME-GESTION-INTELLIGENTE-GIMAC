import { describe, expect, it } from 'vitest'
import { createHash } from 'node:crypto'
import { sha256Hex, sha256Js } from './sha256'

const hex = (o: Uint8Array) => Array.from(o, (b) => b.toString(16).padStart(2, '0')).join('')
const reference = (o: Uint8Array) => createHash('sha256').update(o).digest('hex')

describe('sha256', () => {
  it('implémentation JavaScript conforme (vides, blocs limites, données longues)', () => {
    for (const n of [0, 1, 55, 56, 63, 64, 65, 119, 120, 1000, 100_003]) {
      const o = Uint8Array.from({ length: n }, (_, i) => (i * 31 + 7) & 0xff)
      expect(hex(sha256Js(o))).toBe(reference(o))
    }
    expect(hex(sha256Js(new TextEncoder().encode('abc')))).toBe('ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad')
  })

  it('sha256Hex : Web Crypto si présent, repli JavaScript sinon (http sans contexte sécurisé)', async () => {
    const o = new TextEncoder().encode('Moabi')
    const attendu = reference(o)
    expect(await sha256Hex(o)).toBe(attendu)
    expect(await sha256Hex(new Blob([o]))).toBe(attendu)
    const original = Object.getOwnPropertyDescriptor(globalThis, 'crypto')
    Object.defineProperty(globalThis, 'crypto', { value: {}, configurable: true })
    try {
      expect(await sha256Hex(o.buffer)).toBe(attendu)
    } finally {
      if (original) Object.defineProperty(globalThis, 'crypto', original)
    }
  })
})
