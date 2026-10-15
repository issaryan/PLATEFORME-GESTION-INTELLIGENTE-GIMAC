import { describe, expect, it } from 'vitest'
import type { TypeProjet } from './commun'
import { preuvesDe, referentielDe } from './referentielCR'
import { PREUVES_CR } from './preuvesCR'

// Données D-109 alignées sur l'annexe A : une entrée de preuves par opération du parcours (rangs 3 et suivants) et par
// type, au moins un livrable décrit, délais cible et maximal cohérents, signatures ordonnées par des fonctions internes.
const TYPES: TypeProjet[] = ['CARTE_INTERBANCAIRE', 'PAIEMENT_MOBILE', 'DELEGATAIRE']
const ATTENDUS: Record<string, number> = { CARTE_INTERBANCAIRE: 43, PAIEMENT_MOBILE: 49, DELEGATAIRE: 67 }

describe('preuves, livrables et signatures du CR v1.7 (D-109)', () => {
  it('une entrée par opération du parcours et par type, aucune entrée orpheline', () => {
    for (const t of TYPES) {
      const ops = referentielDe(t).filter((o) => o.etape !== 'PRISE_CONTACT' && o.etape !== 'ADHESION')
      expect(ops.length).toBe(ATTENDUS[t as string])
      for (const o of ops) {
        expect(PREUVES_CR.filter((p) => p.rang === o.rang && (p.types === null || p.types.includes(t))), `${t} ${o.rang}`).toHaveLength(1)
        const p = preuvesDe(t, o.rang)!
        expect(p.livrables.length, `${t} ${o.rang}`).toBeGreaterThan(0)
        expect(p.delaiMaxJours).toBeGreaterThanOrEqual(p.delaiJours)
        expect(p.preuveAttendue).toMatch(/^Preuves et livrables attendus/)
      }
    }
    const rangs = new Set(TYPES.flatMap((t) => referentielDe(t).map((o) => o.rang)))
    expect(PREUVES_CR.filter((p) => !rangs.has(p.rang))).toEqual([])
  })
  it('exemples : ping au rang 13, points de terminaison dans PayWay (M8), fichier XML de fin de journée (C10), grain du service', () => {
    expect(preuvesDe('CARTE_INTERBANCAIRE', '13')?.livrables[0].libelle).toContain('ping')
    expect(preuvesDe('PAIEMENT_MOBILE', 'M8')?.livrables[0].libelle).toContain('PayWay')
    expect(preuvesDe('CARTE_INTERBANCAIRE', 'C10')?.livrables[0].typesPieces).toContain('application/xml')
    expect(preuvesDe('DELEGATAIRE', '16')?.livrables[0].grain).toBe('SERVICE')
    expect(preuvesDe('PAIEMENT_MOBILE', '16')?.livrables[0].grain).toBe('OPERATION')
    expect(preuvesDe('CARTE_INTERBANCAIRE', 'ZZ')).toBeUndefined()
  })
  it('signatures : ordre croissant, fonctions internes, livrables portés déclarés ; procès-verbal sous le code GIMAC', () => {
    for (const p of PREUVES_CR) {
      const codes = p.livrables.map((l) => l.code)
      for (const s of p.signatures) {
        expect(['PARTICIPANT', 'BANQUE_PARTENAIRE', 'PERSONNALISATEUR']).not.toContain(s.signataire)
        expect(s.porte.every((c) => codes.includes(c)), `${p.rang} ${s.code}`).toBe(true)
      }
      const ordres = p.signatures.map((s) => s.ordre)
      expect(ordres).toEqual([...ordres].sort((a, b) => a - b))
    }
    expect(preuvesDe('CARTE_INTERBANCAIRE', '19')?.signatures.map((s) => [s.code, s.signataire])).toEqual([['GIMAC', 'CHEF_DES']])
  })
})
