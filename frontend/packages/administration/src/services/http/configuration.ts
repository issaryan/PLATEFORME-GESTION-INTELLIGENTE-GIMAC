import { http, P } from '@gimac/socle'
import type { ServiceConfiguration } from '../ports'

// Implémentation HTTP de la configuration de l'authentification : /bff/{couche}/iam/configuration,
// réécrit vers /api/v1/configuration de MS-IAM (ConfigurationAuthentificationResource). Le département
// (D-69) n'atteint que la politique de sécurité, les profils et les permissions ; les courriels restent à la console.
export function configurationHttp(couche: 'des' | 'console'): ServiceConfiguration {
  const C = `${couche === 'des' ? P.desIam : P.consoleIam}/configuration`
  // Serveur de courriels de la plateforme : console seulement.
  const COURRIELS = `${P.consoleIam}/configuration/messagerie`
  return {
    politique: () => http.get(`${C}/politique-securite`),
    appliquerPolitique: (politique, motif, assertion) => http.put(`${C}/politique-securite`, { body: { politique, motif }, reauthAssertion: assertion }),
    messagerie: () => http.get(COURRIELS),
    appliquerMessagerie: (messagerie, motif) => http.put(COURRIELS, { body: { messagerie, motif } }),
    testerMessagerie: (destinataire) => http.post(`${COURRIELS}/test`, { body: { destinataire } }),
    profils: () => http.get(`${C}/profils`),
    permissions: () => http.get(`${C}/permissions`),
    definirPermissions: (code, permissions, motif, assertion) =>
      http.put(`${C}/profils/${encodeURIComponent(code)}/permissions`, { body: { permissions, motif }, reauthAssertion: assertion }),
    // Profils créés par l'administration (D-81 §5) : création et suppression, actes sensibles.
    creerProfil: (p, motif, assertion) => http.post(`${C}/profils`, { body: { ...p, motif }, reauthAssertion: assertion }),
    supprimerProfil: (code, motif, assertion) => http.delete(`${C}/profils/${encodeURIComponent(code)}`, { query: { motif }, reauthAssertion: assertion }),
    // Libellé et description d'un profil (D-86 §2) : acte sensible, motif obligatoire.
    modifierProfil: (code, p, motif, assertion) =>
      http.put(`${C}/profils/${encodeURIComponent(code)}`, { body: { libelle: p.libelle, description: p.description, ...(p.externe !== undefined ? { externe: p.externe } : {}), motif }, reauthAssertion: assertion }),
    // Catalogue des fonctionnalités (D-94) : lecture par tout compte interne, modification IAM_PROFILE_EDIT (motif, FIDO2).
    fonctionnalites: (application) => http.get(`${C}/fonctionnalites`, application ? { query: { application } } : {}),
    modifierFonctionnalite: (cle, f, motif, assertion) => http.put(`${C}/fonctionnalites/${encodeURIComponent(cle)}`, { body: { ...f, cle, motif }, reauthAssertion: assertion }),
  }
}
