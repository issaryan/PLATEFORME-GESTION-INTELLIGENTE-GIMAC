// Outils communs des graphiques de métriques (Sparkline, SerieTemporelle, BarresHorizontales) : échelle
// à graduations rondes, tracé tolérant aux trous (valeur nulle = absence de mesure, jamais zéro),
// nombres compacts et graduations temporelles dans la langue de l'interface.

/** Point d'une série temporelle : instant en millisecondes, valeur nulle quand la mesure manque. */
export interface PointMesure {
  t: number
  v: number | null
}

/** Pas « rond » (1, 2, 2,5, 5 × 10^n) couvrant l'étendue en environ `cible` intervalles. */
export function pasRond(etendue: number, cible = 4): number {
  if (!(etendue > 0)) return 1
  const brut = etendue / cible
  const p = Math.pow(10, Math.floor(Math.log10(brut)))
  const n = brut / p
  const m = n < 1.5 ? 1 : n < 2.25 ? 2 : n < 3.5 ? 2.5 : n < 7.5 ? 5 : 10
  return m * p
}

/** Domaine arrondi et graduations : part de `min` (0 par défaut) et monte au multiple rond suivant. */
export function echelle(valeurs: (number | null)[], min?: number, max?: number): { min: number; max: number; graduations: number[] } {
  const vs = valeurs.filter((v): v is number => v !== null && Number.isFinite(v))
  const bas = min ?? Math.min(0, ...vs)
  const hautBrut = max ?? (vs.length ? Math.max(...vs) : 1)
  const haut = hautBrut > bas ? hautBrut : bas + 1
  const pas = pasRond(haut - bas)
  const hautRond = max ?? Math.ceil(haut / pas) * pas
  const basRond = min ?? Math.floor(bas / pas) * pas
  const graduations: number[] = []
  for (let v = basRond; v <= hautRond + pas / 1000; v += pas) graduations.push(Number(v.toFixed(10)))
  return { min: basRond, max: hautRond, graduations }
}

/** Chemin SVG d'une courbe : un nouveau segment après chaque trou (valeur nulle). */
export function cheminCourbe(points: { x: number; y: number | null }[]): string {
  let d = ''
  let leve = true
  for (const p of points) {
    if (p.y === null) {
      leve = true
      continue
    }
    d += (leve ? 'M' : 'L') + p.x.toFixed(1) + ' ' + p.y.toFixed(1) + ' '
    leve = false
  }
  return d.trim()
}

/** Aire sous chaque segment continu de la courbe, fermée sur la ligne de base `y0`. */
export function cheminAire(points: { x: number; y: number | null }[], y0: number): string {
  const segments: { x: number; y: number }[][] = []
  let courant: { x: number; y: number }[] = []
  for (const p of points) {
    if (p.y === null) {
      if (courant.length) segments.push(courant)
      courant = []
    } else courant.push({ x: p.x, y: p.y })
  }
  if (courant.length) segments.push(courant)
  return segments
    .filter((s) => s.length > 1)
    .map((s) => 'M' + s[0].x.toFixed(1) + ' ' + y0.toFixed(1) + ' ' + s.map((q) => 'L' + q.x.toFixed(1) + ' ' + q.y.toFixed(1)).join(' ') + ' L' + s[s.length - 1].x.toFixed(1) + ' ' + y0.toFixed(1) + ' Z')
    .join(' ')
}

const LOCALES: Record<string, string> = { fr: 'fr-FR', en: 'en-GB', es: 'es-ES' }

export function locale(langue: string): string {
  return LOCALES[langue] ?? 'fr-FR'
}

