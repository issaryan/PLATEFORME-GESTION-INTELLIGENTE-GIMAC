import { describe, expect, it } from 'vitest'
import type { Catalogue } from '@gimac/socle'
import { catalogue } from './ecrans/pouvoirs'
import { commun } from './commun'

const ECRANS = import.meta.glob<{ catalogue: Catalogue }>('./ecrans/*.ts', { eager: true })

// Catalogue des pouvoirs hiérarchiques (D-81) : mêmes clés en FR, EN et ES, mêmes variables, aucune valeur vide,
// aucune clé redéfinie ailleurs, jamais de tiret cadratin.
describe('Pouvoirs hiérarchiques : catalogue FR, EN, ES', () => {
  const cles = (l: 'fr' | 'en' | 'es') => Object.keys(catalogue[l] ?? {}).sort()
  const variables = (v: string) => (v.match(/\{\{\s*\w+\s*\}\}/g) ?? []).sort().join(',')
  it('trois langues complètes, variables identiques, sans tiret cadratin', () => {
    expect(cles('en')).toEqual(cles('fr'))
    expect(cles('es')).toEqual(cles('fr'))
    for (const k of cles('fr')) {
      const fr = (catalogue.fr as Record<string, string>)[k]
      for (const l of ['fr', 'en', 'es'] as const) {
        const v = (catalogue[l] as Record<string, string>)[k]
        expect(v.trim(), `${l} ${k}`).not.toBe('')
        expect(v.includes(String.fromCharCode(0x2014)), `${l} ${k}`).toBe(false)
        expect(variables(v), `${l} ${k}`).toBe(variables(fr))
      }
    }
  })
  it('aucune clé redéfinie par un autre catalogue', () => {
    const autres = [commun, ...Object.entries(ECRANS).filter(([f]) => !f.endsWith('/pouvoirs.ts')).map(([, m]) => m.catalogue)]
    const ailleurs = new Set(autres.flatMap((c) => Object.keys(c.fr ?? {})))
    expect(cles('fr').filter((k) => ailleurs.has(k))).toEqual([])
  })
})
