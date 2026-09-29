import { useState, type ReactNode } from 'react'
import type { KeyValueItem } from '../../lib/types'
import { TextArea } from '../../saisie/TextArea/TextArea'
import { Button } from '../../actions/Button/Button'
import { KeyValueList } from '../../affichage/KeyValueList/KeyValueList'
import { Hash } from '../../affichage/Hash/Hash'
import { SignatureField } from '../../saisie/SignatureField/SignatureField'
import { InlineAlert } from '../InlineAlert/InlineAlert'
import { Dialog } from '../Dialog/Dialog'
import { useLibelle } from '../../i18n/LangueProvider'

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
  /** Motif obligatoire (10 caractères au moins) : champ Motif, bouton de signature désactivé sinon. */
  reason?: boolean
  /** Appelé par le bouton de signature (et « Réessayer » après une erreur) avec le motif saisi (chaîne vide sans motif). */
  onSign?: (motif: string) => void
  /** Message d'erreur affiché dans le dialogue (InlineAlert critique). */
  error?: string
  /** Contenu complémentaire, rendu sous le rappel de l'objet. */
  extra?: ReactNode
}

// Confirmation avec réauthentification FIDO2 (§7.5) : quitus, clôture, scellement, purge.
export function SignDialog(p: SignDialogProps) {
  const t = useLibelle()
  const st = p.state || 'idle'
  const [motif, setMotif] = useState('')
  const motifOk = !p.reason || motif.trim().length >= 10
  return (
    <Dialog
      inline={p.inline}
      open={p.open}
      onClose={st === 'waiting' ? undefined : p.onClose}
      title={p.title}
      description={p.description || t('sign.description')}
      footer={[
        <Button key={1} variant="secondary" onClick={p.onClose} disabled={st === 'waiting'} disabledReason={t('sign.enCours')}>
          {st === 'done' ? t('commun.fermer') : t('commun.annuler')}
        </Button>,
        st === 'done' ? <Button key={2} variant="primary" onClick={p.onDone}>{p.doneLabel || t('sign.continuer')}</Button> : null,
      ]}
    >
      <div className="gm-col" style={{ gap: 'var(--gm-space-3)' }}>
        {p.object ? <div className="gm-panel"><KeyValueList items={p.object} /></div> : null}
        {p.extra ?? null}
        {p.reason ? <TextArea name="motif" label={t('confirm.motif')} required value={motif} onChange={setMotif} help={t('confirm.motifAide')} readOnly={st === 'waiting' || st === 'done'} /> : null}
        <div>
          <div className="gm-field-help">{t('sign.empreinte')}</div>
          <Hash value={p.hash} />
        </div>
        <SignatureField
          state={st}
          action={p.action}
          at={p.at}
          onSign={p.onSign ? () => p.onSign!(p.reason ? motif.trim() : '') : undefined}
          disabled={!motifOk}
          disabledReason={motifOk ? undefined : t('confirm.motifObligatoire')}
        />
        {p.error ? <InlineAlert level="crit" title={p.error} /> : null}
        {st === 'done' && p.result ? <InlineAlert level="info" title={t('sign.resultat')}>{p.result}</InlineAlert> : null}
      </div>
    </Dialog>
  )
}
