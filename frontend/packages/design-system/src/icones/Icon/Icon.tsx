import { cx } from '../../lib/cx'
import { ICONS, type IconName } from '../icons'

export interface IconProps {
  name: IconName | (string & {})
  /** 16 (texte, puces), 20 (boutons, navigation), 24 (en-têtes, états vides) ; 12 et 14 dans les puces. */
  size?: number
  /** Nom accessible ; sans libellé, le pictogramme est décoratif (aria-hidden). */
  label?: string
  className?: string
}

// Pictogramme linéaire monochrome à trait de 1,5 px (jeu Lucide), couleur du texte.
export function Icon({ name, size = 16, label, className }: IconProps) {
  const trace = (ICONS as Record<string, string>)[name] ?? ''
  const a11y = label ? { role: 'img', 'aria-label': label } : { 'aria-hidden': true as const }
  return (
    <svg
      className={cx('gm-icon', className)}
      width={size}
      height={size}
      viewBox="0 0 24 24"
      {...a11y}
      dangerouslySetInnerHTML={{ __html: trace }}
    />
  )
}
