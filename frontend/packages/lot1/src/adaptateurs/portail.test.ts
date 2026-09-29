import { describe, expect, it } from 'vitest'
import type { Graphe, ServiceCouvert } from '../modele/homologation'
import { couverturesNonCalculables, niveauService, versDag, versLiaisonProgress } from './portail'

const el = { responsable: 'REUNI', preuve: 'REUNI', horodatage: 'EN_ATTENTE', validation: 'MANQUANT' } as const
const graphe: Graphe = {
  noeuds: [
    { id: 'n1', code: 'OP-1', libelle: 'Un', phase: 'Raccordement', partie: 'PARTICIPANT', etat: 'CLOSE', elements: el, enRetard: false, aRevoir: false },
    { id: 'n2', code: 'OP-2', libelle: 'Deux', phase: 'Certification', partie: 'DES', etat: 'ELIGIBLE', elements: el, enRetard: true, aRevoir: false },
    { id: 'n3', code: 'OP-3', libelle: 'Trois', phase: 'Certification', partie: 'DES', etat: 'VERROUILLEE', elements: el, enRetard: false, aRevoir: false },
  ],
  aretes: [{ from: 'n1', to: 'n2' }, { from: 'n1', to: 'n3' }],
  couches: [['n1'], ['n2', 'n3']],
}
const cov = (taux: number | null, calculable = true) => ({ taux, numerateur: 0, denominateur: 0, calculable, motifNonCalculable: calculable ? undefined : 'aucun service' })
const service = (etatPerimetre: string, c: number | null, m: number | null): ServiceCouvert => ({ serviceCode: 'S', serviceLibelle: 'Retrait', etatPerimetre, certification: cov(c), miseEnService: cov(m) })

describe('adaptateurs du portail', () => {
  it('les couches du service deviennent les colonnes du graphe, sans tri', () => {
    const d = versDag(graphe, (p) => p)
    expect(d.phases.map((p) => p.label)).toEqual(['Raccordement', 'Certification'])
    expect(d.nodes.map((n) => [n.code, n.phase, n.state])).toEqual([['OP-1', 'couche-0', 'closed'], ['OP-2', 'couche-1', 'late'], ['OP-3', 'couche-1', 'locked']])
    expect(d.nodes[1].deps).toEqual(['OP-1'])
    expect(d.nodes[0].elements).toEqual({ owner: true, proof: true, timestamp: false, validation: false })
  })
  it('paliers de couverture par service', () => {
    expect(niveauService(service('HORS_PERIMETRE', 0, 0))).toBe(0)
    expect(niveauService(service('A_ENGAGER', 0, 0))).toBe(1)
    expect(niveauService(service('EN_COURS', 50, 0))).toBe(2)
    expect(niveauService(service('COUVERT', 100, 50))).toBe(3)
    expect(niveauService(service('COUVERT', 100, 100))).toBe(4)
  })
  it('une couverture non calculable n\'est jamais affichée comme un taux inventé', () => {
    const l = { couvertureCertification: cov(null, false), couvertureMiseEnService: cov(40), phaseCourante: 'Certification', versionNumero: 3, services: [] }
    expect(versLiaisonProgress(l)).toMatchObject({ certification: 0, commissioning: 40, referential: '3' })
    expect(couverturesNonCalculables(l)).toEqual(['aucun service'])
  })
})
