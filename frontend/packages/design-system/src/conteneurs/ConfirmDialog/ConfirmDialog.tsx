import { useState } from 'react'
import type { KeyValueItem } from '../../lib/types'
import { Button } from '../../actions/Button/Button'
import { KeyValueList } from '../../affichage/KeyValueList/KeyValueList'
import { TextArea } from '../../saisie/TextArea/TextArea'
import { Checkbox } from '../../saisie/Checkbox/Checkbox'
import { Dialog } from '../Dialog/Dialog'

export interface ConfirmDialogProps {
  title: string
  description?: string
  /** Verbe et objet : jamais « OK ». */
  confirmLabel: string
  onConfirm?: () => void
  onClose?: () => void
  /** Rappel de l'objet de l'action. */
  object?: KeyValueItem[]
  consequences?: string[]
  /** Motif obligatoire (10 caractères au moins). */
  reason?: boolean
  /** Case « je confirme » pour une action irréversible. */
  irreversible?: boolean
  irreversibleLabel?: string
  danger?: boolean
  open?: boolean
  inline?: boolean
}

// Confirmation à deux étapes de toute action à effet (§7.5).
export function ConfirmDialog(p: ConfirmDialogProps) {
  const [checked, setChecked] = useState(false)
  const [reason, setReason] = useState('')
  const ok = (!p.irreversible || checked) && (!p.reason || reason.trim().length >= 10)
  return (
    <Dialog
      inline={p.inline}
      open={p.open}
      onClose={p.onClose}
      title={p.title}
      description={p.description}
      role="alertdialog"
      footer={[
        <Button key={1} variant="secondary" onClick={p.onClose}>Annuler</Button>,
        <Button
          key={2}
          variant={p.danger ? 'danger' : 'primary'}
          disabled={!ok}
          disabledReason={!ok ? (p.reason && reason.trim().length < 10 ? 'Le motif est obligatoire (10 caractères au moins)' : 'Cochez la case de confirmation') : undefined}
          onClick={p.onConfirm}
        >
          {p.confirmLabel}
        </Button>,
      ]}
    >
      <div className="gm-col" style={{ gap: 'var(--gm-space-3)' }}>
        {p.object ? <div className="gm-panel"><KeyValueList items={p.object} /></div> : null}
        {p.consequences ? <ul className="gm-consequences">{p.consequences.map((c, i) => <li key={i}>{c}</li>)}</ul> : null}
        {p.reason ? <TextArea name="motif" label="Motif" required value={reason} onChange={setReason} help="Consigné dans la chronologie et le registre d'audit." /> : null}
        {p.irreversible ? <Checkbox label={p.irreversibleLabel || 'Je confirme : cette action est irréversible.'} checked={checked} onChange={setChecked} /> : null}
      </div>
    </Dialog>
  )
}
