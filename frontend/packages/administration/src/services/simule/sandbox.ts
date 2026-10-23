import type { ApiError } from '@gimac/socle'
import type { CompteProtege, Delegation, EtatEffectifDelegation, PageSandbox, PerimetreSuspendu, VueDelegation } from '../../modele/sandbox'
import type { ServiceSandboxIam } from '../portsSandbox'

// Sandbox GIMAC simulée (D-119) : mêmes formes et mêmes refus que MS-IAM. Données fictives : institutions de
// démonstration 99001 (suspendue) et 99002 (ouverte), administrateurs, partenaires et prestataires de démonstration.

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

const mandat = (id: string, compteId: string, nom: string, debut: string, fin: string, etat: Delegation['etat'], permissions: string[], creeLe: string): [Delegation, string] => [
  { id, type: 'MANDAT', compteId, institutionId: '99002', permissions, composants: [], debut, fin, etat, demandeurId: 'service-account-sbx-espace', approbateurId: null, motif: etat === 'REVOQUEE' ? 'Fin anticipée du contrat' : null, creeLe, closeLe: etat === 'REVOQUEE' ? '2026-10-15T10:00:00Z' : null },
  nom,
]

export const DELEGATIONS_DEMO: [Delegation, string][] = [
  mandat('d-mandat-actif', 'u-integrateur', 'Paul Ebode (Intégrateur Afrique)', '2026-10-01T08:00:00Z', '2026-12-31T18:00:00Z', 'ACCORDEE', ['SBX_CER_EXECUTION', 'SBX_CER_CONSULTATION', 'SBX_RAC_CONSULTATION'], '2026-10-01T08:00:00Z'),
  mandat('d-mandat-programme', 'u-editeur', 'Chantal Owona (Éditeur monétique)', '2026-11-02T08:00:00Z', '2026-12-02T18:00:00Z', 'ACCORDEE', ['SBX_MOB_SIMULATION', 'SBX_MOB_CONSULTATION'], '2026-10-20T08:00:00Z'),
  mandat('d-mandat-echu', 'u-integrateur', 'Paul Ebode (Intégrateur Afrique)', '2026-08-01T08:00:00Z', '2026-09-30T18:00:00Z', 'ACCORDEE', ['SBX_RAC_ADMINISTRATION'], '2026-08-01T08:00:00Z'),
  mandat('d-mandat-revoque', 'u-editeur', 'Chantal Owona (Éditeur monétique)', '2026-09-01T08:00:00Z', '2026-11-30T18:00:00Z', 'REVOQUEE', ['SBX_CER_EXECUTION'], '2026-09-01T08:00:00Z'),
]

/** État effectif à l'instant, comme ReglesDelegation de MS-IAM. */
export function etatEffectif(d: Delegation, maintenant = new Date()): EtatEffectifDelegation {
  const t = maintenant.getTime()
  const debut = Date.parse(d.debut)
  const fin = Date.parse(d.fin)
  if (d.etat === 'REVOQUEE' || d.etat === 'REFUSEE') return d.etat
  if (d.etat === 'DEMANDEE') return fin > t ? 'DEMANDEE' : 'ECHUE'
  if (fin <= t) return 'ECHUE'
  return t < debut ? 'PROGRAMMEE' : 'ACTIVE'
}

/** Page à partir de 0, 100 lignes au plus, comme MS-IAM. */
export function paginer<T>(l: T[], page: number, taille: number): PageSandbox<T> {
  const p = Math.max(0, page)
  const t = Math.min(Math.max(1, taille), 100)
  return { contenu: l.slice(p * t, p * t + t), total: l.length, page: p, taille: t }
}

export function sandboxSimulee(o: OptionsSandboxSimulee, perimetres = PERIMETRES_DEMO, protections = PROTECTIONS_DEMO, delegations = DELEGATIONS_DEMO): ServiceSandboxIam {
  return {
    perimetres: (page, taille) => o.attendre(() => paginer([...perimetres].sort((a, b) => b.depuis.localeCompare(a.depuis)), page, taille)),
    perimetre: (code) =>
      o.attendre(() => {
        if (!CODE.test(code)) throw o.refus(400, 'IAM_SBX_PERIMETRE_INVALIDE', 'Code de périmètre invalide', { code })
        const p = perimetres.find((x) => x.code === code)
        return { code, suspendu: !!p, depuis: p?.depuis ?? null, sessionsRevoquees: 0, rejouee: false }
      }),
    protections: (page, taille) => o.attendre(() => paginer(protections, page, taille)),
    delegations: (type, page, taille) =>
      o.attendre(() =>
        paginer(
          delegations
            .filter(([d]) => !type || d.type === type)
            .sort(([a], [b]) => b.creeLe.localeCompare(a.creeLe))
            .map(([d, nomCompte]): VueDelegation => ({ delegation: d, etat: etatEffectif(d), nomCompte, rejouee: false })),
          page,
          taille,
        ),
      ),
  }
}
