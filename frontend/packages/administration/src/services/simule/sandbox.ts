import type { ApiError } from '@gimac/socle'
import { COMPOSANTS_SANDBOX, type CompteProtege, type Delegation, type EntreeRegistreSandbox, type EtatEffectifDelegation, type PageSandbox, type PerimetreSuspendu, type RebutSandbox, type VueDelegation } from '../../modele/sandbox'
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

/** Auteur des demandes faites depuis la console simulée : il n'approuve jamais sa propre demande (quatre yeux). */
export const SESSION_CONSOLE = 'session-console'
const HEURE = 3_600_000
const DUREE_MAX_PRESTATAIRE = 8 * HEURE

/** Accès de prestataire de démonstration (US-EXI-24), datés autour de l'instant donné : demandé, actif, échu. */
export function prestatairesDemo(maintenant = new Date()): [Delegation, string][] {
  const t = maintenant.getTime()
  const iso = (d: number) => new Date(t + d).toISOString().replace(/\.\d{3}Z$/, 'Z')
  const acces = (id: string, compteId: string, debut: number, fin: number, etat: Delegation['etat'], composants: string[], motif: string, approbateurId: string | null): Delegation => ({
    id, type: 'PRESTATAIRE', compteId, institutionId: null, permissions: [], composants, debut: iso(debut), fin: iso(fin), etat,
    demandeurId: 'demo-exploitant', approbateurId, motif, creeLe: iso(Math.min(debut, 0) - HEURE), closeLe: null,
  })
  return [
    [acces('d-presta-demande', 'u-presta', 24 * HEURE, 28 * HEURE, 'DEMANDEE', ['SBX-TRACE'], 'Montée de version de SBX-TRACE', null), 'Jean Mbarga (Prestataire de maintenance)'],
    [acces('d-presta-actif', 'u-presta', -HEURE, 3 * HEURE, 'ACCORDEE', ['SBX-DONNEES', 'MIFOS'], 'Correctif du connecteur Mifos', 'demo-rssi'), 'Jean Mbarga (Prestataire de maintenance)'],
    [acces('d-presta-echu', 'u-presta-2', -76 * HEURE, -72 * HEURE, 'ACCORDEE', ['SBX-ISO20022'], 'Diagnostic de la façade ISO 20022', 'demo-rssi'), 'Clarisse Atangana (Prestataire réseau)'],
  ]
}

/** Registre d'audit de démonstration (US-EXI-10) : événements à effet des services de la sandbox pour 99001 et 99002. */
export const REGISTRE_DEMO: EntreeRegistreSandbox[] = [
  ['sbx-espace.space-opened.v1', '99001', 'demo-admin-gimac'], ['sbx-espace.mandate-granted.v1', '99002', 'u-admin-99002'],
  ['sbx-raccordement.access-issued.v1', '99001', 'u-dev-99001'], ['sbx-campagne.campaign-opened.v1', '99001', 'u-chef-99001'],
  ['sbx-campagne.execution-recorded.v1', '99001', 'u-testeur-99001'], ['sbx-campagne.verdict-validated.v1', '99001', 'u-certificateur'],
  ['sbx-lecture.access-recorded.v1', '99001', 'service-account-homologation-service'], ['sbx-trace.trace-purged.v1', null, 'sbx-trace'],
  ['sbx-campagne.campaign-closed.v1', '99001', 'u-chef-99001'], ['sbx-donnees.dataset-generated.v1', '99002', 'u-dev-99002'],
  ['sbx-raccordement.access-refused.v1', '99002', 'u-integrateur'], ['sbx-campagne.verdict-validated.v1', '99002', 'u-certificateur'],
].map(([type, institution, acteur], i) => ({
  index: 1200 + i, eventId: `0192f0aa-0000-7000-8000-${String(900000 + i).padStart(12, '0')}`, survenueLe: `2026-10-22T0${7 + Math.floor(i / 6)}:${String((i * 7) % 60).padStart(2, '0')}:00Z`,
  inscritLe: `2026-10-22T0${7 + Math.floor(i / 6)}:${String((i * 7) % 60).padStart(2, '0')}:01Z`, acteur: acteur as string, type: type as string, institution: institution as string | null,
  empreinte: (i * 2654435761 >>> 0).toString(16).padStart(8, '0').repeat(8),
}))

