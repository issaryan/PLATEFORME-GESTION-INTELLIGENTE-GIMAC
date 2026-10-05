// Mon activité (D-76, docs/lot1/mon-activite.md) : agenda, activités du jour, tâches, rapports quotidiens et
// hebdomadaires, équipe ; représentations servies par MS-PILOTAGE (/bff/des/pilotage) et, pour l'organisation,
// par MS-IAM (/bff/des/iam/organisation). L'interface n'en calcule aucune règle : droits, transitions,
// destinataire d'un rapport, retards et équipe viennent des services. Jours AAAA-MM-JJ, semaines ISO
// AAAA-Www, fuseau Africa/Douala.

/** Personne citée par une représentation (auteur, responsable, participant…). */
export type PersonneActivite = { id: string; nomAffiche: string }

// ------------------------------------------------------------------ Organisation (MS-IAM, §1)

export const TYPES_UNITE = ['DIRECTION', 'DEPARTEMENT', 'SERVICE', 'POLE', 'CELLULE'] as const
export type TypeUnite = (typeof TYPES_UNITE)[number]

/** Unité organisationnelle du catalogue de MS-IAM. */
export type Unite = { code: string; libelle: string; type: TypeUnite; parent: string | null; active: boolean }

/** Rattachement d'un compte (GET organisation/moi) : unité, chemin, responsable et chaîne hiérarchique. */
export type Rattachement = {
  id: string
  nomAffiche: string
  unite: Unite | null
  /** Unités de la plus haute à la plus fine. */
  chemin: Unite[]
  responsable: { id: string; nomAffiche: string; fonction: string | null } | null
  /** Responsables successifs (n+1, n+2…), dix au plus. */
  chaine: string[]
  /** Nombre de comptes dont il est le n+1. */
  equipeDirecte: number
}

// ------------------------------------------------------------------ Agenda (§2.1)

export const CATEGORIES_EVENEMENT = ['REUNION', 'ACTIVITE', 'DEPLACEMENT', 'ABSENCE', 'FORMATION', 'AUTRE'] as const
export type CategorieEvenement = (typeof CATEGORIES_EVENEMENT)[number]
export type ReponseInvitation = 'EN_ATTENTE' | 'ACCEPTE' | 'DECLINE'
/** Place de l'utilisateur : propriétaire, participant invité, ou lecture de l'agenda d'un membre de son équipe. */
export type RoleEvenement = 'PROPRIETAIRE' | 'PARTICIPANT' | 'EQUIPE'

export type Evenement = {
  id: string
  titre: string
  description: string | null
  lieu: string | null
  debut: string
  fin: string
  journeeEntiere: boolean
  categorie: CategorieEvenement
  proprietaire: PersonneActivite
  participants: (PersonneActivite & { reponse: ReponseInvitation })[]
  moi: RoleEvenement
}

/** POST /agenda et PUT /agenda/{id} : participants désignés par leur identifiant. */
export type EntreeEvenement = {
  titre: string
  description: string | null
  lieu: string | null
  debut: string
  fin: string
  journeeEntiere: boolean
  categorie: CategorieEvenement
  participants: string[]
}

// ------------------------------------------------------------------ Activités du jour (§2.2)

export const STATUTS_ACTIVITE = ['A_FAIRE', 'EN_COURS', 'FAITE', 'REPORTEE', 'ANNULEE'] as const
export type StatutActivite = (typeof STATUTS_ACTIVITE)[number]

export type Activite = {
  id: string
  jour: string
  titre: string
  description: string | null
  statut: StatutActivite
  /** Temps passé, facultatif, 0 à 1440 minutes. */
  dureeMinutes: number | null
  ordre: number
  tacheId: string | null
  reporteeAu: string | null
  auteur: PersonneActivite
  modifieeLe: string
}

/** POST /activites et PUT /activites/{id}. */
export type EntreeActivite = { jour: string; titre: string; description: string | null; dureeMinutes: number | null; ordre?: number; tacheId?: string | null }

// ------------------------------------------------------------------ Tâches (§2.3)

export const ETATS_TACHE = ['OUVERTE', 'EN_COURS', 'BLOQUEE', 'TERMINEE', 'ANNULEE'] as const
export type EtatTache = (typeof ETATS_TACHE)[number]
export const PRIORITES = ['BASSE', 'NORMALE', 'HAUTE', 'URGENTE'] as const
export type Priorite = (typeof PRIORITES)[number]
export type TypeOrigine = 'INTERNE' | 'OPERATION' | 'REFERENTIEL' | 'DECISION' | 'TICKET' | 'FICHE_ACTION' | 'RISQUE' | 'INDICATEUR'
export type ActionHistorique = 'CREEE' | 'MODIFIEE' | 'REAFFECTEE' | 'TRANSITION'
export type VueTaches = 'moi' | 'creees' | 'equipe'

