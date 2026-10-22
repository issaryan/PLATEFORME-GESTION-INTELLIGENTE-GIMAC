import type { ActeEspace, DemandeOuverture, Espace, Ouverture, PageEspaces } from '../modele'

// Ports de l'espace sandbox du DES (D-122) : seul contrat entre les écrans et les données. Implémentations HTTP
// (passerelle) et simulée au contrat exact (VITE_SERVICES=simule). Toute écriture porte une clé d'idempotence
// (en-tête Idempotency-Key, 8 à 100 caractères visibles) : un acte rejoué avec la même clé rend son résultat d'origine.

export interface ServiceEspaces {
  lister(q: { apres?: string; taille?: number }): Promise<PageEspaces>
  consulter(codeParticipant: string): Promise<Espace>
  ouvrir(demande: DemandeOuverture, cle: string): Promise<Ouverture>
  agir(codeParticipant: string, acte: ActeEspace, cle: string): Promise<Espace>
}

export type ServicesSandbox = { espaces: ServiceEspaces }
