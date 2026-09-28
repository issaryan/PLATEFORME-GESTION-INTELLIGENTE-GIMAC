import { QuitusCircuit } from '@gimac/design-system'

export default function Apercu() {
  return (
    <div style={{ maxWidth: 720 }}>
      <QuitusCircuit quitus={{ tech: { state: 'signed', at: '25/09/2026 16:10' }, sec: { state: 'pending' }, dg: { state: 'todo' } }} />
    </div>
  )
}
