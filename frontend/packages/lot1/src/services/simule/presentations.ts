import { PROFILS_DOMAINE } from '../../domaine'

// Présentation des profils livrés (D-86, docs/lot1/profils-organisation.md §2) : libellé (attribut gimac_libelle du
// rôle) et mission (description), lues dans le domaine généré par infra/keycloak/generer-realm-gimac.py
// (domaine-gimac.json) pour les services simulés. Les services réels les servent par GET /configuration/profils,
// la matrice et GET /pouvoirs/moi.

export type PresentationProfil = { libelle: string; description: string }

export const PRESENTATIONS_PROFILS: Record<string, PresentationProfil> = Object.fromEntries(
  PROFILS_DOMAINE.map((p) => [p.code, { libelle: p.libelle, description: p.description }]),
)
