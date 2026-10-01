import { useState } from 'react'
import { cx } from '../../lib/cx'

export type AvatarSize = 'xs' | 'sm' | 'md' | 'lg' | 'xl'

export interface AvatarProps {
  /** Nom de la personne : nom accessible et source des initiales. */
  name: string
  /** Photo de profil (adresse versionnée servie par la passerelle) ; à défaut ou en cas d'échec, les initiales. */
  src?: string | null
  /** 20, 24, 32 (défaut), 40 ou 96 px. */
  size?: AvatarSize
  /** Purement décoratif (le nom figure déjà à côté) : masqué aux technologies d'assistance. */
  decorative?: boolean
  className?: string
}

/** Initiales d'un nom : premières lettres des deux premiers mots, en capitales. */
export function initiales(nom: string): string {
  return (nom || '')
    .split(/\s+/)
    .filter(Boolean)
    .map((w) => w[0])
    .join('')
    .slice(0, 2)
    .toUpperCase()
}

// Avatar d'une personne (§7.3, docs/lot1/mon-compte.md §4) : cercle (gm-radius-avatar, seule forme ronde du
// système), photo recadrée si le compte en a une, initiales sur fond action-subtle à défaut ou si l'image ne
// se charge pas (photo retirée, réseau). Jamais d'autre décor : ni ombre, ni liseré coloré.
export function Avatar(p: AvatarProps) {
  const [echec, setEchec] = useState<string | null>(null)
  const photo = p.src && echec !== p.src ? p.src : null
  const accessible = p.decorative ? { 'aria-hidden': true as const } : { role: 'img', 'aria-label': p.name }
  return (
    <span className={cx('gm-avatar', p.size && p.size !== 'md' && `gm-avatar-${p.size}`, photo && 'gm-avatar-photo', p.className)} {...accessible}>
      {photo ? <img src={photo} alt="" draggable={false} onError={() => setEchec(photo)} /> : initiales(p.name)}
    </span>
  )
}
