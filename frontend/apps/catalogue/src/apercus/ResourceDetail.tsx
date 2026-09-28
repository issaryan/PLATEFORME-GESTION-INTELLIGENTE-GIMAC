import { ResourceDetail } from '@gimac/design-system'
import { institutionDescriptor, sampleInstitutions } from '../fabriques/institution'

export default function Apercu() {
  return (
    <ResourceDetail
      descriptor={institutionDescriptor}
      row={sampleInstitutions[0]}
      mode="page"
      permissions={['PAR_STATUS_REQUEST', 'PAR_STATUS_APPROVE', 'PAR_SIGNAL']}
      timeline={[{ event: 'Transition approuvée : en homologation vers actif', at: '12/03/2024 10:02', actor: 'Chef du DES', correlationId: 'RQ-2024-0312-2' }]}
      audit={['RQ-2026-0928-14', 'RQ-2026-0927-3']}
    />
  )
}
