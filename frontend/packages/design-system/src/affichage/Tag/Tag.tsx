import { Icon } from '../../icones/Icon/Icon'

export interface TagProps {
  children: string
  onRemove?: () => void
}

// Étiquette neutre, retirable dans les filtres (§7.3).
export function Tag(p: TagProps) {
  return (
    <span className="gm-tag">
      {p.children}
      {p.onRemove ? (
        <button type="button" aria-label={'Retirer ' + p.children} onClick={p.onRemove}>
          <Icon name="x" size={12} />
        </button>
      ) : null}
    </span>
  )
}
