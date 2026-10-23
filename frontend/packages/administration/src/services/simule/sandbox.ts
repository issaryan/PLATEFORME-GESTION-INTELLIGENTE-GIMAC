import type { ApiError } from '@gimac/socle'
import type { CompteProtege, PageSandbox, PerimetreSuspendu } from '../../modele/sandbox'
import type { ServiceSandboxIam } from '../portsSandbox'

// Sandbox GIMAC simulée (D-119) : mêmes formes et mêmes refus que MS-IAM. Données fictives : institutions de
// démonstration 99001 (suspendue) et 99002 (ouverte), administrateurs de démonstration.

export type OptionsSandboxSimulee = {
  attendre: <T>(v: T | (() => T)) => Promise<T>
  refus: (status: number, code: string, message: string, details?: Record<string, unknown>) => ApiError
}

const CODE = /^[A-Za-z0-9_-]{1,64}$/

export const PERIMETRES_DEMO: PerimetreSuspendu[] = [
  { code: '99001', depuis: '2026-10-21T07:30:00Z', acteurId: 'service-account-sbx-espace', motif: 'Statut SUSPENDU au registre des participants' },
]

export const PROTECTIONS_DEMO: CompteProtege[] = [
  { compteId: 'u-admin-99001', nom: 'Awa Ndiaye', institutionId: '99001', depuis: '2026-10-01T09:00:00Z' },
  { compteId: 'u-admin-99002', nom: 'Jean Mbarga', institutionId: '99002', depuis: '2026-10-02T09:00:00Z' },
]

/** Page à partir de 0, 100 lignes au plus, comme MS-IAM. */
export function paginer<T>(l: T[], page: number, taille: number): PageSandbox<T> {
  const p = Math.max(0, page)
  const t = Math.min(Math.max(1, taille), 100)
  return { contenu: l.slice(p * t, p * t + t), total: l.length, page: p, taille: t }
}

export function sandboxSimulee(o: OptionsSandboxSimulee, perimetres = PERIMETRES_DEMO, protections = PROTECTIONS_DEMO): ServiceSandboxIam {
  return {
    perimetres: (page, taille) => o.attendre(() => paginer([...perimetres].sort((a, b) => b.depuis.localeCompare(a.depuis)), page, taille)),
    perimetre: (code) =>
      o.attendre(() => {
        if (!CODE.test(code)) throw o.refus(400, 'IAM_SBX_PERIMETRE_INVALIDE', 'Code de périmètre invalide', { code })
        const p = perimetres.find((x) => x.code === code)
        return { code, suspendu: !!p, depuis: p?.depuis ?? null, sessionsRevoquees: 0, rejouee: false }
      }),
    protections: (page, taille) => o.attendre(() => paginer(protections, page, taille)),
  }
}
