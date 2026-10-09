import type { Compte } from '@gimac/compte'
import { ACTIONS_POUVOIR, PORTEES, completerActions, rangPortee, type ActionPouvoir, type ActionsPouvoir, type PouvoirsProfil, type Portee, type Unite } from '@gimac/lot1'

// Organigramme (D-86 §3) : calculs de présentation sur les représentations de MS-IAM (unités, comptes, matrice des
// pouvoirs). Ils n'ouvrent ni ne ferment aucun geste : MS-IAM et MS-PILOTAGE refont chaque contrôle.

/** Unité et toutes ses sous-unités (codes). */
export function sousUnites(unites: Unite[], code: string): string[] {
  const l: string[] = []
  const visiter = (c: string) => {
    if (l.includes(c)) return
    l.push(c)
    unites.filter((u) => u.parent === c).forEach((u) => visiter(u.code))
  }
  visiter(code)
  return l
}

/** Vrai si l'unité peut être placée sous la cible : ni elle-même, ni l'une de ses sous-unités, cible active. */
export function deplacementPossible(unites: Unite[], code: string, cible: string): boolean {
  const c = unites.find((u) => u.code === cible)
  const u = unites.find((x) => x.code === code)
  return !!c && !!u && c.active && u.parent !== cible && !sousUnites(unites, code).includes(cible)
}

/** Comptes internes actifs d'une unité et de ses sous-unités (effectif de la carte). */
export function effectif(unites: Unite[], comptes: Compte[], code: string): number {
  const codes = sousUnites(unites, code)
  return comptes.filter((c) => c.actif && !c.institutionId && c.unite && codes.includes(c.unite)).length
}

/** Vrai si « responsable » est dans la chaîne des n+1 de « compte » (ou est le compte lui-même). */
export function dansLaChaine(comptes: Compte[], compte: string, responsable: string): boolean {
  if (compte === responsable) return true
  const vus = new Set<string>()
  for (let r = comptes.find((c) => c.id === responsable); r?.responsableId && !vus.has(r.id); r = comptes.find((c) => c.id === r!.responsableId)) {
    vus.add(r.id)
    if (r.responsableId === compte) return true
  }
  return false
}

/** Nœud de l'arbre des membres d'une unité : le compte et ceux dont il est le n+1 dans l'unité. */
export type NoeudMembre = { compte: Compte; enfants: NoeudMembre[] }

/** Membres d'une unité en arbre des n+1 ; racines : les membres dont le n+1 n'est pas membre de l'unité. */
export function arbreMembres(comptes: Compte[], code: string): NoeudMembre[] {
  const membres = comptes.filter((c) => c.unite === code && !c.institutionId).sort((a, b) => a.nomAffiche.localeCompare(b.nomAffiche, 'fr'))
  const ids = new Set(membres.map((m) => m.id))
  const vus = new Set<string>()
  const noeud = (c: Compte): NoeudMembre => {
    vus.add(c.id)
    return { compte: c, enfants: membres.filter((m) => m.responsableId === c.id && !vus.has(m.id)).map(noeud) }
  }
  const racines = membres.filter((m) => !m.responsableId || !ids.has(m.responsableId)).map(noeud)
  // Boucle hiérarchique héritée (jamais acceptée par MS-IAM) : les membres restants deviennent des racines.
  return [...racines, ...membres.filter((m) => !vus.has(m.id)).map(noeud)]
}

/** Portée la plus large de chaque action sur les profils du compte (une action absente vaut AUCUNE). */
export function actionsDe(profils: string[], matrice: PouvoirsProfil[]): ActionsPouvoir {
  const lignes = matrice.filter((l) => profils.includes(l.profil)).map((l) => completerActions(l.actions))
  return Object.fromEntries(ACTIONS_POUVOIR.map((a) => [a, PORTEES[Math.max(0, ...lignes.map((l) => rangPortee(l[a])))] as Portee])) as ActionsPouvoir
}

/**
 * Personnes couvertes par une portée (D-81 §1) : DIRECTS, les comptes dont il est le n+1 ; EQUIPE, toute sa chaîne
 * descendante ; UNITES, l'équipe et les membres des unités qu'il dirige (sous-unités comprises).
 */
