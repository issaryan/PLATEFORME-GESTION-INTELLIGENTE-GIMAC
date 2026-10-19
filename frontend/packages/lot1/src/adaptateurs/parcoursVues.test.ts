import { describe, expect, it } from 'vitest'
import type { DossierLiaison, OperationParcours } from '../modele/parcours'
import { etatAffiche, etapesDuDossier, grapheDuDossier, planningDuDossier, semaineIso } from './parcoursVues'

const t = (cle: string) => cle
const op = (rang: string, etape: string, etat: OperationParcours['etat'], plus: Partial<OperationParcours> = {}) =>
  ({ id: rang, liaisonId: 'L', rang, etape, etapeCode: etape, segment: 'BRANCHE', plateforme: null, grain: 'OPERATION', etat,
    raci: { R: { id: 'p', nom: 'Personne fictive', fonction: 'CONDUCTEUR' }, C: [], V: null, I: [], E: [] },
    echeance: null, sla: null, faiteLe: null, preuve: null, cle: false, secret: false, valeurReference: false, oubliSansSymptome: false, pivot: false,
    predecesseursNonFaits: [], actionsPermises: [], transitionsPermises: [], libelle: `Opération ${rang}`, ...plus }) as unknown as OperationParcours

const etape = (code: string, ordre: number, plus: Record<string, unknown> = {}) => ({ code, libelle: `Étape ${code}`, ordre, etat: 'EN_COURS', actionsFaites: 0, actionsTotal: 0, responsable: null, validateur: null, debut: null, closeLe: null, echeance: null, sla: null, livrables: [], ...plus })
const dossier = (plus: Partial<DossierLiaison> = {}) =>
  ({ id: 'L', dateDemarrage: '2026-09-01', dateCibleProduction: '2026-12-15', etapes: [{ code: 'CADRAGE' }, { code: 'TESTS' }],
    etapesVersion: [etape('TESTS', 2), etape('CADRAGE', 1)], ...plus }) as unknown as DossierLiaison

describe('vues Graphe et Planning du dossier (D-112)', () => {
  it('ordonne les étapes de la version, sinon celles du parcours', () => {
    expect(etapesDuDossier(dossier(), t).map((e) => e.code)).toEqual(['CADRAGE', 'TESTS'])
    expect(etapesDuDossier(dossier({ etapesVersion: [] }), t).map((e) => e.libelle)).toEqual(['parcours.etape.CADRAGE', 'parcours.etape.TESTS'])
  })
  it('traduit les états sans règle métier', () => {
    expect(etatAffiche(op('1', 'CADRAGE', 'FAITE'), '2026-10-19')).toBe('closed')
    expect(etatAffiche(op('1', 'CADRAGE', 'SANS_OBJET'), '2026-10-19')).toBe('out')
    expect(etatAffiche(op('1', 'CADRAGE', 'VERROUILLEE'), '2026-10-19')).toBe('locked')
    expect(etatAffiche(op('1', 'CADRAGE', 'A_FAIRE', { echeance: '2026-10-01' }), '2026-10-19')).toBe('late')
    expect(etatAffiche(op('1', 'CADRAGE', 'A_FAIRE', { sla: 'DEPASSE' }), '2026-10-19')).toBe('late')
    expect(etatAffiche(op('1', 'CADRAGE', 'EN_COURS'), '2026-10-19')).toBe('eligible')
  })
  it('dispose le graphe par étape, avec tous les prédécesseurs et un couloir « sans étape »', () => {
    const g = grapheDuDossier(dossier(), [
      op('3', 'CADRAGE', 'FAITE', { faiteLe: '2026-09-10', predecesseurs: [] }),
      op('16', 'TESTS', 'A_FAIRE', { predecesseurs: ['3', '99'] }),
      op('20', 'AUTRE', 'A_FAIRE', { raci: { R: null, C: [], V: null, I: [], E: [] } as never, libelle: null }),
    ], t, '2026-10-19')
    expect(g.phases.map((p) => p.key)).toEqual(['CADRAGE', 'TESTS', ''])
    const n16 = g.nodes.find((n) => n.code === '16')!
    expect(n16.deps).toEqual(['3'])
    const n20 = g.nodes.find((n) => n.code === '20')!
    expect(n20.phase).toBe('')
    expect(n20.owner).toBe('parcoursVues.aDesigner')
    expect(g.nodes.find((n) => n.code === '3')!.elements).toEqual({ owner: true, proof: false, timestamp: true, validation: true })
  })
  it('prend les prédécesseurs non faits quand le service ne sert pas encore la liste complète', () => {
    const g = grapheDuDossier(dossier(), [op('3', 'CADRAGE', 'A_FAIRE'), op('4', 'CADRAGE', 'VERROUILLEE', { predecesseursNonFaits: ['3'] })], t, '2026-10-19')
    expect(g.nodes.find((n) => n.code === '4')!.deps).toEqual(['3'])
  })
  it('construit le planning des étapes : dates, avancement, retard et ligne du jour', () => {
    const d = dossier({ etapesVersion: [
      etape('CADRAGE', 1, { debut: '2026-09-01', closeLe: '2026-09-14', actionsFaites: 3, actionsTotal: 3, responsable: { nom: 'Responsable fictif' } }),
      etape('TESTS', 2, { debut: '2026-09-20', echeance: '2026-10-10', actionsFaites: 1, actionsTotal: 4 }),
    ] as never })
    const p = planningDuDossier(d, [], t, '2026-10-19')
    expect(p.vide).toBe(false)
    expect(p.start).toBe('01/09')
    expect(p.milestones[0]).toMatchObject({ key: 'CADRAGE', start: 0, duration: 14, done: 100, critical: false, owner: 'Responsable fictif' })
    expect(p.milestones[1]).toMatchObject({ key: 'TESTS', start: 19, done: 25, critical: true, owner: 'parcoursVues.aDesigner' })
    expect(p.today).toBe(48)
    expect(p.weeks![0]).toEqual({ day: 0, label: 'S36' })
  })
  it('déduit les dates des opérations, et signale un planning vide', () => {
    const d = dossier({ etapesVersion: [], dateCibleProduction: null })
    const p = planningDuDossier(d, [op('3', 'CADRAGE', 'FAITE', { faiteLe: '2026-09-05', echeance: '2026-09-08' }), op('4', 'CADRAGE', 'SANS_OBJET', { echeance: '2026-12-30' })], t, '2026-08-01')
    expect(p.milestones).toHaveLength(1)
    expect(p.milestones[0]).toMatchObject({ start: 4, duration: 4, done: 100 })
    expect(p.today).toBeUndefined()
    expect(planningDuDossier(d, [], t, '2026-10-19').vide).toBe(true)
  })
  it('numérote les semaines selon ISO 8601', () => {
    expect(semaineIso('2026-01-01')).toBe(1)
    expect(semaineIso('2026-10-19')).toBe(43)
    expect(semaineIso('2027-01-03')).toBe(53)
  })
})
