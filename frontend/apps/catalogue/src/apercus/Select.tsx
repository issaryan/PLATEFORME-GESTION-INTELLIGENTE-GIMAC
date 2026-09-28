import { Select } from '@gimac/design-system'

export default function Apercu() {
  return (
    <div className="gm-row" style={{ alignItems: 'flex-start' }}>
      <div style={{ width: 240 }}>
        <Select name="categorie" label="Catégorie" required value="BANQUE" options={[{ value: 'BANQUE', label: 'Banque' }, { value: 'EP', label: 'Établissement de paiement' }, { value: 'EMF', label: 'Établissement de microfinance' }]} onChange={() => {}} />
      </div>
      <div style={{ width: 240 }}>
        <Select name="pays" label="Pays" value="CM" options={[{ value: 'CM', label: 'Cameroun' }, { value: 'CF', label: 'République centrafricaine' }, { value: 'CG', label: 'Congo' }, { value: 'GA', label: 'Gabon' }, { value: 'GQ', label: 'Guinée équatoriale' }, { value: 'TD', label: 'Tchad' }]} onChange={() => {}} />
      </div>
    </div>
  )
}
