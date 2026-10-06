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
