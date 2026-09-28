import { IconButton } from '@gimac/design-system'

export default function Apercu() {
  return (
    <div className="gm-row">
      <IconButton variant="ghost" icon="bell-ring" label="Notifications" />
      <IconButton variant="secondary" icon="download" label="Télécharger la pièce" />
      <IconButton variant="ghost" icon="copy" label="Copier l'identifiant" size="sm" />
      <IconButton variant="ghost" icon="x" label="Fermer" />
    </div>
  )
}
