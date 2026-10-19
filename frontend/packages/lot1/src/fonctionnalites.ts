import type { IconName } from '@gimac/design-system'
import { adresseBaseApplication, satisfait, type EntreeNavigation, type GroupeNavigation, type Predicat } from '@gimac/socle'

// Écrans pilotés par la configuration (D-94, docs/lot1/ecrans-par-configuration.md) : le catalogue des fonctionnalités
// est servi par MS-IAM (GET /bff/{des,console}/iam/configuration/fonctionnalites) et modifié par le RSSI ou le super
// administrateur (PUT …/{cle}, IAM_PROFILE_EDIT). Libellé, groupe, ordre, icône, visibilité, prédicat de permissions
// (tous, unDe), widgets du tableau de bord et droits (D-90) viennent de ce catalogue ; le code ne garde que la
// correspondance clé → écran (chemin et composant). La barre latérale, les gardes de route, la navigation de la
// console, le tableau de bord, l'éditeur de profil par fonctionnalité et l'aperçu de la barre latérale en dérivent.
// La passerelle (OPA) et chaque service refont le contrôle à chaque appel.

export type Application = 'des' | 'console' | 'portail'
export const NATURES_DROIT = ['CONSULTER', 'CREER', 'MODIFIER', 'VALIDER', 'SIGNER', 'PUBLIER', 'EXPORTER', 'ADMINISTRER'] as const
export type NatureDroit = (typeof NATURES_DROIT)[number]
/** Prédicat servi : toutes les permissions de `tous` et au moins une de `unDe` (vides : toujours satisfaits). */
export type PredicatServi = { tous: string[]; unDe: string[] }
export type DroitServi = { cle: string; nature: NatureDroit; permission: string; libelle: string }
export type WidgetServi = { cle: string; permissions: PredicatServi }
/** Fonctionnalité telle que la sert MS-IAM (§6.2) ; `groupe` nul : hors barre latérale (droits seulement). */
export type DefinitionFonctionnalite = {
  cle: string
  application: Application
  module: string
  libelle: string
  groupe: string | null
  ordre: number
  icone: string
  visible: boolean
  permissions: PredicatServi
  widgets: WidgetServi[]
  droits: DroitServi[]
  modifieLe?: string | null
  modifiePar?: string | null
}
/** Saisie d'une modification (PUT, sans trace) ; le motif est envoyé à part. */
export type SaisieFonctionnalite = Omit<DefinitionFonctionnalite, 'modifieLe' | 'modifiePar'>

/** État du catalogue servi : en chargement, prêt, ou indisponible (repli prudent : rien au-delà de l'accueil). */
export type EtatCatalogue = { etat: 'chargement' | 'pret' | 'indisponible'; fonctionnalites: DefinitionFonctionnalite[] }

/** Widgets du tableau de bord connus du code (clé → composant), dans leur ordre d'affichage (« Ma journée » d'abord). */
export const WIDGETS = ['maJournee', 'administration', 'supervision', 'infrastructure', 'ddrm', 'cockpit', 'monTravail'] as const
export type CleWidget = (typeof WIDGETS)[number]

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

/** Écran d'une fonctionnalité : chemin dans l'application (adresse absolue pour un écran d'une autre application). */
export type Ecran = { chemin: string }

/**
 * Correspondance clé → écran de l'application interne : seule donnée gardée dans le code. Une fonctionnalité servie
 * dont la clé est inconnue ici n'apparaît pas (aucun écran à ouvrir) ; une clé d'ici absente du catalogue non plus.
 */
export const ECRANS_DES: Record<string, Ecran> = {
  tableauDeBord: { chemin: '/des/tableau-de-bord' },
  messagerie: { chemin: '/des/messagerie' },
  notifications: { chemin: '/des/notifications' },
  maJournee: { chemin: '/des/activite' },
  agenda: { chemin: '/des/agenda' },
  taches: { chemin: '/des/taches' },
  rapports: { chemin: '/des/rapports' },
  equipe: { chemin: '/des/equipe' },
  processus: { chemin: '/des/processus' },
  // Lancer une demande et suivre ses demandes (D-98).
  lancerDemande: { chemin: '/des/demandes/lancer' },
  mesDemandes: { chemin: '/des/demandes' },
  institutions: { chemin: '/des/institutions' },
  // Parcours de certification (D-98) : lancement guidé et actions de la personne.
  lancerCertification: { chemin: '/des/certification/lancer' },
  mesActionsCertification: { chemin: '/des/certification/mes-actions' },
  liaisons: { chemin: '/des/parcours' },
  mesProjets: { chemin: '/des/parcours?mesProjets=1' },
  referentiels: { chemin: '/des/referentiels/operations' },
  adminComptes: { chemin: `${ADMINISTRATION}/comptes` },
  adminDemandes: { chemin: `${ADMINISTRATION}/demandes` },
  adminProfils: { chemin: `${ADMINISTRATION}/profils` },
  adminMatrice: { chemin: `${ADMINISTRATION}/matrice-pouvoirs` },
  adminOrganisation: { chemin: `${ADMINISTRATION}/organisation` },
  adminOrganigramme: { chemin: `${ADMINISTRATION}/organigramme` },
  adminJournal: { chemin: `${ADMINISTRATION}/journal` },
  adminPolitique: { chemin: `${ADMINISTRATION}/politique-securite` },
  // Supervision des liaisons, astreintes et relève de quart (D-114, paquet @gimac/exploitation).
  monitoringSupervision: { chemin: '/des/exploitation/supervision' },
  monitoringAstreintes: { chemin: '/des/exploitation/astreintes' },
  monitoringReleve: { chemin: '/des/exploitation/releve' },
  supervision: { chemin: `${PLATEFORME}/supervision` },
  infrastructure: { chemin: `${PLATEFORME}/infrastructure` },
  configuration: { chemin: `${PLATEFORME}/configuration` },
  journalisation: { chemin: `${PLATEFORME}/journalisation` },
  courriels: { chemin: `${PLATEFORME}/courriels` },
  adminConsole: { chemin: versConsole('/console/tableau-de-bord') },
}

