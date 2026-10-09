import { http, P } from '@gimac/socle'
import type { ServiceOrganisation } from '../ports'

// Organisation du GIMAC (D-76, docs/lot1/mon-activite.md §1.3) : /bff/{des,console}/iam/organisation/unites et
// /bff/{des,console}/iam/comptes/{id}/rattachement, que la passerelle réécrit vers /api/v1 de MS-IAM ; D-102 :
// …/unites/{code}/affectations et …/organisation/organigramme.pdf.
export function organisationHttp(couche: 'des' | 'console'): ServiceOrganisation {
  const I = couche === 'des' ? P.desIam : P.consoleIam
  const U = `${I}/organisation/unites`
  return {
    unites: () => http.get(U),
    creerUnite: (u, motif) => http.post(U, { body: motif ? { ...u, motif } : u }),
    modifierUnite: (code, u, motif) => http.put(`${U}/${encodeURIComponent(code)}`, { body: motif ? { ...u, motif } : u }),
    rattacher: (id, r) => http.put(`${I}/comptes/${encodeURIComponent(id)}/rattachement`, { body: r }),
    // D-102 (docs/lot1/organigramme.md) : affectations, retrait (motif en paramètre, comme la suppression d'un compte), PDF.
    affecter: (code, d) => http.post(`${U}/${encodeURIComponent(code)}/affectations`, { body: d }),
    retirer: (code, id, motif) => http.delete(`${U}/${encodeURIComponent(code)}/affectations/${encodeURIComponent(id)}?motif=${encodeURIComponent(motif)}`),
    organigrammePdf: async (o) => {
      const q = new URLSearchParams({ format: o.format, orientation: o.orientation, membres: String(o.membres) })
      if (o.racine) q.set('racine', o.racine)
      return (await http.blob(`${I}/organisation/organigramme.pdf?${q.toString()}`, 'application/pdf')).blob
    },
  }
}
