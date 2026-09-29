import { StatGrid } from '@gimac/design-system'

export default function Apercu() {
  return (
    <StatGrid
      label="Relations avec les membres"
      items={[
        { key: 'propositions', label: 'Propositions à trancher', value: 1, status: 'warn', statusLabel: 'à trancher', onClick: () => {} },
        { key: 'candidats', label: 'Candidats', value: 3 },
        { key: 'adherents', label: 'Adhérents sans liaison', value: 2 },
        { key: 'cadrage', label: 'Liaisons en cadrage', value: 1, status: 'info', statusLabel: 'à suivre' },
      ]}
    />
  )
}
