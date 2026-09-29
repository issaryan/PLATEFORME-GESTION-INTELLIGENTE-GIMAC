import { FileDropzone, type FileDropzoneProps } from '@gimac/design-system'

// Choix d'un fichier par le FileDropzone du catalogue (clic, clavier, dépôt par glisser, retrait) :
// ChampFichier ne fait que relier le fichier choisi à l'état de la page. Aucun rendu propre.
export type ChampFichierProps = Omit<FileDropzoneProps, 'file' | 'onFile' | 'onRemove'> & { fichier: File | null; onFichier: (f: File | null) => void }

const taille = (o: number) => (o >= 1048576 ? `${(o / 1048576).toFixed(1).replace('.', ',')} Mo` : `${Math.max(1, Math.round(o / 1024))} Ko`)

export function ChampFichier({ fichier, onFichier, ...p }: ChampFichierProps) {
  return (
    <FileDropzone
      {...p}
      file={fichier ? { name: fichier.name, size: taille(fichier.size) } : undefined}
      state={p.state ?? (fichier ? 'done' : 'idle')}
      onFile={(f) => onFichier(f)}
      onRemove={() => onFichier(null)}
    />
  )
}
