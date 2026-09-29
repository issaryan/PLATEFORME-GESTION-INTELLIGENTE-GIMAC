import { LinkList } from '@gimac/design-system'

export default function Apercu() {
  return (
    <LinkList
      label="Mes projets"
      items={[
        { key: '030', href: '#/LinkList', label: 'Établissement de microfinance C', meta: 'LIA-2026-030', status: 'neutral', statusLabel: 'Ouverte' },
        { key: '017', href: '#/LinkList', label: 'Établissement de paiement A', meta: 'LIA-2026-017', status: 'info', statusLabel: 'En homologation' },
      ]}
    />
  )
}
