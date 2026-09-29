import type { CoverageValue, Permitted } from './commun'
import type { CampaignResult, Graphe, ServiceCouvert } from './homologation'

// Projections du portail (HOM-24, CER-13) : aucun champ interne, l'institution vient du jeton côté service.
export type PortailLiaison = Permitted & {
  id: string; typeProjet: string; etat: string; etatsParcourus: string[]; phaseCourante: string; versionNumero: number
  couvertureCertification: CoverageValue; couvertureMiseEnService: CoverageValue; services: ServiceCouvert[]; graphe: Graphe; resultats: CampaignResult[]
}
export type PortailCertification = {
  id: string; liaisonId: string; etatDossier: string; motifCommunicable: string | null
  attestation: { numero: string; etat: string; empreinteDocument: string } | null
}
export type PortailOperation = Permitted & {
  id: string; liaisonId: string; code: string; libelle: string; etat: string; echeance: string | null; joursRestants: number | null
  preuveAttendue: string; accuse: { empreinte: string; dateDepot: string } | null
}
