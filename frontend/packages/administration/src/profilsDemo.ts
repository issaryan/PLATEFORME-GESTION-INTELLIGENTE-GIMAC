// Profils de démonstration de l'administration (services simulés seulement), d'après le domaine « gimac ».
/** Profils de démonstration (services simulés seulement, VITE_PROFIL_DEMO), d'après le domaine « gimac ». */
const LECTURE_DES = ['HOM_REFERENTIEL_READ', 'HOM_LIAISON_READ', 'HOM_LIAISON_LIST', 'HOM_DASHBOARD_READ', 'CER_CIRCUIT_READ', 'CER_DOSSIER_READ', 'CER_REGISTER_READ', 'PAR_INSTITUTION_READ']
const MESSAGERIE = ['COL_MESSAGE_READ', 'COL_MESSAGE_SEND']
const IAM = ['ACCOUNT_READ', 'ACCOUNT_CREATE', 'ACCOUNT_EDIT', 'ACCOUNT_ROLES', 'ACCOUNT_STATUS', 'ACCOUNT_DELETE', 'ACCOUNT_SECURITY', 'JOURNAL_READ', 'PROFILE_EDIT']
export const PROFILS_DEMO: Record<string, { identifiant: string; nom: string; sujet: string; permissions: string[] }> = {
  ROLE_SUPER_ADMIN: {
    identifiant: 'super.admin', nom: 'Serge Mouelle Kombi', sujet: 'u-super-admin',
    permissions: [...LECTURE_DES, ...MESSAGERIE, ...IAM.map((p) => `IAM_${p}`), 'IAM_ROLE_PRIVILEGED_GRANT', 'PLATFORM_SUPERVISION_READ', 'PLATFORM_CONFIG_READ', 'PLATFORM_CONFIG_EDIT',
      'PLATFORM_SECURITY_POLICY_READ', 'PLATFORM_SECURITY_POLICY_EDIT', 'PLATFORM_MESSAGING_EDIT'],
  },
  ROLE_RSSI: {
    identifiant: 'rssi', nom: 'Carine Ebongue Ndjock', sujet: 'u-rssi',
    permissions: [...LECTURE_DES, ...MESSAGERIE, 'CER_QUITUS_SECURITE_SIGN', ...IAM.map((p) => `IAM_${p}`), 'PLATFORM_SUPERVISION_READ', 'PLATFORM_CONFIG_READ',
      'PLATFORM_SECURITY_POLICY_READ', 'PLATFORM_SECURITY_POLICY_EDIT', 'PLATFORM_MESSAGING_EDIT'],
  },
}

/** Profil de démonstration (services simulés seulement) : clé gm.profilDemo du stockage local, sinon VITE_PROFIL_DEMO. */
export const CLE_PROFIL_DEMO = 'gm.profilDemo'
/** Démonstration du premier accès (services simulés seulement) : gm.premierAccesDemo = 1. */
export const CLE_PREMIER_ACCES_DEMO = 'gm.premierAccesDemo'
