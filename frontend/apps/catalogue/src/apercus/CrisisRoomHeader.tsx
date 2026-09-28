import { CrisisRoomHeader } from '@gimac/design-system'

export default function Apercu() {
  return (
    <CrisisRoomHeader
      id="CRI-2026-009"
      level={2}
      since="13:48"
      title="Indisponibilité de la liaison LIA-2026-017"
      present={4}
      expected={6}
      quorum={4}
      members={[
        { role: 'Chef du DES', ack: true },
        { role: 'RSSI', ack: true },
        { role: 'Chef de quart', ack: true },
        { role: 'Production monétique', ack: true },
        { role: 'Conformité', ack: false },
        { role: 'Direction générale (suppléant)', ack: false },
      ]}
    />
  )
}
