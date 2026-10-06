import { getReauthAssertion, type Couche } from '@gimac/socle'
import type { ServicesConsole } from '../ports'
import { comptesHttp } from './comptes'
import { configurationHttp } from './configuration'
import { plateformeHttp } from './plateforme'
import { infrastructureHttp } from './infrastructure'
import { organisationHttp } from './organisation'
import { pouvoirsAdministrationHttp } from './pouvoirs'

/**
 * Services réels de l'administration, par la passerelle : /bff/console/iam pour la console, /bff/des/iam
 * pour le département (D-69, comptes, profils, journal, politique). La supervision, la configuration et
 * l'infrastructure (/api/plateforme) sont servies par la passerelle depuis toute couche (D-79).
 */
export function servicesConsoleHttp(couche: Extract<Couche, 'console' | 'des'> = 'console'): ServicesConsole {
  return {
    comptes: comptesHttp(couche),
    organisation: organisationHttp(couche),
    configuration: configurationHttp(couche),
    // Matrice des pouvoirs et demandes des chefs à traiter (D-81).
    pouvoirs: pouvoirsAdministrationHttp(couche),
    plateforme: plateformeHttp,
    // Plateforme et infrastructure (D-78, D-79) : /api/plateforme, servi par la passerelle depuis toute couche.
    infrastructure: infrastructureHttp,
    reauth: { assertion: (purpose) => getReauthAssertion(couche, purpose) },
  }
}
