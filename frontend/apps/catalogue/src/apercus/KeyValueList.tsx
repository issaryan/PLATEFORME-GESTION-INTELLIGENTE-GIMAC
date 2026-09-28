import { KeyValueList, StatusBadge } from '@gimac/design-system'

export default function Apercu() {
  return (
    <KeyValueList
      items={[
        { key: 'Code participant', value: 'INST-0042', mono: true },
        { key: 'Dénomination', value: 'Établissement de paiement A' },
        { key: 'Catégorie', value: 'Établissement de paiement' },
        { key: "Mode d'adhésion", value: 'Indirect, banque marraine : Banque B' },
        { key: 'Statut', value: <StatusBadge status="ok" label="actif" /> },
      ]}
    />
  )
}
