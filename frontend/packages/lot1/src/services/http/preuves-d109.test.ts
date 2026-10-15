import { afterEach, describe, expect, it, vi } from 'vitest'
import { parcoursHttp, portailParcoursHttp } from './parcours'

// Contrats HTTP de D-109 : service visé d'un dépôt (DES et portail), livrable sans objet motivé, pièce téléchargée avec
// son empreinte (Repr-Digest), chronologie réelle de la liaison.
type Appel = [URL, RequestInit]
const appels = () => (globalThis.fetch as unknown as { mock: { calls: Appel[] } }).mock.calls
afterEach(() => vi.restoreAllMocks())
const ligne = (i: number) => {
  const [u, o] = appels()[i]
  const url = new URL(String(u))
  return `${o.method} ${url.pathname}${url.search}`
}
const corps = (i: number) => {
  const b = appels()[i][1].body
  return b instanceof FormData ? Object.fromEntries([...b.entries()].map(([k, v]) => [k, typeof v === 'string' ? v : `fichier:${(v as File).name}`])) : JSON.parse(String(b ?? 'null'))
}
function brancher(reponse: () => Response = () => new Response(JSON.stringify({ id: 'D1', sha256: 'x' }), { status: 200, headers: { 'Content-Type': 'application/json' } })) {
  vi.spyOn(globalThis, 'fetch').mockImplementation(async () => reponse())
}
const fichier = () => new File(['abc'], 'releve.csv', { type: 'text/csv' })

describe('D-109 : contrats HTTP des preuves', () => {
  it('dépôt d’une preuve et d’un livrable pour un service ; livrable sans objet motivé', async () => {
    brancher()
    await parcoursHttp.deposerPreuve('O1', fichier(), { livrable: 'L1', service: 'RETRAIT' })
    expect(ligne(1)).toBe('POST /bff/des/homologation/parcours/operations/O1/preuves')
    expect(corps(1)).toMatchObject({ documentId: 'D1', livrable: 'L1', service: 'RETRAIT', nom: 'releve.csv' })
    await parcoursHttp.deposerLivrable('O2', fichier(), 'relu', 'L1', 'TRANSFERT')
    expect(corps(3)).toMatchObject({ livrable: 'L1', service: 'TRANSFERT', commentaire: 'relu' })
    await parcoursHttp.deposerLivrable('O2', fichier(), 'relu')
    expect(corps(5)).not.toHaveProperty('service')
    await parcoursHttp.declarerSansObjet('O3', 'L 1', 'Participant non porté')
    expect(ligne(6)).toBe('POST /bff/des/homologation/parcours/operations/O3/livrables/L%201/sans-objet')
    expect(corps(6)).toEqual({ motif: 'Participant non porté' })
    await parcoursHttp.declarerSansObjet('O3', 'L1', 'Service non ouvert', 'RETRAIT')
    expect(corps(7)).toEqual({ motif: 'Service non ouvert', service: 'RETRAIT' })
  })
  it('portail : livrable et service transmis dans le formulaire multipart', async () => {
    brancher(() => new Response(null, { status: 204 }))
    await portailParcoursHttp.deposerLivrable('O4', fichier(), 'c', 'L2', 'RETRAIT')
    expect(ligne(0)).toBe('POST /bff/portail/homologation/operations/O4/livrable')
    expect(corps(0)).toEqual({ fichier: 'fichier:releve.csv', commentaire: 'c', livrable: 'L2', service: 'RETRAIT' })
    await portailParcoursHttp.deposerLivrable('O4', fichier(), 'c')
    expect(corps(1)).toEqual({ fichier: 'fichier:releve.csv', commentaire: 'c' })
  })
  it('pièce téléchargée avec son empreinte ; chronologie de la liaison', async () => {
    // sha-256 de « abc » en base 64 (RFC 9530).
    brancher(() => new Response('abc', { status: 200, headers: { 'Content-Type': 'image/png', 'Repr-Digest': 'sha-256=:ungWv48Bz+pBQUDeXa4iI7ADYaOWF3qctBD/YfIAFa0=:' } }))
    const p = await parcoursHttp.telechargerPiece('O5', 'DOC 1')
    expect(ligne(0)).toBe('GET /bff/des/homologation/parcours/operations/O5/pieces/DOC%201')
    expect(p.empreinte).toBe('ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad')
    vi.restoreAllMocks()
    brancher(() => new Response(JSON.stringify([{ date: '2026-10-15', type: 'DEPOT' }]), { status: 200, headers: { 'Content-Type': 'application/json' } }))
    expect(await parcoursHttp.chronologie('LIA-1')).toEqual([{ date: '2026-10-15', type: 'DEPOT' }])
    expect(ligne(0)).toBe('GET /bff/des/homologation/liaisons/LIA-1/chronologie')
  })
})
