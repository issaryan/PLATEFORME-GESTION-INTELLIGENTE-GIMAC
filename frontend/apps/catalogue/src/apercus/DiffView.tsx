import { DiffView } from '@gimac/design-system'

export default function Apercu() {
  return (
    <DiffView
      leftTitle="Référentiel v3.2 (actif)"
      rightTitle="Référentiel v3.3 (brouillon)"
      lines={[
        { ln: 12, rn: 12, text: 'OP-E01  Personnalisation et clés            responsable: institution' },
        { ln: 13, rn: 13, type: 'mod', left: 'OP-E02  Scénarios de litige émetteur       délai: 10 j', right: 'OP-E02  Scénarios de litige émetteur       délai: 15 j' },
        { ln: 14, type: 'del', left: 'OP-E04  Rapprochement à blanc              responsable: GIMAC' },
        { rn: 14, type: 'add', right: 'OP-E05  Simulation des autorisations       responsable: GIMAC' },
        { rn: 15, type: 'add', right: 'OP-E05  dépend de: OP-E01, OP-E02' },
        { ln: 15, rn: 16, text: 'OP-E03  Bascule émetteur                   responsable: GIMAC' },
      ]}
    />
  )
}
