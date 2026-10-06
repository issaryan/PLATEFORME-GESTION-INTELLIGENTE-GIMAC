import { describe, expect, it } from 'vitest'
import { PERM, PROFILS } from './permissions'
import { FONCTIONNALITES } from './fonctionnalites'
import {
  CATALOGUE_DROITS, DROITS_REGISTRE, NATURES_DROIT, basculerDroit, basculerFonctionnalite, catalogueDroits, comparerCompositions, etatFonctionnalite, permissionPrivilegiee,
  permissionsCatalogue, type FonctionnaliteDroits,
} from './droits'
import { catalogue } from './i18n/ecrans/droits'

// Catalogue des droits (D-90, docs/lot1/roles-fonctionnalites.md §1) : toute permission connue du frontend appartient
// à au moins un droit, chaque droit est libellé en FR, EN et ES ; cocher un droit d'écriture coche la consultation.
const fonctionnalite = (cle: string): FonctionnaliteDroits => CATALOGUE_DROITS.flatMap((m) => m.fonctionnalites).find((f) => f.cle === cle)!

describe('D-90 : catalogue Module > Fonctionnalité > Droit', () => {
  it('chaque permission connue du frontend (PERM, profils du domaine) appartient à au moins un droit', () => {
    const connues = new Set([...Object.values(PERM), ...Object.values(PROFILS).flat()].filter((p) => !p.startsWith('INTERNAL_')))
    const couvertes = permissionsCatalogue()
    expect([...connues].filter((p) => !couvertes.has(p))).toEqual([])
  })

  it('construit depuis le registre : chaque entrée à droits devient une fonctionnalité, dans son module', () => {
    const registre = FONCTIONNALITES.filter((f) => DROITS_REGISTRE[f.cle]?.length)
    const fonctionnalites = CATALOGUE_DROITS.flatMap((m) => m.fonctionnalites)
    for (const f of registre) expect(fonctionnalites.find((x) => x.cle === f.cle)?.registre).toBe(f)
    expect(fonctionnalites.some((f) => f.cle === 'tableauDeBord')).toBe(false)
    expect(CATALOGUE_DROITS.map((m) => m.cle)).toEqual(['accueil', 'messagerie', 'monActivite', 'processus', 'participants', 'homologation', 'certification', 'portail', 'administration', 'exploitation'])
    expect(fonctionnalite('institutions').registre?.groupe).toBe('registre')
    expect(CATALOGUE_DROITS.find((m) => m.cle === 'participants')!.fonctionnalites.map((f) => f.cle)).toEqual(['institutions'])
    // Un registre réduit ne garde que ses modules et les écrans hors barre latérale.
    expect(catalogueDroits([]).map((m) => m.cle)).toEqual(['certification', 'portail'])
  })

  it('libellés FR, EN, ES de chaque module, fonctionnalité hors registre, nature et permission', () => {
    for (const l of ['fr', 'en', 'es'] as const) {
      const c = catalogue[l] as Record<string, string>
      for (const m of CATALOGUE_DROITS) {
        expect(c[m.libelleKey], `${l} ${m.cle}`).toBeTruthy()
        for (const f of m.fonctionnalites) {
          if (!f.registre) expect(c[f.libelleKey], `${l} ${f.cle}`).toBeTruthy()
          for (const d of f.droits) expect(c[`droits.permission.${d.permission}`], `${l} ${d.permission}`).toBeTruthy()
        }
      }
      for (const n of NATURES_DROIT) expect(c[`droits.nature.${n}`]).toBeTruthy()
      expect(Object.values(c).some((v) => v.includes(String.fromCharCode(0x2014)))).toBe(false)
    }
    expect(Object.keys(catalogue.en!).sort()).toEqual(Object.keys(catalogue.fr!).sort())
    expect(Object.keys(catalogue.es!).sort()).toEqual(Object.keys(catalogue.fr!).sort())
  })

  it('un droit d’écriture coché coche la consultation ; la consultation décochée retire les écritures', () => {
    const r = fonctionnalite('referentiels')
    const publier = r.droits.find((d) => d.nature === 'PUBLIER')!
    const p = basculerDroit([], r, publier, true)
    expect(p).toEqual(['HOM_REFERENTIEL_PUBLISH', 'HOM_REFERENTIEL_READ'])
    expect(etatFonctionnalite(r, p)).toBe('partiel')
    const lire = r.droits.find((d) => d.nature === 'CONSULTER')!
    expect(basculerDroit(p, r, lire, false)).toEqual([])
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
    expect(permissionPrivilegiee('PLATFORM_OPS_READ')).toBe(true)
    expect(permissionPrivilegiee('HOM_LIAISON_LIST')).toBe(false)
  })
})