export function couverts(id: string, portee: Portee, comptes: Compte[], unites: Unite[]): Compte[] {
  const r = rangPortee(portee)
  if (r === 0) return []
  const l: Compte[] = []
  const ajouter = (c: Compte) => {
    if (c.id !== id && !c.institutionId && !l.some((x) => x.id === c.id)) l.push(c)
  }
  const descendre = (chef: string, profondeur: number) => comptes.filter((c) => c.responsableId === chef).forEach((c) => {
    if (l.some((x) => x.id === c.id) || profondeur > 10) return
    ajouter(c)
    if (r >= rangPortee('EQUIPE')) descendre(c.id, profondeur + 1)
  })
  descendre(id, 1)
  if (r >= rangPortee('UNITES')) {
    const dirigees = unites.filter((u) => u.responsableId === id).flatMap((u) => sousUnites(unites, u.code))
    comptes.filter((c) => c.unite && dirigees.includes(c.unite)).forEach(ajouter)
  }
  return l.sort((a, b) => a.nomAffiche.localeCompare(b.nomAffiche, 'fr'))
}

/** Pouvoirs effectifs d'un compte : portée et personnes couvertes pour chaque action de la matrice. */
export function pouvoirsEffectifs(compte: Compte, comptes: Compte[], unites: Unite[], matrice: PouvoirsProfil[]): { action: ActionPouvoir; portee: Portee; personnes: Compte[] }[] {
  const actions = actionsDe(compte.profils, matrice)
  return ACTIONS_POUVOIR.map((action) => ({ action, portee: actions[action], personnes: couverts(compte.id, actions[action], comptes, unites) }))
}

// ------------------------------------------------------------------ D-102 (docs/lot1/organigramme.md)

/** Texte comparable : minuscules, sans accents ni espaces superflus (recherche dans l'organigramme et l'annuaire). */
export function normaliser(s: string | null | undefined): string {
  return (s ?? '').normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase().replace(/\s+/g, ' ').trim()
}

/** Unités parentes d'une unité, de la plus proche à la plus haute (sans cycle). */
export function ancetres(unites: Unite[], code: string): string[] {
  const l: string[] = []
  for (let u = unites.find((x) => x.code === code); u?.parent && !l.includes(u.parent); u = unites.find((x) => x.code === u!.parent)) l.push(u.parent)
  return l
}

/**
 * n+1 proposé à une personne affectée à une unité (ReglesOrganisation.responsableParDefaut de MS-IAM) : chef de l'unité ;
 * si l'unité n'a pas de chef ou si la personne est ce chef, chef de l'unité parente la plus proche ; null au sommet.
 */
export function responsableParDefaut(unites: Unite[], compte: string | null, code: string): { id: string; unite: Unite } | null {
  const vus = new Set<string>()
  for (let u = unites.find((x) => x.code === code); u && !vus.has(u.code); u = unites.find((x) => x.code === u!.parent)) {
    vus.add(u.code)
    if (u.responsableId && u.responsableId !== compte) return { id: u.responsableId, unite: u }
  }
  return null
}

export type ResultatRecherche = { type: 'unite'; unite: Unite } | { type: 'personne'; compte: Compte }

/** Unités (libellé, code) puis personnes (nom, identifiant, fonction) qui contiennent le texte cherché ; 8 au plus. */
export function rechercher(unites: Unite[], comptes: Compte[], q: string, max = 8): ResultatRecherche[] {
  const n = normaliser(q)
  if (n.length < 2) return []
  const u: ResultatRecherche[] = unites.filter((x) => normaliser(`${x.libelle} ${x.code}`).includes(n)).map((unite) => ({ type: 'unite', unite }))
  const p: ResultatRecherche[] = comptes.filter((c) => !c.institutionId && normaliser(`${c.nomAffiche} ${c.identifiant} ${c.fonction ?? ''}`).includes(n)).map((compte) => ({ type: 'personne', compte }))
  return [...u, ...p].slice(0, max)
}

/** Nom du fichier PDF proposé par MS-IAM : organigramme-moabi-AAAA-MM-JJ.pdf, date de Douala. */
export function nomFichierOrganigramme(d = new Date()): string {
  const jour = new Intl.DateTimeFormat('en-CA', { timeZone: 'Africa/Douala', year: 'numeric', month: '2-digit', day: '2-digit' }).format(d)
  return `organigramme-moabi-${jour}.pdf`
}

/** Zoom de l'organigramme : bornes et pas. */
export const ZOOM = { min: 0.3, max: 1.6, pas: 0.1 } as const
export function borneZoom(z: number): number {
  return Math.round(Math.min(ZOOM.max, Math.max(ZOOM.min, z)) * 100) / 100
}
