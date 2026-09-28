import { Button, type ButtonProps } from '../Button/Button'
import type { IconName } from '../../icones/icons'

export interface IconButtonProps extends Omit<ButtonProps, 'children' | 'iconOnly' | 'icon'> {
  icon: IconName
  /** Obligatoire : devient l'aria-label du bouton. */
  label: string
}

// Bouton carré à pictogramme seul (§7.1).
export function IconButton(p: IconButtonProps) {
  return (
    <Button {...p} iconOnly aria-label={p.label}>
      {p.label}
    </Button>
  )
}
