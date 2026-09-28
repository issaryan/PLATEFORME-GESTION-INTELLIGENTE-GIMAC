import { MaskingResult } from '@gimac/design-system'

export default function Apercu() {
  return (
    <div className="gm-col" style={{ maxWidth: 640, gap: 'var(--gm-space-2)' }}>
      <MaskingResult result="accepted" text="Aucune donnée de carte détectée. La pièce peut être confirmée." />
      <MaskingResult result="masked" count={2} text="Deux séquences ressemblant à un numéro de carte ont été masquées avant archivage." />
      <MaskingResult result="rejected" text="Le document contient un numéro de carte non masqué (page 3). Masquez le numéro puis déposez à nouveau." />
    </div>
  )
}
