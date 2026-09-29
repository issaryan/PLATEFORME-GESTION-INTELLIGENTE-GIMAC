import { useState, type ReactNode } from 'react'
import type { KeyValueItem } from '../../lib/types'
import { Button } from '../../actions/Button/Button'
import { KeyValueList } from '../../affichage/KeyValueList/KeyValueList'
import { TextArea } from '../../saisie/TextArea/TextArea'
import { Checkbox } from '../../saisie/Checkbox/Checkbox'
import { Dialog } from '../Dialog/Dialog'
import { useLibelle } from '../../i18n/LangueProvider'

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

// Confirmation à deux étapes de toute action à effet (§7.5).
export function ConfirmDialog(p: ConfirmDialogProps) {
  const t = useLibelle()
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
        <Button key={1} variant="secondary" onClick={p.onClose}>{t('commun.annuler')}</Button>,
        <Button
          key={2}
          variant={p.danger ? 'danger' : 'primary'}
          disabled={!ok}
          disabledReason={!ok ? (p.reason && reason.trim().length < 10 ? t('confirm.motifObligatoire') : t('confirm.cocher')) : undefined}
          onClick={() => { if (p.onConfirm) p.onConfirm(p.reason ? reason.trim() : '') }}
        >
          {p.confirmLabel}
        </Button>,
      ]}
    >
      <div className="gm-col" style={{ gap: 'var(--gm-space-3)' }}>
        {p.object ? <div className="gm-panel"><KeyValueList items={p.object} /></div> : null}
        {p.consequences ? <ul className="gm-consequences">{p.consequences.map((c, i) => <li key={i}>{c}</li>)}</ul> : null}
        {p.extra ?? null}
        {p.reason ? <TextArea name="motif" label={t('confirm.motif')} required value={reason} onChange={setReason} help={t('confirm.motifAide')} /> : null}
        {p.irreversible ? <Checkbox label={p.irreversibleLabel || t('confirm.irreversible')} checked={checked} onChange={setChecked} /> : null}
      </div>
    </Dialog>
  )
}
