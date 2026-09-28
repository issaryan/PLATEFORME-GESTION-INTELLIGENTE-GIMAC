import { SearchSelect } from '@gimac/design-system'

export default function Apercu() {
  return (
    <div style={{ width: 420 }}>
      <SearchSelect
        name="banqueReglement"
        label="Banque de règlement"
        required
        query="ban"
        open
        value="INST-0017"
        options={[
          { value: 'INST-0017', code: 'INST-0017', label: 'Banque B' },
          { value: 'INST-0061', code: 'INST-0061', label: 'Banque D' },
          { value: 'INST-0093', code: 'INST-0093', label: 'Banque E, adhésion directe' },
        ]}
        onChange={() => {}}
        onQuery={() => {}}
      />
    </div>
  )
}
