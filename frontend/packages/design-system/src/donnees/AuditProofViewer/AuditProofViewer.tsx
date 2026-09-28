import { cx } from '../../lib/cx'
import { Icon } from '../../icones/Icon/Icon'
import { Button } from '../../actions/Button/Button'
import { Hash } from '../../affichage/Hash/Hash'
import { CorrelationIdChip } from '../../affichage/CorrelationIdChip/CorrelationIdChip'

export interface AuditProofViewerProps {
  eventId: string
  leaf: string
  path: { side: 'left' | 'right'; hash: string }[]
  period: string
  root: string
  publication: string
  tst: string
  tstAt: string
  verdict: 'ok' | 'mismatch'
  correlationId: string
}

// Visualisation d'une preuve d'inclusion du registre d'audit (§7.6, CON-04, gabarit G9).
export function AuditProofViewer(p: AuditProofViewerProps) {
  const ok = p.verdict === 'ok'
  return (
    <div className="gm-proof">
      <div className="gm-proof-col">
        <div className="gm-proof-step">
          <span className="gm-proof-k">Événement (feuille)</span>
          <span className="gm-mono">{p.eventId}</span>
          <Hash value={p.leaf} />
        </div>
        {(p.path || []).map((s, i) => (
          <div key={i} className="gm-proof-step gm-proof-path">
            <span className="gm-proof-k">{"Chemin d'audit, niveau " + (i + 1) + ' (' + (s.side === 'left' ? 'gauche' : 'droite') + ')'}</span>
            <Hash value={s.hash} />
          </div>
        ))}
        <div className="gm-proof-step">
          <span className="gm-proof-k">{'Racine de période ' + p.period}</span>
          <Hash value={p.root} />
        </div>
        <div className="gm-proof-step">
          <span className="gm-proof-k">Jeton d'horodatage</span>
          <Hash value={p.tst} />
          <span className="gm-field-help">{p.tstAt}</span>
        </div>
      </div>
      <div className={cx('gm-proof-verdict', ok ? 'gm-banner-info' : 'gm-banner-crit')}>
        <Icon name={ok ? 'shield-check' : 'octagon-alert'} size={24} />
        <div>
          <div className="gm-banner-title">{ok ? "Preuve d'inclusion vérifiée" : "Preuve d'inclusion non concordante"}</div>
          <div className="gm-field-help">
            {ok ? "La feuille recalculée par le chemin d'audit correspond à la racine publiée " + p.publication + '.' : "La racine recalculée diffère de la racine publiée. Ouvrez un ticket d'anomalie."}
          </div>
          <div style={{ marginTop: 'var(--gm-space-2)' }}>
            <CorrelationIdChip id={p.correlationId} />
          </div>
        </div>
        <div className="gm-row" style={{ marginLeft: 'auto' }}>
          <Button size="sm" variant="secondary" icon="download">Télécharger la preuve</Button>
          <Button size="sm" variant="primary" icon="shield-check">Revérifier</Button>
        </div>
      </div>
    </div>
  )
}
