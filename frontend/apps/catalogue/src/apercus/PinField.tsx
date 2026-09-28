import { PinField } from '@gimac/design-system'

export default function Apercu() {
  return (
    <div className="gm-row" style={{ gap: 'var(--gm-space-7)', alignItems: 'flex-start' }}>
      <PinField label="Code court affiché sur le terminal à vérifier" value="48" help="Valable 10 minutes" />
      <PinField label="Code court" value="481927" error="Le code ne correspond pas. Vérifiez le code affiché sur le terminal puis saisissez à nouveau." />
    </div>
  )
}
