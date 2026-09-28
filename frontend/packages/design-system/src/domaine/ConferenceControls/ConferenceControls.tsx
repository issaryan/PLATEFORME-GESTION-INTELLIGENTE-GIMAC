import { Icon } from '../../icones/Icon/Icon'
import type { IconName } from '../../icones/icons'
import { StatusBadge } from '../../affichage/StatusBadge/StatusBadge'
import { Button } from '../../actions/Button/Button'

export interface ConferenceControlsProps {
  mic?: boolean
  camera?: boolean
  sharing?: boolean
  participants: number
  /** Enregistrement en cours, possible seulement après consentement recueilli. */
  recording?: boolean
}

// Contrôle coupé : variante danger avec libellé explicite, jamais la couleur seule.
function ctl(on: boolean, icon: IconName, offIcon: IconName, label: string) {
  return <Button variant={on ? 'secondary' : 'danger'} icon={on ? icon : offIcon || icon}>{label + (on ? '' : ' (coupé)')}</Button>
}

// Commandes de la conférence audio et vidéo locale (§7.7).
export function ConferenceControls(p: ConferenceControlsProps) {
  return (
    <div className="gm-conf">
      <div className="gm-row">
        {ctl(p.mic !== false, 'mic', 'mic', 'Micro')}
        {ctl(p.camera !== false, 'camera', 'camera', 'Caméra')}
        <Button variant={p.sharing ? 'primary' : 'secondary'} icon="monitor">{p.sharing ? 'Arrêter le partage' : "Partager l'écran"}</Button>
      </div>
      <div className="gm-row" style={{ marginLeft: 'auto' }}>
        <span className="gm-row gm-field-help">
          <Icon name="users" /> {p.participants + ' participants'}
        </span>
        <StatusBadge status={p.recording ? 'crit' : 'neutral'} label={p.recording ? 'enregistrement en cours, consentement recueilli' : 'non enregistrée'} icon={p.recording ? 'video' : null} />
        <Button variant="danger" icon="phone-off">Quitter la conférence</Button>
      </div>
    </div>
  )
}
