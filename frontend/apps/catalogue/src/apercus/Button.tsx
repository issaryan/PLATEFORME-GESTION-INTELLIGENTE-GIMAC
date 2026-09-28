import { Button } from '@gimac/design-system'

export default function Apercu() {
  return (
    <div className="gm-col">
      <div className="gm-row">
        <Button variant="primary" icon="landmark">Enregistrer une institution</Button>
        <Button variant="secondary">Exporter (contrôlé)</Button>
        <Button variant="danger" icon="octagon-alert">Signaler une non-conformité</Button>
        <Button variant="ghost">Ouvrir</Button>
      </div>
      <div className="gm-row">
        <Button variant="primary" size="sm">Acquitter l'alerte</Button>
        <Button variant="primary" loading>Signer le quitus</Button>
        <Button variant="secondary" disabled disabledReason="Le validateur ne peut pas être le responsable de l'opération">Clore l'opération</Button>
        <Button variant="primary" size="lg">Déposer une pièce</Button>
      </div>
    </div>
  )
}
