import { getReauthAssertion, type Couche } from '@gimac/socle'
import type { Services } from '../ports'
import { homologationHttp } from './homologation'
import { certificationHttp } from './certification'
import { portailHttp } from './portail'

/** Services réels, par la passerelle : à employer dès que les routes /bff du lot 1 sont opérationnelles. */
export function servicesHttp(couche: Couche): Services {
  return {
    homologation: homologationHttp,
    certification: certificationHttp,
    portail: portailHttp,
    reauth: { assertion: (purpose) => getReauthAssertion(couche, purpose) },
  }
}
