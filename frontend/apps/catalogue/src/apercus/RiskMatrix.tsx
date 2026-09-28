import { RiskMatrix } from '@gimac/design-system'

export default function Apercu() {
  return (
    <RiskMatrix
      stage="residual"
      risks={[
        { key: 'R1', label: "Indisponibilité prolongée d'une liaison critique", owner: 'Chef du DES', gross: [4, 5], control: [3, 5], residual: [2, 5] },
        { key: 'R2', label: 'Retard de certification par manque de preuves', owner: 'Homologation', gross: [4, 3], control: [3, 3], residual: [3, 3] },
        { key: 'R3', label: "Dépôt d'une pièce contenant des données de carte", owner: 'RSSI', gross: [3, 4], control: [2, 4], residual: [1, 4] },
        { key: 'R4', label: "Sous-effectif sur la grille d'astreinte", owner: 'Chef de quart', gross: [3, 2], control: [2, 2], residual: [2, 2] },
      ]}
    />
  )
}
