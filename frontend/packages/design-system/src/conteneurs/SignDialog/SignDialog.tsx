import type { KeyValueItem } from '../../lib/types'
import { Button } from '../../actions/Button/Button'
import { KeyValueList } from '../../affichage/KeyValueList/KeyValueList'
import { Hash } from '../../affichage/Hash/Hash'
import { SignatureField } from '../../saisie/SignatureField/SignatureField'
import { InlineAlert } from '../InlineAlert/InlineAlert'
import { Dialog } from '../Dialog/Dialog'

export interface SignDialogProps {
  title: string
  /** Action signée, rappelée dans le bloc de signature. */
  action?: string
  /** Empreinte de ce qui sera signé. */
  hash: string
  state?: 'idle' | 'waiting' | 'done' | 'error'
  object?: KeyValueItem[]
  description?: string
  result?: string
  at?: string
  onClose?: () => void
  onDone?: () => void
  doneLabel?: string
  open?: boolean
  inline?: boolean
}

// Confirmation avec réauthentification FIDO2 (§7.5) : quitus, clôture, scellement, purge.
export function SignDialog(p: SignDialogProps) {
  const st = p.state || 'idle'
  return (
    <Dialog
      inline={p.inline}
      open={p.open}
      onClose={st === 'waiting' ? undefined : p.onClose}
      title={p.title}
      description={p.description || "Cette action est signée avec votre clé de sécurité et scellée dans le registre d'audit."}
      footer={[
        <Button key={1} variant="secondary" onClick={p.onClose} disabled={st === 'waiting'} disabledReason="Signature en cours">
          {st === 'done' ? 'Fermer' : 'Annuler'}
        </Button>,
        st === 'done' ? <Button key={2} variant="primary" onClick={p.onDone}>{p.doneLabel || 'Continuer'}</Button> : null,
      ]}
    >
      <div className="gm-col" style={{ gap: 'var(--gm-space-3)' }}>
        {p.object ? <div className="gm-panel"><KeyValueList items={p.object} /></div> : null}
        <div>
          <div className="gm-field-help">Empreinte de ce qui sera signé</div>
          <Hash value={p.hash} />
        </div>
        <SignatureField state={st} action={p.action} at={p.at} />
        {st === 'done' && p.result ? <InlineAlert level="info" title="Résultat :">{p.result}</InlineAlert> : null}
      </div>
    </Dialog>
  )
}
