import { MultiSelect } from '@gimac/design-system'

export default function Apercu() {
  return (
    <div style={{ width: 520 }}>
      <MultiSelect
        name="services"
        label="Services souscrits"
        value={['RETRAIT', 'PAIEMENT', 'TRANSFERT']}
        options={[
          { value: 'RETRAIT', label: 'Retrait' },
          { value: 'PAIEMENT', label: 'Paiement' },
          { value: 'TRANSFERT', label: 'Transfert' },
          { value: 'ECOM', label: 'Commerce électronique' },
        ]}
        onChange={() => {}}
        onQuery={() => {}}
      />
    </div>
  )
}
