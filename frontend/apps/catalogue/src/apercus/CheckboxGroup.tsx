import { CheckboxGroup } from '@gimac/design-system'

export default function Apercu() {
  return (
    <CheckboxGroup
      label="Canaux de notification"
      value={['APP', 'COURRIEL']}
      options={[
        { value: 'APP', label: "Dans l'application" },
        { value: 'COURRIEL', label: 'Courriel' },
        { value: 'SMS', label: 'Message texte (urgent et critique seulement)' },
      ]}
      onChange={() => {}}
    />
  )
}
