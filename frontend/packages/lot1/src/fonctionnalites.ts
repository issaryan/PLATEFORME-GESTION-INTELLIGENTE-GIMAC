import type { IconName } from '@gimac/design-system'
import { adresseBaseApplication, satisfait, type GroupeNavigation, type Predicat } from '@gimac/socle'
import { PERM } from './permissions'

// Registre unique des fonctionnalités de l'application interne (D-79). Chaque fonctionnalité porte sa clé,
// son libellé, son icône, son chemin, son groupe et le prédicat de permissions du jeton qui l'ouvre ; certaines
// apportent un widget au tableau de bord. La barre latérale et le tableau de bord en sont dérivés : un
// profil voit exactement ce que ses permissions ouvrent, jamais une liste de noms de rôle. La passerelle (OPA)
// et chaque service refont le contrôle à chaque appel.

/** Widgets du tableau de bord, dans leur ordre d'affichage (« Ma journée » d'abord). */
export const WIDGETS = ['maJournee', 'administration', 'supervision', 'infrastructure', 'ddrm', 'cockpit', 'monTravail'] as const
export type CleWidget = (typeof WIDGETS)[number]
/** Widget apporté par une fonctionnalité, avec son prédicat propre en plus de celui de la fonctionnalité. */
export type WidgetTableau = { cle: CleWidget; predicat?: Predicat }

export type GroupeFonctionnalites = 'accueil' | 'monActivite' | 'registre' | 'homologation' | 'administration' | 'plateforme'

export type Fonctionnalite = {
  cle: string
  libelleKey: string
  icone: IconName
  /** Chemin de l'écran ; adresse absolue pour un écran d'une autre application (console). */
  chemin: string
  groupe: GroupeFonctionnalites
  /** Prédicat de permissions ; absent : toute session interne. */
  predicat?: Predicat
  widgets?: WidgetTableau[]
}

/** Groupes de la barre latérale, dans leur ordre ; l'accueil n'a pas de titre. */
export const GROUPES: { cle: GroupeFonctionnalites; libelleKey?: string }[] = [
  { cle: 'accueil' },
  { cle: 'monActivite', libelleKey: 'nav.groupe.monActivite' },
  { cle: 'registre', libelleKey: 'nav.groupe.registre' },
  { cle: 'homologation', libelleKey: 'nav.groupe.homologation' },
  { cle: 'administration', libelleKey: 'nav.groupe.administration' },
  { cle: 'plateforme', libelleKey: 'nav.groupe.plateforme' },
]

/** Écrans de l'administration des comptes servis dans l'application interne (D-69). */
export const ADMINISTRATION = '/des/administration'
/** Écrans de la plateforme servis dans l'application interne (D-79) : supervision, infrastructure, configuration. */
export const PLATEFORME = '/des/plateforme'

/**
 * Adresse de la console d'administration (D-62) vue depuis la page : VITE_URL_CONSOLE sous les noms publics,
 * même hôte et port direct (4202 par défaut) en accès par l'adresse IP (adressesApplications du socle).
 */
export const urlConsole = () => adresseBaseApplication('console')
/** Adresse d'un écran de la console (chemin commençant par /console). */
export const versConsole = (chemin: string) => `${urlConsole()}${chemin}`

const tient = (...any: string[]): Predicat => ({ any })

/**
 * « Mon travail » (étapes 3 à 10 du parcours) : tableau de bord d'acteur de MS-HOMOLOGATION (HOM_DASHBOARD_READ,
 * liste des liaisons), pour qui agit dans le parcours : dépôt, validation ou attribution d'une opération, conduite
 * d'une étape, désignation du conducteur, procès-verbal, vérifications et visas de la décision.
 */
export const PREDICAT_MON_TRAVAIL: Predicat = {
  all: [PERM.pilotageLire, PERM.liaisonLister],
  any: [PERM.preuveDeposer, PERM.operationValider, PERM.operationAttribuer, PERM.etapeConduire, PERM.projetDesigner, PERM.pvEtablir, PERM.pvSignerGimac,
    'CER_QUITUS_SECURITE_SIGN', PERM.visaConformite, PERM.visaDg, PERM.securiteVerifier, PERM.dossierVerifier, PERM.ficheChangementLier, PERM.pvMiseEnServiceDeposer],
}
/** Cockpit de pilotage (CR §7.3) : qui désigne les conducteurs de projet (chefs de département) ou vise pour la direction générale. */
export const PREDICAT_COCKPIT: Predicat = { all: [PERM.pilotageLire], any: [PERM.projetDesigner, PERM.visaDg, 'CER_ATTESTATION_STATUS_APPROVE'] }
/** Étapes 1 et 2 (DDRM) : qui instruit les adhésions ou prononce les transitions du registre. */
export const PREDICAT_DDRM: Predicat = { all: [PERM.institutionLire], any: [PERM.adhesionInstruire, PERM.transitionApprouver] }

