import { describe, expect, it } from 'vitest'
import type { Graphe, ServiceCouvert } from '../modele/homologation'
import { etatNoeud, grapheVersDagView, niveauCouverture, taux } from './liaison'

const t = (cle: string, v?: Record<string, unknown>) => (v ? `${cle}:${JSON.stringify(v)}` : cle)
const el = { responsable: 'REUNI', preuve: 'REUNI', horodatage: 'EN_ATTENTE', validation: 'MANQUANT' } as const

describe('DES-09 adaptateurs de la liaison', () => {
  it('rend les couches du service comme colonnes, dans leur ordre, et les arêtes comme dépendances', () => {
    const g: Graphe = {
      noeuds: [
        { id: 'a', code: 'OP-1', libelle: 'A', phase: 'Raccordement', partie: 'DES', etat: 'CLOSE', elements: el, enRetard: false, aRevoir: false },
        { id: 'b', code: 'OP-2', libelle: 'B', phase: 'Certification', partie: 'PARTICIPANT', etat: 'ELIGIBLE', elements: el, enRetard: true, aRevoir: false },
      ],
      aretes: [{ from: 'a', to: 'b' }],
      couches: [['a'], ['b']],
    }
    const d = grapheVersDagView(g, t)
    expect(d.phases.map((p) => p.key)).toEqual(['c0', 'c1'])
    expect(d.nodes[1]).toMatchObject({ code: 'OP-2', phase: 'c1', deps: ['OP-1'], state: 'late', owner: 'partie.PARTICIPANT' })
    expect(d.nodes[0].elements).toEqual({ owner: true, proof: true, timestamp: false, validation: false })
  })
  it('traduit les états sans rien recalculer', () => {
    expect(etatNoeud({ etat: 'CLOSE', enRetard: false })).toBe('closed')
    expect(etatNoeud({ etat: 'VERROUILLEE', enRetard: false })).toBe('locked')
    expect(etatNoeud({ etat: 'HORS_PERIMETRE', enRetard: false })).toBe('out')
    expect(etatNoeud({ etat: 'PREUVE_DEPOSEE', enRetard: false })).toBe('eligible')
  })
  it('garde un taux non calculable à null et range chaque service sur son palier', () => {
    const v = (tx: number | null, c = true) => ({ taux: tx, numerateur: 0, denominateur: c ? 1 : 0, calculable: c })
    expect(taux(v(null, false))).toBeNull()
    expect(taux(v(40))).toBe(40)
    const s = (etat: string, cert: number, mes: number): ServiceCouvert => ({ serviceCode: 'X', serviceLibelle: 'X', etatPerimetre: etat, certification: v(cert), miseEnService: v(mes) })
    expect(niveauCouverture(s('HORS_PERIMETRE', 100, 100))).toBe(0)
    expect(niveauCouverture(s('A_ENGAGER', 0, 0))).toBe(1)
    expect(niveauCouverture(s('EN_COURS', 50, 0))).toBe(2)
    expect(niveauCouverture(s('COUVERT', 100, 50))).toBe(3)
    expect(niveauCouverture(s('COUVERT', 100, 100))).toBe(4)
  })
})
