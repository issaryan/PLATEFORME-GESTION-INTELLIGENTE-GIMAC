import type { GroupeNavigation } from '@gimac/socle'

/** Adresse de la console d'administration (D-62) : VITE_URL_CONSOLE, http://localhost:4202 par défaut. */
export const URL_CONSOLE = String(import.meta.env.VITE_URL_CONSOLE || 'http://localhost:4202').replace(/\/+$/, '')
/** Adresse d'un écran de la console (chemin commençant par /console). */
export const versConsole = (chemin: string) => `${URL_CONSOLE}${chemin}`
/** Écrans de l'administration servis dans le département (D-69) : comptes, profils, journal, politique. */
export const ADMINISTRATION = '/des/administration'

// Entrées de navigation du lot 1, avec la permission qui les rend visibles (DS-GIMAC-01 §6.2).
export const NAVIGATION_DES: GroupeNavigation[] = [
  // Tableau de bord propre à chaque acteur (D-58) : sections composées selon les permissions du jeton.
  // Messagerie de travail entre acteurs du GIMAC (D-64), avec le compteur de non-lus.
  {
    entrees: [
      { cle: 'tableauDeBord', libelleKey: 'nav.tableauDeBord', icone: 'gauge', chemin: '/des/tableau-de-bord' },
      { cle: 'messagerie', libelleKey: 'nav.messagerie', icone: 'messages-square', chemin: '/des/messagerie', permission: 'COL_MESSAGE_READ' },
    ],
  },
  {
    libelleKey: 'nav.groupe.registre',
    entrees: [{ cle: 'institutions', libelleKey: 'nav.institutions', icone: 'landmark', chemin: '/des/institutions', permission: 'PAR_INSTITUTION_READ' }],
  },
  {
    libelleKey: 'nav.groupe.homologation',
    entrees: [
      // Étapes 3 à 10 : liste et dossier numérique des liaisons (D-57) ; les écrans DES-08 et DES-09 restent
      // joignables depuis le dossier (« Graphe des opérations »), sans entrée propre dans le menu.
      { cle: 'liaisons', libelleKey: 'nav.liaisons', icone: 'route', chemin: '/des/parcours', permission: 'HOM_LIAISON_LIST' },
      // « Mes projets » : les liaisons confiées au conducteur de projet (D-58), seul à établir le procès-verbal.
      { cle: 'mesProjets', libelleKey: 'nav.mesProjets', icone: 'user', chemin: '/des/parcours?mesProjets=1', permission: 'CER_PV_ESTABLISH' },
      { cle: 'referentiels', libelleKey: 'nav.referentiels', icone: 'kanban', chemin: '/des/referentiels/operations', permission: 'HOM_REFERENTIEL_READ' },
    ],
  },
  // Administration des comptes (D-62, D-69) : écrans servis dans le département (RSSI, super administrateur),
  // sans changer d'application ; seule la console de la plateforme (supervision, configuration) reste à part.
  {
    libelleKey: 'nav.groupe.administration',
    entrees: [
      { cle: 'adminComptes', libelleKey: 'nav.admin.comptes', icone: 'users', chemin: `${ADMINISTRATION}/comptes`, permission: 'IAM_ACCOUNT_READ' },
      { cle: 'adminProfils', libelleKey: 'nav.admin.profils', icone: 'shield-check', chemin: `${ADMINISTRATION}/profils`, permission: 'IAM_ACCOUNT_READ' },
      { cle: 'adminJournal', libelleKey: 'nav.admin.journal', icone: 'scroll-text', chemin: `${ADMINISTRATION}/journal`, permission: 'IAM_JOURNAL_READ' },
      { cle: 'adminPolitique', libelleKey: 'nav.admin.politique', icone: 'lock', chemin: `${ADMINISTRATION}/politique-securite`, permission: 'PLATFORM_SECURITY_POLICY_READ' },
      { cle: 'adminConsole', libelleKey: 'nav.admin.console', icone: 'monitor', chemin: versConsole('/console/tableau-de-bord'), permission: 'PLATFORM_CONFIG_READ' },
    ],
  },
]
// Portail du participant (D-58) : son tableau de bord, puis ses liaisons et le suivi de chacune.
export const NAVIGATION_PORTAIL: GroupeNavigation[] = [
  {
    entrees: [
      { cle: 'tableauDeBordPortail', libelleKey: 'pp.nav.tableauDeBord', icone: 'gauge', chemin: '/portail/tableau-de-bord', permission: 'HOM_PORTAL_READ' },
      { cle: 'maLiaison', libelleKey: 'pp.nav.mesLiaisons', icone: 'route', chemin: '/portail/liaisons', permission: 'HOM_PORTAL_READ' },
    ],
  },
]
