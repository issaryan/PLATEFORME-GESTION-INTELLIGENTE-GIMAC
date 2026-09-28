import { Icon } from '../../icones/Icon/Icon'

export interface HashProps {
  value?: string
  onCopy?: () => void
}

// Empreinte SHA-256 tronquée (8 premiers et 8 derniers caractères) avec copie (§7.3).
export function Hash(p: HashProps) {
  const v = p.value || ''
  const short = v.length > 20 ? v.slice(0, 8) + '…' + v.slice(-8) : v
  return (
    <span className="gm-chip" title={v}>
      <Icon name="lock" size={14} />
      {short}
      <button type="button" aria-label="Copier l'empreinte" onClick={p.onCopy}><Icon name="copy" size={14} /></button>
    </span>
  )
}
