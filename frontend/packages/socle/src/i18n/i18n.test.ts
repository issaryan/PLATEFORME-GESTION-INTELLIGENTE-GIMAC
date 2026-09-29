import { describe, expect, it } from 'vitest'
import fr from './fr.json'
import en from './en.json'
import es from './es.json'

describe('catalogues du socle', () => {
  it('les trois langues de la zone CEMAC portent exactement les mêmes clés', () => {
    const cles = (c: Record<string, string>) => Object.keys(c).sort()
    expect(cles(en)).toEqual(cles(fr))
    expect(cles(es)).toEqual(cles(fr))
  })
  it('aucune traduction vide', () => {
    for (const c of [fr, en, es]) expect(Object.values(c).filter((v) => !v.trim())).toEqual([])
  })
})
