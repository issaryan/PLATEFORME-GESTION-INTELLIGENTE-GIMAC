import { ConfirmDialog } from '../../conteneurs/ConfirmDialog/ConfirmDialog'
import { SignDialog } from '../../conteneurs/SignDialog/SignDialog'
import type { ResourceDescriptor } from '../descripteur'
import { champ } from '../outils'
import { useLibelle } from '../../i18n/LangueProvider'

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
  const t = useLibelle()
  const desc = p.descriptor
  const row = (p.row || {}) as T
  const archive = !!desc.endpoints.archive || !desc.endpoints.delete
  const g = desc.label.gender === 'f' ? 'f' : 'm'
  const title = t(archive ? (g === 'f' ? 'crud.archiver.f' : 'crud.archiver.m') : g === 'f' ? 'crud.supprimer.f' : 'crud.supprimer.m', { objet: desc.label.singular })
  const object = [
    { key: desc.label.singular, value: String(champ(row, desc.titleField)) },
    { key: t('crud.reference'), value: String(champ(row, desc.idField)), mono: true },
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
          ? [t('crud.archive1'), t('crud.archive2')]
          : [t('crud.suppression')]
      }
    />
  )
}
