import { Fragment } from 'react'
import type { KeyValueItem } from '../../lib/types'

export interface KeyValueListProps {
  items?: KeyValueItem[]
}

// Liste clé-valeur en deux colonnes (§7.3).
export function KeyValueList(p: KeyValueListProps) {
  return (
    <dl className="gm-kv">
      {(p.items || []).map((it, i) => (
        <Fragment key={i}>
          <dt>{it.key}</dt>
          <dd className={it.mono ? 'gm-mono' : undefined}>{it.value}</dd>
        </Fragment>
      ))}
    </dl>
  )
}
