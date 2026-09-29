import { useRef } from 'react'
import { cx } from '../../lib/cx'
import { FieldWrapper } from '../FieldWrapper/FieldWrapper'
import { Icon } from '../../icones/Icon/Icon'
import { IconButton } from '../../actions/IconButton/IconButton'
import { MaskingResult, type MaskingResultProps } from '../../affichage/MaskingResult/MaskingResult'
import { useFieldId2 } from '../../lib/useFieldId2'
import type { BaseProps } from '../../lib/types'
import { useLibelle } from '../../i18n/LangueProvider'

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
  /** Fichier choisi par clic, clavier (Entrée, Espace) ou glisser-déposer. */
  onFile?: (f: File) => void
  /** Retire le fichier choisi (bouton « Retirer le fichier »). */
  onRemove?: () => void
}

// Dépôt de pièce par glisser ou clic, progression et résultat de la chaîne de contrôle (§7.2).
export function FileDropzone(p: FileDropzoneProps) {
  const t = useLibelle()
  const id = useFieldId2(p)
  const input = useRef<HTMLInputElement>(null)
  const ouvrir = () => input.current?.click()
  const actif = !!p.onFile && p.state !== 'uploading'
  return (
    <FieldWrapper id={id} label={p.label} required={p.required} help={p.help} error={p.error} className={p.className}>
      <div
        className={cx('gm-dropzone', p.state === 'uploading' && 'gm-dropzone-busy')}
        tabIndex={0}
        role="button"
        aria-describedby={id + '-help'}
        onClick={actif ? ouvrir : undefined}
        onKeyDown={actif ? (e) => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); ouvrir() } } : undefined}
        onDragOver={actif ? (e) => e.preventDefault() : undefined}
        onDrop={actif ? (e) => { e.preventDefault(); const f = e.dataTransfer.files[0]; if (f) p.onFile!(f) } : undefined}
      >
        <Icon name="upload" size={24} />
        <div>
          <strong>{t('fichier.deposez')}</strong> {t('fichier.ou')} <span className="gm-link">{t('fichier.parcourez')}</span>
        </div>
        <div className="gm-field-help" id={id + '-help'}>
          {t('fichier.types', { types: p.accept || 'PDF, PNG, JPEG', taille: p.maxSize || t('fichier.tailleDefaut') })}
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
              <div className="gm-progress" role="progressbar" aria-label={t('fichier.depot', { nom: p.file.name })} aria-valuenow={p.progress || 0} aria-valuemin={0} aria-valuemax={100}>
                <div className="gm-progress-bar" style={{ width: (p.progress || 0) + '%' }} />
              </div>
            ) : null}
          </div>
          {p.state === 'uploading' ? (
            <span className="gm-num gm-field-help">{(p.progress || 0) + ' %'}</span>
          ) : (
            <IconButton variant="ghost" size="sm" icon="x" label={t('fichier.retirer')} onClick={p.onRemove} />
          )}
        </div>
      ) : null}
      {p.masking ? <MaskingResult {...p.masking} /> : null}
      {p.onFile ? (
        <input
          ref={input}
          type="file"
          hidden
          tabIndex={-1}
          aria-hidden="true"
          accept={p.accept ? p.accept.split(/[,\s]+/).filter(Boolean).map((x) => (x.includes('/') || x.startsWith('.') ? x : '.' + x.toLowerCase())).join(',') : undefined}
          onChange={(e) => { const f = e.target.files && e.target.files[0]; if (f) p.onFile!(f); e.target.value = '' }}
        />
      ) : null}
    </FieldWrapper>
  )
}
