import { ApiError } from '@gimac/socle'
import { PERMISSION_SUPERVISION, type Alerte, type Disponibilite, type ElementSupervise, type EtatSante, type MesureService, type PageSupervision } from '../modele'
import type { ServiceSupervision } from './ports'

export type OptionsSupervisionSimulee = {
  permissions: string[]
  institution?: string | null
  sante?: EtatSante
  elementsSupervises?: ElementSupervise[]
  alertesSandbox?: Alerte[]
  disponibilitesSandbox?: Disponibilite[]
  servicesSupervises?: MesureService[]
  latenceMs?: number
}

export const SANTE_DEMO: EtatSante = {
  id: '10000000-0000-0000-0000-000000000001', niveauGlobal: 'DEGRADE', causeExterne: false, calculeLe: '2026-10-22T08:55:00Z',
  etatsModules: JSON.stringify({ carte: 'DEGRADE', iso20022: 'NOMINAL', lecture: 'NOMINAL', mobile: 'NOMINAL', portail: 'NOMINAL' }),
}

export const ELEMENTS_DEMO: ElementSupervise[] = Array.from({ length: 23 }, (_, i) => ({
  id: `20000000-0000-0000-0000-${String(i + 1).padStart(12, '0')}`,
  nature: i % 2 ? 'POINT_ENTREE' : 'MODULE', module: i % 2 ? 'SBX-CARTE' : 'SBX-MOBILE', codeParticipant: i % 3 ? null : '99001',
  seuilDelai: 'PT0.5S', seuilEchecs: 3, criticite: i % 2 ? 'MAJEURE' : 'MINEURE', externe: i % 2 === 1, enMaintenance: i === 22,
}))

export const ALERTES_DEMO: Alerte[] = [
  { id: '30000000-0000-0000-0000-000000000001', criticite: 'MAJEURE', message: 'Délai dépassé sur la liaison de test', etat: 'LEVEE', leveeLe: '2026-10-22T08:45:00Z', closeLe: null, element: { id: ELEMENTS_DEMO[1].id, nature: ELEMENTS_DEMO[1].nature } },
  { id: '30000000-0000-0000-0000-000000000002', criticite: 'MINEURE', message: 'Retard d’ingestion résorbé', etat: 'CLOSE', leveeLe: '2026-10-21T16:00:00Z', closeLe: '2026-10-21T16:20:00Z', element: { id: ELEMENTS_DEMO[0].id, nature: ELEMENTS_DEMO[0].nature } },
]

export const DISPONIBILITES_DEMO: Disponibilite[] = [
  { id: '40000000-0000-0000-0000-000000000001', code: 'DISPONIBILITE_MENSUELLE', module: 'SBX-CARTE', periode: '2026-10', valeur: 9950, publieLe: '2026-10-22T08:00:00Z' },
  { id: '40000000-0000-0000-0000-000000000002', code: 'DISPONIBILITE_MENSUELLE', module: 'SBX-MOBILE', periode: '2026-10', valeur: 10000, publieLe: '2026-10-22T08:00:00Z' },
]

const MODULES: Record<string, string> = { 'sbx-mobile': 'mobile', 'sbx-iso20022': 'iso20022', 'sbx-carte': 'carte', 'sbx-lecture': 'lecture' }
const NOMS = ['campagne', 'carte', 'donnees', 'espace', 'iso20022', 'lecture', 'mobile', 'raccordement', 'referentiel', 'simulation', 'trace']

/** Onze services, dans l'ordre de SBX-TRACE ; SBX-CARTE dégradé, cohérent avec SANTE_DEMO. */
export const SERVICES_DEMO: MesureService[] = NOMS.map((n, i) => {
  const service = `sbx-${n}`
  const serie = Array.from({ length: 20 }, (_, j) => ({
    le: new Date(Date.UTC(2026, 9, 22, 8, 45, j * 30)).toISOString(),
    latenceMoyenneMs: 20 + ((i * 7 + j * 3) % 25),
    debitParMinute: 40 + ((i * 11 + j * 5) % 60),
    erreurs: j % 9 === 0 ? 1 : 0,
  }))
  return {
    service, module: MODULES[service] ?? 'portail', niveau: n === 'carte' ? 'DEGRADE' : 'NOMINAL', joignable: true,
    mesureLe: '2026-10-22T08:55:00Z', delaiMs: 8 + i, latenceMoyenneMs: serie[19].latenceMoyenneMs, latenceMaxMs: 180 + i * 10,
    debitParMinute: serie[19].debitParMinute, erreurs: n === 'carte' ? 4 : 0, retardKafka: n === 'trace' ? 12 : i % 3,
    rebuts: n === 'campagne' ? 1 : 0, disponibilite24h: n === 'carte' ? 9875 : 10000, serie,
  }
})

const copie = <T,>(x: T): T => structuredClone(x)

export function supervisionSimulee(o: OptionsSupervisionSimulee): ServiceSupervision {
  const attendre = <T,>(f: () => T): Promise<T> => new Promise((ok, ko) => setTimeout(() => {
    try { ok(f()) } catch (e) { ko(e) }
  }, o.latenceMs ?? 0))
  const lecture = <T,>(f: () => T) => attendre(() => {
    if (o.institution || !o.permissions.includes(PERMISSION_SUPERVISION)) throw new ApiError(404, 'SBX_TRA_PERMISSION_ABSENTE', '', 'sim-sbx-supervision')
    return f()
  })
  const page = <T,>(source: T[], q: { page: number; taille: number }): PageSupervision<T> => {
    if (q.page < 0 || q.taille < 1 || q.taille > 100) throw new ApiError(422, 'SBX_TRA_CRITERE_INVALIDE', '', 'sim-sbx-supervision')
    return { elements: copie(source.slice(q.page * q.taille, (q.page + 1) * q.taille)), total: source.length, page: q.page, taille: q.taille, suivant: null }
  }
  const elements = o.elementsSupervises ?? ELEMENTS_DEMO
  const alertes = o.alertesSandbox ?? ALERTES_DEMO
  const disponibilites = o.disponibilitesSandbox ?? DISPONIBILITES_DEMO
  const services = o.servicesSupervises ?? SERVICES_DEMO
  return {
    etatSante: () => lecture(() => copie(o.sante ?? SANTE_DEMO)),
    elements: (q) => lecture(() => page(elements, q)),
    alertes: (q) => lecture(() => page(alertes, q)),
    disponibilites: (q) => lecture(() => page(disponibilites, q)),
    services: (q) => lecture(() => page(services, q)),
  }
}
