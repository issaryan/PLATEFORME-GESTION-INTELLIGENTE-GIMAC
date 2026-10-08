import type { IconName } from '@gimac/design-system'
import { NATURES_DROIT, trier, type Application, type DefinitionFonctionnalite, type NatureDroit } from './fonctionnalites'

// Rôles composés par fonctionnalité (D-90, docs/lot1/roles-fonctionnalites.md) : catalogue Module > Fonctionnalité >
// Droit dérivé du catalogue des fonctionnalités servi par MS-IAM (D-94) ; chaque droit est une action (consulter, créer,
// modifier, valider, signer, publier, exporter, administrer) reliée à une permission. Composer un profil par
// fonctionnalité n'envoie que des permissions (route D-62).

export { NATURES_DROIT, type NatureDroit }

/** Droit : une action sur une fonctionnalité, reliée à une permission du domaine. */
export type Droit = { permission: string; nature: NatureDroit; libelle?: string }

export type FonctionnaliteDroits = {
  cle: string
  /** Libellé servi : clé de traduction ou texte. */
  libelleKey: string
  icone: IconName
  application: Application
  droits: Droit[]
}

export type ModuleDroits = { cle: string; libelleKey: string; fonctionnalites: FonctionnaliteDroits[] }

/** Applications dont les droits se composent dans un profil (la navigation de la console ne porte pas de droits, §6.2). */
const ORDRE_APPLICATIONS: Application[] = ['des', 'portail', 'console']

/**
 * Catalogue Module > Fonctionnalité > Droit : fonctionnalités portant au moins un droit, modules dans l'ordre de leur
 * première fonctionnalité (application interne, puis portail, puis console ; ordre servi).
 */
export function catalogueDroits(catalogue: DefinitionFonctionnalite[]): ModuleDroits[] {
  const fonctionnalites = ORDRE_APPLICATIONS.flatMap((a) => trier(catalogue.filter((f) => f.application === a && f.droits.length > 0)))
  const modules: string[] = []
  fonctionnalites.forEach((f) => { if (!modules.includes(f.module)) modules.push(f.module) })
  return modules.map((m) => ({
    cle: m, libelleKey: `droits.module.${m}`,
    fonctionnalites: fonctionnalites.filter((f) => f.module === m).map((f) => ({
      cle: f.cle, libelleKey: f.libelle, icone: f.icone as IconName, application: f.application,
      droits: f.droits.map((d) => ({ permission: d.permission, nature: d.nature, libelle: d.libelle })),
    })),
  }))
}

/** Permissions couvertes par au moins un droit du catalogue. */
export function permissionsCatalogue(catalogue: ModuleDroits[]): Set<string> {
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
const ordonner = (s: Set<string>) => [...s].sort()

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
  return ordonner(s)
}

/** Coche une fonctionnalité (sa consultation, ou son premier droit s'il n'en a pas) ou la décoche entièrement. */
export function basculerFonctionnalite(permissions: readonly string[], f: FonctionnaliteDroits, coche: boolean): string[] {
  const s = new Set(permissions)
  if (coche) {
    const c = consultations(f)
    ;(c.length ? c : [f.droits[0].permission]).forEach((p) => s.add(p))
  } else f.droits.forEach((x) => s.delete(x.permission))
  return ordonner(s)
}

/** Différences de droits entre deux compositions : permissions présentes dans l'une seulement. */
export function comparerCompositions(a: readonly string[], b: readonly string[]): { seulementA: string[]; seulementB: string[]; communes: string[] } {
  const A = new Set(a)
  const B = new Set(b)
  return { seulementA: ordonner(new Set([...A].filter((p) => !B.has(p)))), seulementB: ordonner(new Set([...B].filter((p) => !A.has(p)))), communes: ordonner(new Set([...A].filter((p) => B.has(p)))) }
}
