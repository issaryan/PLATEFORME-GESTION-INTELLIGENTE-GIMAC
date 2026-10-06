import { getReauthAssertion, type Couche } from '@gimac/socle'
import type { Services } from '../ports'
import { homologationHttp } from './homologation'
import { certificationHttp } from './certification'
import { portailHttp } from './portail'
import { participantHttp } from './participant'
import { decisionHttp, parcoursHttp, portailParcoursHttp } from './parcours'
import { messagerieHttp, messagerieHttpPour } from './messagerie'
import { pilotageHttp } from './pilotage'
import { pouvoirsHttp } from './pouvoirs'
import { apercuHttp } from './apercu'
import { processusHttp } from './processus'

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
    // Pouvoirs hiérarchiques et demandes à l'administration (D-81) : MS-IAM, couche interne.
    pouvoirs: pouvoirsHttp,
    // Processus d'unité (D-87) : MS-PILOTAGE, couche interne.
    processus: processusHttp,
    // Aperçu bureautique (D-83) : MS-DOCUMENT par la couche de l'application ; jamais au portail.
    apercu: couche === 'portail' ? undefined : apercuHttp(couche),
  }
}