/** Accueil de chaque application : seule entrée montrée tant que le catalogue n'est pas servi (repli prudent). */
export const ACCUEIL: Record<'des' | 'console', EntreeNavigation> = {
  des: { cle: 'tableauDeBord', libelleKey: 'nav.tableauDeBord', icone: 'gauge', chemin: '/des/tableau-de-bord' },
  console: { cle: 'consoleTableauDeBord', libelleKey: 'console.nav.tableauDeBord', icone: 'gauge', chemin: '/console/tableau-de-bord' },
}

/** Prédicat du socle d'un prédicat servi. */
export const predicatDe = (p?: PredicatServi | null): Predicat => ({ all: p?.tous ?? [], any: p?.unDe ?? [] })

/** Vrai si la fonctionnalité est visible et que les permissions satisfont son prédicat. */
export const estOuverte = (f: DefinitionFonctionnalite, permissions: readonly string[] | undefined) => f.visible && satisfait(permissions, predicatDe(f.permissions))

/** Tri du catalogue : ordre puis clé (comme le serveur). */
export const trier = (l: DefinitionFonctionnalite[]) => [...l].sort((a, b) => a.ordre - b.ordre || a.cle.localeCompare(b.cle))

/** Fonctionnalités d'une application que les permissions ouvrent, dans l'ordre du catalogue. */
export function fonctionnalitesOuvertes(catalogue: DefinitionFonctionnalite[], application: Application, permissions: readonly string[] | undefined): DefinitionFonctionnalite[] {
  return trier(catalogue.filter((f) => f.application === application && estOuverte(f, permissions)))
}

/** Widgets du tableau de bord que les permissions ouvrent (fonctionnalité ouverte et prédicat du widget), dans l'ordre d'affichage. */
export function widgetsOuverts(catalogue: DefinitionFonctionnalite[], permissions: readonly string[] | undefined): CleWidget[] {
  const ouverts = new Set(fonctionnalitesOuvertes(catalogue, 'des', permissions).flatMap((f) => f.widgets.filter((w) => satisfait(permissions, predicatDe(w.permissions))).map((w) => w.cle)))
  return WIDGETS.filter((w) => ouverts.has(w))
}

/** Accès à l'écran d'une fonctionnalité : catalogue prêt, fonctionnalité visible et ouverte, droit exigé détenu s'il est donné. */
export function accesFonctionnalite(c: EtatCatalogue, cle: string, permissions: readonly string[] | undefined, droit?: string): 'chargement' | 'indisponible' | 'ouvert' | 'refuse' {
  if (c.etat !== 'pret') return c.etat
  const f = c.fonctionnalites.find((x) => x.cle === cle)
  if (!f || !estOuverte(f, permissions)) return 'refuse'
  return !droit || (permissions ?? []).includes(droit) ? 'ouvert' : 'refuse'
}

/** Libellé servi : clé de traduction connue, sinon texte français tel quel. */
export type Traduire = { t: (cle: string) => string; existe: (cle: string) => boolean }
export const libelleServi = (tr: Traduire, libelle: string) => (tr.existe(libelle) ? tr.t(libelle) : libelle)
/** Titre d'un groupe : `<prefixe><groupe>` s'il est traduit, sinon le code du groupe (groupe créé par la configuration). */
export const libelleGroupe = (tr: Traduire, prefixe: string, groupe: string) => (tr.existe(`${prefixe}${groupe}`) ? tr.t(`${prefixe}${groupe}`) : groupe)

/**
 * Barre latérale d'une application depuis le catalogue : fonctionnalités visibles d'un groupe dont la clé a un écran,
 * groupes dans l'ordre de leur première fonctionnalité ; l'accueil (`accueil`) n'a pas de titre. Chaque entrée garde
 * son prédicat : l'enveloppe ne montre que les entrées permises.
 */
export function navigationDepuis(catalogue: DefinitionFonctionnalite[], application: Application, ecrans: Record<string, Ecran>, tr: Traduire, prefixeGroupe: string): GroupeNavigation[] {
  const entrees = trier(catalogue.filter((f) => f.application === application && f.visible && f.groupe && ecrans[f.cle]))
  const groupes: string[] = []
  entrees.forEach((f) => { if (!groupes.includes(f.groupe!)) groupes.push(f.groupe!) })
  return groupes.map((g) => ({
    ...(g === 'accueil' ? {} : { libelleKey: `${prefixeGroupe}${g}`, libelle: libelleGroupe(tr, prefixeGroupe, g) }),
    entrees: entrees.filter((f) => f.groupe === g).map((f) => ({
      cle: f.cle, libelleKey: f.libelle, libelle: libelleServi(tr, f.libelle), icone: f.icone as IconName, chemin: ecrans[f.cle].chemin, predicat: predicatDe(f.permissions),
    })),
  }))
}

/** Repli prudent (catalogue en chargement ou indisponible) : l'accueil seul. */
export const navigationRepli = (application: 'des' | 'console'): GroupeNavigation[] => [{ entrees: [ACCUEIL[application]] }]
