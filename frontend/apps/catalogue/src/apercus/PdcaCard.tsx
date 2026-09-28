import { PdcaCard } from '@gimac/design-system'

export default function Apercu() {
  return (
    <div style={{ maxWidth: 640 }}>
      <PdcaCard
        title="Réduire le délai moyen de résolution des S2"
        indicator="Délai moyen de résolution S2"
        origin="cockpit du DES, septembre 2026"
        phase="do"
        owner="Chef de quart"
        due="31/10/2026"
        guard="Le compte rendu de la phase « réaliser » n'est pas encore déposé."
      />
    </div>
  )
}
