import { Icon } from '../../icones/Icon/Icon'
import { StatusBadge } from '../StatusBadge/StatusBadge'
import { useLibelle } from '../../i18n/LangueProvider'

export interface HashProps {
  value?: string
  onCopy?: () => void
  /** Demande la vérification de l'empreinte au service ; affiche le bouton « Vérifier l'empreinte ». */
  onVerify?: () => void
  /** Résultat de la vérification : true concordante, false divergente, null ou absent non vérifiée. */
  verified?: boolean | null
}

// Empreinte SHA-256 tronquée (8 premiers et 8 derniers caractères) avec copie (§7.3).
export function Hash(p: HashProps) {
  const t = useLibelle()
  const v = p.value || ''
  const short = v.length > 20 ? v.slice(0, 8) + '…' + v.slice(-8) : v
  const puce = (
    <span className="gm-chip" title={v}>
      <Icon name="lock" size={14} />
      {short}
      <button type="button" aria-label={t('hash.copier')} onClick={p.onCopy}><Icon name="copy" size={14} /></button>
      {p.onVerify ? (
        <button type="button" aria-label={t('hash.verifier')} onClick={p.onVerify}><Icon name="shield-check" size={14} /></button>
      ) : null}
    </span>
  )
  if (p.verified === undefined || p.verified === null) return puce
  return (
    <span className="gm-row" style={{ display: 'inline-flex' }}>
      {puce}
      <StatusBadge status={p.verified ? 'ok' : 'crit'} label={p.verified ? t('hash.verifiee') : t('hash.divergente')} />
    </span>
  )
}
