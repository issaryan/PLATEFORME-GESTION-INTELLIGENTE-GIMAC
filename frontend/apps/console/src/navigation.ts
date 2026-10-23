import { adresseBaseApplication } from '@gimac/socle'
import { PROFILS_INTERNES_DEMO, type Ecran } from '@gimac/lot1'
import { PROFILS_DEMO as PROFILS_ADMINISTRATION } from '@gimac/administration'

/**
 * Adresse de l'application interne (D-69, D-79) vue depuis la page : VITE_URL_DES (ou VITE_URL_DEPARTEMENT) sous
 * les noms publics, même hôte et port direct (4200 par défaut) en accès par l'adresse IP. Évaluée au chargement :
 * l'hôte de la page ne change pas au fil de la navigation dans l'application.
 */
export const URL_DEPARTEMENT = adresseBaseApplication('des')

/**
 * Écrans de la console (D-94) : seule la correspondance clé du catalogue → chemin reste dans le code ; libellés,
 * groupes, ordre, icônes, visibilité et permissions viennent du catalogue des fonctionnalités servi par MS-IAM.
 */
export const ECRANS_CONSOLE: Record<string, Ecran> = {
  // Retour au tableau de bord du département, d'où le RSSI et le super administrateur ouvrent la console (D-69).
  consoleDepartement: { chemin: `${URL_DEPARTEMENT}/des/tableau-de-bord` },
  consoleTableauDeBord: { chemin: '/console/tableau-de-bord' },
  consoleMessagerie: { chemin: '/console/messagerie' },
  consoleNotifications: { chemin: '/console/notifications' },
  // Mon activité (D-76, D-79) : tenue dans l'application interne.
  consoleMonActivite: { chemin: `${URL_DEPARTEMENT}/des/activite` },
  consoleComptes: { chemin: '/console/comptes' },
  consoleDemandes: { chemin: '/console/demandes' },
  consoleProfils: { chemin: '/console/profils' },
  consoleMatrice: { chemin: '/console/matrice-pouvoirs' },
  consoleOrganisation: { chemin: '/console/organisation' },
  consoleOrganigramme: { chemin: '/console/organigramme' },
  // Sandbox GIMAC (D-119) : ce que MS-IAM applique à la sandbox.
  consoleSandbox: { chemin: '/console/sandbox' },
  consoleJournal: { chemin: '/console/journal' },
  consolePolitique: { chemin: '/console/politique-securite' },
  consoleSupervision: { chemin: '/console/plateforme/supervision' },
  consoleInfrastructure: { chemin: '/console/plateforme/infrastructure' },
  consoleConfiguration: { chemin: '/console/plateforme/configuration' },
  consoleJournalisation: { chemin: '/console/plateforme/journalisation' },
  consoleCourriels: { chemin: '/console/plateforme/courriels' },
}

export { CLE_PREMIER_ACCES_DEMO, CLE_PROFIL_DEMO } from '@gimac/administration'

/**
 * Profils de démonstration de la console (services simulés seulement) : tout profil interne (comptes communs aux
 * trois applications, D-79), l'identité de la console primant pour le super administrateur, le RSSI et
 * l'administrateur système. Un profil sans permission de console est aiguillé vers l'application interne.
 */
export const PROFILS_DEMO: Record<string, { identifiant: string; nom: string; sujet: string; permissions: string[] }> = { ...PROFILS_INTERNES_DEMO, ...PROFILS_ADMINISTRATION }
