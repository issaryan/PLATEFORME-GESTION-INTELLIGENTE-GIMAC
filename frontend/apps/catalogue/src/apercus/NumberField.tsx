import { NumberField } from '@gimac/design-system'

export default function Apercu() {
  return (
    <div className="gm-row" style={{ alignItems: 'flex-start' }}>
      <div style={{ width: 240 }}><NumberField name="montant" label="Montant" value="1 250 000" unit="XAF" onChange={() => {}} /></div>
      <div style={{ width: 200 }}><NumberField name="seuil" label="Seuil de disponibilité" value="95" unit="%" onChange={() => {}} /></div>
    </div>
  )
}
