import { PermissionPicker } from '@gimac/design-system'

const option = (v: string) => ({ value: v, label: v })

export default function Apercu() {
  return (
    <PermissionPicker
      label="Permissions du profil RSSI"
      value={['IAM_ACCOUNT_READ', 'IAM_JOURNAL_READ', 'PLATFORM_SUPERVISION_READ']}
      groups={[
        { key: 'IAM', label: 'Comptes et profils (IAM)', options: ['IAM_ACCOUNT_READ', 'IAM_ACCOUNT_CREATE', 'IAM_JOURNAL_READ'].map(option) },
        { key: 'PLATFORM', label: 'Plateforme (PLATFORM)', options: ['PLATFORM_SUPERVISION_READ', 'PLATFORM_CONFIG_READ'].map(option) },
      ]}
      onChange={() => {}}
    />
  )
}
