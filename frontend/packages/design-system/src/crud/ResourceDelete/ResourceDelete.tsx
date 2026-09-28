import { ConfirmDialog } from '../../conteneurs/ConfirmDialog/ConfirmDialog'
import { SignDialog } from '../../conteneurs/SignDialog/SignDialog'
import type { ResourceDescriptor } from '../descripteur'
import { champ } from '../outils'

export interface ResourceDeleteProps<T> {
  descriptor: ResourceDescriptor<T>
  row: T
  onClose?: () => void
  onDone?: () => void
  confirm?: 'confirm' | 'sign'
  signHash?: string
  inline?: boolean
}

// Suppression ou archivage à confirmation ; jamais de suppression physique d'un objet réglementaire (§8.3).
export function ResourceDelete<T extends object>(p: ResourceDeleteProps<T>) {
  const desc = p.descriptor
  const row = (p.row || {}) as T
  const archive = !!desc.endpoints.archive || !desc.endpoints.delete
  const verb = archive ? 'Archiver' : 'Supprimer'
  const article = desc.label.gender === 'f' ? 'la ' : 'le '
  const title = verb + ' ' + article + desc.label.singular
  const object = [
    { key: desc.label.singular, value: String(champ(row, desc.titleField)) },
    { key: 'Référence', value: String(champ(row, desc.idField)), mono: true },
  ]
  if (p.confirm === 'sign')
    return (
      <SignDialog
        inline={p.inline}
        title={title}
        action={title}
        hash={p.signHash || '0000000000000000000000000000000000000000000000000000000000000000'}
        object={object}
        onClose={p.onClose}
      />
    )
  return (
    <ConfirmDialog
      inline={p.inline}
      title={title}
      confirmLabel={title}
      danger
      reason
      irreversible={!archive}
      onClose={p.onClose}
      onConfirm={p.onDone}
      object={object}
      consequences={
        archive
          ? [
              "Aucune suppression physique : l'objet est archivé et reste consultable en lecture seule, conformément à la rétention réglementaire.",
              'Les liaisons, tickets et pièces rattachés restent accessibles depuis le registre.',
            ]
          : ["La suppression est définitive et consignée dans le registre d'audit."]
      }
    />
  )
}
