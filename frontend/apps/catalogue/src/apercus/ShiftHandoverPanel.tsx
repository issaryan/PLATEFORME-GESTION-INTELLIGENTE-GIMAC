import { ShiftHandoverPanel } from '@gimac/design-system'

export default function Apercu() {
  return (
    <div style={{ maxWidth: 640 }}>
      <ShiftHandoverPanel shift="Quart de nuit, 27/09 22:00 à 28/09 06:00" summary="2 alertes acquittées, 1 incident S2 en cours (TCK-000302), aucune bascule planifiée." signatures={{ out: '28/09/2026 05:58' }} />
    </div>
  )
}
