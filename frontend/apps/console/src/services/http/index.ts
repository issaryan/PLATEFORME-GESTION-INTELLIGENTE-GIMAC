import { getReauthAssertion } from '@gimac/socle'
import type { ServicesConsole } from '../ports'
import { comptesHttp } from './comptes'
import { configurationHttp } from './configuration'
import { plateformeHttp } from './plateforme'

/** Services réels de la console, par la passerelle (VITE_SERVICES=http). */
export function servicesConsoleHttp(): ServicesConsole {
  return {
    comptes: comptesHttp,
    configuration: configurationHttp,
    plateforme: plateformeHttp,
    reauth: { assertion: (purpose) => getReauthAssertion('console', purpose) },
  }
}
