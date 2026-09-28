import { LogbookEntryList } from '@gimac/design-system'

export default function Apercu() {
  return (
    <div style={{ maxWidth: 760 }}>
      <LogbookEntryList
        entries={[
          { at: '22:00', actor: 'Chef de quart', kind: 'prise de quart', text: 'Prise de quart, 2 opérateurs présents, aucun incident ouvert.', hash: 'a1f3…9c2e' },
          { at: '23:14', actor: 'Opérateur', kind: 'alerte', text: 'ALR-2026-04402 acquittée en 3 min, seuil de rejet franchi sur LIA-2025-102, ticket TCK-000302 ouvert.', hash: '7b0d…41aa' },
          { at: '01:40', actor: 'Opérateur', text: 'Contact du correspondant de Banque B, reprise annoncée à 02:30, ticket passé en attente de tiers.', hash: 'c93e…08f7' },
          { at: '02:35', actor: 'Système', kind: 'transition', text: 'TCK-000302 : en attente de tiers vers en cours (reprise confirmée par la sonde).', hash: 'e514…b6d0' },
        ]}
        draft=""
      />
    </div>
  )
}