/** Nombre compact (1 284 · 12,9 k · 4,2 M), décimales selon l'ordre de grandeur. */
export function formatCompact(v: number | null | undefined, langue = 'fr'): string {
  if (v === null || v === undefined || !Number.isFinite(v)) return '-'
  const a = Math.abs(v)
  const decimales = a >= 100 || Number.isInteger(v) ? 0 : a >= 10 ? 1 : 2
  if (a >= 10000) {
    return new Intl.NumberFormat(locale(langue), { notation: 'compact', maximumFractionDigits: 1 }).format(v)
  }
  return new Intl.NumberFormat(locale(langue), { maximumFractionDigits: decimales }).format(v)
}

/** Graduations temporelles : environ `cible` instants ronds dans [debut, fin]. */
export function graduationsTemps(debut: number, fin: number, cible = 5): number[] {
  if (!(fin > debut)) return [debut]
  const PAS = [60e3, 300e3, 900e3, 1800e3, 3600e3, 3 * 3600e3, 6 * 3600e3, 12 * 3600e3, 86400e3, 2 * 86400e3, 7 * 86400e3, 30 * 86400e3]
  const pas = PAS.find((p) => (fin - debut) / p <= cible) ?? PAS[PAS.length - 1]
  const decalage = new Date(debut).getTimezoneOffset() * 60e3
  const premier = Math.ceil((debut - decalage) / pas) * pas + decalage
  const r: number[] = []
  for (let t = premier; t <= fin; t += pas) r.push(t)
  return r.length ? r : [debut]
}

/** Libellé d'une graduation : heure sur une étendue d'au plus 36 h, date sinon. */
export function libelleTemps(t: number, etendue: number, langue = 'fr'): string {
  const opts: Intl.DateTimeFormatOptions = etendue <= 36 * 3600e3 ? { hour: '2-digit', minute: '2-digit' } : { day: '2-digit', month: '2-digit' }
  return new Intl.DateTimeFormat(locale(langue), opts).format(new Date(t))
}

/** Instant complet (infobulle, vue tableau). */
export function libelleInstant(t: number, langue = 'fr'): string {
  return new Intl.DateTimeFormat(locale(langue), { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' }).format(new Date(t))
}

/** Pas des points d'une série : mesures instantanées (supervision), comptes par jour ou par mois (tableaux de bord). */
export type Granularite = 'instant' | 'jour' | 'mois'

/** Libellé complet d'un point (infobulle, vue tableau) : instant, jour (« lun. 06/10 ») ou mois (« octobre 2026 »). */
export function libellePeriode(t: number, granularite: Granularite, langue = 'fr'): string {
  if (granularite === 'instant') return libelleInstant(t, langue)
  const opts: Intl.DateTimeFormatOptions = granularite === 'jour' ? { weekday: 'short', day: '2-digit', month: '2-digit' } : { month: 'long', year: 'numeric' }
  return new Intl.DateTimeFormat(locale(langue), opts).format(new Date(t))
}

/** Libellé d'une graduation de l'axe du temps selon la granularité (« 06/10 », « oct. 26 »). */
export function libelleGraduation(t: number, granularite: Granularite, etendue: number, langue = 'fr'): string {
  if (granularite === 'mois') return new Intl.DateTimeFormat(locale(langue), { month: 'short', year: '2-digit' }).format(new Date(t))
  if (granularite === 'jour') return new Intl.DateTimeFormat(locale(langue), { day: '2-digit', month: '2-digit' }).format(new Date(t))
  return libelleTemps(t, etendue, langue)
}

/** Graduations prises parmi les points eux-mêmes (mois de longueurs inégales) : un point sur k, le dernier compris. */
export function graduationsPoints(instants: number[], cible = 5): number[] {
  if (instants.length <= cible) return instants
  const k = Math.ceil(instants.length / cible)
  const r: number[] = []
  for (let i = instants.length - 1; i >= 0; i -= k) r.unshift(instants[i])
  return r
}

/** Classe de série : quatre teintes dans l'ordre fixe, puis « Autres ». */
export function classeSerie(index: number): string {
  return index < 4 ? 'gm-serie-' + (index + 1) : 'gm-serie-autre'
}
