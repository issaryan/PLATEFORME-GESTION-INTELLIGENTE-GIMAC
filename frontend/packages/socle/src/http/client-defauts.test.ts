import { afterEach, describe, expect, it, vi } from 'vitest'
import { http, reprDigestToHex } from './client'

// Comportements par défaut du client (sans configureHttp) et méthodes secondaires : PUT, téléchargement
// avec empreinte Repr-Digest (RFC 9530).
afterEach(() => vi.restoreAllMocks())
const repondre = (status: number, corps: BodyInit | null, entetes: Record<string, string>) =>
  vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce(new Response(corps, { status, headers: entetes }))

describe('client HTTP : valeurs par défaut', () => {
  it('401 sans réauthentification configurée : erreur, sans rejeu', async () => {
    const spy = repondre(401, JSON.stringify({ code: 'HTTP_401' }), { 'Content-Type': 'application/json', 'X-Correlation-Id': 'c1' })
    await expect(http.get('/bff/des/x')).rejects.toMatchObject({ status: 401 })
    expect(spy).toHaveBeenCalledTimes(1)
  })
  it('403 : erreur normalisée, crochet par défaut sans effet', async () => {
    repondre(403, JSON.stringify({ code: 'INTERDIT', message: 'non' }), { 'Content-Type': 'application/json' })
    await expect(http.post('/bff/des/x', { body: { a: 1 } })).rejects.toMatchObject({ status: 403, code: 'INTERDIT' })
  })
  it('PUT avec corps JSON, réponse 204 sans contenu', async () => {
    const spy = repondre(204, null, {})
    await expect(http.put('/bff/des/x', { body: { a: 1 } })).resolves.toBeUndefined()
    expect(spy.mock.calls[0][1]?.method).toBe('PUT')
  })
  it('téléchargement : contenu et empreinte de Repr-Digest', async () => {
    repondre(200, 'PDF', { 'Content-Type': 'application/pdf', 'Repr-Digest': 'sha-256=:uU0nuZNNPgilLlLX2n2r+sSE7+N6U4DukIj3rOLvzek=:' })
    const r = await http.blob('/bff/des/x.pdf', 'application/pdf')
    expect(await r.blob.text()).toBe('PDF')
    expect(r.empreinte).toBe('b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9')
  })
  it('empreinte absente ou mal formée : null', () => {
    expect(reprDigestToHex(null)).toBeNull()
    expect(reprDigestToHex('md5=:abc:')).toBeNull()
  })
})
