import { Icon } from '../../icones/Icon/Icon'
import { Button } from '../../actions/Button/Button'
import { Tag } from '../../affichage/Tag/Tag'
import { SearchInput } from '../../saisie/SearchInput/SearchInput'
import type { Option } from '../../lib/types'
import { useLibelle } from '../../i18n/LangueProvider'

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
  const t = useLibelle()
  const f = p.filters || []
  const n = p.active ? p.active.length : 0
  return (
    <div className="gm-filterbar" role="search" aria-label={t('filtres.label')}>
      <div className="gm-filterbar-row">
        <SearchInput label={t('commun.rechercher')} value={p.query || ''} onChange={p.onQuery} width={280} />
        {f.map((x) => {
          if (x.type === 'daterange')
            return (
              <div key={x.key} className="gm-filter">
                <span className="gm-filter-label">{x.label}</span>
                <button type="button" className="gm-input gm-filter-btn gm-num">
                  <Icon name="calendar" />
                  {x.value || t('filtres.toutesDates')}
                  <Icon name="chevron-down" size={14} />
                </button>
              </div>
            )
          return (
            <div key={x.key} className="gm-filter">
              <span className="gm-filter-label">{x.label}</span>
              <select className="gm-input" aria-label={x.label} value={x.value || ''} onChange={(e) => p.onChange && p.onChange(x.key, e.target.value)}>
                <option value="">{t('filtres.tous')}</option>
                {(x.options || []).map((o) => (
                  <option key={o.value} value={o.value}>{o.label}</option>
                ))}
              </select>
            </div>
          )
        })}
        <span style={{ flex: 1 }} />
        <Button variant="ghost" size="sm" icon="refresh" onClick={p.onReset}>{t('filtres.reinitialiser')}</Button>
        <Button variant="secondary" size="sm" icon="flag">{p.savedLabel || t('filtres.enregistrer')}</Button>
      </div>
      {p.active && n ? (
        <div className="gm-filterbar-active">
          <span className="gm-field-help">{t(n > 1 ? 'filtres.actifs' : 'filtres.actif', { n })}</span>
          {p.active.map((a) => (
            <Tag key={a.key} onRemove={() => p.onRemove && p.onRemove(a.key)}>{a.label}</Tag>
          ))}
          {p.favorites ? (
            <span className="gm-row" style={{ marginLeft: 'auto', gap: 'var(--gm-space-1)' }}>
              <span className="gm-field-help">{t('filtres.favoris')}</span>
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
