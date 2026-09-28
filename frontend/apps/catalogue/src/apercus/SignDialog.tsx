import { SignDialog } from '@gimac/design-system'
import { Scene } from '../outils/Scene'

export default function Apercu() {
  return (
    <Scene hauteur={560}>
      <SignDialog
        inline
        title="Signer le quitus technique"
        action="Signer le quitus technique"
        state="idle"
        hash="3f9a1c7e2b4d8f01a6c5e9b2d7f4a1c83e6b9d2f5a8c1e4b7d0f3a6c9e2b5d8f"
        object={[
          { key: 'Liaison', value: 'LIA-2026-017', mono: true },
          { key: 'Institution', value: 'Établissement de paiement A' },
          { key: "Socle d'évaluation", value: 'référentiel v3.2, 14 opérations closes' },
          { key: 'Signataire', value: 'Chef du DES (vous)' },
        ]}
      />
    </Scene>
  )
}
