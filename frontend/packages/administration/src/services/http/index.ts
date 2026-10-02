import { getReauthAssertion, type Couche } from '@gimac/socle'
import type { ServicesConsole } from '../ports'
import { comptesHttp } from './comptes'
import { configurationHttp } from './configuration'
import { plateformeHttp } from './plateforme'

/**
 * Services réels de l'administration, par la passerelle : /bff/console/iam pour la console, /bff/des/iam
 * pour le département (D-69, comptes, profils, journal, politique). La supervision reste à la console.
 */
export function servicesConsoleHttp(couche: Extract<Couche, 'console' | 'des'> = 'console'): ServicesConsole {
  return {
    comptes: comptesHttp(couche),
    configuration: configurationHttp(couche),
    plateforme: plateformeHttp,
    reauth: { assertion: (purpose) => getReauthAssertion(couche, purpose) },
  }
}
