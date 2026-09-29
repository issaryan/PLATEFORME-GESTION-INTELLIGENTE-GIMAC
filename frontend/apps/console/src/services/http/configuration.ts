import { http, P } from '@gimac/socle'
import type { ServiceConfiguration } from '../ports'

// Implémentation HTTP de la configuration de l'authentification : /bff/console/iam/configuration,
// réécrit vers /api/v1/configuration de MS-IAM (ConfigurationAuthentificationResource).
const C = `${P.consoleIam}/configuration`

export const configurationHttp: ServiceConfiguration = {
  politique: () => http.get(`${C}/politique-securite`),
  appliquerPolitique: (politique, motif, assertion) => http.put(`${C}/politique-securite`, { body: { politique, motif }, reauthAssertion: assertion }),
  messagerie: () => http.get(`${C}/messagerie`),
  appliquerMessagerie: (messagerie, motif) => http.put(`${C}/messagerie`, { body: { messagerie, motif } }),
  testerMessagerie: (destinataire) => http.post(`${C}/messagerie/test`, { body: { destinataire } }),
  profils: () => http.get(`${C}/profils`),
  permissions: () => http.get(`${C}/permissions`),
  definirPermissions: (code, permissions, motif, assertion) =>
    http.put(`${C}/profils/${encodeURIComponent(code)}/permissions`, { body: { permissions, motif }, reauthAssertion: assertion }),
}
