import { ChatPanel } from '@gimac/design-system'

export default function Apercu() {
  return (
    <div style={{ maxWidth: 720 }}>
      <ChatPanel
        room="Crise CRI-2026-009"
        members={6}
        verified
        height={420}
        messages={[
          { author: 'Chef de quart', at: '13:50', text: "Sonde en échec depuis 13:46, aucun trafic sur la liaison. Correspondant de l'institution injoignable pour l'instant." },
          { author: 'Chef du DES', at: '13:52', text: "Cellule ouverte. @RSSI merci de confirmer qu'aucun changement n'était planifié.", mine: true, read: true },
          { author: 'RSSI', at: '13:55', text: "Aucun changement planifié côté GIMAC. Fiche de changement de l'institution jointe.", attachment: 'fiche-changement-EPA-0928.pdf' },
          { author: 'Chef du DES', at: '13:56', text: 'Passage au niveau 2, convocation de la production monétique.', mine: true, read: false },
        ]}
        draft=""
      />
    </div>
  )
}
