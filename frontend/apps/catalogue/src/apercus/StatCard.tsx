import { StatCard } from '@gimac/design-system'

export default function Apercu() {
  return (
    <div className="gm-row" style={{ alignItems: 'stretch' }}>
      <StatCard label="Respect des engagements" value="97,4" unit="%" trend={{ direction: 'up', delta: '+1,2 pt' }} threshold="95 %" status="ok" statusLabel="nominal" onClick={() => {}} />
      <StatCard label="Alertes non acquittées" value="3" trend={{ direction: 'up', delta: '+2' }} status="warn" statusLabel="à surveiller" onClick={() => {}} />
      <StatCard label="Suspens ouverts à J+30" value="12" status="crit" statusLabel="seuil franchi" threshold="10" onClick={() => {}} />
      <StatCard label="Délai moyen de résolution" value="2 h 15" unit="min" trend={{ direction: 'down', delta: '-18 min' }} />
    </div>
  )
}
