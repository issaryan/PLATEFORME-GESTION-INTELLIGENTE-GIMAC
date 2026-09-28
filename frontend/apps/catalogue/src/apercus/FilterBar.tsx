import { FilterBar } from '@gimac/design-system'

export default function Apercu() {
  return (
    <FilterBar
      query=""
      filters={[
        { key: 'statut', label: 'Statut', value: 'ACTIF', options: [{ value: 'ACTIF', label: 'actif' }, { value: 'A_REGULARISER', label: 'à régulariser' }, { value: 'SUSPENDU', label: 'suspendu' }] },
        { key: 'pays', label: 'Pays', options: [{ value: 'CM', label: 'Cameroun' }, { value: 'GA', label: 'Gabon' }] },
        { key: 'periode', label: 'Échéance', type: 'daterange', value: '28/09/2026 au 05/10/2026' },
      ]}
      active={[{ key: 'statut', label: 'Statut : actif' }, { key: 'periode', label: 'Échéance : 7 prochains jours' }]}
      favorites={['À régulariser cette semaine', 'Mes institutions']}
    />
  )
}
