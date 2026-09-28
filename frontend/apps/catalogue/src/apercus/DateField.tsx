import { DateField, DateRangeField, DateTimeField } from '@gimac/design-system'

export default function Apercu() {
  return (
    <div className="gm-row" style={{ alignItems: 'flex-start', gap: 'var(--gm-space-6)' }}>
      <div style={{ width: 320 }}>
        <DateField name="echeance" label="Date d'échéance de la pièce" value="28/09/2026" required open onChange={() => {}} />
      </div>
      <div className="gm-col" style={{ width: 360 }}>
        <DateTimeField name="debut" label="Début de l'incident" date="28/09/2026" time="13:42" onChange={() => {}} />
        <DateRangeField name="periode" label="Période du rapport" from="21/09/2026" to="28/09/2026" onChange={() => {}} />
      </div>
    </div>
  )
}
