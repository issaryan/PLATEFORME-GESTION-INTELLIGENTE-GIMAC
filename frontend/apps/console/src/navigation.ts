import { ADRESSES_APPLICATIONS, type GroupeNavigation } from '@gimac/socle'
import { PROFILS_INTERNES_DEMO } from '@gimac/lot1'
import { PROFILS_DEMO as PROFILS_ADMINISTRATION } from '@gimac/administration'

/** Adresse de l'application interne (D-69, D-79) : VITE_URL_DES (ou VITE_URL_DEPARTEMENT), http://localhost:4200 par défaut. */
export const URL_DEPARTEMENT = ADRESSES_APPLICATIONS.des

// Entrées de navigation de la console, avec la permission qui les rend visibles (DS-GIMAC-01 §6.2) :
// une entrée non permise est absente du DOM. Le tableau de bord est composé selon les permissions.
export const NAVIGATION_CONSOLE: GroupeNavigation[] = [
  {
    entrees: [
      // Retour au tableau de bord du département, d'où le RSSI et le super administrateur ouvrent la console (D-69).
      { cle: 'departement', libelleKey: 'console.nav.departement', icone: 'arrow-left', chemin: `${URL_DEPARTEMENT}/des/tableau-de-bord` },
      { cle: 'tableauDeBord', libelleKey: 'console.nav.tableauDeBord', icone: 'gauge', chemin: '/console/tableau-de-bord' },
      // Messagerie instantanée de travail (D-64) : tout membre interne du GIMAC, dont le RSSI et le super administrateur.
      { cle: 'messagerie', libelleKey: 'console.nav.messagerie', icone: 'messages-square', chemin: '/console/messagerie', permission: 'COL_MESSAGE_READ' },
      // Notifications de la plateforme (D-80) : « Voir tout » de la cloche ; tout membre interne.
      { cle: 'notifications', libelleKey: 'console.nav.notifications', icone: 'bell-ring', chemin: '/console/notifications', permission: 'NOT_INBOX_READ' },
      // Mon activité (D-76, D-79) : journée, agenda, tâches et rapports de chacun, tenus dans l'application interne.
      { cle: 'monActivite', libelleKey: 'console.nav.monActivite', icone: 'square-check', chemin: `${URL_DEPARTEMENT}/des/activite`, permission: 'PIL_ACTIVITY_OWN' },
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
      // Centre de contrôle : état en temps réel et graphiques des métriques (services, serveurs, conteneurs).
      { cle: 'supervision', libelleKey: 'console.nav.supervision', icone: 'gauge', chemin: '/console/plateforme/supervision', permission: 'PLATFORM_SUPERVISION_READ' },
      // Exploitation de l'infrastructure (D-78) : charges, journaux, gestes ; lecture seule sans PLATFORM_OPS_EXECUTE.
      { cle: 'infrastructure', libelleKey: 'console.nav.infrastructure', icone: 'rotate-ccw', chemin: '/console/plateforme/infrastructure', permission: 'PLATFORM_OPS_READ' },
      { cle: 'configuration', libelleKey: 'console.nav.configuration', icone: 'monitor', chemin: '/console/plateforme/configuration', permission: 'PLATFORM_CONFIG_READ' },
      { cle: 'journalisation', libelleKey: 'console.nav.journalisation', icone: 'file-text', chemin: '/console/plateforme/journalisation', permission: 'PLATFORM_CONFIG_READ' },
      // Serveur d'envoi des courriels de la plateforme (invitations, alertes) : distinct de la messagerie instantanée.
      { cle: 'courriels', libelleKey: 'console.nav.courriels', icone: 'send', chemin: '/console/plateforme/courriels', permission: 'PLATFORM_MESSAGING_EDIT' },
    ],
  },
]

export { CLE_PREMIER_ACCES_DEMO, CLE_PROFIL_DEMO } from '@gimac/administration'

/**
 * Profils de démonstration de la console (services simulés seulement) : tout profil interne (comptes communs aux
 * trois applications, D-79), l'identité de la console primant pour le super administrateur, le RSSI et
 * l'administrateur système. Un profil sans permission de console est aiguillé vers l'application interne.
 */
export const PROFILS_DEMO: Record<string, { identifiant: string; nom: string; sujet: string; permissions: string[] }> = { ...PROFILS_INTERNES_DEMO, ...PROFILS_ADMINISTRATION }
