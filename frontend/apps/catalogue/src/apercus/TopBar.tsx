import { TopBar } from '@gimac/design-system'
import logo from '@gimac/design-system/assets/logo/gimac-logo-couleur.png'

export default function Apercu() {
  return (
    <div className="gm-col" style={{ gap: 'var(--gm-space-2)', margin: 'calc(-1 * var(--gm-space-4))' }}>
      <TopBar layer="des" logo={logo} breadcrumb={[{ label: 'Institutions membres', href: '#' }, { label: 'Établissement de paiement A' }]} user="Chef de quart" />
      <TopBar layer="console" logo={logo} user="Auditeur" live={false} />
    </div>
  )
}
