import table from './services-perimetre.json'
import type { Liaison } from '../modele/homologation'

// Rattachement des services du périmètre (D-133) : code → interface → famille → nom usuel, lu dans le seul fichier
// versionné services-perimetre.json, dont chaque ligne cite sa source. Un code absent de la table est rangé dans
// « Autres services » sous son code, jamais dans une famille inventée.

export type Langue = 'fr' | 'en' | 'es'
type Libelle = Record<Langue, string>
export type ServiceDecrit = { code: string; interface: string | null; famille: string; libelle: string; connu: boolean }
export type FamilleServices = { code: string; libelle: string; services: ServiceDecrit[] }
export type InterfaceServices = { code: string | null; libelle: string; familles: FamilleServices[] }

type Ligne = { code: string; interface: string | null; famille: string; source: string; libelle: Libelle }
export const TABLE = table as { sources: Record<string, string>; interfaces: { code: string; source: string; libelle: Libelle }[]; familles: { code: string; source: string; libelle: Libelle }[]; services: Ligne[] }

export const AUTRES = 'AUTRES'
export const TRANSVERSE = 'TRANSVERSE'

/** Interfaces d'un type de projet (TypeReferentiel de SBX-REFERENTIEL) ; autre type : toutes. */
export function interfacesDuType(type: Liaison['typeProjet'] | string): string[] {
  if (type === 'CARTE_INTERBANCAIRE') return ['CARTE_ISO8583']
  if (type === 'PAIEMENT_MOBILE') return ['MOBILE_PROPRIETAIRE', 'MOBILE_ISO20022']
  return TABLE.interfaces.map((i) => i.code)
}

const langue = (l: string): Langue => (l.startsWith('en') ? 'en' : l.startsWith('es') ? 'es' : 'fr')

/** Description d'un code pour un type de projet : ligne de l'interface du type d'abord, puis toute ligne du code. */
export function decrire(code: string, type: string, l: string): ServiceDecrit {
  const interfaces = interfacesDuType(type)
  const ligne = TABLE.services.find((s) => s.code === code && (s.interface === null || interfaces.includes(s.interface))) ?? TABLE.services.find((s) => s.code === code)
  if (!ligne) return { code, interface: null, famille: AUTRES, libelle: code, connu: false }
  return { code, interface: ligne.interface, famille: ligne.famille, libelle: ligne.libelle[langue(l)], connu: true }
}

/** Services rangés par interface puis par famille, dans l'ordre de la table ; un code inconnu va dans « Autres services ». */
export function grouper(codes: readonly string[], type: string, l: string): InterfaceServices[] {
  const lg = langue(l)
  const decrits = codes.map((c) => decrire(c, type, l))
  const ordreInterfaces: (string | null)[] = [...TABLE.interfaces.map((i) => i.code), null]
  return ordreInterfaces
    .map((ic) => {
      const ici = decrits.filter((d) => d.interface === ic)
      const familles = TABLE.familles
        .map((f) => ({ code: f.code, libelle: f.libelle[lg], services: ici.filter((d) => d.famille === f.code).sort((a, b) => a.libelle.localeCompare(b.libelle)) }))
        .filter((f) => f.services.length > 0)
      const i = TABLE.interfaces.find((x) => x.code === ic)
      return { code: ic, libelle: i ? i.libelle[lg] : TABLE.familles.find((f) => f.code === AUTRES)!.libelle[lg], familles }
    })
    .filter((i) => i.familles.length > 0)
}
