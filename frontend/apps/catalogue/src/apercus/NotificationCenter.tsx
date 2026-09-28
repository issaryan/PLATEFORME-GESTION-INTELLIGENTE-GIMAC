import { NotificationCenter } from '@gimac/design-system'

export default function Apercu() {
  return (
    <NotificationCenter
      height={500}
      notifications={[
        { id: 1, level: 'critical', kind: 'alert', title: 'Alerte ALR-2026-04417 non acquittée', text: 'Liaison LIA-2026-017 injoignable depuis 4 min, acquittement attendu sous 4 min.', at: 'il y a 4 min', ref: 'ALR-2026-04417' },
        { id: 2, level: 'urgent', kind: 'ticket', title: 'Incident S1 TCK-000318 : engagement à 80 %', text: "Il reste 48 min sur l'engagement de résolution.", at: 'il y a 12 min', ref: 'TCK-000318', acked: true },
        { id: 3, level: 'important', kind: 'deadline', title: '3 pièces légales arrivent à échéance', text: 'Établissement de paiement A, Banque B, Banque D : échéance sous 7 jours.', at: 'il y a 2 h', ref: 'DES-03' },
        { id: 4, level: 'important', kind: 'liaison', title: 'Quitus technique signé sur LIA-2026-017', text: 'Le quitus sécurité et conformité est maintenant attendu.', at: 'hier 16:10', ref: 'LIA-2026-017', read: true },
        { id: 5, level: 'info', kind: 'document', title: 'Rapport de relève déposé', text: 'Quart de nuit du 27/09, contresigné et scellé.', at: 'hier 06:02', ref: 'DOC-2026-7B12' },
      ]}
    />
  )
}
