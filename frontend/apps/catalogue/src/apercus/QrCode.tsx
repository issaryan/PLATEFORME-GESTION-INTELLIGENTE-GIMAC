import { QrCode } from '@gimac/design-system'

// Code QR de configuration du TOTP, calculé dans le navigateur (aucun service tiers).
export default function Apercu() {
  return <QrCode value="otpauth://totp/GIMAC%20DES:c.ndong?secret=JBSWY3DPEHPK3PXP&issuer=GIMAC%20DES&digits=6&period=30" label="Code QR de configuration de l'application d'authentification" size={160} />
}
