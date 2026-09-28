export interface DateTimeProps {
  local: string
  utc?: string
  iso?: string
  relative?: string
}

// Horodatage local avec l'UTC en infobulle (§7.3).
export function DateTime(p: DateTimeProps) {
  return <time className="gm-num" dateTime={p.iso} title={p.utc ? p.utc + ' UTC' : undefined}>{p.relative || p.local}</time>
}
