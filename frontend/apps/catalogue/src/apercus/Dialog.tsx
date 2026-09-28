import { Button, Dialog } from '@gimac/design-system'
import { Scene } from '../outils/Scene'

export default function Apercu() {
  return (
    <Scene hauteur={340}>
      <Dialog
        inline
        title="Session expirée"
        description="Votre session a expiré après 30 minutes d'inactivité. Vos brouillons sont conservés."
        footer={[
          <Button key={1} variant="secondary">Se déconnecter</Button>,
          <Button key={2} variant="primary" icon="key-round">Se réauthentifier</Button>,
        ]}
      >
        <p style={{ margin: 0 }}>
          Réauthentifiez-vous auprès du fournisseur d'identité pour reprendre là où vous étiez. Le brouillon « Enregistrer une institution, étape 2 » sera restauré.
        </p>
      </Dialog>
    </Scene>
  )
}
