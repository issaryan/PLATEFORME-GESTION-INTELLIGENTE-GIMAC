import { FileDropzone } from '@gimac/design-system'

export default function Apercu() {
  return (
    <div className="gm-row" style={{ alignItems: 'flex-start', gap: 'var(--gm-space-6)' }}>
      <div style={{ width: 400 }}>
        <FileDropzone name="piece" label="Pièce légale" required accept="PDF, PNG, JPEG" maxSize="20 Mo" file={{ name: 'agrement-cobac-2026.pdf', size: '1,8 Mo' }} state="uploading" progress={62} />
      </div>
      <div style={{ width: 400 }}>
        <FileDropzone
          name="preuve"
          label="Preuve de l'opération"
          accept="PDF"
          maxSize="20 Mo"
          file={{ name: 'rapport-tests-OP-2026-1187.pdf', size: '640 Ko' }}
          state="done"
          masking={{ result: 'rejected', text: 'Le document contient un numéro de carte non masqué (page 3). Masquez le numéro puis déposez à nouveau.' }}
        />
      </div>
    </div>
  )
}
