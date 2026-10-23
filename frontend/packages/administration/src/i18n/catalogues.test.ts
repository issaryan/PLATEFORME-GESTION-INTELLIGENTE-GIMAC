import { describe, expect, it } from 'vitest'
import { fr } from './fr'
import { en } from './en'
import { es } from './es'
import { sandbox } from './sandbox'

describe('catalogues de l’administration', () => {
  it('FR, EN et ES portent les mêmes clés, sans traduction vide', () => {
    const cles = (c: Record<string, string>) => Object.keys(c).sort()
    expect(cles(en)).toEqual(cles(fr))
    expect(cles(es)).toEqual(cles(fr))
    for (const c of [fr, en, es]) expect(Object.values(c).filter((v) => !v.trim())).toEqual([])
  })
  it('Sandbox GIMAC (D-119) : FR, EN et ES portent les mêmes clés, sans traduction vide ni collision', () => {
    const cles = (c: Record<string, string>) => Object.keys(c).sort()
    expect(cles(sandbox.en)).toEqual(cles(sandbox.fr))
    expect(cles(sandbox.es)).toEqual(cles(sandbox.fr))
    for (const c of [sandbox.fr, sandbox.en, sandbox.es]) expect(Object.values(c).filter((v) => !v.trim())).toEqual([])
    expect(cles(sandbox.fr).filter((k) => k in fr)).toEqual([])
  })
})
