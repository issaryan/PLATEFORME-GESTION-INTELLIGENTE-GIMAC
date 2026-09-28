import { PolicyMatrix } from '@gimac/design-system'

export default function Apercu() {
  return (
    <div style={{ maxWidth: 900 }}>
      <PolicyMatrix
        roles={['Chef du DES', 'Homologation', 'Opérateur', 'Conformité', 'Correspondant']}
        permissions={[
          { key: 'PAR_INSTITUTION_CREATE', label: 'Enregistrer une institution', grants: { 'Chef du DES': true } },
          { key: 'PAR_STATUS_APPROVE', label: 'Approuver une transition de statut', grants: { 'Chef du DES': true, Conformité: true } },
          { key: 'HOM_OPERATION_CLOSE', label: 'Clore une opération', grants: { Homologation: true }, pending: { Opérateur: true } },
          { key: 'MON_ALERT_ACK', label: 'Acquitter une alerte', grants: { Opérateur: true, 'Chef du DES': true } },
          { key: 'DOC_DEPOSIT', label: 'Déposer une pièce', grants: { Conformité: true, Correspondant: true } },
        ]}
      />
    </div>
  )
}
