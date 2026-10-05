import { getReauthAssertion, type Couche } from '@gimac/socle'
import type { Services } from '../ports'
import { homologationHttp } from './homologation'
import { certificationHttp } from './certification'
import { portailHttp } from './portail'
import { participantHttp } from './participant'
import { decisionHttp, parcoursHttp, portailParcoursHttp } from './parcours'
import { messagerieHttp, messagerieHttpPour } from './messagerie'
import { pilotageHttp } from './pilotage'

/** Services réels, par la passerelle : à employer dès que les routes /bff du lot 1 sont opérationnelles. */
export function servicesHttp(couche: Couche): Services {
  return {
    participant: participantHttp,
    parcours: parcoursHttp,
    decision: decisionHttp,
    portailParcours: portailParcoursHttp,
    homologation: homologationHttp,
    certification: certificationHttp,
    portail: portailHttp,
    reauth: { assertion: (purpose) => getReauthAssertion(couche, purpose) },
    messagerie: couche === 'console' ? messagerieHttpPour('console') : messagerieHttp,
    // Mon activité (D-76) : couche interne seulement.
    pilotage: pilotageHttp,
  }
}
