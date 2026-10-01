import type { ReactNode } from 'react'
import { cx } from '../../lib/cx'
import { Icon } from '../../icones/Icon/Icon'
import type { IconName } from '../../icones/icons'

export interface NoteProps {
  /** Titre facultatif de la note. */
  title?: ReactNode
  /** Pictogramme (info par défaut) ; null pour une note sans pictogramme. */
  icon?: IconName | null
  className?: string
  children?: ReactNode
}

// Note d'aide (D-63) : explication, règle ou rappel posé dans une section, sur une teinte de fond douce
// à angles droits et bordure de 1 px, avec pictogramme ; jamais un paragraphe à nu, jamais une bande latérale. Pour un
// état ou un refus, InlineAlert.
export function Note(p: NoteProps) {
  const icon = p.icon === undefined ? 'info' : p.icon
  return (
    <div className={cx('gm-note', p.className)}>
      {icon ? <Icon name={icon} size={16} /> : null}
      <div className="gm-note-texte">
        {p.title ? <div className="gm-note-titre">{p.title}</div> : null}
        {p.children}
      </div>
    </div>
  )
}
