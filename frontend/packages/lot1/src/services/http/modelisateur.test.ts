import { afterEach, describe, expect, it, vi } from 'vitest'
import { homologationHttp } from './homologation'

// Contrats HTTP du modélisateur (D-77, §4) : chemins de la passerelle /bff/des/homologation/referentiels/**,
// méthodes et corps exacts ; pilote facultatif à la création d'un brouillon.
type Appel = [URL, RequestInit]
afterEach(() => vi.restoreAllMocks())
function brancher(...reponses: { status?: number; corps?: unknown }[]) {
  return vi.spyOn(globalThis, 'fetch').mockImplementation(async () => {
    const r = reponses.shift() ?? {}
    return r.status === 204 ? new Response(null, { status: 204 }) : new Response(JSON.stringify(r.corps ?? {}), { status: r.status ?? 200, headers: { 'Content-Type': 'application/json' } })
  })
}
const lignes = (f: ReturnType<typeof brancher>) => (f.mock.calls as unknown as Appel[]).map(([u, o]) => `${o.method} ${new URL(String(u)).pathname}`)
const corps = (f: ReturnType<typeof brancher>, i: number) => JSON.parse(String((f.mock.calls as unknown as Appel[])[i][1].body ?? 'null'))

describe('homologationHttp : modélisateur de processus', () => {
  it('graphe, catalogue, opérations, positions, arcs, pilote, brouillon', async () => {
    const f = brancher({ corps: { noeuds: [] } }, { corps: { pilotes: [] } }, { status: 201, corps: { code: 'X1' } }, { corps: { code: 'X 1' } }, { status: 204 }, { status: 204 }, { corps: { code: 'B' } },
      { corps: { piloteId: 'u-2' } }, { status: 201, corps: { id: 'v' } }, { status: 201, corps: { id: 'w' } })
    const saisie = { libelle: 'L', predecesseurs: ['A'] } as never
    expect(await homologationHttp.grapheReferentiel('R1')).toEqual({ noeuds: [] })
    await homologationHttp.catalogueReferentiels()
    expect(await homologationHttp.creerOperation('R1', { ...(saisie as object), code: 'X1' } as never)).toEqual({ code: 'X1' })
    await homologationHttp.enregistrerOperation('R1', 'X 1', saisie)
    expect(await homologationHttp.supprimerOperation('R1', 'X1')).toBeUndefined()
    await homologationHttp.placerOperations('R1', [{ code: 'A', x: 1, y: 2 }])
    await homologationHttp.arcReferentiel('R1', { de: 'A', vers: 'B', present: true })
    expect(await homologationHttp.designerPilote('R1', 'u-2')).toEqual({ piloteId: 'u-2' })
    await homologationHttp.creerBrouillon({ typeProjet: 'DELEGATAIRE', libelle: 'D', piloteId: 'u-2' })
    await homologationHttp.creerBrouillon({ typeProjet: 'DELEGATAIRE', libelle: 'D', piloteId: '' })
    const R = '/bff/des/homologation/referentiels'
    expect(lignes(f)).toEqual([
      `GET ${R}/R1/graphe`, `GET ${R}/catalogue`, `POST ${R}/R1/operations`, `PUT ${R}/R1/operations/X%201`, `DELETE ${R}/R1/operations/X1`, `PUT ${R}/R1/positions`,
      `PUT ${R}/R1/arcs`, `PUT ${R}/R1/pilote`, `POST ${R}`, `POST ${R}`,
    ])
    expect(corps(f, 2)).toEqual({ libelle: 'L', predecesseurs: ['A'], code: 'X1' })
    expect(corps(f, 3)).toEqual({ libelle: 'L', predecesseurs: ['A'] })
    expect(corps(f, 5)).toEqual([{ code: 'A', x: 1, y: 2 }])
    expect(corps(f, 6)).toEqual({ de: 'A', vers: 'B', present: true })
    expect(corps(f, 7)).toEqual({ compteId: 'u-2' })
    expect(corps(f, 8)).toEqual({ typeProjet: 'DELEGATAIRE', libelle: 'D', piloteId: 'u-2' })
    expect(corps(f, 9)).toEqual({ typeProjet: 'DELEGATAIRE', libelle: 'D' })
  })
  it('refus du service normalisé (409 HOM_REFERENTIAL_CYCLE)', async () => {
    brancher({ status: 409, corps: { code: 'HOM_REFERENTIAL_CYCLE', message: 'Cycle', correlationId: 'c-9' } })
    await expect(homologationHttp.arcReferentiel('R1', { de: 'B', vers: 'A', present: true })).rejects.toMatchObject({ status: 409, code: 'HOM_REFERENTIAL_CYCLE' })
  })
})
