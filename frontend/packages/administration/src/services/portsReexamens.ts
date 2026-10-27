import type { CampagneReexamen, SaisieDecisionReexamen, VueReexamen } from '../modele/reexamens'

/** Campagnes de réexamen des accès de MS-IAM (IAM-18, US-EXI-23). */
export interface ServiceReexamens {
  lister(): Promise<CampagneReexamen[]>
  ouvrir(): Promise<CampagneReexamen>
  lire(id: string): Promise<VueReexamen>
  decider(id: string, compteId: string, d: SaisieDecisionReexamen): Promise<VueReexamen>
  cloturer(id: string): Promise<CampagneReexamen>
}
