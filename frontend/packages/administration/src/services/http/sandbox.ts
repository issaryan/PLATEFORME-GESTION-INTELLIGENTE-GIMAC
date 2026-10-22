import { http, P } from '@gimac/socle'
import type { ServiceSandboxIam } from '../portsSandbox'

// Sandbox GIMAC dans la console (D-119) : /bff/console/iam/sandbox, que la passerelle réécrit vers /api/v1/sandbox de
// MS-IAM (SandboxConsoleResource) ; pages à partir de 0.
export function sandboxHttp(): ServiceSandboxIam {
  const S = `${P.consoleIam}/sandbox`
  return {
    perimetres: (page, taille) => http.get(`${S}/perimetres`, { query: { page, taille } }),
    perimetre: (code) => http.get(`${S}/perimetres/${encodeURIComponent(code)}`),
    protections: (page, taille) => http.get(`${S}/protections`, { query: { page, taille } }),
  }
}
