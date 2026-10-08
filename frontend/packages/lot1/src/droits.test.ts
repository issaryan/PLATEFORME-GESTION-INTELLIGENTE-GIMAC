import { describe, expect, it } from 'vitest'
import { PERM, PROFILS } from './permissions'
import {
  NATURES_DROIT, basculerDroit, basculerFonctionnalite, catalogueDroits, comparerCompositions, etatFonctionnalite, permissionPrivilegiee, permissionsCatalogue,
  type FonctionnaliteDroits,
} from './droits'
import { FONCTIONNALITES_INITIALES as CATALOGUE } from './services/simule/fonctionnalites'
import { catalogue } from './i18n/ecrans/droits'
import { catalogue as d94 } from './i18n/ecrans/d91-d94'

// Catalogue des droits (D-90) dérivé du catalogue des fonctionnalités servi par MS-IAM (D-94) : toute permission connue
// du frontend appartient à au moins un droit ; cocher un droit d'écriture coche la consultation.
const CATALOGUE_DROITS = catalogueDroits(CATALOGUE)
const fonctionnalite = (cle: string): FonctionnaliteDroits => CATALOGUE_DROITS.flatMap((m) => m.fonctionnalites).find((f) => f.cle === cle)!
describe('D-90 : catalogue Module > Fonctionnalité > Droit', () => {
  it('chaque permission connue du frontend (PERM, profils du domaine) appartient à au moins un droit', () => {
    const connues = new Set([...Object.values(PERM), ...Object.values(PROFILS).flat()].filter((p) => !p.startsWith('INTERNAL_')))
    const couvertes = permissionsCatalogue(CATALOGUE_DROITS)
    expect([...connues].filter((p) => !couvertes.has(p))).toEqual([])
    // Conduite d'un projet et fonctions opérationnelles (D-94 §6.5, D-97) : droits de la fonctionnalité des liaisons.
    const fonctions: string[] = [PERM.projetConduire, PERM.fonctionDdrm, PERM.fonctionProduction, PERM.fonctionReseau, PERM.fonctionSysteme, PERM.fonctionExploitation]
    expect(fonctionnalite('liaisons').droits.filter((d) => fonctions.includes(d.permission))).toEqual(
      fonctions.map((p) => ({ permission: p, nature: 'MODIFIER', libelle: `droits.permission.${p}` })),
    )
    for (const p of fonctions) expect((d94.fr as Record<string, string>)[`droits.permission.${p}`]).toBeTruthy()
  })

  it('modules dans l’ordre de leur première fonctionnalité ; fonctionnalités sans droit absentes ; référentiels métier', () => {
    expect(CATALOGUE_DROITS.map((m) => m.cle)).toEqual(['messagerie', 'accueil', 'monActivite', 'processus', 'participants', 'homologation', 'administration', 'exploitation', 'certification', 'referentielsMetier', 'portail'])
    expect(CATALOGUE_DROITS.flatMap((m) => m.fonctionnalites).some((f) => f.cle === 'tableauDeBord')).toBe(false)
    expect(fonctionnalite('referentielsMetierAdmin').droits).toEqual([{ permission: 'REF_METIER_ADMIN', nature: 'ADMINISTRER', libelle: 'Administration des référentiels métier, tous domaines' }])
    expect(catalogueDroits([])).toEqual([])
  })

  it('libellés FR, EN, ES de chaque module et nature ; aucun tiret cadratin', () => {
    for (const l of ['fr', 'en', 'es'] as const) {
      const c = { ...(catalogue[l] as Record<string, string>), ...(d94[l] as Record<string, string>) }
      for (const m of CATALOGUE_DROITS) expect(c[m.libelleKey], `${l} ${m.cle}`).toBeTruthy()
      for (const n of NATURES_DROIT) expect(c[`droits.nature.${n}`]).toBeTruthy()
      expect(Object.values(c).some((v) => v.includes(String.fromCharCode(0x2014)))).toBe(false)
    }
    expect(Object.keys(d94.en!).sort()).toEqual(Object.keys(d94.fr!).sort())
    expect(Object.keys(d94.es!).sort()).toEqual(Object.keys(d94.fr!).sort())
  })

  it('un droit d’écriture coché coche la consultation ; la consultation décochée retire les écritures', () => {
    const r = fonctionnalite('referentiels')
    const publier = r.droits.find((d) => d.nature === 'PUBLIER')!
    const p = basculerDroit([], r, publier, true)
    expect(p).toEqual(['HOM_REFERENTIEL_PUBLISH', 'HOM_REFERENTIEL_READ', 'HOM_REFERENTIEL_READ_ALL'])
    expect(etatFonctionnalite(r, p)).toBe('partiel')
    const lire = r.droits.find((d) => d.nature === 'CONSULTER')!
    expect(basculerDroit(p, r, lire, false)).toEqual(['HOM_REFERENTIEL_READ_ALL'])
    expect(basculerDroit(['X'], r, publier, false)).toEqual(['X'])
    expect(basculerDroit([], r, lire, true)).toEqual(['HOM_REFERENTIEL_READ'])
    expect(etatFonctionnalite(r, r.droits.map((d) => d.permission))).toBe('tout')
    expect(etatFonctionnalite(r, [])).toBe('aucun')
  })

  it('cocher une fonctionnalité coche sa consultation (ou son premier droit) ; la décocher retire tous ses droits', () => {
    expect(basculerFonctionnalite([], fonctionnalite('liaisons'), true)).toEqual(['HOM_DASHBOARD_READ', 'HOM_LIAISON_LIST', 'HOM_LIAISON_READ'])
    expect(basculerFonctionnalite([], fonctionnalite('courriels'), true)).toEqual(['PLATFORM_MESSAGING_EDIT'])
    expect(basculerFonctionnalite(['HOM_REFERENTIEL_READ', 'HOM_REFERENTIEL_EDIT', 'Z'], fonctionnalite('referentiels'), false)).toEqual(['Z'])
  })

  it('comparaison de deux compositions et droits privilégiés', () => {
    expect(comparerCompositions(['A', 'B'], ['B', 'C'])).toEqual({ seulementA: ['A'], seulementB: ['C'], communes: ['B'] })
    expect(permissionPrivilegiee('IAM_ACCOUNT_READ')).toBe(true)
    expect(permissionPrivilegiee('REF_METIER_ADMIN')).toBe(false)
  })
})
