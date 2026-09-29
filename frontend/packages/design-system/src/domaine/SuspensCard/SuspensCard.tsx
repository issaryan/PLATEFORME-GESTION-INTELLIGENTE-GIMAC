import type { Status } from '../../lib/types'
import { Icon } from '../../icones/Icon/Icon'
import { Tag } from '../../affichage/Tag/Tag'
import { StatusBadge } from '../../affichage/StatusBadge/StatusBadge'
import { KeyValueList } from '../../affichage/KeyValueList/KeyValueList'
import { Money } from '../../affichage/Money/Money'
import { Button } from '../../actions/Button/Button'
import { useLibelle } from '../../i18n/LangueProvider'

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
  const t = useLibelle()
  const age: [Status, string] = p.ageDays >= 30 ? ['crit', 'J+30'] : p.ageDays >= 5 ? ['warn', 'J+5'] : ['info', 'J+1']
  return (
    <article className="gm-card gm-suspens" aria-label={p.id}>
      <div className="gm-card-header">
        <div className="gm-row">
          <span className="gm-mono">{p.id}</span>
          <Tag>{p.type}</Tag>
        </div>
        <div className="gm-row">
          <StatusBadge status={age[0]} label={t('suspens.anciennete', { palier: age[1], jours: p.ageDays })} icon="clock" />
          <StatusBadge status={p.status} label={p.statusLabel} />
        </div>
      </div>
      <div className="gm-card-body">
        <KeyValueList
          items={[
            { key: t('suspens.journee'), value: p.day },
            { key: t('commun.institution'), value: p.institution },
            { key: t('suspens.pivots'), value: <span className="gm-mono">{(p.pivots || []).join(' · ')}</span> },
            { key: t('commun.montant'), value: <Money amount={p.amount} /> },
            { key: t('elements.responsable'), value: p.owner },
            {
              key: t('suspens.attestation'),
              value: p.attestation ? (
                <span className="gm-row">
                  <Icon name="file-text" />
                  {p.attestation}
                </span>
              ) : (
                <span className="gm-field-help">{t('suspens.nonDeposee')}</span>
              ),
            },
          ]}
        />
      </div>
      <div className="gm-card-footer">
        <span className="gm-field-help" style={{ marginRight: 'auto' }}>{t('suspens.nonReinjection')}</span>
        <Button size="sm" variant="secondary">{t('suspens.contester')}</Button>
        <Button size="sm" variant="primary" icon="pen-line" disabled={!p.attestation} disabledReason={t('suspens.attestationRequise')}>{t('suspens.valider')}</Button>
      </div>
    </article>
  )
}
