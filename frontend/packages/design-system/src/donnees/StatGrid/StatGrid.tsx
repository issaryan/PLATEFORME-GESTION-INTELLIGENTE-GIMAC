import { StatCard, type StatCardProps } from '../../affichage/StatCard/StatCard'

export interface StatGridProps {
  /** Nom du groupe d'indicateurs, annoncé aux technologies d'assistance. */
  label: string
  items: (StatCardProps & { key: string })[]
}

// Grille d'indicateurs d'un tableau de bord (D-58, D-63) : StatCard alignées à hauteur égale, renvoyées
// à la ligne sous la largeur disponible. Pour un flux temps réel, LiveStatGroup.
export function StatGrid(p: StatGridProps) {
  return (
    <div className="gm-stat-grid" role="group" aria-label={p.label}>
      {p.items.map(({ key, ...s }) => (
        <StatCard key={key} {...s} />
      ))}
    </div>
  )
}
