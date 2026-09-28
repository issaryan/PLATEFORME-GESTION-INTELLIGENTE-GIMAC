import { Button, IconButton, InstitutionHeader } from '@gimac/design-system'

export default function Apercu() {
  return (
    <InstitutionHeader
      code="INST-0042"
      category="Établissement de paiement"
      country="CM"
      name="Établissement de paiement A"
      acronym="EPA"
      status="ok"
      statusLabel="actif"
      compliance="warn"
      complianceLabel="à régulariser"
      mode="indirecte, banque marraine : Banque B"
      actions={[
        <Button key={1} variant="secondary" icon="route">Demander un changement de statut</Button>,
        <Button key={2} variant="primary" icon="landmark">Modifier la fiche</Button>,
        <IconButton key={3} variant="ghost" icon="menu" label="Plus d'actions" />,
      ]}
    />
  )
}
