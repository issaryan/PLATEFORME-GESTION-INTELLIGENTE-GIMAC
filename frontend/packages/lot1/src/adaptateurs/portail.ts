import type { CoverageBarProps, DagViewProps, LiaisonProgressProps, OperationState } from '@gimac/design-system'
import type { CoverageValue } from '../modele/commun'
import type { ElementState, Graphe, OperationNodeData, ServiceCouvert } from '../modele/homologation'

// Adaptateurs purs : graphe et couvertures renvoyés par MS-HOMOLOGATION vers DagView et LiaisonProgress.
// Les couches du graphe sont celles du service : DagView les affiche en colonnes, sans rien trier.

export function etatNoeud(n: OperationNodeData): OperationState {
  if (n.enRetard) return 'late'
  if (n.etat === 'CLOSE') return 'closed'
  if (n.etat === 'VERROUILLEE') return 'locked'
  if (n.etat === 'HORS_PERIMETRE') return 'out'
  return 'eligible'
}
const present = (e: ElementState) => e === 'REUNI'

export function versDag(g: Graphe, libellePartie: (p: string) => string): Pick<DagViewProps, 'phases' | 'nodes'> {
  const parId = new Map(g.noeuds.map((n) => [n.id, n]))
  const phases = g.couches.map((couche, i) => ({
    key: `couche-${i}`,
    label: [...new Set(couche.map((id) => parId.get(id)?.phase).filter((x): x is string => !!x))].join(' · '),
  }))
  const couche = new Map(g.couches.flatMap((c, i) => c.map((id) => [id, `couche-${i}`] as const)))
  const nodes = g.noeuds.map((n) => ({
    code: n.code,
    label: n.libelle,
    owner: libellePartie(n.partie),
    state: etatNoeud(n),
    phase: couche.get(n.id) ?? 'couche-0',
    deps: g.aretes.filter((a) => a.to === n.id).map((a) => parId.get(a.from)?.code).filter((x): x is string => !!x),
    elements: { owner: present(n.elements.responsable), proof: present(n.elements.preuve), timestamp: present(n.elements.horodatage), validation: present(n.elements.validation) },
  }))
  return { phases, nodes }
}

/** Palier d'affichage d'un service : 0 hors périmètre, 1 inclus, 2 en essai, 3 certifié, 4 en service. */
export function niveauService(s: ServiceCouvert): CoverageBarProps['level'] {
  if (s.etatPerimetre === 'HORS_PERIMETRE') return 0
  if (s.miseEnService.calculable && s.miseEnService.taux === 100) return 4
  if (s.certification.calculable && s.certification.taux === 100) return 3
  if (s.etatPerimetre === 'EN_COURS' || s.etatPerimetre === 'COUVERT') return 2
  return 1
}

export const taux = (c: CoverageValue): number => (c.calculable && c.taux !== null ? c.taux : 0)

export function versLiaisonProgress(l: { couvertureCertification: CoverageValue; couvertureMiseEnService: CoverageValue; phaseCourante: string; versionNumero: number; services: ServiceCouvert[] }): LiaisonProgressProps {
  return {
    certification: taux(l.couvertureCertification),
    commissioning: taux(l.couvertureMiseEnService),
    phase: l.phaseCourante,
    referential: String(l.versionNumero),
    services: l.services.map((s) => ({ service: s.serviceLibelle, level: niveauService(s) })),
  }
}

/** Couvertures non calculables, avec leur motif, à signaler à côté des anneaux. */
export const couverturesNonCalculables = (l: { couvertureCertification: CoverageValue; couvertureMiseEnService: CoverageValue }) =>
  [l.couvertureCertification, l.couvertureMiseEnService].filter((c) => !c.calculable).map((c) => c.motifNonCalculable ?? '')
