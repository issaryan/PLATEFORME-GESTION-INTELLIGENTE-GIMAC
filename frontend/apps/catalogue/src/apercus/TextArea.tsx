import { TextArea } from '@gimac/design-system'

export default function Apercu() {
  return (
    <div style={{ maxWidth: 520 }}>
      <TextArea name="motif" label="Motif de la demande de changement de statut" required value="Dossier de mise en service complet : les trois quitus sont signés et l'attestation est déposée." help="Le motif est consigné dans la chronologie et le registre d'audit." onChange={() => {}} />
    </div>
  )
}
