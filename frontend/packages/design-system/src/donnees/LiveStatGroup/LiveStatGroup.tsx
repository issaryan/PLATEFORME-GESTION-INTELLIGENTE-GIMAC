import type { Status } from '../../lib/types'
import { StatCard, type StatCardProps } from '../../affichage/StatCard/StatCard'
import { StatusBadge } from '../../affichage/StatusBadge/StatusBadge'
import { useLibelle } from '../../i18n/LangueProvider'

export type LiveState = 'connecting' | 'live' | 'lost'

export interface LiveStatGroupProps {
  title: string
  /** État du flux temps réel (SSE) qui alimente les indicateurs. */
  state: LiveState
  /** Heure de la dernière mise à jour, déjà formatée. */
  updatedAt?: string
  items: (StatCardProps & { key: string })[]
  /** Niveau du titre (2 par défaut). */
  level?: 2 | 3
}

const ETATS: Record<LiveState, [Status, 'direct.connexion' | 'direct.connecte' | 'direct.perdu']> = {
  connecting: ['info', 'direct.connexion'],
  live: ['ok', 'direct.connecte'],
  lost: ['warn', 'direct.perdu'],
}

// Indicateurs en temps réel (supervision de la plateforme, D-61) : StatCard alimentées par un flux,
// état de la connexion annoncé (role=status) et zone aria-live polie pour les valeurs.
export function LiveStatGroup(p: LiveStatGroupProps) {
  const t = useLibelle()
  const [statut, cle] = ETATS[p.state]
  const Titre = p.level === 3 ? 'h3' : 'h2'
  return (
    <section className="gm-col" aria-label={p.title}>
      <div className="gm-row" style={{ justifyContent: 'space-between' }}>
        <Titre className="gm-card-title">{p.title}</Titre>
        <span role="status">
          <StatusBadge status={statut} label={t(cle)} />
        </span>
      </div>
      <div className="gm-stat-grid" aria-live="polite">
        {p.items.map(({ key, ...s }) => (
          <StatCard key={key} {...s} />
        ))}
      </div>
      {p.updatedAt ? <span className="gm-type-small">{t('direct.misAJour', { heure: p.updatedAt })}</span> : null}
    </section>
  )
}
