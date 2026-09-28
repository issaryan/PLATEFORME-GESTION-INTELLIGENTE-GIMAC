import type { Status } from '../../lib/types'
import { StatusBadge } from '../../affichage/StatusBadge/StatusBadge'
import { Hash } from '../../affichage/Hash/Hash'
import { Money } from '../../affichage/Money/Money'

export interface ReconciliationSource {
  name: string
  imported: boolean
  rows?: number
  hash?: string
}

export interface ReconciliationGap {
  type: string
  count: number
  amount: number
}

export interface ReconciliationSummaryProps {
  day: string
  institution: string
  status: Status
  statusLabel: string
  sources?: ReconciliationSource[]
  gaps?: ReconciliationGap[]
}

// Synthèse d'une journée de rapprochement (§7.7, DES-21).
export function ReconciliationSummary(p: ReconciliationSummaryProps) {
  return (
    <div className="gm-recap">
      <div className="gm-row" style={{ justifyContent: 'space-between' }}>
        <div>
          <div className="gm-field-help">Journée de rapprochement</div>
          <strong className="gm-num" style={{ fontSize: 18 }}>{p.day}</strong>
        </div>
        <div>
          <div className="gm-field-help">Institution</div>
          <strong>{p.institution}</strong>
        </div>
        <StatusBadge status={p.status} label={p.statusLabel} />
      </div>
      <div className="gm-recap-grid">
        {(p.sources || []).map((s) => (
          <div key={s.name} className="gm-panel">
            <div className="gm-field-help">{s.name}</div>
            <div className="gm-row">
              <StatusBadge status={s.imported ? 'ok' : 'warn'} label={s.imported ? 'importé' : 'manquant'} />
              {s.rows !== undefined ? <span className="gm-num gm-field-help">{s.rows + ' lignes'}</span> : null}
            </div>
            {s.hash ? <Hash value={s.hash} /> : null}
          </div>
        ))}
      </div>
      <table className="gm-table gm-table-compact" style={{ border: 'var(--gm-border-1) solid var(--gm-color-border)' }}>
        <thead>
          <tr>
            <th>Typologie d'écart</th>
            <th className="gm-num">Nombre</th>
            <th className="gm-num">Montant</th>
            <th>État</th>
          </tr>
        </thead>
        <tbody>
          {(p.gaps || []).map((g) => (
            <tr key={g.type}>
              <td>{g.type}</td>
              <td className="gm-num">{g.count}</td>
              <td className="gm-num"><Money amount={g.amount} /></td>
              <td><StatusBadge status={g.count ? 'warn' : 'ok'} label={g.count ? g.count + ' suspens ouverts' : 'aucun écart'} /></td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
