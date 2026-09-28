import { SplitButton } from '@gimac/design-system'

export default function Apercu() {
  return (
    <div style={{ height: 96 }}>
      <SplitButton
        icon="pen-line"
        open
        items={[
          { key: 'refuse', icon: 'x', label: 'Refuser avec motif' },
          { key: 'delegate', icon: 'users', label: 'Déléguer la signature' },
          { separator: true },
          { key: 'history', icon: 'scroll-text', label: 'Voir les signatures précédentes' },
        ]}
      >
        Signer le quitus
      </SplitButton>
    </div>
  )
}
