import { useState } from 'react'
import type { Status } from '../../lib/types'
import { Button } from '../../actions/Button/Button'
import { StatusBadge } from '../../affichage/StatusBadge/StatusBadge'
import { Tabs } from '../../navigation/Tabs/Tabs'

export type RiskLevel = 1 | 2 | 3 | 4 | 5
export type RiskCoord = [RiskLevel, RiskLevel]
export type RiskStage = 'gross' | 'control' | 'residual'

export interface Risk {
  key: string
  label: string
  owner: string
  gross: RiskCoord
  control?: RiskCoord
  residual?: RiskCoord
}

export interface RiskMatrixProps {
  risks: Risk[]
  stage?: RiskStage
}

const RM = [1, 2, 3, 4, 5]
function zone(pr: number, im: number): [Status, string] {
  const s = pr * im
  return s >= 15 ? ['crit', 'critique'] : s >= 8 ? ['warn', 'élevé'] : s >= 4 ? ['info', 'modéré'] : ['ok', 'faible']
}

// Matrice 5 × 5 probabilité × impact, risques positionnés (§7.6, DES-24).
export function RiskMatrix(p: RiskMatrixProps) {
  const [stage, setStage] = useState<string>(p.stage || 'residual')
  const risks = (p.risks || []).map((r) => {
    const v = r[stage as RiskStage] || r.residual || r.gross
    return { ...r, pr: v[0] as number, im: v[1] as number }
  })
  return (
    <div className="gm-riskm">
      <div className="gm-dag-toolbar">
        <Tabs items={[{ key: 'gross', label: 'Brut' }, { key: 'control', label: 'Maîtrise' }, { key: 'residual', label: 'Résiduel' }]} value={stage} onChange={setStage} />
        <span style={{ flex: 1 }} />
        <Button size="sm" variant="ghost">Vue tableau</Button>
      </div>
      <div className="gm-row" style={{ alignItems: 'flex-start', gap: 'var(--gm-space-5)' }}>
        <table className="gm-matrix" role="grid" aria-label="Matrice des risques, probabilité par impact">
          <thead>
            <tr>
              <th scope="col">{'Probabilité \\ Impact'}</th>
              {RM.map((i) => (
                <th key={i} scope="col">{i}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {RM.slice().reverse().map((pr) => (
              <tr key={pr}>
                <th scope="row">{pr}</th>
                {RM.map((im) => {
                  const z = zone(pr, im)
                  const here = risks.filter((r) => r.pr === pr && r.im === im)
                  return (
                    <td key={im} className="gm-matrix-cell gm-risk-cell">
                      <span className="gm-risk-zone" data-zone={z[0]}>{z[1]}</span>
                      {here.length ? (
                        <span className="gm-row" style={{ gap: 4, justifyContent: 'center' }}>
                          {here.map((r) => (
                            <span key={r.key} className="gm-risk-pin" title={r.label}>{r.key}</span>
                          ))}
                        </span>
                      ) : null}
                    </td>
                  )
                })}
              </tr>
            ))}
          </tbody>
        </table>
        <ol className="gm-risk-list">
          {risks.map((r) => {
            const z = zone(r.pr, r.im)
            return (
              <li key={r.key}>
                <span className="gm-risk-pin">{r.key}</span>
                <span style={{ flex: 1 }}>
                  {r.label}
                  <span className="gm-field-help" style={{ display: 'block' }}>{'P' + r.pr + ' × I' + r.im + ' · ' + r.owner}</span>
                </span>
                <StatusBadge status={z[0]} label={z[1]} />
              </li>
            )
          })}
        </ol>
      </div>
    </div>
  )
}
