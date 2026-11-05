import { describe, expect, it } from 'vitest'
import type { Catalogue } from '@gimac/socle'
import { catalogue as erreurs } from './ecrans/processus-erreurs'
import { catalogue as suivi } from './ecrans/processus-suivi'
import { catalogue as processus } from './ecrans/processus'
import { commun } from './commun'

const ECRANS = import.meta.glob<{ catalogue: Catalogue }>('./ecrans/*.ts', { eager: true })
// Sources Java de MS-PILOTAGE, lues telles quelles : le contrat des codes de refus suit le service sans copie.
const SOURCES = import.meta.glob<string>('../../../../../ms-pilotage/src/main/java/**/*.java', { query: '?raw', import: 'default', eager: true })

// Catalogues des processus d'unité (D-87), découpés en trois fichiers : mêmes clés en FR, EN et ES, aucune clé
// définie deux fois, refus du rendu PDF de MS-PILOTAGE (D-127) traduit.
describe("Processus d'unité : catalogues FR, EN, ES", () => {
  const parties: Catalogue[] = [processus, suivi, erreurs]
  const cles = (c: Catalogue, l: 'fr' | 'en' | 'es') => Object.keys(c[l] ?? {}).sort()

  it('trois langues complètes dans chaque fichier, sans doublon entre fichiers', () => {
    for (const c of parties) {
      expect(cles(c, 'en')).toEqual(cles(c, 'fr'))
      expect(cles(c, 'es')).toEqual(cles(c, 'fr'))
    }
    const toutes = parties.flatMap((c) => cles(c, 'fr'))
    expect(new Set(toutes).size).toBe(toutes.length)
  })

  it('PIL_PDF_INDISPONIBLE (export PDF d’une instance) traduit en FR, EN et ES', () => {
    for (const l of ['fr', 'en', 'es'] as const) {
      expect(erreurs[l]['proc.erreur.PIL_PDF_INDISPONIBLE']?.trim(), l).toBeTruthy()
    }
  })

  it('tout code PIL_* lancé par MS-PILOTAGE (PilotageException) a un libellé proc.erreur ou act.erreur en FR, EN et ES', () => {
    const codes = new Set<string>()
    for (const source of Object.values(SOURCES)) {
      for (const m of source.matchAll(/PilotageException\.\w+\(\s*"(PIL_[A-Z0-9_]+)"/g)) codes.add(m[1])
    }
    expect(codes.size).toBeGreaterThan(50)
    const tous = [commun, ...Object.values(ECRANS).map((m) => m.catalogue)]
    for (const l of ['fr', 'en', 'es'] as const) {
      const cles = new Set(tous.flatMap((c) => Object.keys(c[l] ?? {})))
      const sansLibelle = [...codes].filter((c) => !cles.has(`proc.erreur.${c}`) && !cles.has(`act.erreur.${c}`)).sort()
      expect(sansLibelle, l).toEqual([])
    }
  })
})

