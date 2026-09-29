import { describe, expect, it } from 'vitest'
import { parSegment, suiteCycle, versArbre, versLignesDiff } from './referentiel'
import type { ReferenceOperation, ReferentielVersion } from '../modele/homologation'

const t = (k: string, v?: Record<string, unknown>) => (v ? `${k}:${JSON.stringify(v)}` : k)
const version = (id: string, numero: number | null, etat: ReferentielVersion['etat'], typeProjet: ReferentielVersion['typeProjet'] = 'CARTE_INTERBANCAIRE'): ReferentielVersion => ({
  id, typeProjet, numero, libelle: id, etat, dateCreation: '', datePublication: null, versionSourceId: null, versionActiveId: null, transitionsPermises: [], actionsPermises: [],
})
const op = (code: string, segment: ReferenceOperation['segment'], libelle = code, predecesseurs: string[] = []): ReferenceOperation => ({
  versionId: 'V', code, libelle, phase: 'P', segment, partie: 'DES', serviceCode: 'RETRAIT', natureCouverture: 'AUCUNE', idScenario: null, predecesseurs,
  fonctionResponsable: '', preuveAttendue: {}, transitionsPermises: [], actionsPermises: [],
})

describe('DES-12 adaptateurs', () => {
  it('groupe les versions par type, brouillon en tête puis numéros décroissants', () => {
    const arbre = versArbre([version('A2', 2, 'RETIREE'), version('A3', 3, 'PUBLIEE'), version('A4', null, 'BROUILLON'), version('M2', 2, 'PUBLIEE', 'PAIEMENT_MOBILE')], t)
    expect(arbre.map((n) => n.key)).toEqual(['type:CARTE_INTERBANCAIRE', 'type:PAIEMENT_MOBILE'])
    expect(arbre[0].children?.map((n) => n.key)).toEqual(['A4', 'A3', 'A2'])
    expect(arbre[0].children?.[1].badge).toMatchObject({ status: 'ok', label: 'referentiel.etat.PUBLIEE' })
  })
  it('range les opérations dans l\'ordre des segments', () => {
    const g = parSegment([op('C', 'TRONC_COMMUN_AVAL'), op('A', 'TRONC_COMMUN_AMONT'), op('B', 'BRANCHE')])
    expect(g.map((x) => x.segment)).toEqual(['TRONC_COMMUN_AMONT', 'BRANCHE', 'TRONC_COMMUN_AVAL'])
  })
  it('compare deux versions par code : conservée, modifiée, retirée, ajoutée', () => {
    const l = versLignesDiff([op('A', 'BRANCHE'), op('B', 'BRANCHE'), op('C', 'BRANCHE')], [op('A', 'BRANCHE'), op('B', 'BRANCHE', 'B modifiée'), op('D', 'BRANCHE')])
    expect(l.map((x) => x.type ?? 'egal')).toEqual(['egal', 'mod', 'del', 'add'])
    expect(l[2]).toMatchObject({ ln: 3 })
    expect(l[3]).toMatchObject({ rn: 3 })
  })
  it('restitue un cycle fermé dans l\'ordre du service', () => {
    expect(suiteCycle(['A', 'B', 'C'])).toBe('A → B → C → A')
    expect(suiteCycle([])).toBe('')
  })
})
