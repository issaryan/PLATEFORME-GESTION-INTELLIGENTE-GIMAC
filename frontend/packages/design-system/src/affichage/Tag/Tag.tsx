import { Icon } from '../../icones/Icon/Icon'
import { Avatar } from '../Avatar/Avatar'
import { useLibelle } from '../../i18n/LangueProvider'

export interface TagProps {
  children: string
  onRemove?: () => void
  /** Personne désignée (destinataire) : avatar de 20 px, photo ou initiales, avant le libellé. */
  avatar?: { name: string; src?: string | null }
  /** Libellé long tronqué par des points de suspension, lu en entier dans l'infobulle (sélecteurs multiples). */
  tronque?: boolean
}

// Étiquette neutre, retirable dans les filtres (§7.3).
export function Tag(p: TagProps) {
  const t = useLibelle()
  return (
    <span className={p.avatar ? 'gm-tag gm-tag-personne' : 'gm-tag'}>
      {p.avatar ? <Avatar name={p.avatar.name} src={p.avatar.src} size="xs" decorative /> : null}
      {p.tronque ? <span className="gm-tag-texte" title={p.children}>{p.children}</span> : p.children}
      {p.onRemove ? (
        <button type="button" aria-label={t('tag.retirer', { libelle: p.children })} onClick={p.onRemove}>
          <Icon name="x" size={12} />
        </button>
      ) : null}
    </span>
  )
}
