import { cx } from '../../lib/cx'
import type { Status } from '../../lib/types'
import { StatusBadge } from '../../affichage/StatusBadge/StatusBadge'

export interface WallTile {
  id: string
  name: string
  state: 'ok' | 'warn' | 'crit' | 'maint'
  value: string
  unit?: string
  metric: string
  alerts?: number
}

export interface WallBoardProps {
  tiles: WallTile[]
  clock?: string
}

const ETATS: Record<WallTile['state'], [Status, string]> = {
  ok: ['ok', 'en service'],
  warn: ['warn', 'dégradée'],
  crit: ['crit', 'injoignable'],
  maint: ['neutral', 'maintenance'],
}

// Tableau mural des liaisons supervisées, sans interaction (§7.6, gabarit G7).
export function WallBoard(p: WallBoardProps) {
  return (
    <div className="gm-wall" role="region" aria-label="Tableau mural des liaisons supervisées" aria-live="polite">
      <div className="gm-wall-head">
        <span className="gm-topbar-layer">LIAISONS SUPERVISÉES</span>
        <span className="gm-num" style={{ marginLeft: 'auto' }}>{p.clock || '14:05:32'}</span>
        <span className="gm-row" style={{ gap: 6, fontSize: 12 }}>
          <span className="gm-dot gm-dot-ok" />
          temps réel
        </span>
      </div>
      <div className="gm-wall-grid">
        {(p.tiles || []).map((t) => {
          const m = ETATS[t.state] || ETATS.ok
          return (
            <div key={t.id} className={cx('gm-wall-tile', 'gm-wall-' + t.state)}>
              <div className="gm-row" style={{ justifyContent: 'space-between' }}>
                <span className="gm-mono">{t.id}</span>
                <StatusBadge status={m[0]} label={m[1]} />
              </div>
              <div className="gm-wall-name">{t.name}</div>
              <div className="gm-wall-stat gm-num">
                {t.value}
                <span className="gm-wall-unit">{t.unit}</span>
              </div>
              <div className="gm-row" style={{ justifyContent: 'space-between', fontSize: 12, color: 'var(--gm-color-text-secondary)' }}>
                <span>{t.metric}</span>
                <span className="gm-num">{t.alerts ? t.alerts + ' alerte' + (t.alerts > 1 ? 's' : '') : 'aucune alerte'}</span>
              </div>
            </div>
          )
        })}
      </div>
    </div>
  )
}
