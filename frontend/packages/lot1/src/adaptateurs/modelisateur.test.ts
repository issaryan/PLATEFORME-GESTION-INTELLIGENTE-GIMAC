import { describe, expect, it } from 'vitest'
import {
  couloirs, dispositionAuto, erreursSaisie, libelleEntree, libelleOperation, ongletDuChamp, operationVierge, positionsEffectives, saisieDe,
} from './modelisateur'
import { CATALOGUE, calculerGraphe, versionsInitiales } from '../services/simule/referentiels'

// Adaptateurs du modélisateur (D-77, §6) : disposition automatique par couches de Kahn et colonnes d'étape,
// couloirs, corps d'enregistrement, contrôles de forme de la modale.
const { versions, operations } = versionsInitiales()
const graphe = calculerGraphe(versions[0], operations['REF-CI-3'])
const t = (k: string, o?: Record<string, unknown>) => (o?.defaultValue ? `${k}|${o.defaultValue}` : k)

describe('Modélisateur : disposition automatique et couloirs', () => {
  it('colonnes par étape (rang du catalogue) puis par couche ; bandes par segment, sans chevauchement', () => {
    const p = dispositionAuto(graphe.noeuds, CATALOGUE)
    expect(p.size).toBe(graphe.noeuds.length)
    // Cadrage (3, 4, 5) avant le lancement (6), lui-même avant les ateliers.
    expect(p.get('3')!.x).toBeLessThan(p.get('6')!.x)
    expect(p.get('6')!.x).toBeLessThan(p.get('8')!.x)
    // 4 et 5 : même étape, même couche (successeurs de 3) → même colonne, lignes distinctes.
    expect(p.get('4')!.x).toBe(p.get('5')!.x)
    expect(p.get('4')!.y).not.toBe(p.get('5')!.y)
    // Bandes : amont au-dessus de la branche, elle-même au-dessus de l'aval.
    expect(p.get('3')!.y).toBeLessThan(p.get('C1')!.y)
    expect(Math.max(...graphe.noeuds.filter((n) => n.segment === 'BRANCHE').map((n) => p.get(n.code)!.y))).toBeLessThan(p.get('27')!.y)
    const cles = [...p.values()].map((x) => `${x.x},${x.y}`)
    expect(new Set(cles).size).toBe(cles.length)
  })
  it('positions enregistrées prioritaires ; couloirs d\'étape et de segment', () => {
    const noeuds = graphe.noeuds.map((n, i) => (i === 0 ? { ...n, position: null } : n))
    const auto = dispositionAuto(noeuds, CATALOGUE)
    const p = positionsEffectives(noeuds, auto)
    expect(p.get(noeuds[0].code)).toEqual(auto.get(noeuds[0].code))
    expect(p.get(noeuds[1].code)).toEqual(noeuds[1].position)
    const c = couloirs(noeuds, p, CATALOGUE, t)
    expect(c.etapes.map((e) => e.libelle)[0]).toBe('parcours.etape.CADRAGE|Cadrage')
    expect(c.segments.map((s) => s.cle)).toEqual(['segment-AMONT', 'segment-BRANCHE', 'segment-AVAL'])
    expect(couloirs([], p, CATALOGUE, t)).toEqual({ etapes: [], segments: [] })
    const sans = couloirs([{ ...noeuds[0], etape: null, segment: null }], new Map(), CATALOGUE, t)
    expect(sans.etapes[0].libelle).toBe('modelisateur.sansEtape')
    expect(dispositionAuto([{ ...noeuds[0], etape: null, segment: null, couche: null, ordre: null }], CATALOGUE).get(noeuds[0].code)).toEqual({ x: 0, y: 40 })
  })
})

