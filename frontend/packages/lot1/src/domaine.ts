import domaine from './domaine-gimac.json'

// Catalogue du domaine Keycloak « gimac » (permissions, profils livrés, libellé, mission, caractère externe), écrit par
// infra/keycloak/generer-realm-gimac.py dans domaine-gimac.json, lu comme MS-IAM le lit dans Keycloak : source unique des
// services simulés et des profils de démonstration (recette ADM-01, ADM-09). Aucun profil n'y est recopié à la main ;
// domaine.test.ts échoue dès que ce fichier diverge de infra/keycloak/gimac-realm.json.

export type ProfilDomaine = { code: string; libelle: string; description: string; externe: boolean; permissions: string[] }

/** Permissions du domaine (GET /configuration/permissions de MS-IAM : rôles de permission, internes compris). */
export const PERMISSIONS_DOMAINE: readonly string[] = domaine.permissions
/** Profils livrés avec le domaine (rôles composites ROLE_*), dans l'ordre du domaine. */
export const PROFILS_DOMAINE: readonly ProfilDomaine[] = domaine.profils
/** Permissions d'un profil livré ; vide pour un code inconnu. */
export const permissionsDuProfil = (code: string): string[] => [...(PROFILS_DOMAINE.find((p) => p.code === code)?.permissions ?? [])]
/** Règle de MS-IAM (ReglesProfils.privilegie) : une permission IAM_* ou PLATFORM_* rend un profil privilégié. */
export const compositionPrivilegiee = (permissions: readonly string[]) => permissions.some((p) => p.startsWith('IAM_') || p.startsWith('PLATFORM_'))
/** Permission qui donne l'octroi des profils privilégiés (ReglesAdministration.OCTROI_PRIVILEGIE). */
export const OCTROI_PRIVILEGIE = 'IAM_ROLE_PRIVILEGED_GRANT'
/** Permissions ajoutées par MS-IAM à tout profil créé (ReglesProfils.PERMISSIONS_COMMUNES) : messagerie, Mon activité, notifications. */
export const PERMISSIONS_COMMUNES: readonly string[] = ['COL_MESSAGE_READ', 'COL_MESSAGE_SEND', 'PIL_ACTIVITY_OWN', 'NOT_INBOX_READ']
