import { Breadcrumb } from '@gimac/design-system'

export default function Apercu() {
  return <Breadcrumb items={[{ label: 'Institutions membres', href: '#' }, { label: 'Établissement de paiement A', href: '#' }, { label: 'Pièces légales' }]} />
}
