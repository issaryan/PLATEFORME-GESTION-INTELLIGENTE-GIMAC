import { TextField } from '@gimac/design-system'

const rien = () => {}

export default function Apercu() {
  return (
    <div className="gm-col" style={{ maxWidth: 420 }}>
      <TextField name="denomination" label="Dénomination" value="Établissement de paiement A" required onChange={rien} />
      <TextField name="code" label="Code participant" value="INST-0042" mono help="Attribué par le GIMAC, immuable" onChange={rien} />
      <TextField name="sigle" label="Sigle" value="" error="Le sigle contient un numéro de carte non masqué. Masquez le numéro puis saisissez à nouveau." onChange={rien} />
      <TextField name="date" label="Date d'adhésion" value="12/03/2024" readOnly onChange={rien} />
    </div>
  )
}
