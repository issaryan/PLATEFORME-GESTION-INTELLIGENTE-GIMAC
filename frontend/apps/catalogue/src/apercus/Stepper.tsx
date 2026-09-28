import { Stepper } from '@gimac/design-system'

export default function Apercu() {
  return <Stepper steps={[{ label: 'Identité', state: 'done' }, { label: 'Adhésion', state: 'current' }, { label: 'Correspondants', state: 'todo' }, { label: 'Résumé', state: 'todo' }]} />
}
