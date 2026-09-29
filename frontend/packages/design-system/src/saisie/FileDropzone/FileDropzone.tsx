import { cx } from '../../lib/cx'
import { FieldWrapper } from '../FieldWrapper/FieldWrapper'
import { Icon } from '../../icones/Icon/Icon'
import { IconButton } from '../../actions/IconButton/IconButton'
import { MaskingResult, type MaskingResultProps } from '../../affichage/MaskingResult/MaskingResult'
import { useFieldId2 } from '../../lib/useFieldId2'
import type { BaseProps } from '../../lib/types'

export interface FileDropzoneProps extends BaseProps {
  id?: string
  name?: string
  label: string
  required?: boolean
  help?: string
  error?: string
  /** Types admis, annoncés. */
  accept?: string
  /** Taille maximale, annoncée. */
  maxSize?: string
  file?: { name: string; size: string }
  state?: 'idle' | 'uploading' | 'done'
  progress?: number
  masking?: MaskingResultProps
}

// Dépôt de pièce par glisser ou clic, progression et résultat de la chaîne de contrôle (§7.2).
export function FileDropzone(p: FileDropzoneProps) {
  const id = useFieldId2(p)
  return (
    <FieldWrapper id={id} label={p.label} required={p.required} help={p.help} error={p.error} className={p.className}>
      <div className={cx('gm-dropzone', p.state === 'uploading' && 'gm-dropzone-busy')} tabIndex={0} role="button" aria-describedby={id + '-help'}>
        <Icon name="upload" size={24} />
        <div>
          <strong>Déposez le fichier ici</strong> ou <span className="gm-link">parcourez votre appareil</span>
        </div>
        <div className="gm-field-help" id={id + '-help'}>
          {'Types admis : ' + (p.accept || 'PDF, PNG, JPEG') + ' · taille maximale ' + (p.maxSize || '20 Mo')}
        </div>
      </div>
      {p.file ? (
        <div className="gm-file">
          <Icon name="file-text" size={20} />
          <div style={{ flex: 1 }}>
            <div>
              {p.file.name} <span className="gm-field-help">{p.file.size}</span>
            </div>
            {p.state === 'uploading' ? (
              <div className="gm-progress" role="progressbar" aria-label={'Dépôt de ' + p.file.name} aria-valuenow={p.progress || 0} aria-valuemin={0} aria-valuemax={100}>
                <div className="gm-progress-bar" style={{ width: (p.progress || 0) + '%' }} />
              </div>
            ) : null}
          </div>
          {p.state === 'uploading' ? (
            <span className="gm-num gm-field-help">{(p.progress || 0) + ' %'}</span>
          ) : (
            <IconButton variant="ghost" size="sm" icon="x" label="Retirer le fichier" />
          )}
        </div>
      ) : null}
      {p.masking ? <MaskingResult {...p.masking} /> : null}
    </FieldWrapper>
  )
}
