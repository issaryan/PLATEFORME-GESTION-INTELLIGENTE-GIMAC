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
