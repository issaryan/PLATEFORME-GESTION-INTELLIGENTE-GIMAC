import { cx } from '../../lib/cx'
import { Icon } from '../../icones/Icon/Icon'
import type { IconName } from '../../icones/icons'
import type { Status } from '../../lib/types'
import { useLibelle } from '../../i18n/LangueProvider'
import type { CleLibelle } from '../../i18n/libelles'

export interface MaskingResultProps {
  result: 'accepted' | 'masked' | 'rejected'
  /** Nombre de séquences masquées. */
  count?: number
  text: string
}

const MAP: Record<MaskingResultProps['result'], [Status, CleLibelle, IconName]> = {
  accepted: ['ok', 'masquage.acceptee', 'circle-check'],
  masked: ['warn', 'masquage.masquee', 'eye'],
  rejected: ['crit', 'masquage.refusee', 'octagon-alert'],
}

// Résultat du contrôle de masquage renvoyé par le service (§7.3).
export function MaskingResult(p: MaskingResultProps) {
  const t = useLibelle()
  const m = MAP[p.result] || MAP.accepted
  return (
    <div className={cx('gm-inline', 'gm-banner-' + m[0])} style={{ borderLeft: 'var(--gm-border-4) solid' }} role={p.result === 'rejected' ? 'alert' : 'status'}>
      <Icon name={m[2]} size={20} />
      <div className="gm-banner-text">
        <div className="gm-banner-title">
          {t('masquage.titre', { resultat: t(m[1]) })}
          {p.result === 'masked' && p.count ? t(p.count > 1 ? 'masquage.sequences' : 'masquage.sequence', { n: p.count }) : ''}
        </div>
        <div>{p.text}</div>
      </div>
    </div>
  )
}
