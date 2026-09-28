import { ConferenceControls } from '@gimac/design-system'

export default function Apercu() {
  return (
    <div className="gm-col" style={{ gap: 'var(--gm-space-2)' }}>
      <ConferenceControls mic camera={false} sharing={false} participants={5} recording />
    </div>
  )
}
