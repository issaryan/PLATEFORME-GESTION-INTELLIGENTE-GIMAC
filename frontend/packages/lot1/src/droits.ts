import type { IconName } from '@gimac/design-system'
import { FONCTIONNALITES, type Fonctionnalite } from './fonctionnalites'

// Rôles composés par fonctionnalité (D-90, docs/lot1/roles-fonctionnalites.md) : catalogue Module > Fonctionnalité >
// Droit construit depuis le registre des fonctionnalités (D-79), chaque entrée de la barre latérale étant complétée
// de ses droits ; quelques écrans hors barre latérale (dossier de certification, attestations, portail) y sont
// ajoutés. Un droit est une action (consulter, créer, modifier, valider, signer, publier, exporter, administrer)
// reliée à une permission. Le catalogue est local au frontend : MS-IAM ne sert pas encore GET
// /configuration/fonctionnalites. Composer un profil par fonctionnalité n'envoie que des permissions (route D-62).

export const NATURES_DROIT = ['CONSULTER', 'CREER', 'MODIFIER', 'VALIDER', 'SIGNER', 'PUBLIER', 'EXPORTER', 'ADMINISTRER'] as const
export type NatureDroit = (typeof NATURES_DROIT)[number]

/** Droit : une action sur une fonctionnalité, reliée à une permission du domaine. */
export type Droit = { permission: string; nature: NatureDroit }

export type FonctionnaliteDroits = {
  cle: string
  libelleKey: string
  icone: IconName
  /** Entrée de la barre latérale (registre D-79) ; absent : écran ou geste hors barre latérale. */
  registre?: Fonctionnalite
  droits: Droit[]
}

export type ModuleDroits = { cle: string; libelleKey: string; fonctionnalites: FonctionnaliteDroits[] }

const d = (nature: NatureDroit, ...permissions: string[]): Droit[] => permissions.map((permission) => ({ permission, nature }))

/** Droits de chaque entrée du registre des fonctionnalités, par clé. */
export const DROITS_REGISTRE: Record<string, Droit[]> = {
  messagerie: [...d('CONSULTER', 'COL_MESSAGE_READ'), ...d('CREER', 'COL_MESSAGE_SEND')],
  notifications: d('CONSULTER', 'NOT_INBOX_READ'),
  maJournee: d('CONSULTER', 'PIL_ACTIVITY_OWN'),
  agenda: d('CONSULTER', 'PIL_ACTIVITY_OWN'),
  taches: [...d('CONSULTER', 'PIL_ACTIVITY_OWN'), ...d('CREER', 'PIL_TASK_ASSIGN')],
  rapports: d('CONSULTER', 'PIL_ACTIVITY_OWN'),
  equipe: [...d('CONSULTER', 'PIL_TEAM_READ'), ...d('CREER', 'PIL_TASK_ASSIGN')],
  processus: d('CONSULTER', 'PIL_ACTIVITY_OWN'),
  institutions: [
    ...d('CONSULTER', 'PAR_INSTITUTION_READ'), ...d('CREER', 'PAR_INSTITUTION_CREATE', 'PAR_TRANSITION_REQUEST'), ...d('MODIFIER', 'PAR_INSTITUTION_EDIT'),
    ...d('VALIDER', 'PAR_ADHESION_INSTRUCT', 'PAR_TRANSITION_APPROVE'),
  ],
  liaisons: [
    ...d('CONSULTER', 'HOM_LIAISON_LIST', 'HOM_LIAISON_READ', 'HOM_DASHBOARD_READ'), ...d('CREER', 'HOM_LIAISON_OPEN', 'HOM_EVIDENCE_SUBMIT'),
    ...d('MODIFIER', 'HOM_PROJECT_DESIGNATE', 'HOM_OPERATION_ASSIGN', 'HOM_OPERATION_REOPEN', 'HOM_STEP_MANAGE', 'HOM_GRID_EDIT'),
    ...d('VALIDER', 'HOM_OPERATION_VALIDATE', 'HOM_SCENARIO_QUALIFY'), ...d('EXPORTER', 'HOM_EVIDENCE_FILE_EXPORT'),
    ...d('ADMINISTRER', 'HOM_LIAISON_UNFREEZE', 'HOM_DEADLINE_CONFIG', 'HOM_REPLAY_ADMIN'),
  ],
  mesProjets: [...d('CONSULTER', 'HOM_LIAISON_LIST'), ...d('CREER', 'CER_PV_ESTABLISH')],
  referentiels: [...d('CONSULTER', 'HOM_REFERENTIEL_READ'), ...d('MODIFIER', 'HOM_REFERENTIEL_EDIT'), ...d('PUBLIER', 'HOM_REFERENTIEL_PUBLISH'), ...d('ADMINISTRER', 'HOM_REFERENTIEL_MIGRATE')],
  adminComptes: [
    ...d('CONSULTER', 'IAM_ACCOUNT_READ'), ...d('CREER', 'IAM_ACCOUNT_CREATE'), ...d('MODIFIER', 'IAM_ACCOUNT_EDIT'),
    ...d('ADMINISTRER', 'IAM_ACCOUNT_ROLES', 'IAM_ACCOUNT_STATUS', 'IAM_ACCOUNT_SECURITY', 'IAM_ACCOUNT_DELETE'),
  ],
  adminDemandes: d('CONSULTER', 'IAM_ACCOUNT_READ'),
  adminProfils: [...d('CONSULTER', 'IAM_ACCOUNT_READ'), ...d('MODIFIER', 'IAM_PROFILE_EDIT'), ...d('ADMINISTRER', 'IAM_ROLE_PRIVILEGED_GRANT')],
  adminMatrice: d('MODIFIER', 'IAM_PROFILE_EDIT'),
  adminOrganisation: [...d('CONSULTER', 'IAM_ACCOUNT_READ'), ...d('MODIFIER', 'IAM_ACCOUNT_EDIT')],
  adminOrganigramme: [...d('CONSULTER', 'IAM_ACCOUNT_READ'), ...d('MODIFIER', 'IAM_ACCOUNT_EDIT')],
  adminJournal: d('CONSULTER', 'IAM_JOURNAL_READ'),
  adminPolitique: [...d('CONSULTER', 'PLATFORM_SECURITY_POLICY_READ'), ...d('MODIFIER', 'PLATFORM_SECURITY_POLICY_EDIT')],
  supervision: d('CONSULTER', 'PLATFORM_SUPERVISION_READ'),
  infrastructure: [...d('CONSULTER', 'PLATFORM_OPS_READ'), ...d('ADMINISTRER', 'PLATFORM_OPS_EXECUTE')],
  configuration: [...d('CONSULTER', 'PLATFORM_CONFIG_READ'), ...d('MODIFIER', 'PLATFORM_CONFIG_EDIT')],
  journalisation: [...d('CONSULTER', 'PLATFORM_CONFIG_READ'), ...d('MODIFIER', 'PLATFORM_CONFIG_EDIT')],
  courriels: d('MODIFIER', 'PLATFORM_MESSAGING_EDIT'),
  adminConsole: d('CONSULTER', 'PLATFORM_CONFIG_READ'),
}

