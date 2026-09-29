import { describe, expect, it } from 'vitest'
import { catalogue } from '../../../i18n/ecrans/parcours-portail'
import type { Catalogue } from '@gimac/socle'
import { commun } from '../../../i18n/commun'

const ECRANS = import.meta.glob<{ catalogue: Catalogue }>('../../../i18n/ecrans/*.ts', { eager: true })

// Catalogue du portail du participant : mêmes clés en FR, EN et ES, mêmes variables d'interpolation,
// aucune valeur vide, et aucune clé du préfixe « pp. » redéfinie ailleurs.
describe('Portail : catalogue FR, EN, ES', () => {
  const cles = (l: 'fr' | 'en' | 'es') => Object.keys(catalogue[l] ?? {}).sort()
  const variables = (v: string) => (v.match(/\{\{\s*\w+\s*\}\}/g) ?? []).sort().join(',')

  it('trois langues complètes, variables identiques', () => {
    expect(cles('en')).toEqual(cles('fr'))
    expect(cles('es')).toEqual(cles('fr'))
    for (const k of cles('fr')) {
      const fr = (catalogue.fr as Record<string, string>)[k]
      for (const l of ['en', 'es'] as const) {
        const v = (catalogue[l] as Record<string, string>)[k]
        expect(v.trim(), `${l} ${k}`).not.toBe('')
        expect(variables(v), `${l} ${k}`).toBe(variables(fr))
      }
    }
  })

  it('aucune clé redéfinie par un autre catalogue (la dernière chargée l’emporterait en silence)', () => {
    const autres = [commun, ...Object.entries(ECRANS).filter(([f]) => !f.endsWith('parcours-portail.ts')).map(([, m]) => m.catalogue)]
    const ailleurs = new Set(autres.flatMap((c) => Object.keys(c.fr ?? {})))
    expect(cles('fr').filter((k) => ailleurs.has(k))).toEqual([])
  })
})