export type Tache = {
  id: string
  titre: string
  description: string | null
  echeance: string | null
  priorite: Priorite
  etat: EtatTache
  responsable: PersonneActivite
  createur: PersonneActivite
  origine: { type: TypeOrigine; reference: string | null; service: string | null; lien: string | null }
  enRetard: boolean
  creeeLe: string
  termineeLe: string | null
  historique?: { quand: string; acteur: PersonneActivite; action: ActionHistorique; detail: string | null }[]
}

/** POST /taches et PUT /taches/{id} ; responsableId absent : pour soi (création) ou inchangé (modification). */
export type EntreeTache = { titre: string; description: string | null; echeance: string | null; priorite: Priorite; responsableId?: string }

/**
 * Transitions offertes par l'interface (CC p.238, p.245) ; le service les refait (409 PIL_TRANSITION_INTERDITE).
 * BLOQUEE et ANNULEE exigent un motif, comme la réouverture d'une tâche terminée.
 */
export const TRANSITIONS_TACHE: Record<EtatTache, EtatTache[]> = {
  OUVERTE: ['EN_COURS', 'BLOQUEE', 'ANNULEE'],
  EN_COURS: ['BLOQUEE', 'TERMINEE', 'ANNULEE'],
  BLOQUEE: ['EN_COURS', 'ANNULEE'],
  TERMINEE: ['EN_COURS'],
  ANNULEE: [],
}
export const motifExige = (de: EtatTache, vers: EtatTache) => vers === 'BLOQUEE' || vers === 'ANNULEE' || (de === 'TERMINEE' && vers === 'EN_COURS')

// ------------------------------------------------------------------ Rapports (§2.4)

export type TypeRapport = 'QUOTIDIEN' | 'HEBDOMADAIRE'
export type ModeRapport = 'PREREMPLI' | 'REDIGE' | 'DOCUMENT'
export type EtatRapport = 'BROUILLON' | 'SOUMIS' | 'VISE'
export type VueRapports = 'moi' | 'recus'

export type DocumentRapport = { ticket: string; nom: string; typeMime: string; taille: number }

export type Rapport = {
  id: string
  type: TypeRapport
  /** Jour (AAAA-MM-JJ) ou semaine ISO (AAAA-Www). */
  periode: string
  mode: ModeRapport
  contenu: string | null
  document: DocumentRapport | null
  etat: EtatRapport
  auteur: PersonneActivite
  destinataire: PersonneActivite | null
  soumisLe: string | null
  viseLe: string | null
  visa: { par: PersonneActivite; commentaire: string | null } | null
  indicateurs: { activitesFaites: number; activitesPrevues: number; minutes: number; tachesTerminees: number }
}

/**
 * POST /rapports : brouillon ; PUT /rapports/{id} : contenu et document du brouillon. Le document n'est désigné
 * que par son ticket (dépôt fragmenté de référence = identifiant du rapport) ; le service en relit le nom, le
 * type et la taille auprès de MS-DOCUMENT.
 */
export type EntreeRapport = { type: TypeRapport; periode: string; mode: ModeRapport; contenu: string | null; document?: { ticket: string } | null }

/** Bornes du contrat, annoncées avant l'envoi. */
export const LIMITES_RAPPORT = {
  contenuMax: 20_000,
  types: '.pdf,.doc,.docx,.odt,.txt,.png,.jpg,.jpeg',
  tailleMaxOctets: 50 * 1024 * 1024,
} as const

// ------------------------------------------------------------------ Équipe et synthèse (§2.5, §2.6)

export type EtatRapportDuJour = 'ABSENT' | EtatRapport

export type MembreEquipe = {
  collaborateur: PersonneActivite & { fonction: string | null; unite: string | null; niveau: number }
  activites: { prevues: number; faites: number; enCours: number }
  rapportDuJour: EtatRapportDuJour
  tachesOuvertes: number
  tachesEnRetard: number
  absent: boolean
}

export type Synthese = {
  activites: { prevues: number; faites: number; enCours: number }
  evenements: Evenement[]
  taches: { ouvertes: number; enRetard: number; echeanceProche: number }
  rapportDuJour: EtatRapportDuJour
  rapportsARecevoir: number
}

export type PagePilotage<T> = { contenu: T[]; total: number }
