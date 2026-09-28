import { StateMachineBar } from '@gimac/design-system'

export default function Apercu() {
  return (
    <StateMachineBar
      label="Statut de l'institution"
      current="EN_HOMOLOGATION"
      allowed={['ACTIF']}
      states={[{ key: 'CANDIDAT', label: 'candidat' }, { key: 'EN_HOMOLOGATION', label: 'en homologation' }, { key: 'ACTIF', label: 'actif' }, { key: 'A_REGULARISER', label: 'à régulariser' }, { key: 'SUSPENDU', label: 'suspendu' }, { key: 'RETIRE', label: 'retiré' }]}
    />
  )
}
