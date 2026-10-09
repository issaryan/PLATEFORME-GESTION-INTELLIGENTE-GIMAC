import { PROFILS_INTERNES_DEMO, compositionPrivilegiee, permissionsDuProfil } from '@gimac/lot1'

// Profils de démonstration de l'administration (services simulés seulement, VITE_PROFIL_DEMO) : identité de
// démonstration de chaque compte de la console et composition du profil lue dans le domaine Keycloak généré
// (domaine-gimac.json, infra/keycloak/generer-realm-gimac.py), jamais recopiée (recette ADM-09).
// Comptes communs aux trois applications (recette ACC-05) : la console reprend l'identité de l'application interne pour
// chaque profil privilégié (composition IAM_* ou PLATFORM_*, règle de MS-IAM), sans aucun nom de profil.
export const PROFILS_DEMO: Record<string, { identifiant: string; nom: string; sujet: string; permissions: string[] }> = Object.fromEntries(
  Object.entries(PROFILS_INTERNES_DEMO)
    .filter(([role]) => compositionPrivilegiee(permissionsDuProfil(role)))
    .map(([role, p]) => [role, { identifiant: p.identifiant, nom: p.nom, sujet: p.sujet, permissions: permissionsDuProfil(role) }]),
)

/** Profil de démonstration (services simulés seulement) : clé gm.profilDemo du stockage local, sinon VITE_PROFIL_DEMO. */
export const CLE_PROFIL_DEMO = 'gm.profilDemo'
/** Démonstration du premier accès (services simulés seulement) : gm.premierAccesDemo = 1. */
export const CLE_PREMIER_ACCES_DEMO = 'gm.premierAccesDemo'
/** Orchestrateur de l'infrastructure simulée (services simulés seulement) : gm.orchestrateurDemo = KUBERNETES, DOCKER ou AUCUN. */
export const CLE_ORCHESTRATEUR_DEMO = 'gm.orchestrateurDemo'