export const FONCTIONNALITES: Fonctionnalite[] = [
  // Tableau de bord propre à chaque acteur (D-58) ; messagerie de travail entre acteurs (D-64).
  { cle: 'tableauDeBord', libelleKey: 'nav.tableauDeBord', icone: 'gauge', chemin: '/des/tableau-de-bord', groupe: 'accueil' },
  { cle: 'messagerie', libelleKey: 'nav.messagerie', icone: 'messages-square', chemin: '/des/messagerie', groupe: 'accueil', predicat: tient(PERM.messageLire) },
  // Notifications de la plateforme (D-80) : « Voir tout » de la cloche ; tout profil interne.
  { cle: 'notifications', libelleKey: 'nav.notifications', icone: 'bell-ring', chemin: '/des/notifications', groupe: 'accueil', predicat: tient(PERM.notificationsLire) },
  // Mon activité (D-76 §6) : tout membre interne ; « Mon équipe » pour les responsables.
  { cle: 'maJournee', libelleKey: 'nav.maJournee', icone: 'square-check', chemin: '/des/activite', groupe: 'monActivite', predicat: tient(PERM.activitePropre), widgets: [{ cle: 'maJournee' }] },
  { cle: 'agenda', libelleKey: 'nav.agenda', icone: 'calendar', chemin: '/des/agenda', groupe: 'monActivite', predicat: tient(PERM.activitePropre) },
  { cle: 'taches', libelleKey: 'nav.taches', icone: 'kanban', chemin: '/des/taches', groupe: 'monActivite', predicat: tient(PERM.activitePropre) },
  { cle: 'rapports', libelleKey: 'nav.rapports', icone: 'file-text', chemin: '/des/rapports', groupe: 'monActivite', predicat: tient(PERM.activitePropre) },
  { cle: 'equipe', libelleKey: 'nav.equipe', icone: 'users', chemin: '/des/equipe', groupe: 'monActivite', predicat: tient(PERM.equipeLire) },
  // Registre des institutions (étapes 1 et 2).
  { cle: 'institutions', libelleKey: 'nav.institutions', icone: 'landmark', chemin: '/des/institutions', groupe: 'registre', predicat: tient(PERM.institutionLire), widgets: [{ cle: 'ddrm', predicat: PREDICAT_DDRM }] },
  // Étapes 3 à 10 : liste et dossier numérique des liaisons (D-57), cockpit de pilotage et « Mon travail ».
  {
    cle: 'liaisons', libelleKey: 'nav.liaisons', icone: 'route', chemin: '/des/parcours', groupe: 'homologation', predicat: tient(PERM.liaisonLister),
    widgets: [{ cle: 'cockpit', predicat: PREDICAT_COCKPIT }, { cle: 'monTravail', predicat: PREDICAT_MON_TRAVAIL }],
  },
  // « Mes projets » : les liaisons confiées au conducteur de projet (D-58), seul à établir le procès-verbal.
  { cle: 'mesProjets', libelleKey: 'nav.mesProjets', icone: 'user', chemin: '/des/parcours?mesProjets=1', groupe: 'homologation', predicat: tient(PERM.pvEtablir) },
  { cle: 'referentiels', libelleKey: 'nav.referentiels', icone: 'kanban', chemin: '/des/referentiels/operations', groupe: 'homologation', predicat: tient(PERM.referentielLire) },
  // Administration des comptes (D-62, D-69), servie dans l'application interne.
  { cle: 'adminComptes', libelleKey: 'nav.admin.comptes', icone: 'users', chemin: `${ADMINISTRATION}/comptes`, groupe: 'administration', predicat: tient(PERM.comptesLire), widgets: [{ cle: 'administration' }] },
  { cle: 'adminProfils', libelleKey: 'nav.admin.profils', icone: 'shield-check', chemin: `${ADMINISTRATION}/profils`, groupe: 'administration', predicat: tient(PERM.comptesLire) },
  // Catalogue des unités organisationnelles (D-76 §1.1) : lecture IAM_ACCOUNT_READ, édition IAM_ACCOUNT_EDIT.
  { cle: 'adminOrganisation', libelleKey: 'nav.admin.organisation', icone: 'landmark', chemin: `${ADMINISTRATION}/organisation`, groupe: 'administration', predicat: tient(PERM.comptesLire) },
  { cle: 'adminJournal', libelleKey: 'nav.admin.journal', icone: 'scroll-text', chemin: `${ADMINISTRATION}/journal`, groupe: 'administration', predicat: tient(PERM.journalAdministrationLire) },
  { cle: 'adminPolitique', libelleKey: 'nav.admin.politique', icone: 'lock', chemin: `${ADMINISTRATION}/politique-securite`, groupe: 'administration', predicat: tient(PERM.politiqueLire) },
  // Plateforme (D-79) : les écrans de la console servis aussi dans l'application interne ; la console reste à part.
  { cle: 'supervision', libelleKey: 'nav.plateforme.supervision', icone: 'eye', chemin: `${PLATEFORME}/supervision`, groupe: 'plateforme', predicat: tient(PERM.supervisionLire), widgets: [{ cle: 'supervision' }] },
  { cle: 'infrastructure', libelleKey: 'nav.plateforme.infrastructure', icone: 'rotate-ccw', chemin: `${PLATEFORME}/infrastructure`, groupe: 'plateforme', predicat: tient(PERM.exploitationLire), widgets: [{ cle: 'infrastructure' }] },
  { cle: 'configuration', libelleKey: 'nav.plateforme.configuration', icone: 'monitor', chemin: `${PLATEFORME}/configuration`, groupe: 'plateforme', predicat: tient(PERM.configurationLire) },
  { cle: 'journalisation', libelleKey: 'nav.plateforme.journalisation', icone: 'file-text', chemin: `${PLATEFORME}/journalisation`, groupe: 'plateforme', predicat: tient(PERM.configurationLire) },
  { cle: 'courriels', libelleKey: 'nav.plateforme.courriels', icone: 'send', chemin: `${PLATEFORME}/courriels`, groupe: 'plateforme', predicat: tient(PERM.courrielsPlateforme) },
  { cle: 'adminConsole', libelleKey: 'nav.admin.console', icone: 'arrow-right', chemin: versConsole('/console/tableau-de-bord'), groupe: 'plateforme', predicat: tient(PERM.configurationLire) },
]

