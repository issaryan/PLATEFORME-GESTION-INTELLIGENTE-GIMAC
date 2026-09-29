import { http, P } from '@gimac/socle'
import type { ServiceComptes } from '../ports'

// Implémentation HTTP de l'administration des comptes : /bff/console/iam/comptes, que la passerelle
// réécrit vers /api/v1/comptes de MS-IAM (ComptesResource).
const C = `${P.consoleIam}/comptes`
const id = (x: string) => `${C}/${encodeURIComponent(x)}`

export const comptesHttp: ServiceComptes = {
  lister: (f) => http.get(C, { query: { page: Math.max(f.page - 1, 0), taille: f.taille, q: f.q || undefined, profil: f.profil || undefined, etat: f.etat || undefined } }),
  indicateurs: () => http.get(`${C}/indicateurs`),
  journal: (f) => http.get(`${C}/journal`, { query: { page: Math.max(f.page - 1, 0), taille: f.taille, compte: f.compte || undefined } }),
  lire: (x) => http.get(id(x)),
  sessions: (x) => http.get(`${id(x)}/sessions`),
  creer: (n) => http.post(C, { body: n }),
  modifier: (x, f) => http.put(id(x), { body: f }),
  definirProfils: (x, profils, motif, assertion) => http.put(`${id(x)}/profils`, { body: { profils, motif }, reauthAssertion: assertion }),
  activer: (x, motif) => http.post(`${id(x)}/activation`, { body: { motif } }),
  desactiver: (x, motif) => http.post(`${id(x)}/desactivation`, { body: { motif } }),
  supprimer: (x, motif, assertion) => http.delete(id(x), { query: { motif }, reauthAssertion: assertion }),
  exigerActions: (x, actions, parCourriel, motif) => http.post(`${id(x)}/actions-requises`, { body: { actions, parCourriel, motif } }),
  reinitialiserSecondFacteur: (x, motif) => http.post(`${id(x)}/second-facteur/reinitialisation`, { body: { motif } }),
  motDePasseProvisoire: (x, motif) => http.post(`${id(x)}/mot-de-passe-provisoire`, { body: { motif } }),
  fermerSessions: (x, motif) => http.post(`${id(x)}/sessions/fermeture`, { body: { motif } }),
}
