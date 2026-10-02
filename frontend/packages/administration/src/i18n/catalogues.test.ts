import { describe, expect, it } from 'vitest'
import { fr } from './fr'
import { en } from './en'
import { es } from './es'

describe('catalogues de l’administration', () => {
  it('FR, EN et ES portent les mêmes clés, sans traduction vide', () => {
    const cles = (c: Record<string, string>) => Object.keys(c).sort()
    expect(cles(en)).toEqual(cles(fr))
    expect(cles(es)).toEqual(cles(fr))
    for (const c of [fr, en, es]) expect(Object.values(c).filter((v) => !v.trim())).toEqual([])
  })
})

