import { Tabs } from '@gimac/design-system'

export default function Apercu() {
  return (
    <Tabs
      items={[{ key: 'detail', label: 'Détail' }, { key: 'conventions', label: 'Conventions' }, { key: 'pieces', label: 'Pièces légales et échéances' }, { key: 'chronologie', label: 'Chronologie' }, { key: 'audit', label: 'Audit' }]}
      value="pieces"
      onChange={() => {}}
    />
  )
}
