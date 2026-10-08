import { describe, expect, it } from 'vitest'
import type { Fonction, OperationParcours } from '../../../../modele/parcours'
import { carteDecision, compter, concerne, externe, filtrer, grouper, lienDuGeste, lireCodeControle, ongletDuGeste, plateformes } from './regles'

// Règles de présentation de l'onglet « Opérations » : filtres, regroupement, code de contrôle.
const P = (fonction: Fonction) => ({ id: `U-${fonction}`, nom: fonction, fonction })
const op = (o: Partial<OperationParcours>): OperationParcours => ({
  id: `L:${o.rang ?? '1'}`, liaisonId: 'L', rang: '1', etape: 'INTEGRATION', segment: 'AMONT', plateforme: null, grain: 'OPERATION', etat: 'A_FAIRE',
  raci: { R: P('SNA'), C: [], V: P('CONDUCTEUR'), I: [], E: [] }, echeance: null, sla: null, faiteLe: null, preuve: null, cle: false, secret: false,
  valeurReference: false, oubliSansSymptome: false, pivot: false, predecesseursNonFaits: [], transitionsPermises: [], actionsPermises: [], ...o,
})

describe('onglet Opérations : règles de présentation', () => {
  it('code de contrôle : normalisé, contrôlé, refusé sans troncature au-delà de 8 caractères', () => {
    expect(lireCodeControle(' a1 b2c3 ')).toEqual({ valeur: 'A1B2C3', erreur: null })
    expect(lireCodeControle('')).toEqual({ valeur: '', erreur: 'vide' })
    expect(lireCodeControle('ZZ12')).toEqual({ valeur: 'ZZ12', erreur: 'format' })
    expect(lireCodeControle('abc')).toEqual({ valeur: 'ABC', erreur: 'format' })
    expect(lireCodeControle('0123456789ABCDEF0123456789ABCDEF')).toEqual({ valeur: '', erreur: 'tropLong' })
  })

  it('filtres : vue, étape, ma fonction, recherche par rang ou libellé', () => {
    const ops = [
      op({ rang: '13', raci: { R: P('POLE_RESEAU'), C: [], V: P('CONDUCTEUR'), I: [], E: [] } }),
      op({ rang: '15', etat: 'EN_VALIDATION' }),
      op({ rang: '16', etape: 'TESTS', etat: 'BLOQUEE', sla: 'DEPASSE', raci: { R: P('PARTICIPANT'), C: [P('SNA')], V: null, I: [], E: [] } }),
      op({ rang: '12', etat: 'FAITE' }),
    ]
    const lib = (o: OperationParcours) => `libellé ${o.rang}`
    const base = { vue: '' as const, etape: '', miennes: false, recherche: '' }
    expect(filtrer(ops, { ...base, vue: 'a-faire' }, [], lib).map((o) => o.rang)).toEqual(['13', '16'])
    expect(filtrer(ops, { ...base, vue: 'a-valider' }, [], lib).map((o) => o.rang)).toEqual(['15'])
    expect(filtrer(ops, { ...base, vue: 'retard' }, [], lib).map((o) => o.rang)).toEqual(['16'])
    expect(filtrer(ops, { ...base, etape: 'TESTS' }, [], lib).map((o) => o.rang)).toEqual(['16'])
    expect(filtrer(ops, { ...base, miennes: true }, ['POLE_RESEAU'], lib).map((o) => o.rang)).toEqual(['13'])
    expect(filtrer(ops, { ...base, miennes: true }, ['SNA'], lib).map((o) => o.rang)).toEqual(['15', '16', '12'])
    expect(filtrer(ops, { ...base, recherche: '15' }, [], lib).map((o) => o.rang)).toEqual(['15'])
    expect(filtrer(ops, { ...base, recherche: 'LIBELLÉ 1' }, [], lib)).toHaveLength(4)
    expect(concerne(ops[0], ['CONDUCTEUR'])).toBe(true)
    expect(concerne(op({ raci: { R: null, C: [], V: null, I: [], E: [] } }), ['SNA'])).toBe(false)
  })

  it('étapes de la version (D-93) : ordre configuré, étape hors liste à la suite ; à signer comptée à valider', () => {
    const ops = [op({ rang: 'A', etapeCode: 'RECETTE' }), op({ rang: 'B', etapeCode: 'CADRAGE_ETENDU' }), op({ rang: 'C', etapeCode: 'AUTRE', etat: 'EN_SIGNATURE' })]
    expect(grouper(ops, ['CADRAGE_ETENDU', 'RECETTE']).map((g) => g.etape)).toEqual(['CADRAGE_ETENDU', 'RECETTE', 'AUTRE'])
    const base = { vue: '' as const, etape: '', miennes: false, recherche: '' }
    expect(filtrer(ops, { ...base, etape: 'RECETTE' }, [], () => '').map((o) => o.rang)).toEqual(['A'])
    expect(filtrer(ops, { ...base, vue: 'a-valider' }, [], () => '').map((o) => o.rang)).toEqual(['C'])
    expect(compter(ops).aValider).toBe(1)
  })

  it('regroupement par étape du parcours puis par segment ; plateformes distinctes', () => {
    const ops = [
      op({ rang: '27', etape: 'MISE_EN_PRODUCTION', segment: 'AVAL', plateforme: null }),
      op({ rang: 'C1', etape: 'MISE_EN_PRODUCTION', segment: 'BRANCHE', plateforme: 'SWITCH_INTERBANCAIRE' }),
      op({ rang: 'C2', etape: 'MISE_EN_PRODUCTION', segment: 'BRANCHE', plateforme: 'SWITCH_INTERBANCAIRE' }),
      op({ rang: '12', etape: 'INTEGRATION' }),
    ]
    const g = grouper(ops)
    expect(g.map((x) => x.etape)).toEqual(['INTEGRATION', 'MISE_EN_PRODUCTION'])
    expect(g[1].segments.map((s) => s.segment)).toEqual(['BRANCHE', 'AVAL'])
    expect(plateformes(g[1].segments[0].operations)).toEqual(['SWITCH_INTERBANCAIRE'])
    expect(compter([...ops, op({ etat: 'FAITE' }), op({ etat: 'EN_VALIDATION', sla: 'DEPASSE' })])).toEqual({ total: 6, faites: 1, aFaire: 4, aValider: 1, retard: 1 })
  })

  it('geste ailleurs lu sur la vue de l’opération (D-93), jamais sur son rang ; opérations externes du catalogue', () => {
    const vues = ['AFFECTATION', 'DECISION', 'ATTESTATIONS', 'MISE_EN_SERVICE', 'ESSAIS', 'AUCUNE', 'CEREMONIE', undefined] as const
    expect(vues.map((vue) => ongletDuGeste({ vue }))).toEqual(['affectation', 'decision', 'decision', 'mise-en-service', null, null, null, null])
    expect(lienDuGeste('affectation', { liaisonId: 'L', id: 'L:6' })).toBe('/des/parcours/L?onglet=etapes&action=affecter')
    expect(lienDuGeste('decision', { liaisonId: 'L', id: 'L:20' })).toBe('/des/parcours/L?onglet=decision&operation=L%3A20')
    // D-98 : carte de l'onglet Décision visée par ?operation=, lue sur le déclencheur, le jalon, le geste et la vue.
    expect(carteDecision(undefined)).toBeNull()
    expect(carteDecision({ vue: 'DECISION', declencheur: 'certification.pv-established.v1' })).toBe('pv')
    expect(carteDecision({ vue: 'DECISION', declencheur: 'certification.pv-signed.v1', jalon: 'CERTIFIEE' })).toBe('pv')
    expect(carteDecision({ vue: 'ESSAIS', geste: 'CONSTATER_COUVERTURE', jalon: 'APTE_CERTIFICATION' })).toBe('pv')
    expect(carteDecision({ vue: 'DECISION', declencheur: 'certification.change-record-constituted.v1' })).toBe('approbation')
    expect(carteDecision({ vue: 'DECISION', declencheur: 'certification.change-record-referenced.v1' })).toBe('approbation')
    expect(carteDecision({ vue: 'ATTESTATIONS', declencheur: null })).toBe('approbation')
    expect(carteDecision({ vue: 'DECISION', declencheur: 'certification.security-approved.v1' })).toBe('approbation')
    expect(carteDecision({ vue: 'AUCUNE', jalon: 'APPROUVEE_SECURITE' })).toBe('approbation')
    expect(carteDecision({ vue: 'DECISION' })).toBe('pv')
    expect(carteDecision({ vue: 'MISE_EN_SERVICE', geste: 'FICHE_SUIVI' })).toBeNull()
    const externes = ['PARTICIPANT', 'PERSONNALISATEUR', 'JURISTE_EXTERNE']
    expect(externe(op({ raci: { R: P('PARTICIPANT'), C: [], V: null, I: [], E: [] } }), externes)).toBe(true)
    expect(externe(op({ raci: { R: P('JURISTE_EXTERNE'), C: [], V: null, I: [], E: [] } }), externes)).toBe(true)
    expect(externe(op({}), externes)).toBe(false)
    expect(externe(op({ raci: { R: null, C: [], V: null, I: [], E: [] } }), externes)).toBe(false)
  })
})
