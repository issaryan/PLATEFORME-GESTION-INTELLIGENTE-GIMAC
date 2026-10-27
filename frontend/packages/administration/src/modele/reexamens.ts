export type EtatReexamen = 'OUVERTE' | 'CLOSE'
export type DecisionReexamen = 'EN_ATTENTE' | 'MAINTENU' | 'REDUIT' | 'REVOQUE'

export type CampagneReexamen = {
  id: string
  period: string
  state: EtatReexamen
  openedAt: string
  approverId: string | null
  closedAt: string | null
  digest: string | null
  enAttente: number
}

export type AccesReexamen = {
  compteId: string
  identifiant: string
  nomAffiche: string
  institutionId: string | null
  profils: string[]
  permissions: string[]
  decision: DecisionReexamen
  comment: string | null
  decidedBy: string | null
  decidedAt: string | null
}

export type VueReexamen = { campagne: CampagneReexamen; acces: AccesReexamen[] }
export type SaisieDecisionReexamen = { decision: Exclude<DecisionReexamen, 'EN_ATTENTE'>; profils?: string[]; comment?: string }
