import { describe, expect, it } from 'vitest'
import { AUTRES, decrire, grouper, interfacesDuType, TABLE } from './services-perimetre'
import { PERIMETRES } from '../services/simule/donnees'
import { SOUSCRITS } from '../services/simule/perimetre'

// D-133 : table de rattachement code → interface → famille → nom usuel. Le test échoue si un service du périmètre
// (catalogue de MS-HOMOLOGATION d'après l'annexe A du CR, services simulés) n'y a pas de libellé, si une ligne ne cite
// pas sa source, ou si une famille n'est pas celle du TDR.
// Catalogue de MS-HOMOLOGATION lu tel quel (Vite, ?raw) : la table suit le code du service, jamais une recopie.
const CATALOGUE_HOMOLOGATION = Object.values(
  import.meta.glob<string>('../../../../../ms-homologation/src/main/java/cm/gimac/des/homologation/domain/parcours/CatalogueServices.java', { query: '?raw', import: 'default', eager: true }),
)[0]

describe('table des services du périmètre', () => {
  it('chaque ligne cite une source connue et porte ses libellés en fr, en et es', () => {
    for (const l of [...TABLE.interfaces, ...TABLE.familles, ...TABLE.services]) {
      expect(TABLE.sources[l.source], l.code).toBeTruthy()
      for (const lg of ['fr', 'en', 'es'] as const) expect(l.libelle[lg]?.trim(), `${l.code} ${lg}`).toBeTruthy()
    }
    const cles = TABLE.services.map((s) => `${s.interface}:${s.code}`)
    expect(new Set(cles).size).toBe(cles.length)
    for (const s of TABLE.services) {
      expect(TABLE.familles.map((f) => f.code)).toContain(s.famille)
      if (s.interface) expect(TABLE.interfaces.map((i) => i.code)).toContain(s.interface)
    }
    // Familles du tableau « Famille de services » du TDR seulement, plus « Autres services ».
    expect(TABLE.familles.filter((f) => f.code !== AUTRES).every((f) => f.source === 'TDR-FAMILLES')).toBe(true)
  })
  it('tout service du périmètre a un libellé', () => {
    const java = CATALOGUE_HOMOLOGATION ?? ''
    const codes = [...java.matchAll(/Map\.entry\("([A-Z_]+)"/g)].map((m) => m[1]).filter((c) => !['CARTE_INTERBANCAIRE', 'PAIEMENT_MOBILE', 'DELEGATAIRE'].includes(c))
    expect(codes.length).toBeGreaterThan(5)
    const perimetres = Object.entries({ ...PERIMETRES }).flatMap(([type, cs]) => cs.map((c) => [type, c]))
    const souscrits = Object.entries(SOUSCRITS).flatMap(([type, cs]) => cs.map((c) => [type, c]))
    for (const c of codes) expect(decrire(c, 'GENERIQUE', 'fr').connu, c).toBe(true)
    for (const [type, c] of [...perimetres, ...souscrits]) expect(decrire(c, type, 'fr').connu, `${type} ${c}`).toBe(true)
  })
  it('interface du type de projet, puis famille ; code inconnu dans « Autres services »', () => {
    expect(interfacesDuType('CARTE_INTERBANCAIRE')).toEqual(['CARTE_ISO8583'])
    expect(interfacesDuType('DELEGATAIRE')).toHaveLength(3)
    expect(decrire('TRANSFERT', 'PAIEMENT_MOBILE', 'fr')).toMatchObject({ interface: 'MOBILE_PROPRIETAIRE', famille: 'TRANSFERTS_PAIEMENT_MARCHAND' })
    expect(decrire('TRANSFERT', 'CARTE_INTERBANCAIRE', 'en')).toMatchObject({ interface: 'CARTE_ISO8583', famille: AUTRES, libelle: 'Transfer' })
    expect(decrire('W2W', 'CARTE_INTERBANCAIRE', 'es').libelle).toBe('Transferencia de monedero a monedero')
    expect(decrire('INCONNU', 'PAIEMENT_MOBILE', 'fr')).toEqual({ code: 'INCONNU', interface: null, famille: AUTRES, libelle: 'INCONNU', connu: false })
    const g = grouper(['CWD', 'W2W', 'INCONNU', 'TRANSVERSE'], 'PAIEMENT_MOBILE', 'fr')
    expect(g.map((x) => x.code)).toEqual(['MOBILE_PROPRIETAIRE', null])
    expect(g[0].familles.map((f) => f.libelle)).toEqual(['Transferts et paiement marchand', 'Coupons (retrait sans carte, retrait d\'espèces, achat de bon, remises en espèces)'])
    expect(g[1]).toMatchObject({ libelle: 'Autres services' })
  })
})
