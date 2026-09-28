import { ResourceList } from '@gimac/design-system'
import { institutionDescriptor, sampleInstitutions } from '../fabriques/institution'

export default function Apercu() {
  return (
    <ResourceList
      inline
      descriptor={institutionDescriptor}
      rows={sampleInstitutions}
      total={1284}
      permissions={['PAR_INSTITUTION_LIST', 'PAR_INSTITUTION_READ', 'PAR_INSTITUTION_CREATE', 'PAR_STATUS_REQUEST', 'PAR_STATUS_APPROVE', 'PAR_SIGNAL', 'PAR_EXPORT', 'PAR_REMIND']}
      activeFilters={[{ key: 'statut', label: 'Statut : tous sauf retiré' }]}
      favorites={['À régulariser']}
      state="ok"
    />
  )
}
