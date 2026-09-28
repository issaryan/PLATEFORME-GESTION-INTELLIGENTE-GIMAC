import { Icon } from '../../icones/Icon/Icon'
import { Button } from '../../actions/Button/Button'
import { Tag } from '../../affichage/Tag/Tag'
import { SearchInput } from '../../saisie/SearchInput/SearchInput'
import type { Option } from '../../lib/types'

export interface FilterDef {
  key: string
  label: string
  type?: 'select' | 'daterange'
  value?: string
  options?: Option[]
}

export interface FilterBarProps {
  query?: string
  onQuery?: (q: string) => void
  filters: FilterDef[]
  onChange?: (key: string, value: string) => void
  /** Puces des filtres actifs, retirables. */
  active?: { key: string; label: string }[]
  onRemove?: (key: string) => void
  favorites?: string[]
  onReset?: () => void
  savedLabel?: string
}

// Barre de filtres persistante en tête d'une liste (DS-GIMAC-01 §7.6, gabarit G2).
export function FilterBar(p: FilterBarProps) {
  const f = p.filters || []
  const n = p.active ? p.active.length : 0
  return (
    <div className="gm-filterbar" role="search" aria-label="Filtres">
      <div className="gm-filterbar-row">
        <SearchInput label="Rechercher" value={p.query || ''} onChange={p.onQuery} width={280} />
        {f.map((x) => {
          if (x.type === 'daterange')
            return (
              <div key={x.key} className="gm-filter">
                <span className="gm-filter-label">{x.label}</span>
                <button type="button" className="gm-input gm-filter-btn gm-num">
                  <Icon name="calendar" />
                  {x.value || 'Toutes les dates'}
                  <Icon name="chevron-down" size={14} />
                </button>
              </div>
            )
          return (
            <div key={x.key} className="gm-filter">
              <span className="gm-filter-label">{x.label}</span>
              <select className="gm-input" aria-label={x.label} value={x.value || ''} onChange={(e) => p.onChange && p.onChange(x.key, e.target.value)}>
                <option value="">Tous</option>
                {(x.options || []).map((o) => (
                  <option key={o.value} value={o.value}>{o.label}</option>
                ))}
              </select>
            </div>
          )
        })}
        <span style={{ flex: 1 }} />
        <Button variant="ghost" size="sm" icon="refresh" onClick={p.onReset}>Réinitialiser</Button>
        <Button variant="secondary" size="sm" icon="flag">{p.savedLabel || 'Enregistrer ce filtre'}</Button>
      </div>
      {p.active && n ? (
        <div className="gm-filterbar-active">
          <span className="gm-field-help">{n + ' filtre' + (n > 1 ? 's' : '') + ' actif' + (n > 1 ? 's' : '') + ' :'}</span>
          {p.active.map((a) => (
            <Tag key={a.key} onRemove={() => p.onRemove && p.onRemove(a.key)}>{a.label}</Tag>
          ))}
          {p.favorites ? (
            <span className="gm-row" style={{ marginLeft: 'auto', gap: 'var(--gm-space-1)' }}>
              <span className="gm-field-help">Favoris :</span>
              {p.favorites.map((fv) => (
                <Button key={fv} size="sm" variant="ghost">{fv}</Button>
              ))}
            </span>
          ) : null}
        </div>
      ) : null}
    </div>
  )
}
