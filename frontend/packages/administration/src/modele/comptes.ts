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
}

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
