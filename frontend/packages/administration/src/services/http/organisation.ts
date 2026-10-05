import { http, P } from '@gimac/socle'
import type { ServiceOrganisation } from '../ports'

// Organisation du GIMAC (D-76, docs/lot1/mon-activite.md §1.3) : /bff/{des,console}/iam/organisation/unites et
// /bff/{des,console}/iam/comptes/{id}/rattachement, que la passerelle réécrit vers /api/v1 de MS-IAM.
export function organisationHttp(couche: 'des' | 'console'): ServiceOrganisation {
  const I = couche === 'des' ? P.desIam : P.consoleIam
  const U = `${I}/organisation/unites`
  return {
    unites: () => http.get(U),
    creerUnite: (u) => http.post(U, { body: u }),
    modifierUnite: (code, u) => http.put(`${U}/${encodeURIComponent(code)}`, { body: u }),
    rattacher: (id, r) => http.put(`${I}/comptes/${encodeURIComponent(id)}/rattachement`, { body: r }),
  }
}
