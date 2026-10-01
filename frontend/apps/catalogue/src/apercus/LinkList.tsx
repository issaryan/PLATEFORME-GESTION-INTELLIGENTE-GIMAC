import { LinkList } from '@gimac/design-system'

const INSTITUTIONS = ['Établissement de microfinance C', 'Établissement de paiement A', 'Banque B', 'Banque D', 'Fintech E', 'Banque F', 'Établissement de paiement G']

export default function Apercu() {
  return (
    <LinkList
      label="Mes projets"
      items={INSTITUTIONS.map((nom, i) => ({ key: String(i), href: '#/LinkList', label: nom, meta: `LIA-2026-0${30 + i}`, status: i % 2 ? 'info' : 'neutral', statusLabel: i % 2 ? 'en homologation' : 'ouverte' }))}
    />
  )
}
