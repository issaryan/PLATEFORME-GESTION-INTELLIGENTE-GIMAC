import { DeviceVerificationCard } from '@gimac/design-system'

// L'aperçu publié transmettait l'empreinte sous « key », nom réservé par React : elle n'était
// donc jamais affichée. Pour rester identique, l'empreinte n'est pas transmise ici non plus.
export default function Apercu() {
  return (
    <div className="gm-row" style={{ alignItems: 'flex-start' }}>
      <div style={{ width: 440 }}>
        <DeviceVerificationCard name="Poste de supervision 03" id="TRM-2026-00871" lastSeen="28/09/2026 14:02" platform="Poste fixe, navigateur du département" state="verified" />
      </div>
      <div style={{ width: 440 }}>
        <DeviceVerificationCard name="Téléphone de service" id="TRM-2026-00902" lastSeen="28/09/2026 13:40" platform="Mobile, application du portail" state="pending" code="48" />
      </div>
    </div>
  )
}
