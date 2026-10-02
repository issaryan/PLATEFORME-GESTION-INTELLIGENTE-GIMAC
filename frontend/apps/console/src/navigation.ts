import type { GroupeNavigation } from '@gimac/socle'

/** Adresse de l'application du département (D-69) : VITE_URL_DEPARTEMENT, http://localhost:4200 par défaut. */
export const URL_DEPARTEMENT = String(import.meta.env.VITE_URL_DEPARTEMENT || 'http://localhost:4200').replace(/\/+$/, '')

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

export { CLE_PREMIER_ACCES_DEMO, CLE_PROFIL_DEMO, PROFILS_DEMO } from '@gimac/administration'