/** Vrai si les permissions ouvrent la fonctionnalité. */
export const estOuverte = (f: Fonctionnalite, permissions: readonly string[] | undefined) => satisfait(permissions, f.predicat)

/** Fonctionnalités que les permissions ouvrent, dans l'ordre du registre. */
export function fonctionnalitesOuvertes(permissions: readonly string[] | undefined, registre = FONCTIONNALITES): Fonctionnalite[] {
  return registre.filter((f) => estOuverte(f, permissions))
}

/** Widgets du tableau de bord que les permissions ouvrent, dans l'ordre d'affichage, sans doublon. */
export function widgetsOuverts(permissions: readonly string[] | undefined, registre = FONCTIONNALITES): CleWidget[] {
  const ouverts = new Set(fonctionnalitesOuvertes(permissions, registre).flatMap((f) => (f.widgets ?? []).filter((w) => satisfait(permissions, w.predicat)).map((w) => w.cle)))
  return WIDGETS.filter((w) => ouverts.has(w))
}

/** Barre latérale dérivée du registre : chaque entrée garde son prédicat, l'enveloppe ne montre que les entrées permises. */
export function navigationDepuis(registre = FONCTIONNALITES): GroupeNavigation[] {
  return GROUPES.map((g) => ({
    ...(g.libelleKey ? { libelleKey: g.libelleKey } : {}),
    entrees: registre.filter((f) => f.groupe === g.cle).map((f) => ({ cle: f.cle, libelleKey: f.libelleKey, icone: f.icone, chemin: f.chemin, predicat: f.predicat })),
  })).filter((g) => g.entrees.length > 0)
}
