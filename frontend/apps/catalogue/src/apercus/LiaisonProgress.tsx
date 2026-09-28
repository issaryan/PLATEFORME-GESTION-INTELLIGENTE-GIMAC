import { LiaisonProgress } from '@gimac/design-system'

export default function Apercu() {
  return (
    <div style={{ maxWidth: 760 }}>
      <LiaisonProgress
        certification={100}
        commissioning={60}
        phase="Mise en service : quitus sécurité et conformité en attente"
        referential="v3.2 (tronc commun + branche « émetteur »)"
        services={[
          { service: 'Retrait', level: 4 },
          { service: 'Paiement', level: 3 },
          { service: 'Transfert', level: 2 },
          { service: 'Commerce électronique', level: 0 },
        ]}
      />
    </div>
  )
}