/** Module d'une entrée du registre (par défaut : son groupe de la barre latérale). */
const MODULE_DE: Record<string, string> = {
  messagerie: 'messagerie', processus: 'processus', institutions: 'participants', registre: 'participants', plateforme: 'exploitation',
}

/** Écrans et gestes hors barre latérale, rangés dans leur module. */
const HORS_REGISTRE: (Omit<FonctionnaliteDroits, 'registre'> & { module: string })[] = [
  {
    module: 'certification', cle: 'dossierCertification', libelleKey: 'droits.fct.dossierCertification', icone: 'shield-check',
    droits: [
      ...d('CONSULTER', 'CER_DOSSIER_READ', 'CER_CIRCUIT_READ'), ...d('CREER', 'CER_GOLIVE_REPORT_SUBMIT'), ...d('MODIFIER', 'CER_CHANGE_RECORD_LINK'),
      ...d('VALIDER', 'CER_DOSSIER_VERIFY', 'CER_SECURITY_VERIFY'),
    ],
  },
  {
    module: 'certification', cle: 'quitus', libelleKey: 'droits.fct.quitus', icone: 'key-round',
    droits: [...d('CONSULTER', 'CER_DOSSIER_READ'), ...d('SIGNER', 'CER_QUITUS_TECHNIQUE_SIGN', 'CER_QUITUS_SECURITE_SIGN', 'CER_QUITUS_CONFORMITE_SIGN', 'CER_QUITUS_DG_SIGN')],
  },
  {
    module: 'certification', cle: 'attestations', libelleKey: 'droits.fct.attestations', icone: 'file-text',
    droits: [...d('CONSULTER', 'CER_REGISTER_READ'), ...d('CREER', 'CER_ATTESTATION_ISSUE', 'CER_ATTESTATION_STATUS_REQUEST'), ...d('VALIDER', 'CER_ATTESTATION_STATUS_APPROVE')],
  },
  { module: 'certification', cle: 'registre', libelleKey: 'droits.fct.registre', icone: 'scroll-text', droits: [...d('CONSULTER', 'CER_REGISTER_READ'), ...d('EXPORTER', 'CER_REGISTER_EXPORT')] },
  {
    module: 'portail', cle: 'portail', libelleKey: 'droits.fct.portail', icone: 'globe',
    droits: [...d('CONSULTER', 'HOM_PORTAL_READ', 'CER_PORTAL_READ'), ...d('CREER', 'HOM_PORTAL_SUBMIT'), ...d('SIGNER', 'CER_PORTAL_SIGN')],
  },
]

