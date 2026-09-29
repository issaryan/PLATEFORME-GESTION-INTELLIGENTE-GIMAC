import type { CoverageBarProps, DagNode, DagViewProps, OperationState } from '@gimac/design-system'
import type { CoverageValue } from '../modele/commun'
import type { ElementState, Graphe, OperationNodeData, ServiceCouvert } from '../modele/homologation'

// Adaptateurs purs : réponses du service → propriétés des composants du catalogue. Aucune règle métier :
// états, couches, taux et transitions viennent du service ; ici, seulement la mise en forme.

type Traduire = (cle: string, valeurs?: Record<string, unknown>) => string

/** État d'une opération (dictionnaire du service) → variante visuelle du nœud du graphe. */
export function etatNoeud(n: Pick<OperationNodeData, 'etat' | 'enRetard'>): OperationState {
  if (n.enRetard) return 'late'
  if (n.etat === 'CLOSE') return 'closed'
  if (n.etat === 'VERROUILLEE') return 'locked'
  if (n.etat === 'HORS_PERIMETRE') return 'out'
  return 'eligible'
}

const reuni = (e: ElementState) => e === 'REUNI'

/**
 * Graphe du service → DagView. Les colonnes sont les couches topologiques fournies par le service
 * (GET /liaisons/{id}/graphe), dans leur ordre : DagView ne trie rien.
 */
export function grapheVersDagView(g: Graphe, t: Traduire): DagViewProps {
  const couche = new Map<string, number>()
  g.couches.forEach((c, i) => c.forEach((id) => couche.set(id, i)))
  const code = new Map(g.noeuds.map((n) => [n.id, n.code]))
  const phases = g.couches.map((c, i) => {
    const libelles = [...new Set(c.map((id) => g.noeuds.find((n) => n.id === id)?.phase).filter((p): p is string => !!p))]
    return { key: `c${i}`, label: t('graphe.couche', { n: i + 1, phase: libelles.join(', ') }) }
  })
  const nodes: DagNode[] = g.noeuds.map((n) => ({
    code: n.code,
    label: n.libelle,
    owner: t(`partie.${n.partie}`),
    state: etatNoeud(n),
    phase: `c${couche.get(n.id) ?? 0}`,
    deps: g.aretes.filter((a) => a.to === n.id).map((a) => code.get(a.from) ?? a.from),
    elements: { owner: reuni(n.elements.responsable), proof: reuni(n.elements.preuve), timestamp: reuni(n.elements.horodatage), validation: reuni(n.elements.validation) },
  }))
  return { phases, nodes }
}

/** Taux calculé par le service ; null quand il n'est pas calculable (aucun dénominateur). */
export const taux = (c: CoverageValue): number | null => (c.calculable ? c.taux : null)

/**
 * Service couvert → barre de couverture à quatre paliers (inclus, en essai, certifié, en service).
 * Le palier traduit l'état de périmètre et les taux déjà calculés par le service.
 */
export function niveauCouverture(s: ServiceCouvert): CoverageBarProps['level'] {
  if (s.etatPerimetre === 'HORS_PERIMETRE') return 0
  if (s.miseEnService.calculable && s.miseEnService.taux === 100) return 4
  if (s.certification.calculable && s.certification.taux === 100) return 3
  if (s.etatPerimetre === 'EN_COURS' || s.etatPerimetre === 'COUVERT') return 2
  return 1
}

export const servicesVersCoverage = (services: ServiceCouvert[]): CoverageBarProps[] =>
  services.map((s) => ({ service: s.serviceLibelle, level: niveauCouverture(s) }))
