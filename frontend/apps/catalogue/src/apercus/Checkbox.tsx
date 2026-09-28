import { Checkbox, RadioGroup } from '@gimac/design-system'

export default function Apercu() {
  return (
    <div className="gm-col" style={{ gap: 'var(--gm-space-3)' }}>
      <Checkbox label="Je confirme la clôture de l'opération OP-2026-1187" checked onChange={() => {}} />
      <Checkbox label="Inclure les liaisons closes" checked={false} onChange={() => {}} />
      <RadioGroup name="mode" label="Mode d'adhésion" value="DIRECT" options={[{ value: 'DIRECT', label: 'Direct' }, { value: 'INDIRECT', label: 'Indirect' }]} onChange={() => {}} />
    </div>
  )
}
