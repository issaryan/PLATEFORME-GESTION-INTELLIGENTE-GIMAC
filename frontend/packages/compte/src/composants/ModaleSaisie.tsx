import type { ReactNode } from 'react'
import { CrudDialog, type CrudContext, type IconName, type KeyValueItem } from '@gimac/design-system'

// Point unique d'emploi de la modale CRUD standard du système de conception (CrudDialog, D-63) dans
// « Mon compte » : fiche, mot de passe, code TOTP et clé de sécurité (contexte « edit » ou « create »),
// suppression d'une clé et fermeture des sessions (« delete », « confirm »). Une évolution du contrat de
// la modale ne touche que ce fichier.
export type ErreurModale = { title: string; text?: ReactNode; correlationId?: string }

export type ModaleCompteProps = {
  id: string
  context?: CrudContext
  title: string
  description?: string
  submitLabel: string
  submitIcon?: IconName
  onSubmit: () => void
  onClose: () => void
  submitting?: boolean
  submitDisabled?: boolean
  submitDisabledReason?: string
  error?: ErreurModale | null
  size?: 'sm' | 'md' | 'lg'
  /** Saisie modifiée : la fermeture demande de confirmer l'abandon. */
  dirty?: boolean
  object?: KeyValueItem[]
  irreversible?: boolean
  children?: ReactNode
}

export function ModaleCompte(p: ModaleCompteProps) {
  return (
    <CrudDialog id={p.id} context={p.context ?? 'edit'} title={p.title} description={p.description} submitLabel={p.submitLabel} submitIcon={p.submitIcon}
      onSubmit={() => p.onSubmit()} onClose={p.onClose} submitting={p.submitting} submitDisabled={p.submitDisabled} submitDisabledReason={p.submitDisabledReason}
      error={p.error ?? undefined} size={p.size} dirty={p.dirty} object={p.object} irreversible={p.irreversible}>
      {p.children}
    </CrudDialog>
  )
}