describe('Modélisateur : saisie', () => {
  it('corps d\'enregistrement complet, sans code ni champ calculé ; secret sans valeur ; déclencheur hors événement retiré', () => {
    const n = graphe.noeuds.find((x) => x.code === '19')!
    const s = saisieDe(n)
    expect(s).not.toHaveProperty('code')
    expect(s).not.toHaveProperty('couche')
    expect(s).not.toHaveProperty('successeurs')
    expect(s).toMatchObject({ geste: 'EVENEMENT_EXTERNE', declencheur: 'certification.pv-signed.v1', jalon: 'CERTIFIEE', livrables: [{ code: 'L1' }] })
    expect(saisieDe({ ...n, geste: 'DEPOSER_PREUVE' }).declencheur).toBeNull()
    expect(saisieDe({ ...n, secret: true, valeurReference: 'x' }).valeurReference).toBeNull()
    const vide = saisieDe({ code: 'X' } as never)
    expect(vide).toMatchObject({ libelle: '', consultes: [], livrables: [], cle: false, delaiJours: null })
  })
  it('contrôles de forme : code, requis, V ≠ R, délai, déclencheur, secret, auto-prédécesseur, livrables', () => {
    const o = { ...operationVierge(), code: 'X1', libelle: 'L', etape: 'TESTS' as const, responsable: 'SNA' as const, validateur: 'CONDUCTEUR' as const }
    expect(erreursSaisie(o, true, [])).toEqual({})
    expect(erreursSaisie({ ...o, code: '' }, true, []).code).toBe('commun.champObligatoire')
    expect(erreursSaisie({ ...o, code: '-A' }, true, []).code).toBe('modelisateur.erreur.code')
    expect(erreursSaisie({ ...o, code: 'A'.repeat(41) }, true, []).code).toBe('modelisateur.erreur.code')
    expect(erreursSaisie(o, true, ['X1']).code).toBe('erreur.HOM_OPERATION_CODE_DUPLICATE')
    expect(erreursSaisie(o, false, ['X1']).code).toBeUndefined()
    expect(erreursSaisie({ ...o, validateur: 'SNA' }, false, []).validateur).toBe('modelisateur.erreur.validateurResponsable')
    expect(erreursSaisie({ ...o, validateur: null, responsable: null, etape: null, segment: null, libelle: ' ' }, false, [])).toMatchObject({
      validateur: 'commun.champObligatoire', responsable: 'commun.champObligatoire', etape: 'commun.champObligatoire', segment: 'commun.champObligatoire', libelle: 'commun.champObligatoire' })
    expect(erreursSaisie({ ...o, libelle: 'x'.repeat(301), description: 'x'.repeat(4001), valeurReference: 'x'.repeat(121) }, false, [])).toMatchObject({
      libelle: 'modelisateur.erreur.long', description: 'modelisateur.erreur.long', valeurReference: 'modelisateur.erreur.long' })
    for (const d of [null, -1, 366, 1.5]) expect(erreursSaisie({ ...o, delaiJours: d }, false, []).delaiJours).toBe('modelisateur.erreur.delai')
    expect(erreursSaisie({ ...o, geste: 'EVENEMENT_EXTERNE' }, false, []).declencheur).toBe('modelisateur.erreur.declencheur')
    expect(erreursSaisie({ ...o, secret: true, valeurReference: 'v' }, false, []).valeurReference).toBe('erreur.HOM_OPERATION_SECRET_VALUE')
    expect(erreursSaisie({ ...o, predecesseurs: ['X1'] }, false, []).predecesseurs).toBe('erreur.HOM_OPERATION_SELF_PREDECESSOR')
    const l = { code: 'L1', libelle: 'a', nature: 'LIVRABLE' as const, deposant: 'SNA' as const, typesPieces: [], obligatoire: true }
    expect(erreursSaisie({ ...o, livrables: [l, { ...l }, { ...l, code: ' ', libelle: '', deposant: null }] }, false, [])).toEqual({
      'livrables.1.code': 'modelisateur.erreur.livrableDuplique', 'livrables.2.code': 'commun.champObligatoire', 'livrables.2.libelle': 'commun.champObligatoire', 'livrables.2.deposant': 'commun.champObligatoire' })
  })
  it('onglet de chaque champ ; libellés du catalogue et des opérations du parcours', () => {
    expect(['code', 'livrables.0.code', 'validateur', 'jalon', 'delaiJours'].map(ongletDuChamp)).toEqual(['general', 'livrables', 'roles', 'execution', 'chronologie'])
    expect(libelleEntree(t, 'parcours.fonction', { code: 'SNA', libelle: 'Normes' })).toBe('parcours.fonction.SNA|Normes')
    expect(libelleEntree(t, null, { code: 'SNA', libelle: 'Normes' })).toBe('Normes')
    expect(libelleOperation(t, { rang: '17', libelle: 'Constat' })).toBe('Constat')
    expect(libelleOperation(t, { rang: '17', libelle: null })).toBe('17')
  })
})
