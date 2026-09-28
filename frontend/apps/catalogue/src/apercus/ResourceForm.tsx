import { ResourceForm } from '@gimac/design-system'
import { institutionDescriptor } from '../fabriques/institution'

export default function Apercu() {
  return (
    <ResourceForm
      descriptor={institutionDescriptor}
      mode="create"
      step={1}
      steps={[
        { label: 'Identité', fields: ['codeParticipant', 'denomination', 'sigle', 'categorie', 'pays'] },
        { label: 'Adhésion', fields: ['modeAdhesion', 'banqueReglement', 'banqueMarraine'] },
        { label: 'Correspondants', fields: [] },
        { label: 'Résumé', fields: [] },
      ]}
      initial={{ codeParticipant: 'INST-0104', denomination: 'Établissement de paiement G', categorie: 'EP', pays: 'CM', modeAdhesion: 'INDIRECT', banqueReglement_q: 'ban' }}
      errors={{ banqueMarraine: 'La banque marraine est obligatoire en adhésion indirecte.' }}
    />
  )
}
