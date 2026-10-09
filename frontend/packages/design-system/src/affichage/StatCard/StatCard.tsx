import type { ReactNode } from 'react'
import { cx } from '../../lib/cx'
import type { Status } from '../../lib/types'
import { useLibelle } from '../../i18n/LangueProvider'
import { Icon } from '../../icones/Icon/Icon'
import { STATUS_ICON } from '../../icones/statusIcon'
import { Sparkline } from '../Sparkline/Sparkline'

export interface StatCardProps {
  label: ReactNode
  value: ReactNode
  unit?: string
  /** Ligne de contexte : variation par rapport à la période précédente. */
  trend?: { delta: string; direction: 'up' | 'down' }
  threshold?: string
  /** Ligne de contexte libre (échéance, période). */
  context?: ReactNode
  status?: Status
  statusLabel?: string
  onClick?: () => void
  className?: string
  /** Tuile de mesure : mini-courbe de l'évolution récente (valeurs dans l'ordre du temps, null = trou). */
  serie?: (number | null)[]
  /** Nom de la mesure pour l'étiquette accessible de la mini-courbe (à défaut, le libellé s'il est textuel). */
  serieLibelle?: string
}

// Carte d'indicateur (DS-GIMAC-01 §7.3) : libellé (small), valeur (stat, chiffres tabulaires), unité ;
// ligne de contexte : statut sur pictogramme et libellé uniquement, tendance (flèche et variation),
// seuil, échéance. Surface plate, bordure de 1 px, angles droits, aucune ombre ni pictogramme décoratif.
// Carte cliquable : bordure d'action au survol, anneau de focus, activée au clavier (Entrée, Espace).
// Tuile de mesure : avec `serie`, une mini-courbe de l'évolution récente s'affiche sous la valeur.
export function StatCard(p: StatCardProps) {
  const t = useLibelle()
  const st = p.status
  const tendance = p.trend
  // Valeur faite de mots (« Opérationnelle ») : taille réduite et césure, pour rester dans la carte.
  const texte = typeof p.value === 'string' && /\p{L}/u.test(p.value) && !/\d/.test(p.value)
  return (
    <div
      className={cx('gm-card', 'gm-stat', st && 'gm-stat-' + st, p.onClick && 'gm-stat-lien', p.className)}
      role={p.onClick ? 'button' : undefined}
      tabIndex={p.onClick ? 0 : undefined}
      onClick={p.onClick}
      onKeyDown={
        p.onClick
          ? (e) => {
              if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault()
                p.onClick!()
              }
            }
          : undefined
      }
    >
      <span className="gm-stat-label">{p.label}</span>
      <div className={cx('gm-stat-value', texte && 'gm-stat-value-texte')}>
        {p.value}
        {p.unit ? <span className="gm-stat-unit">{p.unit}</span> : null}
      </div>
      {p.serie && p.serie.length ? (
        <div className="gm-stat-tendance-courbe">
          <Sparkline points={p.serie} label={p.serieLibelle ?? (typeof p.label === 'string' ? p.label : '')} unit={p.unit} aire />
        </div>
      ) : null}
      {tendance || p.threshold || st || p.context ? (
        <div className="gm-stat-meta">
          {st ? (
            <span className={cx('gm-stat-etat', 'gm-stat-etat-' + st)}>
              {STATUS_ICON[st] ? <Icon name={STATUS_ICON[st]!} size={16} /> : null}
              {p.statusLabel || st}
            </span>
          ) : null}
          {tendance ? (
            <span className="gm-stat-tendance">
              <Icon name={tendance.direction === 'up' ? 'arrow-up' : 'arrow-down'} size={16} />
              {tendance.delta}
            </span>
          ) : null}
          {p.threshold ? <span>{t('commun.seuil', { valeur: p.threshold })}</span> : null}
          {p.context ? <span>{p.context}</span> : null}
        </div>
      ) : null}
    </div>
  )
}
