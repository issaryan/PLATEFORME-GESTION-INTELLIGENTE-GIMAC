import type { ReactNode } from 'react'
import type { Status } from '../../lib/types'
import { cx } from '../../lib/cx'
import { Icon } from '../../icones/Icon/Icon'
import { STATUS_ICON } from '../../icones/statusIcon'

export interface SettingsGridItem {
  key: string
  /** Libellé du réglage. */
  label: string
  /** Valeur mise en valeur. */
  value: ReactNode
  unit?: string
  /** Aide sous la valeur : plancher, plage admise, effet. */
  hint?: ReactNode
  /** État de la valeur (activée, désactivée…) : pictogramme de statut devant la valeur (§3.3). */
  status?: Status
}

export interface SettingsGridProps {
  /** Nom du groupe de réglages, annoncé aux technologies d'assistance. */
  label: string
  items: SettingsGridItem[]
  className?: string
}

// Grille de réglages en lecture (politique de sécurité, configuration, D-63) : une tuile par réglage,
// libellé discret, valeur en grand avec son unité, aide (plancher, plage) en dessous ; colonnes régulières ;
// cartouches à plat, bordure de 1 px, angles droits.
export function SettingsGrid(p: SettingsGridProps) {
  return (
    <dl className={cx('gm-reglages', p.className)} aria-label={p.label}>
      {p.items.map((i) => (
        <div key={i.key} className="gm-reglage">
          <dt>{i.label}</dt>
          <dd className="gm-reglage-valeur">
            {i.status && STATUS_ICON[i.status] ? <span className={cx('gm-reglage-etat', 'gm-stat-etat-' + i.status)}><Icon name={STATUS_ICON[i.status]!} size={16} /></span> : null}
            {i.value}
            {i.unit ? <span className="gm-reglage-unite">{i.unit}</span> : null}
          </dd>
          {i.hint ? <dd className="gm-reglage-aide">{i.hint}</dd> : null}
        </div>
      ))}
    </dl>
  )
}
