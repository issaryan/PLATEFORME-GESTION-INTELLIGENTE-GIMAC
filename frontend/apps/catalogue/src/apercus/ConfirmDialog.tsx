import { ConfirmDialog } from '@gimac/design-system'
import { Scene } from '../outils/Scene'

export default function Apercu() {
  return (
    <Scene hauteur={660}>
      <ConfirmDialog
        inline
        danger
        title="Signaler une non-conformité"
        confirmLabel="Signaler la non-conformité"
        reason
        irreversible
        irreversibleLabel="Je confirme le signalement ; la production monétique sera notifiée."
        object={[
          { key: 'Institution', value: 'Établissement de paiement A' },
          { key: 'Code participant', value: 'INST-0042', mono: true },
          { key: 'Statut actuel', value: 'actif' },
        ]}
        consequences={[
          'La plateforme ne suspend rien : la production monétique est notifiée et décide.',
          "Le signalement est visible dans la chronologie de l'institution et dans le registre d'audit.",
        ]}
      />
    </Scene>
  )
}
