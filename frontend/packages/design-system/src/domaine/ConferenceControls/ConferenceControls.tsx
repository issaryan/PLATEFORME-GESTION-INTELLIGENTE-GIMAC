import { Icon } from '../../icones/Icon/Icon'
import type { IconName } from '../../icones/icons'
import { StatusBadge } from '../../affichage/StatusBadge/StatusBadge'
import { Button } from '../../actions/Button/Button'
import { useLibelle } from '../../i18n/LangueProvider'

export interface ConferenceControlsProps {
  mic?: boolean
  camera?: boolean
  sharing?: boolean
  participants: number
  /** Enregistrement en cours, possible seulement après consentement recueilli. */
  recording?: boolean
}

// Contrôle coupé : variante danger avec libellé explicite, jamais la couleur seule.
function ctl(on: boolean, icon: IconName, offIcon: IconName, label: string, coupe: string) {
  return <Button variant={on ? 'secondary' : 'danger'} icon={on ? icon : offIcon || icon}>{label + (on ? '' : coupe)}</Button>
}

// Commandes de la conférence audio et vidéo locale (§7.7).
export function ConferenceControls(p: ConferenceControlsProps) {
  const t = useLibelle()
  return (
    <div className="gm-conf">
      <div className="gm-row">
        {ctl(p.mic !== false, 'mic', 'mic', t('conference.micro'), t('conference.coupe'))}
        {ctl(p.camera !== false, 'camera', 'camera', t('conference.camera'), t('conference.coupe'))}
        <Button variant={p.sharing ? 'primary' : 'secondary'} icon="monitor">{p.sharing ? t('conference.arreterPartage') : t('conference.partager')}</Button>
      </div>
      <div className="gm-row" style={{ marginLeft: 'auto' }}>
        <span className="gm-row gm-field-help">
          <Icon name="users" /> {t('conference.participants', { n: p.participants })}
        </span>
        <StatusBadge status={p.recording ? 'crit' : 'neutral'} label={p.recording ? t('conference.enregistrement') : t('conference.nonEnregistree')} icon={p.recording ? 'video' : null} />
        <Button variant="danger" icon="phone-off">{t('conference.quitter')}</Button>
      </div>
    </div>
  )
}
