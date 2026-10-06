import { http, P } from '@gimac/socle'
import type { MatricePouvoirs, PouvoirsProfil } from '@gimac/lot1'
import type { ServicePouvoirsAdministration } from '../ports'

// Pouvoirs hiérarchiques côté administration (D-81, docs/lot1/pouvoirs-hierarchiques.md §3 et §4) :
// /bff/{des,console}/iam/pouvoirs/matrice et /bff/{des,console}/iam/demandes (vue « a-traiter »), que la passerelle
// réécrit vers /api/v1 de MS-IAM. Actes sensibles (matrice, acceptation) : assertion FIDO2 dans X-Reauth-Assertion.
/** Matrice rendue sous forme { profils: [...] } ; une liste nue est acceptée (forme non figée par le contrat, §3). */
export const normaliserMatrice = (m: MatricePouvoirs | PouvoirsProfil[]): MatricePouvoirs => (Array.isArray(m) ? { profils: m } : m)

export function pouvoirsAdministrationHttp(couche: 'des' | 'console'): ServicePouvoirsAdministration {
  const I = couche === 'des' ? P.desIam : P.consoleIam
  const MATRICE = `${I}/pouvoirs/matrice`
  const DEMANDES = `${I}/demandes`
  const demande = (id: string) => `${DEMANDES}/${encodeURIComponent(id)}`
  return {
    matrice: () => http.get<MatricePouvoirs | PouvoirsProfil[]>(MATRICE).then(normaliserMatrice),
    modifierMatrice: (profil, e, motif, assertion) => http.put(`${MATRICE}/${encodeURIComponent(profil)}`, { body: { ...e, motif }, reauthAssertion: assertion }),
    demandes: (q) => http.get(DEMANDES, { query: { vue: 'a-traiter', etat: q.etat, page: q.page, taille: q.taille } }),
    demande: (id) => http.get(demande(id)),
    accepter: (id, motif, assertion) => http.post(`${demande(id)}/acceptation`, { body: { motif }, reauthAssertion: assertion }),
    refuser: (id, motif) => http.post(`${demande(id)}/refus`, { body: { motif } }),
  }
}
