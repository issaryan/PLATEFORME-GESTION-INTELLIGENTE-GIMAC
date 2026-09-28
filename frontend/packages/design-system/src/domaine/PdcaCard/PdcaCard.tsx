import { cx } from '../../lib/cx'
import type { Status } from '../../lib/types'
import { Icon } from '../../icones/Icon/Icon'
import { StatusBadge } from '../../affichage/StatusBadge/StatusBadge'
import { KeyValueList } from '../../affichage/KeyValueList/KeyValueList'
import { Card } from '../../conteneurs/Card/Card'
import { InlineAlert } from '../../conteneurs/InlineAlert/InlineAlert'
import { Button } from '../../actions/Button/Button'

export type PdcaPhase = 'plan' | 'do' | 'check' | 'act'

const PDCA: ReadonlyArray<readonly [PdcaPhase, string]> = [
  ['plan', 'Planifier'],
  ['do', 'Réaliser'],
  ['check', 'Vérifier'],
  ['act', 'Agir'],
]

export interface PdcaCardProps {
  title: string
  indicator: string
  origin?: string
  phase: PdcaPhase
  owner: string
  due: string
  /** Garde de passage à l'étape suivante, renvoyée par le service. */
  guard?: string
  status?: Status
  statusLabel?: string
}

// Fiche d'action PDCA (§7.7, projets et cockpit).
export function PdcaCard(p: PdcaCardProps) {
  const idx = PDCA.findIndex((x) => x[0] === p.phase)
  return (
    <Card
      title={p.title}
      actions={<StatusBadge status={p.status || 'info'} label={p.statusLabel || 'en cours'} />}
      footer={[
        <Button key={1} variant="secondary" size="sm">Ouvrir la fiche</Button>,
        <Button key={2} variant="primary" size="sm" disabled={!!p.guard} disabledReason={p.guard}>Passer à l'étape suivante</Button>,
      ]}
    >
      <div className="gm-col" style={{ gap: 'var(--gm-space-3)' }}>
        <div className="gm-field-help">
          {"Indicateur d'origine : "}
          <strong>{p.indicator}</strong>
          {p.origin ? ' · ' + p.origin : ''}
        </div>
        <ol className="gm-pdca">
          {PDCA.map((s, i) => (
            <li key={s[0]} className={cx('gm-pdca-step', i < idx && 'gm-pdca-done', i === idx && 'gm-pdca-current')} aria-current={i === idx ? 'step' : undefined}>
              <span className="gm-step-num">{i < idx ? <Icon name="check" size={14} /> : i + 1}</span>
              {s[1]}
            </li>
          ))}
        </ol>
        {p.guard ? <InlineAlert level="warn" title="Garde de passage :">{p.guard}</InlineAlert> : null}
        <KeyValueList
          items={[
            { key: 'Responsable', value: p.owner },
            { key: 'Échéance de la phase', value: p.due },
          ]}
        />
      </div>
    </Card>
  )
}
