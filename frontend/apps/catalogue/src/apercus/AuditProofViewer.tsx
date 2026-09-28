import { AuditProofViewer } from '@gimac/design-system'

export default function Apercu() {
  return (
    <div style={{ maxWidth: 760 }}>
      <AuditProofViewer
        eventId="EVT-2026-09-28-004417"
        leaf="3f9a1c7e2b4d8f01a6c5e9b2d7f4a1c83e6b9d2f5a8c1e4b7d0f3a6c9e2b5d8f"
        path={[
          { side: 'right', hash: '9b2d7f4a1c83e6b9d2f5a8c1e4b7d0f3a6c9e2b5d8f3f9a1c7e2b4d8f01a6c5e' },
          { side: 'left', hash: 'a6c5e9b2d7f4a1c83e6b9d2f5a8c1e4b7d0f3a6c9e2b5d8f3f9a1c7e2b4d8f01' },
          { side: 'left', hash: 'e6b9d2f5a8c1e4b7d0f3a6c9e2b5d8f3f9a1c7e2b4d8f01a6c5e9b2d7f4a1c83' },
        ]}
        period="semaine 39"
        root="d7f4a1c83e6b9d2f5a8c1e4b7d0f3a6c9e2b5d8f3f9a1c7e2b4d8f01a6c5e9b2"
        publication="PUB-2026-W39-001"
        tst="0f3a6c9e2b5d8f3f9a1c7e2b4d8f01a6c5e9b2d7f4a1c83e6b9d2f5a8c1e4b7d"
        tstAt="28/09/2026 00:05, autorité d'horodatage interne"
        verdict="ok"
        correlationId="RQ-2026-0928-31"
      />
    </div>
  )
}
