import { SettingsGrid } from '@gimac/design-system'

export default function Apercu() {
  return (
    <SettingsGrid
      label="Mots de passe"
      items={[
        { key: 'longueur', label: 'Longueur minimale', value: 14, hint: 'De 12 à 128.' },
        { key: 'expiration', label: 'Expiration', value: 90, unit: 'jours', hint: 'De 0 à 365.' },
        { key: 'identifiant', label: "Interdire l'identifiant dans le mot de passe", value: 'activée', status: 'ok' },
        { key: 'force', label: 'Protection contre la force brute', value: 'désactivée', status: 'warn' },
      ]}
    />
  )
}
