import { SlaClock } from '@gimac/design-system'

export default function Apercu() {
  return (
    <div className="gm-row" style={{ alignItems: 'stretch' }}>
      <div style={{ width: 340 }}><SlaClock elapsedMin={135} targetMin={240} /></div>
      <div style={{ width: 340 }}><SlaClock elapsedMin={262} targetMin={240} label="Délai de résolution S1" /></div>
      <div style={{ width: 340 }}><SlaClock elapsedMin={90} targetMin={480} frozen frozenMin={35} /></div>
    </div>
  )
}
