import type { ReactNode } from 'react'
import type { KeyValueItem } from '../../lib/types'
import { CrudDialog } from '../CrudDialog/CrudDialog'

export interface ConfirmDialogProps {
  title: string
  description?: string
  /** Verbe et objet : jamais « OK ». */
  confirmLabel: string
  /** Reçoit le motif saisi (chaîne vide sans champ de motif). */
  onConfirm?: (motif: string) => void
  onClose?: () => void
  /** Rappel de l'objet de l'action. */
  object?: KeyValueItem[]
  consequences?: string[]
  /** Contenu complémentaire, rendu sous la liste des conséquences. */
  extra?: ReactNode
  /** Motif obligatoire (10 caractères au moins). */
  reason?: boolean
  /** Case « je confirme » pour une action irréversible. */
  irreversible?: boolean
  irreversibleLabel?: string
  danger?: boolean
  open?: boolean
  inline?: boolean
}

// Confirmation à deux étapes de toute action à effet (§7.5) : modale CRUD standard en contexte
// « confirmation » (ou « suppression » pour une action destructive et irréversible).
export function ConfirmDialog(p: ConfirmDialogProps) {
  return (
    <CrudDialog
      context={p.danger && p.irreversible ? 'delete' : 'confirm'}
      open={p.open}
      inline={p.inline}
      title={p.title}
      description={p.description}
      submitLabel={p.confirmLabel}
      danger={!!p.danger}
      onSubmit={(motif) => p.onConfirm?.(motif)}
      onClose={() => p.onClose?.()}
      object={p.object}
      consequences={p.consequences}
      reason={!!p.reason}
      irreversible={!!p.irreversible}
      irreversibleLabel={p.irreversibleLabel}
      role="alertdialog"
    >
      {p.extra ?? null}
    </CrudDialog>
  )
}
