import { Switch } from '@gimac/design-system'

export default function Apercu() {
  return (
    <div className="gm-row" style={{ gap: 'var(--gm-space-5)' }}>
      <Switch label="Mode sombre" checked={false} onChange={() => {}} />
      <Switch label="Densité compacte" checked onChange={() => {}} />
      <Switch label="Notifications par courriel" checked disabled onChange={() => {}} />
    </div>
  )
}
