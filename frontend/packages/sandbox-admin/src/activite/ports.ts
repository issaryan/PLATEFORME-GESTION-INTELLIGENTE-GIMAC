import type { Activite, CodeRetour, FiltreActivite, Granularite, LiaisonCertification, LigneCampagne, LigneCas, LigneParticipant, PageStatistiques, Synthese, TriCampagnes } from './modele'

// Port des statistiques des tests (D-121) : seul contrat entre l'écran « Activité des tests » et ses données.
// Implémentations HTTP (passerelle) et simulée au contrat exact (VITE_SERVICES=simule). Lecture seule.

export type Pagination = { page: number; taille: number }

export interface ServiceActivite {
  synthese(f: FiltreActivite): Promise<Synthese>
  activite(f: FiltreActivite, granularite: Granularite): Promise<Activite>
  participants(f: FiltreActivite, p: Pagination): Promise<PageStatistiques<LigneParticipant>>
  campagnes(f: FiltreActivite, tri: TriCampagnes, p: Pagination): Promise<PageStatistiques<LigneCampagne>>
  cas(f: FiltreActivite, p: Pagination): Promise<PageStatistiques<LigneCas>>
  /** Codes de retour de SBX-TRACE (exchange-captured.v1) : null si la route n'est pas servie (source indisponible). */
  codesRetour(f: FiltreActivite, p: Pagination): Promise<PageStatistiques<CodeRetour> | null>
  /** Liaison de MS-HOMOLOGATION (HOM_LIAISON_READ) pour la chaîne de certification. */
  liaison(id: string): Promise<LiaisonCertification>
}
