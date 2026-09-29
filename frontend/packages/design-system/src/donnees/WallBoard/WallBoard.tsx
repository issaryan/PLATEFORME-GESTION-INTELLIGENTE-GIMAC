import { cx } from '../../lib/cx'
import type { Status } from '../../lib/types'
import { StatusBadge } from '../../affichage/StatusBadge/StatusBadge'
import { useLibelle } from '../../i18n/LangueProvider'
import type { CleLibelle } from '../../i18n/libelles'

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

const ETATS: Record<WallTile['state'], [Status, CleLibelle]> = {
  ok: ['ok', 'mur.service'],
  warn: ['warn', 'mur.degradee'],
  crit: ['crit', 'mur.injoignable'],
  maint: ['neutral', 'mur.maintenance'],
}

// Tableau mural des liaisons supervisées, sans interaction (§7.6, gabarit G7).
export function WallBoard(p: WallBoardProps) {
  const t = useLibelle()
  return (
    <div className="gm-wall" role="region" aria-label={t('mur.label')} aria-live="polite">
      <div className="gm-wall-head">
        <span className="gm-topbar-layer">{t('mur.titre')}</span>
        <span className="gm-num" style={{ marginLeft: 'auto' }}>{p.clock || '14:05:32'}</span>
        <span className="gm-row" style={{ gap: 6, fontSize: 12 }}>
          <span className="gm-dot gm-dot-ok" />
          {t('mur.tempsReel')}
        </span>
      </div>
      <div className="gm-wall-grid">
        {(p.tiles || []).map((tuile) => {
          const m = ETATS[tuile.state] || ETATS.ok
          return (
            <div key={tuile.id} className={cx('gm-wall-tile', 'gm-wall-' + tuile.state)}>
              <div className="gm-row" style={{ justifyContent: 'space-between' }}>
                <span className="gm-mono">{tuile.id}</span>
                <StatusBadge status={m[0]} label={t(m[1])} />
              </div>
              <div className="gm-wall-name">{tuile.name}</div>
              <div className="gm-wall-stat gm-num">
                {tuile.value}
                <span className="gm-wall-unit">{tuile.unit}</span>
              </div>
              <div className="gm-row" style={{ justifyContent: 'space-between', fontSize: 12, color: 'var(--gm-color-text-secondary)' }}>
                <span>{tuile.metric}</span>
                <span className="gm-num">{tuile.alerts ? t(tuile.alerts > 1 ? 'mur.alertes' : 'mur.alerte', { n: tuile.alerts }) : t('mur.aucuneAlerte')}</span>
              </div>
            </div>
          )
        })}
      </div>
    </div>
  )
}
