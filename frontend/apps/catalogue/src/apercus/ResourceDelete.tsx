import { ResourceDelete } from '@gimac/design-system'
import { institutionDescriptor, sampleInstitutions } from '../fabriques/institution'
import { Scene } from '../outils/Scene'

const rien = () => {}

export default function Apercu() {
  return (
    <Scene hauteur={480} titre="Établissement de paiement A" aide={false}>
      <ResourceDelete inline descriptor={institutionDescriptor} row={sampleInstitutions[0]} onClose={rien} onDone={rien} />
    </Scene>
  )
}
