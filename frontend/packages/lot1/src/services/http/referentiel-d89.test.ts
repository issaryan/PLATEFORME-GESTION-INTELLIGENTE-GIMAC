import { afterEach, describe, expect, it, vi } from 'vitest'
import { homologationHttp } from './homologation'

// Contrats HTTP de D-89 (docs/lot1/referentiel-droits-etapes.md §4) : étapes configurables, catalogue de la version,
// lecture filtrée (X-Gimac-Filtre) et export CSV ; chemins, méthodes et corps exacts.
type Appel = [URL, RequestInit]
afterEach(() => vi.restoreAllMocks())
function brancher(...reponses: Response[]) {
  return vi.spyOn(globalThis, 'fetch').mockImplementation(async () => reponses.shift() ?? new Response('{}', { headers: { 'Content-Type': 'application/json' } }))
}
const json = (corps: unknown, status = 200, entetes: Record<string, string> = {}) => new Response(JSON.stringify(corps), { status, headers: { 'Content-Type': 'application/json', ...entetes } })
const appels = (f: ReturnType<typeof brancher>) => (f.mock.calls as unknown as Appel[]).map(([u, o]) => `${o.method} ${new URL(String(u)).pathname}${new URL(String(u)).search}`)
const corps = (f: ReturnType<typeof brancher>, i: number) => JSON.parse(String((f.mock.calls as unknown as Appel[])[i][1].body ?? 'null'))

describe('homologationHttp : référentiel D-89', () => {
  it('étapes : ajout, modification, ordre, retrait ; catalogue de la version ; liste filtrée ; export', async () => {
    const f = brancher(
      json({ code: 'RECETTE' }, 201), json({ code: 'RECETTE' }), json([{ code: 'A' }]), new Response(null, { status: 204 }), json({ etapesVersion: [] }),
      json([{ code: '3' }], 200, { 'X-Gimac-Filtre': 'CONCERNE' }), json([], 200, { 'X-Gimac-Filtre': 'COMPLET' }), json([]),
      new Response('code;libelle\n', { headers: { 'Content-Type': 'text/csv' } }),
    )
    const R = '/bff/des/homologation/referentiels'
    await homologationHttp.ajouterEtape('R1', { code: 'RECETTE', libelle: 'Recette', couleur: 'ACCENT', canonique: null, ordre: 8 })
    await homologationHttp.modifierEtape('R1', 'RECETTE', { libelle: 'Recette interne', couleur: 'INFO', canonique: 'TESTS' })
    await homologationHttp.ordonnerEtapes('R1', ['A', 'B'])
    expect(await homologationHttp.supprimerEtape('R1', 'RECETTE')).toBeUndefined()
    await homologationHttp.catalogueReferentiels('R1')
    expect(await homologationHttp.operationsReferentiel('R1')).toEqual({ operations: [{ code: '3' }], filtre: 'CONCERNE' })
    expect((await homologationHttp.operationsReferentiel('R1')).filtre).toBe('COMPLET')
    expect((await homologationHttp.operationsReferentiel('R1')).filtre).toBe('COMPLET')
    const e = await homologationHttp.exporterReferentiel('R1')
    expect(await e.blob.text()).toBe('code;libelle\n')
    expect(e.nom).toBe('referentiel-R1.csv')
    expect(appels(f)).toEqual([
      `POST ${R}/R1/etapes`, `PUT ${R}/R1/etapes/RECETTE`, `PUT ${R}/R1/etapes/ordre`, `DELETE ${R}/R1/etapes/RECETTE`, `GET ${R}/catalogue?version=R1`,
      `GET ${R}/R1/operations`, `GET ${R}/R1/operations`, `GET ${R}/R1/operations`, `GET ${R}/R1/export`,
    ])
    expect(corps(f, 0)).toEqual({ code: 'RECETTE', libelle: 'Recette', couleur: 'ACCENT', canonique: null, ordre: 8 })
    expect(corps(f, 1)).toEqual({ libelle: 'Recette interne', couleur: 'INFO', canonique: 'TESTS' })
    expect(corps(f, 2)).toEqual({ codes: ['A', 'B'] })
    expect((f.mock.calls as unknown as Appel[])[8][1].headers).toBeInstanceOf(Headers)
    expect(((f.mock.calls as unknown as Appel[])[8][1].headers as Headers).get('Accept')).toBe('text/csv')
  })
})