/** Modules, dans l'ordre d'affichage. */
export const MODULES_DROITS = ['accueil', 'messagerie', 'monActivite', 'processus', 'participants', 'homologation', 'certification', 'portail', 'administration', 'exploitation'] as const

/** Catalogue Module > Fonctionnalité > Droit, dérivé du registre (les entrées sans droit, comme le tableau de bord, en sont absentes). */
export function catalogueDroits(registre: Fonctionnalite[] = FONCTIONNALITES): ModuleDroits[] {
  const fonctionnalites: (FonctionnaliteDroits & { module: string })[] = [
    ...registre.filter((f) => DROITS_REGISTRE[f.cle]?.length).map((f) => ({
      module: MODULE_DE[f.cle] ?? MODULE_DE[f.groupe] ?? f.groupe, cle: f.cle, libelleKey: f.libelleKey, icone: f.icone, registre: f, droits: DROITS_REGISTRE[f.cle],
    })),
    ...HORS_REGISTRE,
  ]
  return MODULES_DROITS.map((m) => ({
    cle: m, libelleKey: `droits.module.${m}`,
    fonctionnalites: fonctionnalites.filter((f) => f.module === m).map(({ module: _m, ...f }) => f),
  })).filter((m) => m.fonctionnalites.length > 0)
}

export const CATALOGUE_DROITS: ModuleDroits[] = catalogueDroits()

/** Permissions couvertes par au moins un droit du catalogue. */
export function permissionsCatalogue(catalogue: ModuleDroits[] = CATALOGUE_DROITS): Set<string> {
  return new Set(catalogue.flatMap((m) => m.fonctionnalites.flatMap((f) => f.droits.map((x) => x.permission))))
}

/** Permission privilégiée (D-81) : un droit qui la porte rend le profil privilégié. */
export const permissionPrivilegiee = (p: string) => /^(IAM|PLATFORM)_/.test(p)

/** État d'une fonctionnalité pour un ensemble de permissions : tous ses droits, une partie, ou aucun. */
export function etatFonctionnalite(f: FonctionnaliteDroits, permissions: readonly string[]): 'tout' | 'partiel' | 'aucun' {
  const n = f.droits.filter((x) => permissions.includes(x.permission)).length
  return n === 0 ? 'aucun' : n === f.droits.length ? 'tout' : 'partiel'
}

const consultations = (f: FonctionnaliteDroits) => f.droits.filter((x) => x.nature === 'CONSULTER').map((x) => x.permission)
const ecritures = (f: FonctionnaliteDroits) => f.droits.filter((x) => x.nature !== 'CONSULTER').map((x) => x.permission)
const trier = (s: Set<string>) => [...s].sort()

/**
 * Coche ou décoche un droit : un droit d'écriture coché coche la consultation de sa fonctionnalité ; la consultation
 * décochée retire les droits d'écriture de la fonctionnalité.
 */
export function basculerDroit(permissions: readonly string[], f: FonctionnaliteDroits, droit: Droit, coche: boolean): string[] {
  const s = new Set(permissions)
  if (coche) {
    s.add(droit.permission)
    if (droit.nature !== 'CONSULTER') consultations(f).forEach((p) => s.add(p))
  } else {
    s.delete(droit.permission)
    if (droit.nature === 'CONSULTER') ecritures(f).forEach((p) => s.delete(p))
  }
  return trier(s)
}

/** Coche une fonctionnalité (sa consultation, ou son premier droit s'il n'en a pas) ou la décoche entièrement. */
export function basculerFonctionnalite(permissions: readonly string[], f: FonctionnaliteDroits, coche: boolean): string[] {
  const s = new Set(permissions)
  if (coche) {
    const c = consultations(f)
    ;(c.length ? c : [f.droits[0].permission]).forEach((p) => s.add(p))
  } else f.droits.forEach((x) => s.delete(x.permission))
  return trier(s)
}

/** Différences de droits entre deux compositions : permissions du catalogue présentes dans l'une seulement. */
export function comparerCompositions(a: readonly string[], b: readonly string[]): { seulementA: string[]; seulementB: string[]; communes: string[] } {
  const A = new Set(a)
  const B = new Set(b)
  return { seulementA: trier(new Set([...A].filter((p) => !B.has(p)))), seulementB: trier(new Set([...B].filter((p) => !A.has(p)))), communes: trier(new Set([...A].filter((p) => B.has(p)))) }
}
