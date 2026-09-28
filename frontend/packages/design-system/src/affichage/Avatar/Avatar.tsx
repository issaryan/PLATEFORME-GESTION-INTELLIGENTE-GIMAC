export interface AvatarProps {
  name: string
}

// Initiales sur cercle de 32 px, jamais de photo (§7.3).
export function Avatar(p: AvatarProps) {
  const init = (p.name || '').split(/\s+/).map((w) => w[0]).join('').slice(0, 2).toUpperCase()
  return <span className="gm-avatar" role="img" aria-label={p.name}>{init}</span>
}
