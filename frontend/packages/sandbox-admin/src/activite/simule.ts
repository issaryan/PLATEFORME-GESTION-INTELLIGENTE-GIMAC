import { ApiError } from '@gimac/socle'
import {
  PERMISSION_ACTIVITE,
  PERMISSION_LIAISON,
  type CodeRetour,
  type EtatLiaison,
  type FiltreActivite,
  type Granularite,
  type LiaisonCertification,
  type LigneCampagne,
  type LigneCas,
  type LigneParticipant,
  type PageStatistiques,
  type Taux,
  type Verdicts,
} from './modele'
import type { Pagination, ServiceActivite } from './ports'

// Statistiques simulées au contrat exact de StatistiquesConsoleController (SBX-LECTURE) : bornes du filtre, pages de
// 1 à 100, 404 SBX_LEC_INTROUVABLE sans SBX_SUP_CONSULTATION. Jeu fictif et déterministe : cinq participants (99001 à
// 99005), sept campagnes sur les trois interfaces, treize semaines d'exécutions jusqu'au 04/11/2026. Les agrégats sont
// calculés ici parce que le service est simulé ; l'implémentation HTTP ne fait que lire ceux de la base.

export type ExecutionDemo = { campagne: string; cas: string; service: string; le: string; verdict: 'favorable' | 'defavorable' | 'non_execute' | null }
export type CampagneDemo = { id: string; code: string; liaison: string; participant: string; interfaceTest: string; etat: string; ouverteLe: string; closeLe: string | null; cas: string[] }

export type OptionsActiviteSimulee = {
  permissions: string[]
  campagnes?: CampagneDemo[]
  executions?: ExecutionDemo[]
  liaisons?: LiaisonCertification[]
  /** Codes de retour de SBX-TRACE ; absents (null), le panneau affiche « source indisponible ». */
  codesRetour?: CodeRetour[] | null
  maintenant?: () => Date
}

const JOUR = 86_400_000
const ORIGINE = Date.parse('2026-08-05T00:00:00Z')
const uuid = (n: number) => `00000000-0000-4000-8000-${String(n).padStart(12, '0')}`
const iso = (ms: number) => new Date(ms).toISOString().replace('.000Z', 'Z')

const CAS: Record<string, string[]> = {
  ISO20022: ['PACS008-NOMINAL', 'PACS008-RC09', 'PACS002-RETOUR', 'CAMT056-ANNULATION', 'PAIN013-DEMANDE', 'ACMT023-VERIFICATION'],
  CARTE: ['0100-AUTORISATION', '0200-RETRAIT', '0400-ANNULATION', '0800-ECHO', '0120-AVIS'],
  MOBILE: ['MOB-TRANSFERT', 'MOB-STATUT', 'MOB-NOTIFICATION', 'MOB-FICHIER-FIN-JOURNEE'],
}

/** Campagnes fictives : participant, interface, jour d'ouverture, durée, état et taux de réussite visé. */
const PLAN: [string, string, number, number, string, number][] = [
  ['99001', 'ISO20022', 0, 40, 'TERMINEE', 0.92],
  ['99001', 'CARTE', 45, 46, 'EN_COURS', 0.7],
  ['99002', 'MOBILE', 10, 30, 'SUSPENDUE', 0.45],
  ['99003', 'ISO20022', 30, 61, 'EN_COURS', 0.62],
  ['99004', 'CARTE', 5, 50, 'TERMINEE', 0.88],
  ['99004', 'MOBILE', 60, 31, 'EN_COURS', 0.8],
  ['99005', 'ISO20022', 20, 35, 'TERMINEE', 0.97],
]

export const CAMPAGNES_DEMO: CampagneDemo[] = PLAN.map(([participant, interfaceTest, debut, duree, etat], i) => ({
  id: uuid(101 + i),
  code: `CMP-${participant}-${interfaceTest}`,
  liaison: uuid(Number(participant)),
  participant,
  interfaceTest,
  etat,
  ouverteLe: iso(ORIGINE + debut * JOUR + 8 * 3_600_000),
  closeLe: etat === 'TERMINEE' ? iso(ORIGINE + (debut + duree) * JOUR + 17 * 3_600_000) : null,
  cas: CAS[interfaceTest],
}))

/** Générateur congruentiel : le même jeu à chaque chargement. */
function suite(graine: number) {
  let x = graine
  return () => {
    x = (x * 1_103_515_245 + 12_345) % 2_147_483_648
    return x / 2_147_483_648
  }
}

