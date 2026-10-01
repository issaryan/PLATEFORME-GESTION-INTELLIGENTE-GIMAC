import { useCallback, useId, useRef, useState, type FormEvent, type ReactNode } from 'react'
import type { KeyValueItem } from '../../lib/types'
import type { IconName } from '../../icones/icons'
import { Button } from '../../actions/Button/Button'
import { KeyValueList } from '../../affichage/KeyValueList/KeyValueList'
import { TextArea } from '../../saisie/TextArea/TextArea'
import { Checkbox } from '../../saisie/Checkbox/Checkbox'
import { InlineAlert } from '../InlineAlert/InlineAlert'
import { Dialog } from '../Dialog/Dialog'
import { useLibelle } from '../../i18n/LangueProvider'

/**
 * Contexte de la modale CRUD : il fixe la taille, le pictogramme et la couleur du bouton principal, le rôle
 * annoncé et les garde-fous par défaut.
 * - create : création d'un objet (large, bouton « + ») ;
 * - edit : modification ou mise à jour (large) ;
 * - delete : suppression ou action destructive (moyenne, mise en garde, case « irréversible », bouton danger) ;
 * - sensitive : action sensible (moyenne, motif obligatoire, réauthentification FIDO2 annoncée si `reauth`) ;
 * - confirm : confirmation d'une action à effet (moyenne) ;
 * - detail : lecture seule (large, seul « Fermer » au pied).
 */
export type CrudContext = 'create' | 'edit' | 'delete' | 'sensitive' | 'confirm' | 'detail'

export interface CrudDialogProps {
  context?: CrudContext
  /** Titre : verbe et objet (« Modifier la fiche », « Supprimer le compte »). */
  title: string
  /** Sous-titre de contexte : l'objet concerné (« Compte c.ndong · Carine Ebongue »). */
  subject?: ReactNode
  description?: string
  /** Petite (440 px), moyenne (560 px) ou large (720 px) ; par défaut selon le contexte. */
  size?: 'sm' | 'md' | 'lg'
  open?: boolean
  /** Fermeture (Annuler, ×, Échap, clic sur le voile) ; refusée pendant l'envoi, confirmée si le formulaire est modifié. */
  onClose: () => void
  /** Action principale ; reçoit le motif saisi (chaîne vide sans motif). Absente en lecture seule. */
  onSubmit?: (motif: string) => void
  /** Libellé du bouton principal : verbe et objet, jamais « OK ». Sans libellé, pas de bouton principal. */
  submitLabel?: string
  submitIcon?: IconName
  /** Envoi en cours : bouton principal occupé, fermeture impossible. */
  submitting?: boolean
  submitDisabled?: boolean
  /** Raison affichée quand le bouton principal est désactivé : jamais désactivé sans explication. */
  submitDisabledReason?: string
  /** Libellé du bouton secondaire (« Annuler », « Fermer » en lecture seule). */
  cancelLabel?: string
  /** Formulaire modifié : toute fermeture demande de confirmer l'abandon de la saisie. */
  dirty?: boolean
  /** Refus du service, affiché en tête du corps (InlineAlert critique avec l'identifiant de corrélation). */
  error?: { title: string; text?: ReactNode; correlationId?: string }
  /** Rappel de l'objet de l'action (paires clé-valeur). */
  object?: KeyValueItem[]
  /** Conséquences de l'action, en liste (mise en garde d'une suppression). */
  consequences?: string[]
  /** Motif obligatoire (10 caractères au moins) ; vrai par défaut pour une action sensible. */
  reason?: boolean
  /** Case « je confirme : irréversible » ; vraie par défaut pour une suppression. */
  irreversible?: boolean
  irreversibleLabel?: string
  /** Le geste exige la réauthentification FIDO2 : annoncée au pied du corps, clé demandée par l'action principale. */
  reauth?: boolean
  /** Boutons complémentaires du pied, avant le bouton principal (« Étape précédente »). */
  extraActions?: ReactNode
  /** Bouton principal en danger (défaut : suppression) : désactivation, réinitialisation… */
  danger?: boolean
  /** Rôle annoncé : alertdialog par défaut pour delete, sensitive et confirm. */
  role?: 'dialog' | 'alertdialog'
  id?: string
  inline?: boolean
  children?: ReactNode
}

const TAILLE: Record<CrudContext, 'sm' | 'md' | 'lg'> = { create: 'lg', edit: 'lg', delete: 'md', sensitive: 'md', confirm: 'md', detail: 'lg' }
const ICONE: Partial<Record<CrudContext, IconName>> = { create: 'plus', edit: 'pen-line', delete: 'x', sensitive: 'key-round' }

