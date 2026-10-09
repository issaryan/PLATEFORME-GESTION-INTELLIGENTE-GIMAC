import { PERMISSIONS_DOMAINE, PROFILS_DOMAINE, type ProfilDomaine } from '@gimac/lot1'

// Catalogue des profils et des permissions du domaine « gimac » pour le serveur simulé de la console : lu dans
// domaine-gimac.json (@gimac/lot1) qu'écrit infra/keycloak/generer-realm-gimac.py, comme MS-IAM le lit dans
// Keycloak (recette ADM-01). Plus aucune recopie : catalogue.test.ts échoue dès qu'il diverge du domaine.

/** Permissions servies par GET /configuration/permissions (rôles de permission du domaine, internes compris). */
export const PERMISSIONS_REALM: readonly string[] = PERMISSIONS_DOMAINE

/** Profils livrés : code, libellé, mission, caractère externe (attribut gimac_externe) et composition. */
export const PROFILS_REALM: readonly ProfilDomaine[] = PROFILS_DOMAINE
