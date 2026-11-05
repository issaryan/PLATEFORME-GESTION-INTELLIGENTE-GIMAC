import type { PointMesure } from '@gimac/design-system'
import { INTERFACES, type FiltreActivite, type Granularite, type InterfaceTest, type PointActivite } from './modele'

// Filtres de l'écran « Activité des tests », portés par l'adresse (?periode=&participant=&interface=) : une seule
// rangée au-dessus de tous les graphes, qui lisent la même tranche. La période se termine à minuit UTC suivant.

export const PERIODES = [7, 30, 90, 365] as const
export type Periode = (typeof PERIODES)[number]
const JOUR = 86_400_000

export type Vue = { periode: Periode; filtre: FiltreActivite; granularite: Granularite }

export function lireVue(params: URLSearchParams, maintenant: Date): Vue {
  const demandee = Number(params.get('periode'))
  const periode = (PERIODES as readonly number[]).includes(demandee) ? (demandee as Periode) : 30
  const au = Date.UTC(maintenant.getUTCFullYear(), maintenant.getUTCMonth(), maintenant.getUTCDate() + 1)
  const participant = /^[0-9]{5}$/.test(params.get('participant') ?? '') ? (params.get('participant') as string) : undefined
  const brute = params.get('interface') ?? ''
  const interfaceTest = (INTERFACES as readonly string[]).includes(brute) ? (brute as InterfaceTest) : undefined
  const filtre: FiltreActivite = { du: iso(au - periode * JOUR), au: iso(au), participant, interface: interfaceTest }
  return { periode, filtre, granularite: periode <= 31 ? 'JOUR' : periode <= 92 ? 'SEMAINE' : 'MOIS' }
}

const iso = (ms: number) => new Date(ms).toISOString().replace('.000Z', 'Z')

/** Débuts de période (UTC) couvrant [du, au[ au pas demandé : chaque pas sans exécution vaut zéro, pas un trou. */
export function pas(f: FiltreActivite, g: Granularite): number[] {
  const d = new Date(f.du)
  if (g === 'SEMAINE') d.setUTCDate(d.getUTCDate() - ((d.getUTCDay() + 6) % 7))
  if (g === 'MOIS') d.setUTCDate(1)
  const fin = Date.parse(f.au)
  const l: number[] = []
  while (d.getTime() < fin) {
    l.push(d.getTime())
    if (g === 'MOIS') d.setUTCMonth(d.getUTCMonth() + 1)
    else d.setUTCDate(d.getUTCDate() + (g === 'SEMAINE' ? 7 : 1))
  }
  return l
}

/** Série d'une interface sur tous les pas de la période. */
export function serie(points: PointActivite[], interfaceTest: string, debuts: number[]): PointMesure[] {
  const parPeriode = new Map(points.filter((p) => p.interfaceTest === interfaceTest).map((p) => [Date.parse(`${p.periode}T00:00:00Z`), p.executions]))
  // Le pas UTC est placé à minuit local du même jour : l'axe et les libellés du design-system sont en heure locale.
  return debuts.map((t) => {
    const d = new Date(t)
    return { t: new Date(d.getUTCFullYear(), d.getUTCMonth(), d.getUTCDate()).getTime(), v: parPeriode.get(t) ?? 0 }
  })
}

/** Taux du service (« 0.5000 ») en pourcentage. */
export const pourcent = (valeur: string) => Math.round(Number(valeur) * 1000) / 10

/** Jours entiers entre deux instants (au moins un). */
export const jours = (du: string, au: string) => Math.max(1, Math.round((Date.parse(au) - Date.parse(du)) / JOUR))
