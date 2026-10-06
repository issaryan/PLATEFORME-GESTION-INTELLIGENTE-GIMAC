// Profils de démonstration de l'administration (services simulés seulement), d'après le domaine « gimac ».
/** Profils de démonstration (services simulés seulement, VITE_PROFIL_DEMO), d'après le domaine « gimac ». */
const LECTURE_DES = ['HOM_REFERENTIEL_READ', 'HOM_LIAISON_READ', 'HOM_LIAISON_LIST', 'HOM_DASHBOARD_READ', 'CER_CIRCUIT_READ', 'CER_DOSSIER_READ', 'CER_REGISTER_READ', 'PAR_INSTITUTION_READ']
// Messagerie (D-64), « Mon activité » (D-76) et notifications de la plateforme (D-80) : tout profil interne.
const MESSAGERIE = ['COL_MESSAGE_READ', 'COL_MESSAGE_SEND', 'PIL_ACTIVITY_OWN', 'NOT_INBOX_READ']
const IAM = ['ACCOUNT_READ', 'ACCOUNT_CREATE', 'ACCOUNT_EDIT', 'ACCOUNT_ROLES', 'ACCOUNT_STATUS', 'ACCOUNT_DELETE', 'ACCOUNT_SECURITY', 'JOURNAL_READ', 'PROFILE_EDIT']
export const PROFILS_DEMO: Record<string, { identifiant: string; nom: string; sujet: string; permissions: string[] }> = {
  ROLE_SUPER_ADMIN: {
    identifiant: 'super.admin', nom: 'Serge Mouelle Kombi', sujet: 'u-super-admin',
    permissions: [...LECTURE_DES, ...MESSAGERIE, ...IAM.map((p) => `IAM_${p}`), 'IAM_ROLE_PRIVILEGED_GRANT', 'PLATFORM_SUPERVISION_READ', 'PLATFORM_CONFIG_READ', 'PLATFORM_CONFIG_EDIT',
      'PLATFORM_OPS_READ', 'PLATFORM_OPS_EXECUTE',
      'PLATFORM_SECURITY_POLICY_READ', 'PLATFORM_SECURITY_POLICY_EDIT', 'PLATFORM_MESSAGING_EDIT',
      // Référentiel des opérations (D-89) : le super administrateur l'édite et le publie.
      'HOM_REFERENTIEL_EDIT', 'HOM_REFERENTIEL_PUBLISH'],
  },
  ROLE_RSSI: {
    identifiant: 'rssi', nom: 'Carine Ebongue Ndjock', sujet: 'u-rssi',
    permissions: [...LECTURE_DES, ...MESSAGERIE, 'CER_QUITUS_SECURITE_SIGN', ...IAM.map((p) => `IAM_${p}`), 'PLATFORM_SUPERVISION_READ', 'PLATFORM_CONFIG_READ', 'PLATFORM_OPS_READ',
      'PIL_TEAM_READ', 'PIL_TASK_ASSIGN',
      'PLATFORM_SECURITY_POLICY_READ', 'PLATFORM_SECURITY_POLICY_EDIT', 'PLATFORM_MESSAGING_EDIT', 'HOM_REFERENTIEL_READ_ALL'],
  },
  // Administrateur système (D-78) : supervision, configuration en lecture et exploitation de l'infrastructure ;
  // messagerie et « Mon activité » ; n'administre pas les comptes. Même identité dans les trois applications.
  ROLE_ADMIN_SYSTEME: {
    identifiant: 'admin.systeme', nom: 'Thierry Abessolo Nkoulou', sujet: 'u-admin-systeme',
    permissions: [...MESSAGERIE, 'PLATFORM_SUPERVISION_READ', 'PLATFORM_CONFIG_READ', 'PLATFORM_OPS_READ', 'PLATFORM_OPS_EXECUTE'],
  },
}

/** Profil de démonstration (services simulés seulement) : clé gm.profilDemo du stockage local, sinon VITE_PROFIL_DEMO. */
export const CLE_PROFIL_DEMO = 'gm.profilDemo'
/** Démonstration du premier accès (services simulés seulement) : gm.premierAccesDemo = 1. */
export const CLE_PREMIER_ACCES_DEMO = 'gm.premierAccesDemo'