export const EXECUTIONS_DEMO: ExecutionDemo[] = PLAN.flatMap(([, , debut, duree, etat, reussite], i) => {
  const c = CAMPAGNES_DEMO[i]
  const tirer = suite(7 + i)
  const fin = etat === 'TERMINEE' || etat === 'SUSPENDUE' ? debut + duree : Math.min(debut + duree, 91)
  const lignes: ExecutionDemo[] = []
  for (let j = debut; j < fin; j++) {
    const n = Math.floor(tirer() * 5)
    for (let k = 0; k < n; k++) {
      const cas = c.cas[Math.floor(tirer() * c.cas.length)]
      const r = tirer()
      const verdict = r < 0.06 ? null : r < 0.12 ? 'non_execute' : tirer() < reussite ? 'favorable' : 'defavorable'
      lignes.push({ campagne: c.id, cas, service: cas.split('-')[0], le: iso(ORIGINE + j * JOUR + (9 + k) * 3_600_000), verdict })
    }
  }
  return lignes
})

export const LIAISONS_DEMO: LiaisonCertification[] = (
  [['99001', 'CERTIFIEE'], ['99002', 'GELEE'], ['99003', 'EN_HOMOLOGATION'], ['99004', 'APTE_CERTIFICATION'], ['99005', 'MISE_EN_SERVICE_AUTORISEE']] as [string, EtatLiaison][]
).map(([code, etat]) => {
  const ordre: EtatLiaison[] = ['OUVERTE', 'EN_HOMOLOGATION', 'APTE_CERTIFICATION', 'MISE_EN_SERVICE_AUTORISEE', 'CERTIFIEE']
  const rang = ordre.indexOf(etat)
  return { id: uuid(Number(code)), institutionNom: `Institution fictive ${code}`, etat, etatsParcourus: rang < 0 ? ['OUVERTE', 'EN_HOMOLOGATION'] : ordre.slice(0, rang) }
})

const refus = (status: number, code: string) => new ApiError(status, code, '', `sim-sbx-lec-${code}`)
const invalide = () => refus(400, 'SBX_LEC_CRITERE_INVALIDE')
const taux = (n: number, d: number): Taux => ({ numerateur: n, denominateur: d, valeur: (d === 0 ? 0 : Math.floor((n / d) * 10_000) / 10_000).toFixed(4) })
const compter = (l: ExecutionDemo[]): Verdicts => ({
  favorables: l.filter((x) => x.verdict === 'favorable').length,
  defavorables: l.filter((x) => x.verdict === 'defavorable').length,
  nonExecutes: l.filter((x) => x.verdict === 'non_execute').length,
  enAttente: l.filter((x) => x.verdict === null).length,
})
const reussite = (v: Verdicts) => taux(v.favorables, v.favorables + v.defavorables)
const grouper = <T,>(l: T[], cle: (x: T) => string) => l.reduce((m, x) => m.set(cle(x), [...(m.get(cle(x)) ?? []), x]), new Map<string, T[]>())

/** Début de période (UTC) d'une date au pas demandé : jour, lundi de la semaine ou premier du mois. */
export function debutPeriode(le: string, g: Granularite): string {
  const d = new Date(le)
  if (g === 'MOIS') return `${le.slice(0, 7)}-01`
  if (g === 'SEMAINE') d.setUTCDate(d.getUTCDate() - ((d.getUTCDay() + 6) % 7))
  return d.toISOString().slice(0, 10)
}

