import { describe, expect, it } from 'vitest'
import type { TypeProjet } from './commun'
import { BRANCHE_CARTE, BRANCHE_DELEGATAIRE, BRANCHE_MOBILE, TRONC_AMONT, TRONC_AVAL, referentielDe } from './referentielCR'

// Référentiel de l'annexe A du CR v1.4 (D-59) : tronc amont 1 à 26, branche du type de projet,
// tronc aval 27 à 35 ; la branche mobile compte 17 opérations (les mentions « 21 » du CR sont une erreur).
const TYPES: TypeProjet[] = ['CARTE_INTERBANCAIRE', 'PAIEMENT_MOBILE', 'DELEGATAIRE']
const rangs = (l: { rang: string }[]) => l.map((o) => o.rang)

describe('référentiel du compte rendu de cadrage (annexe A)', () => {
  it('troncs et branches : 26 + 9 opérations communes, branches C1–C10, M1–M17, D1–D34', () => {
    expect(rangs(TRONC_AMONT)).toEqual(Array.from({ length: 26 }, (_, i) => String(i + 1)))
    expect(rangs(TRONC_AVAL)).toEqual(Array.from({ length: 9 }, (_, i) => String(i + 27)))
    expect(rangs(BRANCHE_CARTE)).toEqual(Array.from({ length: 10 }, (_, i) => `C${i + 1}`))
    expect(rangs(BRANCHE_MOBILE)).toEqual(Array.from({ length: 17 }, (_, i) => `M${i + 1}`))
    expect(rangs(BRANCHE_DELEGATAIRE)).toEqual(Array.from({ length: 34 }, (_, i) => `D${i + 1}`))
  })
  it('referentielDe : tronc amont filtré par type, une seule branche, tronc aval', () => {
    expect(referentielDe('CARTE_INTERBANCAIRE')).toHaveLength(26 + 10 + 9)
    // Le rang 5 ne concerne pas le paiement mobile (colonnes C et D seulement).
    expect(referentielDe('PAIEMENT_MOBILE')).toHaveLength(25 + 17 + 9)
    expect(rangs(referentielDe('PAIEMENT_MOBILE'))).not.toContain('5')
    expect(referentielDe('DELEGATAIRE')).toHaveLength(26 + 34 + 9)
    expect(referentielDe('PAIEMENT_MOBILE').filter((o) => o.segment === 'BRANCHE')).toHaveLength(17)
    TYPES.forEach((t) => expect(new Set(referentielDe(t).filter((o) => o.segment === 'BRANCHE').map((o) => o.types.join()))).toEqual(new Set([t])))
  })
  it('le rang 27 suit la dernière opération de la branche, sans altérer le tronc aval partagé', () => {
    const r27 = (t: TypeProjet) => referentielDe(t).find((o) => o.rang === '27')!.predecesseurs
    expect(r27('CARTE_INTERBANCAIRE')).toEqual(['C10'])
    expect(r27('PAIEMENT_MOBILE')).toEqual(['M17'])
    expect(r27('DELEGATAIRE')).toEqual(['D34'])
    expect(TRONC_AVAL[0].predecesseurs).toEqual([])
  })
  it('graphe cohérent : prédécesseurs connus et antérieurs (ordre topologique), rangs uniques', () => {
    TYPES.forEach((t) => {
      const l = referentielDe(t)
      expect(new Set(rangs(l)).size).toBe(l.length)
      l.forEach((o, i) => o.predecesseurs.forEach((p) => {
        const j = l.findIndex((x) => x.rang === p)
        expect(j, `${t} ${o.rang} ← ${p}`).toBeGreaterThanOrEqual(0)
        expect(j, `${t} ${o.rang} ← ${p}`).toBeLessThan(i)
      }))
    })
  })
  it('attributs de l’annexe : un seul grain de phase (rang 15), pièce pivot (19), oubli sans symptôme (28)', () => {
    const l = referentielDe('DELEGATAIRE')
    expect(rangs(l.filter((o) => o.grain === 'PHASE'))).toEqual(['15'])
    expect(rangs(l.filter((o) => o.pivot))).toEqual(['19'])
    expect(rangs(l.filter((o) => o.oubliSansSymptome))).toEqual(['28'])
    // Aucune valeur secrète : les opérations sur clé ne portent que la trace et le code de contrôle.
    expect(rangs(referentielDe('CARTE_INTERBANCAIRE').filter((o) => o.cle))).toEqual(['C1', '29'])
    expect(rangs(referentielDe('PAIEMENT_MOBILE').filter((o) => o.cle))).toEqual(['M3', 'M9', '29'])
    expect(rangs(l.filter((o) => o.cle))).toEqual(['D2', 'D3', 'D12', 'D27', '29'])
    expect(rangs(referentielDe('CARTE_INTERBANCAIRE').filter((o) => o.valeurReference))).toEqual(['12', 'C2', 'C3'])
  })
  it('plateformes d’exécution : branche mobile sur la plateforme mobile, délégataire sur les deux switchs', () => {
    expect(new Set(BRANCHE_CARTE.map((o) => o.plateforme))).toEqual(new Set(['SWITCH_INTERBANCAIRE']))
    expect(new Set(BRANCHE_MOBILE.map((o) => o.plateforme))).toEqual(new Set(['PLATEFORME_MOBILE']))
    expect(new Set(BRANCHE_DELEGATAIRE.map((o) => o.plateforme))).toEqual(new Set([null, 'SWITCH_DELEGATAIRE', 'SWITCH_INTERBANCAIRE']))
    expect(TRONC_AMONT.every((o) => o.plateforme === null && o.segment === 'AMONT')).toBe(true)
    // Porteurs : le premier est le responsable principal ; aucune opération sans porteur.
    TYPES.forEach((t) => expect(referentielDe(t).every((o) => o.porteurs.length > 0)).toBe(true))
    expect(BRANCHE_DELEGATAIRE[0].porteurs).toEqual(['CONDUCTEUR', 'PERSONNALISATEUR'])
  })
})
