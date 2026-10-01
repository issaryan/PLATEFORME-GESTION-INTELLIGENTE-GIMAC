import { Avatar } from '@gimac/design-system'
import { photoDemoCatalogue } from '../outils/photoDemo'

// Avatar : initiales à défaut de photo, photo recadrée sinon, cinq tailles (docs/lot1/mon-compte.md).
export default function Apercu() {
  return (
    <div className="gm-row" style={{ alignItems: 'center', gap: 'var(--gm-space-3)' }}>
      <Avatar name="Chef de quart" />
      <Avatar name="Opérateur Nuit" size="lg" />
      <Avatar name="Rachel Biyong" src={photoDemoCatalogue('u-sna')} size="xs" />
      <Avatar name="Rachel Biyong" src={photoDemoCatalogue('u-sna')} size="sm" />
      <Avatar name="Rachel Biyong" src={photoDemoCatalogue('u-sna')} />
      <Avatar name="Hervé Kamga" src={photoDemoCatalogue('u-rssi')} size="lg" />
      <Avatar name="Hervé Kamga" src={photoDemoCatalogue('u-rssi')} size="xl" />
    </div>
  )
}
