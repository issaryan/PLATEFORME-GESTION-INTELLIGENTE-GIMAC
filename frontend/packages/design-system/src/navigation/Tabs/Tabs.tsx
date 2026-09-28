import { useState } from 'react'

export interface TabsProps {
  items?: { key: string; label: string }[]
  value?: string
  onChange?: (key: string) => void
}

// Onglets d'une fiche, navigation clavier par flèches (§7.4).
export function Tabs(p: TabsProps) {
  const items = p.items || []
  const [interne, setInterne] = useState(p.value || (items[0] && items[0].key))
  const cur = p.value !== undefined ? p.value : interne
  function go(k: string) {
    setInterne(k)
    if (p.onChange) p.onChange(k)
  }
  return (
    <div className="gm-tabs" role="tablist">
      {items.map((it) => (
        <button
          key={it.key}
          role="tab"
          className="gm-tab"
          aria-selected={cur === it.key ? 'true' : 'false'}
          tabIndex={cur === it.key ? 0 : -1}
          onClick={() => go(it.key)}
          onKeyDown={(e) => {
            const i = items.findIndex((x) => x.key === cur)
            if (e.key === 'ArrowRight') go(items[(i + 1) % items.length].key)
            if (e.key === 'ArrowLeft') go(items[(i - 1 + items.length) % items.length].key)
          }}
        >
          {it.label}
        </button>
      ))}
    </div>
  )
}
