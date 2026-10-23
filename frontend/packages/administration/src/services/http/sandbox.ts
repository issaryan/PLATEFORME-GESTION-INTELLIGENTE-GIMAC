import { http, P } from '@gimac/socle'
import type { ServiceSandboxIam } from '../portsSandbox'

// Sandbox GIMAC dans la console (D-119) : /bff/console/iam/sandbox, que la passerelle réécrit vers /api/v1/sandbox de
// MS-IAM (SandboxConsoleResource) ; pages à partir de 0.
export function sandboxHttp(): ServiceSandboxIam {
  const S = `${P.consoleIam}/sandbox`
  const A = '/bff/console/audit/sandbox'
  return {
    perimetres: (page, taille) => http.get(`${S}/perimetres`, { query: { page, taille } }),
    perimetre: (code) => http.get(`${S}/perimetres/${encodeURIComponent(code)}`),
    protections: (page, taille) => http.get(`${S}/protections`, { query: { page, taille } }),
    delegations: (type, page, taille) => http.get(`${S}/delegations`, { query: { type, page, taille } }),
    // US-EXI-24 : accès de prestataire, sous IAM_ACCOUNT_SECURITY et deux facteurs.
    demanderPrestataire: (d) => http.post(`${S}/delegations/prestataire`, { body: d }),
    approuverPrestataire: (id) => http.post(`${S}/delegations/${encodeURIComponent(id)}/approbation`, { body: {} }),
    refuserPrestataire: (id, motif) => http.post(`${S}/delegations/${encodeURIComponent(id)}/refus`, { body: { motif } }),
    revoquerPrestataire: (id, motif) => http.post(`${S}/delegations/${encodeURIComponent(id)}/revocation`, { body: { motif } }),
    // US-EXI-10 : registre d'audit de la sandbox, /bff/console/audit/sandbox vers /api/v1/registre/sandbox de MS-AUDIT.
    registre: (institution, page, taille) => http.get(A, { query: { institution: institution || undefined, page, taille } }),
    exporterRegistre: async (institution) => (await http.blob(`${A}/export${institution ? `?institution=${encodeURIComponent(institution)}` : ''}`, 'text/csv')).blob,
    rebut: (page, taille) => http.get(`${A}/rebut`, { query: { page, taille } }),
  }
}
