import type { Status } from '../../lib/types'
import { Icon } from '../../icones/Icon/Icon'
import { Tag } from '../../affichage/Tag/Tag'
import { StatusBadge } from '../../affichage/StatusBadge/StatusBadge'
import { KeyValueList } from '../../affichage/KeyValueList/KeyValueList'
import { Money } from '../../affichage/Money/Money'
import { Button } from '../../actions/Button/Button'

export interface SuspensCardProps {
  id: string
  type: string
  /** Ancienneté en jours : J+1, J+5, J+30. */
  ageDays: number
  status: Status
  statusLabel: string
  day: string
  institution: string
  pivots?: string[]
  amount: number
  owner: string
  attestation?: string
}

// Carte d'un suspens de rapprochement (§7.7, DES-22).
export function SuspensCard(p: SuspensCardProps) {
  const age: [Status, string] = p.ageDays >= 30 ? ['crit', 'J+30'] : p.ageDays >= 5 ? ['warn', 'J+5'] : ['info', 'J+1']
  return (
    <article className="gm-card gm-suspens" aria-label={p.id}>
      <div className="gm-card-header">
        <div className="gm-row">
          <span className="gm-mono">{p.id}</span>
          <Tag>{p.type}</Tag>
        </div>
        <div className="gm-row">
          <StatusBadge status={age[0]} label={'ancienneté ' + age[1] + ' (' + p.ageDays + ' j)'} icon="clock" />
          <StatusBadge status={p.status} label={p.statusLabel} />
        </div>
      </div>
      <div className="gm-card-body">
        <KeyValueList
          items={[
            { key: 'Journée', value: p.day },
            { key: 'Institution', value: p.institution },
            { key: 'Identifiants pivots', value: <span className="gm-mono">{(p.pivots || []).join(' · ')}</span> },
            { key: 'Montant', value: <Money amount={p.amount} /> },
            { key: 'Responsable', value: p.owner },
            {
              key: 'Attestation',
              value: p.attestation ? (
                <span className="gm-row">
                  <Icon name="file-text" />
                  {p.attestation}
                </span>
              ) : (
                <span className="gm-field-help">non déposée</span>
              ),
            },
          ]}
        />
      </div>
      <div className="gm-card-footer">
        <span className="gm-field-help" style={{ marginRight: 'auto' }}>La plateforme ne réinjecte rien : la régularisation est attestée, jamais exécutée ici.</span>
        <Button size="sm" variant="secondary">Contester (litige)</Button>
        <Button size="sm" variant="primary" icon="pen-line" disabled={!p.attestation} disabledReason="Une attestation doit d'abord être déposée">Valider la régularisation</Button>
      </div>
    </article>
  )
}
