import { Icon } from '../../icones/Icon/Icon'
import { useLibelle } from '../../i18n/LangueProvider'

export interface CorrelationIdChipProps {
  id?: string
  onCopy?: () => void
}

// Identifiant de corrélation en chasse fixe avec copie (§7.3).
export function CorrelationIdChip(p: CorrelationIdChipProps) {
  const t = useLibelle()
  return (
    <span className="gm-chip">
      <span data-testid="correlation-id">{p.id}</span>
      <button type="button" aria-label={t('correlation.copier')} onClick={p.onCopy}><Icon name="copy" size={14} /></button>
    </span>
  )
}
