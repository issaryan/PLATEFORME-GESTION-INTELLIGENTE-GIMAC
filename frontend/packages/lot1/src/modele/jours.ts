// Jours et semaines de « Mon activité » (D-76) : fuseau Africa/Douala (UTC+1, sans heure d'été), jours
// AAAA-MM-JJ, semaines ISO AAAA-Www. Fonctions pures, communes aux écrans et au service simulé.

const DECALAGE_MS = 3_600_000
const JOUR_MS = 86_400_000
const JOUR = /^(\d{4})-(\d{2})-(\d{2})$/

const versUtc = (jour: string) => {
  const m = JOUR.exec(jour)
  if (!m) throw new Error(`Jour invalide : ${jour}`)
  return Date.UTC(Number(m[1]), Number(m[2]) - 1, Number(m[3]))
}
const depuisUtc = (ms: number) => new Date(ms).toISOString().slice(0, 10)

/** Jour à Douala d'un instant (maintenant par défaut). */
export const jourDe = (instant: Date | string = new Date()) => depuisUtc(new Date(instant).getTime() + DECALAGE_MS)
export const aujourdhui = () => jourDe(new Date())
export const ajouterJours = (jour: string, n: number) => depuisUtc(versUtc(jour) + n * JOUR_MS)
/** Jour de la semaine, 1 (lundi) à 7 (dimanche). */
export const jourSemaine = (jour: string) => ((new Date(versUtc(jour)).getUTCDay() + 6) % 7) + 1
export const lundiDe = (jour: string) => ajouterJours(jour, 1 - jourSemaine(jour))
/** Les sept jours (lundi à dimanche) de la semaine d'un jour. */
export const semaineDe = (jour: string) => Array.from({ length: 7 }, (_, i) => ajouterJours(lundiDe(jour), i))
/** Jour ouvré suivant (le lundi après un vendredi). */
export const jourOuvreSuivant = (jour: string) => {
  let j = ajouterJours(jour, 1)
  while (jourSemaine(j) > 5) j = ajouterJours(j, 1)
  return j
}

/** Semaine ISO d'un jour (AAAA-Www) : celle de son jeudi. */
export function semaineIso(jour: string): string {
  const jeudi = ajouterJours(jour, 4 - jourSemaine(jour))
  const annee = Number(jeudi.slice(0, 4))
  const numero = Math.floor((versUtc(jeudi) - Date.UTC(annee, 0, 1)) / JOUR_MS / 7) + 1
  return `${annee}-W${String(numero).padStart(2, '0')}`
}

/** Lundi d'une semaine ISO (AAAA-Www). */
export function lundiDeSemaine(semaine: string): string {
  const m = /^(\d{4})-W(\d{2})$/.exec(semaine)
  if (!m) throw new Error(`Semaine invalide : ${semaine}`)
  const quatreJanvier = `${m[1]}-01-04`
  return ajouterJours(lundiDe(quatreJanvier), (Number(m[2]) - 1) * 7)
}

/** Instant ISO 8601 à Douala d'un jour et d'une heure HH:MM. */
export const instant = (jour: string, heure: string) => `${jour}T${heure}:00+01:00`
/** Heure à Douala (HH:MM) d'un instant. */
export const heureDe = (iso: string) => new Date(new Date(iso).getTime() + DECALAGE_MS).toISOString().slice(11, 16)
/** Minutes écoulées depuis minuit à Douala. */
export const minutesDe = (iso: string) => {
  const [h, m] = heureDe(iso).split(':').map(Number)
  return h * 60 + m
}

/** Saisie JJ/MM/AAAA vers AAAA-MM-JJ ; null si la date n'existe pas. */
export function depuisSaisie(saisie: string): string | null {
  const m = /^(\d{2})\/(\d{2})\/(\d{4})$/.exec(saisie.trim())
  if (!m) return null
  const jour = `${m[3]}-${m[2]}-${m[1]}`
  return depuisUtc(versUtc(jour)) === jour ? jour : null
}
/** AAAA-MM-JJ vers la saisie JJ/MM/AAAA. */
export const versSaisie = (jour: string) => `${jour.slice(8, 10)}/${jour.slice(5, 7)}/${jour.slice(0, 4)}`
/** Heure HH:MM valide. */
export const heureValide = (h: string) => /^([01]\d|2[0-3]):[0-5]\d$/.test(h.trim())
