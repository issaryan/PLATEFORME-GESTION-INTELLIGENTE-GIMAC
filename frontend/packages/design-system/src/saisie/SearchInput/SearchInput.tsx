import { Icon } from '../../icones/Icon/Icon'

export interface SearchInputProps {
  label?: string
  value?: string
  onChange?: (v: string) => void
  width?: number
}

// Recherche avec pictogramme (§7.2).
export function SearchInput(p: SearchInputProps) {
  return (
    <div className="gm-input-wrap" style={{ width: p.width || 320 }}>
      <input type="search" className="gm-input" aria-label={p.label || 'Rechercher'} value={p.value} onChange={(e) => { if (p.onChange) p.onChange(e.target.value) }} />
      <span className="gm-input-suffix"><Icon name="search" /></span>
    </div>
  )
}
