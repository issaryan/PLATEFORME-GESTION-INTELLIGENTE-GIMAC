// Contrat de SBX-ESPACE, routes de la console (web/rest/espace/Vues.java, D-118, D-122) : dates ISO 8601 en temps
// universel, aucune donnée personnelle. Servies sous /bff/des/sbx-espace/** (réécrit en /api/v1/console/**).

export type EtatEspace = 'EN_OUVERTURE' | 'OUVERT' | 'SUSPENDU' | 'FERME'
export type OrigineSuspension = 'DECISION_GIMAC' | 'STATUT_REGISTRE'
export type EtatSaga = 'EN_COURS' | 'EN_REPRISE' | 'EN_COMPENSATION' | 'TERMINEE' | 'COMPENSEE'

export const ETATS_ESPACE: readonly EtatEspace[] = ['EN_OUVERTURE', 'OUVERT', 'SUSPENDU', 'FERME']

/** EspaceVM. */
export type Espace = {
  codeEspace: string
  codeParticipant: string
  etat: EtatEspace
  motifsSuspension: OrigineSuspension[]
  statutRegistre: string | null
  versionStatutRegistre: number
  administrateursActifs: number
  dateOuverture: string | null
  dateSuspension: string | null
  dateFermeture: string | null
}

/** PageVM : `suivant` est le curseur de la page suivante, absent (null) en fin de liste. */
export type PageEspaces = { elements: Espace[]; suivant: string | null }

/** DemandeOuvertureVM : code participant à cinq chiffres, correspondant actif de l'institution (compte de MS-IAM). */
export type DemandeOuverture = { codeParticipant: string; correspondantId: string }

/** OuvertureVM : 201 si l'espace est ouvert (saga TERMINEE), 202 si la saga attend sa reprise ou compense. */
export type Ouverture = { idSaga: string; etatSaga: EtatSaga; codeParticipant: string; etatEspace: EtatEspace | null; motifEchec: string | null }

/** Actes de la console sur un espace (UC-INS-09). */
export type ActeEspace = 'suspension' | 'retablissement' | 'fermeture'

/** Permissions de l'écran (la mention SBX_ESP_* de D-118 est une coquille, corrigée par D-122). */
export const PERMISSION = { consultation: 'SBX_HAB_CONSULTATION', administration: 'SBX_HAB_ADMINISTRATION' } as const

// Contrat de supervision de SBX-TRACE (D-121), servi sous /bff/des/sbx-trace et réécrit en /api/v1/console.
export const PERMISSION_SUPERVISION = 'SBX_SUP_CONSULTATION'
export type NiveauSante = 'NOMINAL' | 'DEGRADE' | 'INDISPONIBLE' | 'EN_MAINTENANCE'
export type Criticite = 'MINEURE' | 'MAJEURE' | 'CRITIQUE'
export type PageSupervision<T> = { elements: T[]; total: number; page: number; taille: number; suivant: string | null }
export type EtatSante = { id: string; niveauGlobal: NiveauSante; etatsModules: string; causeExterne: boolean; calculeLe: string }
export type ElementSupervise = {
  id: string
  nature: string
  module: string
  codeParticipant: string | null
  seuilDelai: string | null
  seuilEchecs: number | null
  criticite: Criticite
  externe: boolean
  enMaintenance: boolean
}
export type Alerte = {
  id: string
  criticite: Criticite
  message: string
  etat: 'LEVEE' | 'CLOSE'
  leveeLe: string
  closeLe: string | null
  element: { id: string; nature: string }
}
export type Disponibilite = { id: string; code: string; module: string | null; periode: string; valeur: number; publieLe: string }

/** Mesure par service (VueService de SBX-TRACE, lecture /services) : valeurs absentes (null) non mesurées. */
export type PointMesure = { le: string; latenceMoyenneMs: number | null; debitParMinute: number | null; erreurs: number | null }
export type MesureService = {
  service: string
  module: string
  niveau: NiveauSante | null
  joignable: boolean
  mesureLe: string | null
  delaiMs: number
  latenceMoyenneMs: number | null
  latenceMaxMs: number | null
  debitParMinute: number | null
  erreurs: number | null
  retardKafka: number | null
  rebuts: number | null
  /** Centièmes de pour cent, sur les dernières 24 heures. */
  disponibilite24h: number | null
  serie: PointMesure[]
}
