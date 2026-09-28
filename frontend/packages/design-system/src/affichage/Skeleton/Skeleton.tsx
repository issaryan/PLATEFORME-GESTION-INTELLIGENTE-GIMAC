export interface SkeletonProps {
  width?: number | string
  height?: number | string
}

// Bloc de chargement animé en opacité (§7.3).
export function Skeleton(p: SkeletonProps) {
  return <div className="gm-skeleton" style={{ width: p.width || '100%', height: p.height || 14 }} aria-hidden="true" />
}
