import type { PersonneActivite } from './pilotage'

// Pouvoirs des chefs sur leurs subordonnés (D-81, docs/lot1/pouvoirs-hierarchiques.md) : matrice des pouvoirs
// (profil × action → portée), pouvoirs de l'utilisateur (GET pouvoirs/moi) et demandes adressées à
// l'administration. Représentations servies par MS-IAM (/bff/{des,console}/iam). L'interface ne s'en sert que
// pour n'offrir que les gestes permis ; MS-IAM et MS-PILOTAGE refont le contrôle à chaque appel.

/**
 * Actions de la matrice (§3), dans l'ordre d'affichage ; PROCESSUS_DEFINIR et PROCESSUS_LANCER (D-86 §4) : définir et
 * publier, ou lancer, les processus des unités couvertes (D-87). Une action absente d'une représentation vaut AUCUNE.
 */
export const ACTIONS_POUVOIR = [
  'TACHE_CONFIER', 'TACHE_SUIVRE', 'ACTIVITE_LIRE', 'RAPPORT_VISER', 'RATTACHEMENT_MODIFIER', 'COMPTE_DEMANDER', 'PROFIL_DEMANDER', 'STATUT_DEMANDER',
  'PROCESSUS_DEFINIR', 'PROCESSUS_LANCER',
] as const
export type ActionPouvoir = (typeof ACTIONS_POUVOIR)[number]

/** Portées, de la plus étroite à la plus large (§1) : UNITES = équipe et membres des unités dirigées. */
export const PORTEES = ['AUCUNE', 'DIRECTS', 'EQUIPE', 'UNITES'] as const
export type Portee = (typeof PORTEES)[number]

export type ActionsPouvoir = Record<ActionPouvoir, Portee>

/**
 * Ligne de la matrice : portée de chaque action et profils qu'un titulaire peut proposer pour un subordonné ;
 * libellé du profil (D-86 §5, attribut gimac_libelle), absent des représentations antérieures.
 */
export type PouvoirsProfil = { profil: string; libelle?: string; actions: ActionsPouvoir; profilsDemandables: string[] }

/** GET /pouvoirs/matrice. */
export type MatricePouvoirs = { profils: PouvoirsProfil[] }

/** PUT /pouvoirs/matrice/{profil}. */
export type EntreePouvoirs = { actions: ActionsPouvoir; profilsDemandables: string[] }

/**
 * Compte du périmètre (§2) : niveau 1 pour un subordonné direct, 2 et plus dans la chaîne, 0 pour un membre
 * d'une unité dirigée hors de la chaîne des responsables.
 */
export type Subordonne = {
  id: string
  nomAffiche: string
  fonction: string | null
  unite: string | null
  responsableId: string | null
  niveau: number
  actif: boolean
}

/**
 * GET /pouvoirs/moi : portée la plus large de chaque action sur l'ensemble des profils détenus ; libellés des profils
 * demandables (D-86 §5 : { "ROLE_…": "libellé" }), absents des représentations antérieures.
 */
export type MesPouvoirs = { actions: ActionsPouvoir; profilsDemandables: string[]; subordonnes: Subordonne[]; unitesDirigees: string[]; libellesProfils?: Record<string, string> }

/** Matrice vide (portée AUCUNE partout) : un profil sans pouvoir, ou une lecture en attente. */
export const AUCUN_POUVOIR: ActionsPouvoir = Object.fromEntries(ACTIONS_POUVOIR.map((a) => [a, 'AUCUNE'])) as ActionsPouvoir

/** Actions complétées : une action absente (représentation antérieure à D-86) vaut AUCUNE. */
export const completerActions = (a: Partial<ActionsPouvoir> | undefined): ActionsPouvoir => ({ ...AUCUN_POUVOIR, ...(a ?? {}) })

/** Rang d'une portée (AUCUNE = 0) ; une portée inconnue vaut AUCUNE. */
export const rangPortee = (p: Portee | undefined) => Math.max(PORTEES.indexOf(p ?? 'AUCUNE'), 0)

/** Vrai si la portée de l'action couvre ce compte du périmètre (le service décide à chaque appel). */
export function couvre(p: Pick<MesPouvoirs, 'actions'> | undefined, action: ActionPouvoir, s: Pick<Subordonne, 'niveau'>): boolean {
  const r = rangPortee(p?.actions[action])
  if (r === 0) return false
  if (r === rangPortee('DIRECTS')) return s.niveau === 1
  if (r === rangPortee('EQUIPE')) return s.niveau >= 1
  return true
}

/** Vrai si la portée de l'action couvre les unités dirigées (UNITES). */
export const couvreUnites = (p: Pick<MesPouvoirs, 'actions'> | undefined, action: ActionPouvoir) => rangPortee(p?.actions[action]) === rangPortee('UNITES')

/** Comptes couverts par une action, dans l'ordre du périmètre. */
export const couverts = (p: MesPouvoirs | undefined, action: ActionPouvoir) => (p?.subordonnes ?? []).filter((s) => couvre(p, action, s))

// ------------------------------------------------------------------ Demandes à l'administration (§4)

export const TYPES_DEMANDE = ['CREATION_COMPTE', 'PROFILS', 'STATUT'] as const
export type TypeDemandeIam = (typeof TYPES_DEMANDE)[number]
export const ETATS_DEMANDE = ['EN_ATTENTE', 'ACCEPTEE', 'REFUSEE', 'ANNULEE'] as const
export type EtatDemande = (typeof ETATS_DEMANDE)[number]

export type ContenuCreation = {
  identifiant: string
  prenom: string
  nom: string
  email: string
  telephone?: string | null
  fonction: string
  unite: string
  responsableId: string
  profils: string[]
}
export type ContenuProfils = { ajouter: string[]; retirer: string[] }
export type ContenuStatut = { actif: boolean }
export type ContenuDemande = ContenuCreation | ContenuProfils | ContenuStatut

export type Demande = {
  id: string
  type: TypeDemandeIam
  demandeur: PersonneActivite
  /** Null pour une création de compte. */
  cible: PersonneActivite | null
  contenu: ContenuDemande
  motif: string
  etat: EtatDemande
  decision: { par: PersonneActivite; motif: string | null; le: string } | null
  creeeLe: string
}

/** POST /demandes. */
export type NouvelleDemande =
  | { type: 'CREATION_COMPTE'; cibleId: null; contenu: ContenuCreation; motif: string }
  | { type: 'PROFILS'; cibleId: string; contenu: ContenuProfils; motif: string }
  | { type: 'STATUT'; cibleId: string; contenu: ContenuStatut; motif: string }

export type VueDemandes = 'miennes' | 'a-traiter'
export type FiltreDemandes = { vue: VueDemandes; etat?: EtatDemande; page?: number; taille?: number }

/** Liste paginée de MS-IAM (pages à partir de 0). */
export type PageDemandes = { contenu: Demande[]; total: number; page: number; taille: number }

/** Bornes du motif d'une demande (§4). */
export const MOTIF_DEMANDE = { min: 5, max: 500 } as const

export const estCreation = (d: Pick<Demande, 'type' | 'contenu'>): d is Demande & { contenu: ContenuCreation } => d.type === 'CREATION_COMPTE'
export const estProfils = (d: Pick<Demande, 'type' | 'contenu'>): d is Demande & { contenu: ContenuProfils } => d.type === 'PROFILS'
