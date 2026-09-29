import { IconButton, Toast } from '@gimac/design-system'

// L'emplacement réel est fixe en bas à droite de la fenêtre ; l'aperçu montre le retour qu'il porte.
export default function Apercu() {
  return (
    <div className="gm-col">
      <Toast action={<IconButton icon="x" variant="ghost" size="sm" label="Fermer" />}>Action « Désigner le conducteur » effectuée.</Toast>
    </div>
  )
}
