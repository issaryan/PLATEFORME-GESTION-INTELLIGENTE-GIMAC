import { prefixeIam, type Couche } from '../http/prefixes'

// Photos de profil (docs/lot1/mon-compte.md §1) : l'image d'un compte est servie par MS-IAM, au travers de
// la passerelle, à l'adresse /bff/<couche>/iam/avatars/{compteId}?v=<version> (portail : /bff/portail/iam/moi/avatar,
// seule photo qui lui est ouverte, §5). La version (empreinte courte
// rendue par MS-IAM dans /moi, l'annuaire et les comptes) change à chaque remplacement : l'image est mise en
// cache sans limite par le navigateur (immutable) et une nouvelle version force son rechargement. Sans
// version, le compte n'a pas de photo : les initiales s'affichent.
//
// Services simulés : aucune passerelle ne sert les images. Les photos de démonstration et celles déposées
// pendant la démonstration sont gardées en mémoire de la page (adresses data:, admises par la politique de
// sécurité du contenu) ; urlAvatar les rend en priorité. Avec les services réels, ce registre reste vide.

const registre = new Map<string, { version: string; adresse: string }>()
let sequence = 0

/** Adresse de la photo d'un compte, ou undefined sans photo (initiales). */
export function urlAvatar(couche: Couche, compteId: string | null | undefined, version: string | null | undefined): string | undefined {
  if (!compteId || !version) return undefined
  const simulee = registre.get(compteId)
  if (simulee) return simulee.adresse
  // Portail : seule la photo du titulaire est lisible, par /moi/avatar (mon-compte.md §5).
  if (couche === 'portail') return `${prefixeIam(couche)}/moi/avatar?v=${encodeURIComponent(version)}`
  return `${prefixeIam(couche)}/avatars/${encodeURIComponent(compteId)}?v=${encodeURIComponent(version)}`
}

/** Registre des photos des services simulés (démonstration et tests ; jamais avec les services réels). */
export const avatarsSimules = {
  /** Range une photo et rend sa nouvelle version, comme MS-IAM (empreinte courte). */
  definir(compteId: string, adresse: string): string {
    const version = `sim${(++sequence).toString(36)}${adresse.length.toString(36)}`.slice(0, 12)
    registre.set(compteId, { version, adresse })
    return version
  },
  retirer(compteId: string): void {
    registre.delete(compteId)
  },
  /** Version de la photo rangée, ou null. */
  version(compteId: string): string | null {
    return registre.get(compteId)?.version ?? null
  },
  /** Photo de démonstration rangée une seule fois par compte ; rend sa version. */
  demo(compteId: string): string {
    return registre.get(compteId)?.version ?? this.definir(compteId, photoDemo(compteId))
  },
  reinitialiser(): void {
    registre.clear()
  },
}

// Palettes des photos de démonstration : paysages simples (ciel, soleil, collines), sans visage.
const PALETTES = [
  ['#9cc3d5', '#f3d27a', '#4f7a4a', '#2f5233'],
  ['#e8b48a', '#fbe3b0', '#8a5a3c', '#5b3a29'],
  ['#b9c9e8', '#ffffff', '#5d7fa3', '#34506f'],
  ['#c7dcc0', '#f6e7a1', '#6d8f4e', '#3e5a2d'],
  ['#f0c9a8', '#fff1d6', '#c0754a', '#7a4127'],
  ['#a9d1cf', '#f8f4e3', '#3f8a83', '#22524e'],
]

function empreinte(graine: string): number {
  let h = 2166136261
  for (const c of graine) h = Math.imul(h ^ c.charCodeAt(0), 16777619)
  return h >>> 0
}

/** Photo de démonstration déterministe (SVG en adresse data:, 256 × 256), propre à chaque compte. */
export function photoDemo(graine: string): string {
  const h = empreinte(graine)
  const [ciel, soleil, colline, premierPlan] = PALETTES[h % PALETTES.length]
  const sx = 60 + (h % 140)
  const sy = 50 + ((h >>> 8) % 50)
  const c1 = 120 + ((h >>> 12) % 60)
  const c2 = 150 + ((h >>> 16) % 50)
  const svg =
    `<svg xmlns="http://www.w3.org/2000/svg" width="256" height="256" viewBox="0 0 256 256">` +
    `<rect width="256" height="256" fill="${ciel}"/>` +
    `<circle cx="${sx}" cy="${sy}" r="28" fill="${soleil}"/>` +
    `<path d="M0 ${c1} Q 64 ${c1 - 40} 128 ${c1} T 256 ${c1 - 10} V256 H0 Z" fill="${colline}"/>` +
    `<path d="M0 ${c2 + 30} Q 96 ${c2 - 10} 180 ${c2 + 20} T 256 ${c2} V256 H0 Z" fill="${premierPlan}"/>` +
    `</svg>`
  return `data:image/svg+xml;charset=utf-8,${encodeURIComponent(svg)}`
}