// Modale CRUD standard (D-63) : toute création, modification, suppression, action sensible ou lecture de
// détail déclenchée depuis une fiche ou une liste s'ouvre dans cette fenêtre, adaptée à son contexte.
// Assemblage de Dialog (voile, focus piégé, Échap, aria-modal, titre et sous-titre de contexte), d'un
// formulaire natif et d'un pied d'actions standard (Annuler, action principale reliée au formulaire par
// l'attribut form, avec état de chargement). Pendant l'envoi, ni Échap, ni le voile, ni « Annuler » ne
// ferment ; un formulaire modifié demande confirmation avant l'abandon de la saisie.
export function CrudDialog(p: CrudDialogProps) {
  const t = useLibelle()
  const contexte = p.context || 'edit'
  const auto = useId().replace(/:/g, '')
  const id = p.id || `gm-crud-${auto}`
  const formulaire = `${id}-form`
  const reason = p.reason ?? contexte === 'sensitive'
  const irreversible = p.irreversible ?? contexte === 'delete'
  const [motif, setMotif] = useState('')
  const [coche, setCoche] = useState(false)
  const [abandon, setAbandon] = useState(false)
  const motifOk = !reason || motif.trim().length >= 10
  const cocheOk = !irreversible || coche
  const bloque = !!p.submitDisabled || !motifOk || !cocheOk
  const raison = p.submitDisabled ? p.submitDisabledReason : !motifOk ? t('confirm.motifObligatoire') : !cocheOk ? t('confirm.cocher') : undefined

  // Fermeture demandée (Annuler, ×, Échap, voile) : lue par référence, le piège de focus ne se réarmant pas.
  const etat = useRef({ submitting: p.submitting, dirty: p.dirty, abandon, onClose: p.onClose })
  etat.current = { submitting: p.submitting, dirty: p.dirty, abandon, onClose: p.onClose }
  const fermer = useCallback(() => {
    const e = etat.current
    if (e.submitting) return
    if (e.abandon) return setAbandon(false)
    if (e.dirty) return setAbandon(true)
    e.onClose()
  }, [])

  const soumettre = (e: FormEvent) => {
    e.preventDefault()
    if (p.submitting || bloque || abandon || !p.onSubmit || !p.submitLabel) return
    p.onSubmit(reason ? motif.trim() : '')
  }

  const pied = abandon
    ? [
        <Button key="reprendre" variant="secondary" onClick={() => setAbandon(false)}>{t('crudDialog.abandon.reprendre')}</Button>,
        <Button key="abandonner" variant="danger" onClick={() => p.onClose()}>{t('crudDialog.abandon.confirmer')}</Button>,
      ]
    : [
        <Button key="annuler" variant="secondary" onClick={fermer} disabled={p.submitting} disabledReason={t('crudDialog.enCours')}>
          {p.cancelLabel || t(contexte === 'detail' ? 'commun.fermer' : 'commun.annuler')}
        </Button>,
        p.extraActions ? <span key="extra" className="gm-row">{p.extraActions}</span> : null,
        p.submitLabel && contexte !== 'detail' ? (
          <Button
            key="soumettre"
            type="submit"
            form={formulaire}
            variant={p.danger ?? contexte === 'delete' ? 'danger' : 'primary'}
            icon={p.submitIcon ?? ICONE[contexte]}
            loading={p.submitting}
            disabled={bloque}
            disabledReason={raison}
          >
            {p.submitLabel}
          </Button>
        ) : null,
      ]

  return (
    <Dialog
      open={p.open}
      inline={p.inline}
      id={id}
      size={p.size || TAILLE[contexte]}
      title={p.title}
      subtitle={p.subject}
      description={p.description}
      role={p.role || (contexte === 'delete' || contexte === 'sensitive' || contexte === 'confirm' ? 'alertdialog' : 'dialog')}
      className={`gm-crud gm-crud-${contexte}`}
      onClose={p.submitting ? undefined : fermer}
      footer={pied}
    >
      <form id={formulaire} className="gm-col" noValidate onSubmit={soumettre} aria-labelledby={id + '-title'} data-context={contexte}>
        {abandon ? (
          <InlineAlert level="warn" title={t('crudDialog.abandon.titre')}>{t('crudDialog.abandon.texte')}</InlineAlert>
        ) : null}
        {p.error ? <InlineAlert level="crit" title={p.error.title} correlationId={p.error.correlationId}>{p.error.text}</InlineAlert> : null}
        {contexte === 'delete' && irreversible ? <InlineAlert level="warn" title={t('crudDialog.suppression')} /> : null}
        {p.object ? <div className="gm-panel"><KeyValueList items={p.object} /></div> : null}
        {p.consequences ? <ul className="gm-consequences">{p.consequences.map((c, i) => <li key={i}>{c}</li>)}</ul> : null}
        {p.children}
        {reason ? <TextArea name="motif" label={t('confirm.motif')} required value={motif} onChange={setMotif} help={t('confirm.motifAide')} readOnly={p.submitting} /> : null}
        {irreversible ? <Checkbox name="irreversible" label={p.irreversibleLabel || t('confirm.irreversible')} checked={coche} onChange={setCoche} /> : null}
        {p.reauth ? <InlineAlert level="info" title={t('crudDialog.reauth')} /> : null}
      </form>
    </Dialog>
  )
}
