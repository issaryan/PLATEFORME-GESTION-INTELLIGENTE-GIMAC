import { Banner, Button } from '@gimac/design-system'

export default function Apercu() {
  return (
    <div className="gm-col" style={{ gap: 'var(--gm-space-2)' }}>
      <Banner level="info" title="Mises à jour en temps réel interrompues," onDismiss={() => {}}>reconnexion en cours.</Banner>
      <Banner level="warn" title="Échéance proche :">3 pièces légales arrivent à échéance cette semaine.</Banner>
      <Banner level="crit" title="Alerte critique ALR-2026-04417 :" action={<Button size="sm" variant="primary">Acquitter l'alerte</Button>}>liaison LIA-2026-017 injoignable depuis 4 min.</Banner>
    </div>
  )
}