export function activiteSimulee(o: OptionsActiviteSimulee): ServiceActivite {
  const campagnes = new Map((o.campagnes ?? CAMPAGNES_DEMO).map((c) => [c.id, c]))
  const toutes = o.executions ?? EXECUTIONS_DEMO
  const liaisons = new Map((o.liaisons ?? LIAISONS_DEMO).map((l) => [l.id, l]))
  const lire = <T,>(f: () => T) => Promise.resolve().then(f)
  const lecteur = () => {
    if (!o.permissions.includes(PERMISSION_ACTIVITE)) throw refus(404, 'SBX_LEC_INTROUVABLE')
  }
  const retenues = (f: FiltreActivite) => {
    lecteur()
    const du = Date.parse(f.du)
    const au = Date.parse(f.au)
    if (!(du < au) || au - du > 366 * JOUR || (f.participant && !/^[0-9]{5}$/.test(f.participant))) throw invalide()
    return toutes.filter((x) => {
      const c = campagnes.get(x.campagne)!
      const t = Date.parse(x.le)
      return t >= du && t < au && (!f.participant || c.participant === f.participant) && (!f.interface || c.interfaceTest === f.interface)
    })
  }
  const page = <T,>(l: T[], p: Pagination): PageStatistiques<T> => {
    if (p.page < 0 || p.taille < 1 || p.taille > 100) throw invalide()
    const suite = (p.page + 1) * p.taille < l.length
    return { elements: l.slice(p.page * p.taille, (p.page + 1) * p.taille), total: l.length, page: p.page, taille: p.taille, suivant: suite ? String(p.page + 1) : null }
  }
  // Groupes jamais vides : chaque ligne agrégée porte au moins une exécution.
  const premier = (l: ExecutionDemo[]) => l.map((x) => x.le).sort()[0]
  const dernier = (l: ExecutionDemo[]) => l.map((x) => x.le).sort()[l.length - 1]

  return {
    synthese: (f) => lire(() => {
      const l = retenues(f)
      const parCampagne = grouper(l, (x) => x.campagne)
      const parEtat: Record<string, number> = {}
      parCampagne.forEach((_, id) => { const e = campagnes.get(id)!.etat; parEtat[e] = (parEtat[e] ?? 0) + 1 })
      const derniersCas = [...grouper(l, (x) => `${x.campagne}/${x.cas}`).values()].map((x) => x.at(-1)!)
      const casFavorables = derniersCas.filter((x) => x.verdict === 'favorable').length
      const casJuges = derniersCas.filter((x) => x.verdict === 'favorable' || x.verdict === 'defavorable').length
      return {
        du: f.du, au: f.au, calculeLe: (o.maintenant ? o.maintenant() : new Date()).toISOString(), executions: l.length,
        participantsActifs: new Set(l.map((x) => campagnes.get(x.campagne)!.participant)).size, campagnes: parCampagne.size, campagnesParEtat: parEtat,
        verdicts: compter(l), tauxReussiteCas: taux(casFavorables, casJuges),
      }
    }),
    activite: (f, g) => lire(() => {
      const l = retenues(f)
      if (g === 'JOUR' && Date.parse(f.au) - Date.parse(f.du) > 92 * JOUR) throw invalide()
      const points = [...grouper(l, (x) => `${debutPeriode(x.le, g)}|${campagnes.get(x.campagne)!.interfaceTest}`).entries()]
        .sort(([a], [b]) => a.localeCompare(b))
        .map(([k, x]) => ({ periode: k.split('|')[0], interfaceTest: k.split('|')[1], executions: x.length, verdicts: compter(x) }))
      return { du: f.du, au: f.au, granularite: g, points }
    }),
    participants: (f, p) => lire(() => {
      const lignes: LigneParticipant[] = [...grouper(retenues(f), (x) => campagnes.get(x.campagne)!.participant).entries()].sort(([a], [b]) => a.localeCompare(b)).map(([code, l]) => {
        const recente = campagnes.get([...l].sort((a, b) => b.le.localeCompare(a.le))[0].campagne)!
        const v = compter(l)
        return {
          codeParticipant: code, identifiantLiaison: recente.liaison, interfaces: [...new Set(l.map((x) => campagnes.get(x.campagne)!.interfaceTest))].sort(),
          campagnes: new Set(l.map((x) => x.campagne)).size, executions: l.length, verdicts: v, tauxReussite: reussite(v), premierEssai: premier(l), dernierEssai: dernier(l),
          derniereCampagne: { identifiantCampagne: recente.id, etat: recente.etat },
        }
      })
      return page(lignes, p)
    }),
    campagnes: (f, tri, p) => lire(() => {
      const lignes: LigneCampagne[] = [...grouper(retenues(f), (x) => x.campagne).entries()].map(([id, l]) => {
        const c = campagnes.get(id)!
        const v = compter(l)
        const cas = new Set(l.map((x) => x.cas)).size
        return {
          identifiantCampagne: id, code: c.code, identifiantLiaison: c.liaison, codeParticipant: c.participant, interfaceTest: c.interfaceTest, etat: c.etat,
          executions: l.length, tauxReussite: reussite(v), tauxAvancement: taux(cas, c.cas.length), premierEssai: premier(l), dernierEssai: dernier(l),
          ouverteLe: c.ouverteLe, closeLe: c.closeLe, versionBilan: c.closeLe ? 1 : null,
        }
      })
      const valeur = (x: LigneCampagne) => (x.tauxReussite.denominateur ? Number(x.tauxReussite.valeur) : 2)
      lignes.sort(tri === 'TAUX_REUSSITE' ? (a, b) => valeur(a) - valeur(b) || a.identifiantCampagne.localeCompare(b.identifiantCampagne) : (a, b) => String(b.dernierEssai).localeCompare(String(a.dernierEssai)))
      return page(lignes, p)
    }),
    cas: (f, p) => lire(() => {
      const lignes: LigneCas[] = [...grouper(retenues(f), (x) => `${x.cas}|${campagnes.get(x.campagne)!.interfaceTest}`).entries()].map(([k, l]) => {
        const v = compter(l)
        return { codeCas: k.split('|')[0], codeService: l[0].service, interfaceTest: k.split('|')[1], executions: l.length, verdicts: v, tauxReussite: reussite(v) }
      })
      lignes.sort((a, b) => b.verdicts.defavorables - a.verdicts.defavorables || a.codeCas.localeCompare(b.codeCas))
      return page(lignes, p)
    }),
    codesRetour: (f, p) => lire(() => {
      retenues(f)
      return o.codesRetour ? page(o.codesRetour, p) : null
    }),
    liaison: (id) => lire(() => {
      if (!o.permissions.includes(PERMISSION_LIAISON)) throw refus(403, 'HOM_ACCES_REFUSE')
      const l = liaisons.get(id)
      if (!l) throw refus(404, 'HOM_LIAISON_INTROUVABLE')
      return l
    }),
  }
}
