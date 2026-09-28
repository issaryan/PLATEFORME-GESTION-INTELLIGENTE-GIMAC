import { TicketCard, TicketStateBar } from '@gimac/design-system'

export default function Apercu() {
  return (
    <div className="gm-col">
      <div className="gm-row" style={{ alignItems: 'stretch' }}>
        <TicketCard id="TCK-000318" severity="S1" title="Liaison LIA-2026-017 injoignable" institution="Établissement de paiement A" status="info" statusLabel="en cours" due="28/09/2026 15:05" late={false} />
        <TicketCard id="TCK-000302" severity="S3" title="Écart de rapprochement récurrent sur les retraits" institution="Banque B" status="neutral" statusLabel="en attente de tiers" due="27/09/2026 18:00" late />
        <TicketCard id="TCK-000321" type="demande" title="Ajout d'un correspondant de conformité" institution="Banque D" status="info" statusLabel="nouveau" due="30/09/2026 12:00" />
      </div>
      <TicketStateBar current="EN_COURS" allowed={['EN_ATTENTE_TIERS', 'RESOLU']} />
    </div>
  )
}
