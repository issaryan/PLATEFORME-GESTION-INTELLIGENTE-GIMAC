import type { ActionRequise, Compte } from '@gimac/compte'

// Administration des comptes (D-62) : formes de ComptesResource de MS-IAM, relayé par
// /bff/console/iam/comptes. Aucune inscription libre : tout compte est créé depuis la console.

export type Page<T> = { contenu: T[]; total: number }

/** Valeurs du filtre d'état acceptées par MS-IAM (AdministrationComptesService.etat). */
export const ETATS_FILTRE = ['ACTIF', 'INACTIF', 'PREMIER_ACCES', 'SANS_2FA'] as const
export type EtatFiltre = (typeof ETATS_FILTRE)[number]

/** Pagination de l'interface à partir de 1 ; celle du service à partir de 0. */
export type FiltreComptes = { page: number; taille: number; q?: string; profil?: string; etat?: EtatFiltre }

export type NouveauCompte = {
  identifiant: string
  prenom: string
  nom: string
  email: string
  telephone?: string | null
  fonction?: string | null
  institutionId?: string | null
  profils: string[]
  envoyerInvitation: boolean
  motDePasseProvisoire: boolean
}

/** Le mot de passe provisoire n'est rendu qu'une fois, s'il a été demandé. */
export type Creation = { compte: Compte; invitationEnvoyee: boolean; motDePasseProvisoire: string | null }

export type FicheCompte = {
  prenom?: string | null
  nom?: string | null
  email?: string | null
  telephone?: string | null
  fonction?: string | null
  langue?: string | null
  institutionId?: string | null
}

export type IndicateursComptes = {
  total: number
  actifs: number
  inactifs: number
  premierAccesEnAttente: number
  sansSecondFacteur: number
  privilegies: number
  creesDepuis30Jours: number
  actesDepuis24Heures: number
  parProfil: Record<string, number>
  /** 30 derniers jours (aujourd'hui compris, fuseau de Douala), zéro les jours sans événement. */
  creationsParJour?: ParJour[]
  actesParJour?: ParJour[]
}

/** Compte d'un jour (date ISO AAAA-MM-JJ). */
export type ParJour = { jour: string; nombre: number }

export type EntreeJournal = {
  id: string
  horodatage: string
  acteurId: string
  acteurNom: string | null
  action: string
  cibleId: string | null
  cibleLibelle: string | null
  motif: string | null
  details: string | null
  correlationId: string | null
}

export type FiltreJournal = { page: number; taille: number; compte?: string }

export type { ActionRequise, Compte }

/**
 * PUT …/comptes/{id}/rattachement (D-76, docs/lot1/mon-activite.md §1.2) : null retire l'unité ou le responsable. Motif
 * facultatif, porté au journal (D-102).
 */
export type DemandeRattachement = { unite: string | null; responsableId: string | null; motif?: string }

/**
 * POST …/organisation/unites/{code}/affectations (D-102, docs/lot1/organigramme.md §2) : 1 à 50 comptes internes actifs,
 * motif de 3 à 500 caractères ; responsableId absent : n+1 résolu (chef de l'unité ou de la plus proche unité parente),
 * null : aucun n+1.
 */
export type DemandeAffectation = { comptes: string[]; responsableId?: string | null; motif: string }
export type EtatRattachement = { unite: string | null; responsableId: string | null }
/** Effet d'une affectation ou d'un retrait sur une personne. */
export type ResultatAffectation = { id: string; nomAffiche: string; avant: EtatRattachement; apres: EtatRattachement }
/** GET …/organisation/organigramme.pdf (D-102 §4) : branche (absente : toute l'organisation), papier, annexe des membres. */
export type OptionsOrganigrammePdf = { racine?: string | null; format: 'A4' | 'A3'; orientation: 'paysage' | 'portrait'; membres: boolean }
