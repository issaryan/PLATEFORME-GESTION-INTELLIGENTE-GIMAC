import { Icon } from '../../icones/Icon/Icon'

export interface CorrelationIdChipProps {
  id?: string
  onCopy?: () => void
}

// Identifiant de corrélation en chasse fixe avec copie (§7.3).
export function CorrelationIdChip(p: CorrelationIdChipProps) {
  return (
    <span className="gm-chip">
      {p.id}
      <button type="button" aria-label="Copier l'identifiant de corrélation" onClick={p.onCopy}><Icon name="copy" size={14} /></button>
    </span>
  )
}
