import { Skeleton } from '@gimac/design-system'

export default function Apercu() {
  return (
    <div className="gm-col" style={{ gap: 'var(--gm-space-2)', maxWidth: 480 }}>
      <Skeleton width="40%" height={22} />
      <Skeleton />
      <Skeleton width="85%" />
      <Skeleton width="60%" />
    </div>
  )
}
