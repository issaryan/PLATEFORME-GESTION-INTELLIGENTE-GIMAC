import type { GroupeNavigation } from '@gimac/socle'

// Entrées de navigation de la console, avec la permission qui les rend visibles (DS-GIMAC-01 §6.2) :
// une entrée non permise est absente du DOM. Le tableau de bord est composé selon les permissions.
export const NAVIGATION_CONSOLE: GroupeNavigation[] = [
  {
    entrees: [
      { cle: 'tableauDeBord', libelleKey: 'console.nav.tableauDeBord', icone: 'gauge', chemin: '/console/tableau-de-bord' },
      // Messagerie instantanée de travail (D-64) : tout membre interne du GIMAC, dont le RSSI et le super administrateur.
      { cle: 'messagerie', libelleKey: 'console.nav.messagerie', icone: 'messages-square', chemin: '/console/messagerie', permission: 'COL_MESSAGE_READ' },
    ],
  },
  {
    libelleKey: 'console.nav.groupe.comptes',
    entrees: [
      { cle: 'comptes', libelleKey: 'console.nav.comptes', icone: 'users', chemin: '/console/comptes', permission: 'IAM_ACCOUNT_READ' },
      { cle: 'profils', libelleKey: 'console.nav.profils', icone: 'shield-check', chemin: '/console/profils', permission: 'IAM_ACCOUNT_READ' },
      { cle: 'journal', libelleKey: 'console.nav.journal', icone: 'scroll-text', chemin: '/console/journal', permission: 'IAM_JOURNAL_READ' },
    ],
  },
  {
    libelleKey: 'console.nav.groupe.securite',
    entrees: [
      { cle: 'politique', libelleKey: 'console.nav.politique', icone: 'lock', chemin: '/console/politique-securite', permission: 'PLATFORM_SECURITY_POLICY_READ' },
    ],
  },
  {
    libelleKey: 'console.nav.groupe.plateforme',
    entrees: [
      { cle: 'configuration', libelleKey: 'console.nav.configuration', icone: 'monitor', chemin: '/console/plateforme/configuration', permission: 'PLATFORM_CONFIG_READ' },
      { cle: 'journalisation', libelleKey: 'console.nav.journalisation', icone: 'file-text', chemin: '/console/plateforme/journalisation', permission: 'PLATFORM_CONFIG_READ' },
      // Serveur d'envoi des courriels de la plateforme (invitations, alertes) : distinct de la messagerie instantanée.
      { cle: 'courriels', libelleKey: 'console.nav.courriels', icone: 'send', chemin: '/console/plateforme/courriels', permission: 'PLATFORM_MESSAGING_EDIT' },
    ],
  },
]

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