/** Enveloppes de démonstration écartées au rebut (AUD-07). */
export const REBUT_DEMO: RebutSandbox[] = [
  { id: 1, eventId: 'evt-sans-time', sujet: 'sbx-lecture.access', motif: 'TIME_INVALIDE', recuLe: '2026-10-22T08:12:00Z' },
  { id: 2, eventId: null, sujet: 'sbx-campagne.campaign', motif: 'JSON_ILLISIBLE', recuLe: '2026-10-22T08:20:00Z' },
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

export function sandboxSimulee(o: OptionsSandboxSimulee, perimetres = PERIMETRES_DEMO, protections = PROTECTIONS_DEMO, initiales?: [Delegation, string][]): ServiceSandboxIam {
  const delegations: [Delegation, string][] = (initiales ?? [...DELEGATIONS_DEMO, ...prestatairesDemo()]).map(([d, n]) => [{ ...d }, n])
  const vue = ([d, nomCompte]: [Delegation, string]): VueDelegation => ({ delegation: d, etat: etatEffectif(d), nomCompte, rejouee: false })
  /** Accès de prestataire existant (404 IAM_SBX_DELEGATION_INTROUVABLE sinon). */
  const acces = (id: string) => {
    const l = delegations.find(([d]) => d.id === id && d.type === 'PRESTATAIRE')
    if (!l) throw o.refus(404, 'IAM_SBX_DELEGATION_INTROUVABLE', 'Délégation introuvable')
    return l
  }
  /** Demande encore ouverte (409 IAM_SBX_DELEGATION_CLOSE sinon). */
  const ouverte = (l: [Delegation, string]) => {
    const e = etatEffectif(l[0])
    if (e !== 'DEMANDEE') throw o.refus(409, 'IAM_SBX_DELEGATION_CLOSE', 'Demande déjà décidée ou échue', { etat: e })
    return l
  }
  const changer = (l: [Delegation, string], m: Partial<Delegation>) => {
    l[0] = { ...l[0], ...m }
    return vue(l)
  }
  const maintenant = () => new Date().toISOString().replace(/\.\d{3}Z$/, 'Z')
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
            .map(vue),
          page,
          taille,
        ),
      ),
    // US-EXI-24 : mêmes contrôles que DelegationsSandboxService de MS-IAM.
    demanderPrestataire: (d) =>
      o.attendre(() => {
        if (!d.composants.length) throw o.refus(400, 'IAM_SBX_COMPOSANT_REQUIS', 'Au moins un composant technique')
        const inconnus = d.composants.filter((c) => !(COMPOSANTS_SANDBOX as readonly string[]).includes(c))
        if (inconnus.length) throw o.refus(400, 'IAM_SBX_COMPOSANT_INCONNU', 'Composant technique inconnu', { composants: inconnus })
        const debut = Date.parse(d.debut)
        const fin = Date.parse(d.fin)
        if (!(debut < fin)) throw o.refus(422, 'IAM_SBX_PERIODE_INVALIDE', 'La date de début précède la date de fin')
        if (fin <= Date.now()) throw o.refus(422, 'IAM_SBX_PERIODE_ECHUE', 'La date de fin est déjà passée')
        if (fin - debut > DUREE_MAX_PRESTATAIRE) throw o.refus(422, 'IAM_SBX_DUREE_EXCESSIVE', 'Durée supérieure au maximum paramétré', { dureeMax: 'PT8H' })
        const n: [Delegation, string] = [{
          id: `d-presta-${delegations.length + 1}`, type: 'PRESTATAIRE', compteId: d.compteId, institutionId: null, permissions: [], composants: [...d.composants].sort(),
          debut: d.debut, fin: d.fin, etat: 'DEMANDEE', demandeurId: SESSION_CONSOLE, approbateurId: null, motif: d.motif, creeLe: maintenant(), closeLe: null,
        }, d.compteId]
        delegations.push(n)
        return vue(n)
      }),
    approuverPrestataire: (id) =>
      o.attendre(() => {
        const l = ouverte(acces(id))
        if (l[0].demandeurId === SESSION_CONSOLE) throw o.refus(403, 'IAM_SBX_APPROBATION_PROPRE', 'L’auteur de la demande ne l’approuve pas')
        return changer(l, { etat: 'ACCORDEE', approbateurId: SESSION_CONSOLE })
      }),
    refuserPrestataire: (id, motif) => o.attendre(() => changer(ouverte(acces(id)), { etat: 'REFUSEE', motif, closeLe: maintenant() })),
    registre: (institution, page, taille) =>
      o.attendre(() => paginer([...REGISTRE_DEMO].filter((e) => !institution || e.institution === institution.trim()).sort((a, b) => b.index - a.index), page, taille)),
    // Le fichier est composé après le délai simulé : un Blob ne se clone pas avec les réponses simulées.
    exporterRegistre: async (institution) => {
      const lignes = await o.attendre(() => REGISTRE_DEMO.filter((e) => !institution || e.institution === institution.trim()))
      const csv = ['index,identifiant,survenu_le,inscrit_le,acteur,type,institution,empreinte_feuille', ...lignes.map((e) => [e.index, e.eventId, e.survenueLe, e.inscritLe, e.acteur, e.type, e.institution ?? '', e.empreinte].join(','))]
      return new Blob([csv.join('\r\n') + '\r\n'], { type: 'text/csv;charset=utf-8' })
    },
    rebut: (page, taille) => o.attendre(() => ({ ...paginer([...REBUT_DEMO].sort((a, b) => b.recuLe.localeCompare(a.recuLe)), page, taille), seuil: 10, alerte: REBUT_DEMO.length > 10 })),
    revoquerPrestataire: (id, motif) =>
      o.attendre(() => {
        const l = acces(id)
        return l[0].etat === 'REVOQUEE' ? vue(l) : changer(l, { etat: 'REVOQUEE', motif, closeLe: maintenant() })
      }),
  }
}
