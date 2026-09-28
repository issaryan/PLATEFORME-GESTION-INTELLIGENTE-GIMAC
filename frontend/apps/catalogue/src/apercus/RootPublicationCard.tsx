import { RootPublicationCard } from '@gimac/design-system'

export default function Apercu() {
  return (
    <div style={{ maxWidth: 640 }}>
      <RootPublicationCard
        period="septembre 2026, semaine 39"
        root="3f9a1c7e2b4d8f01a6c5e9b2d7f4a1c83e6b9d2f5a8c1e4b7d0f3a6c9e2b5d8f"
        tst="d7f4a1c83e6b9d2f5a8c1e4b7d0f3a6c9e2b5d8f3f9a1c7e2b4d8f01a6c5e9b2"
        publication="PUB-2026-W39-001"
        count={184203}
        at="28/09/2026 00:05"
        verdict="ok"
      />
    </div>
  )
}
