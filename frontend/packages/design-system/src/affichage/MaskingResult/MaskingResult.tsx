import { cx } from '../../lib/cx'
import { Icon } from '../../icones/Icon/Icon'
import type { IconName } from '../../icones/icons'
import type { Status } from '../../lib/types'

export interface MaskingResultProps {
  result: 'accepted' | 'masked' | 'rejected'
  /** Nombre de séquences masquées. */
  count?: number
  text: string
}

const MAP: Record<MaskingResultProps['result'], [Status, string, IconName]> = {
  accepted: ['ok', 'acceptée', 'circle-check'],
  masked: ['warn', 'masquée', 'eye'],
  rejected: ['crit', 'refusée', 'octagon-alert'],
}

// Résultat du contrôle de masquage renvoyé par le service (§7.3).
export function MaskingResult(p: MaskingResultProps) {
  const m = MAP[p.result] || MAP.accepted
  return (
    <div className={cx('gm-inline', 'gm-banner-' + m[0])} style={{ borderLeft: 'var(--gm-border-4) solid' }} role={p.result === 'rejected' ? 'alert' : 'status'}>
      <Icon name={m[2]} size={20} />
      <div className="gm-banner-text">
        <div className="gm-banner-title">
          Contrôle de masquage : {m[1]}
          {p.result === 'masked' && p.count ? ' (' + p.count + ' séquence' + (p.count > 1 ? 's' : '') + ' masquée' + (p.count > 1 ? 's' : '') + ')' : ''}
        </div>
        <div>{p.text}</div>
      </div>
    </div>
  )
}
