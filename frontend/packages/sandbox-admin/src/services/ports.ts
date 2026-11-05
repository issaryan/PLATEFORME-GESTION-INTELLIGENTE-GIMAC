import type { ServiceActivite } from '../activite/ports'
import type { ActeEspace, Alerte, DemandeOuverture, Disponibilite, ElementSupervise, Espace, EtatSante, Ouverture, PageEspaces, PageSupervision } from '../modele'

// Ports de l'espace sandbox du DES (D-122) : seul contrat entre les écrans et les données. Implémentations HTTP
// (passerelle) et simulée au contrat exact (VITE_SERVICES=simule). Toute écriture porte une clé d'idempotence
// (en-tête Idempotency-Key, 8 à 100 caractères visibles) : un acte rejoué avec la même clé rend son résultat d'origine.

export interface ServiceEspaces {
  lister(q: { apres?: string; taille?: number }): Promise<PageEspaces>
  consulter(codeParticipant: string): Promise<Espace>
  ouvrir(demande: DemandeOuverture, cle: string): Promise<Ouverture>
  agir(codeParticipant: string, acte: ActeEspace, cle: string): Promise<Espace>
}

export interface ServiceSupervision {
  etatSante(): Promise<EtatSante>
  elements(q: { page: number; taille: number }): Promise<PageSupervision<ElementSupervise>>
  alertes(q: { page: number; taille: number }): Promise<PageSupervision<Alerte>>
  disponibilites(q: { page: number; taille: number }): Promise<PageSupervision<Disponibilite>>
}

export type ServicesSandbox = { espaces: ServiceEspaces; supervision: ServiceSupervision; activite: ServiceActivite }
