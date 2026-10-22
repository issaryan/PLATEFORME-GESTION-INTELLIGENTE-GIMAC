import { afterEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '@gimac/socle'
import { espacesHttp, servicesSandboxHttp } from './http'

// Contrat HTTP de SBX-ESPACE par la passerelle (D-118, D-122) : /bff/des/sbx-espace/espaces, réécrit en
// /api/v1/console/espaces ; en-tête Idempotency-Key sur chaque écriture ; problèmes RFC 9457 traduits par leur code.
const json = (corps: unknown, status = 200) => new Response(JSON.stringify(corps), { status, headers: { 'Content-Type': 'application/json' } })
type Espion = { mock: { calls: unknown[][] } }
const appels = (spy: Espion) =>
  spy.mock.calls.map(([u, i]) => {
    const url = new URL(String(u))
    return `${(i as RequestInit | undefined)?.method} ${url.pathname}${url.search}`
  })
const init = (spy: Espion, i: number) => spy.mock.calls[i][1] as RequestInit

afterEach(() => vi.restoreAllMocks())

describe('SBX-ESPACE par la passerelle', () => {
  it('lectures : liste par curseur et fiche', async () => {
    const spy = vi.spyOn(globalThis, 'fetch').mockImplementation(async () => json({ elements: [], suivant: null }))
    await espacesHttp.lister({ apres: '99002', taille: 20 })
    await espacesHttp.lister({})
    await espacesHttp.consulter('99 01')
    expect(appels(spy)).toEqual(['GET /bff/des/sbx-espace/espaces?apres=99002&taille=20', 'GET /bff/des/sbx-espace/espaces', 'GET /bff/des/sbx-espace/espaces/99%2001'])
  })

  it('écritures : ouverture et actes, corps et clé d’idempotence', async () => {
    const spy = vi.spyOn(globalThis, 'fetch').mockImplementation(async () => json({}, 201))
    await servicesSandboxHttp().espaces.ouvrir({ codeParticipant: '99004', correspondantId: 'c1' }, 'cle-ouverture-1')
    await espacesHttp.agir('99001', 'suspension', 'cle-acte-1')
    await espacesHttp.agir('99001', 'retablissement', 'cle-acte-2')
    await espacesHttp.agir('99003', 'fermeture', 'cle-acte-3')
    expect(appels(spy)).toEqual([
      'POST /bff/des/sbx-espace/espaces',
      'POST /bff/des/sbx-espace/espaces/99001/suspension',
      'POST /bff/des/sbx-espace/espaces/99001/retablissement',
      'POST /bff/des/sbx-espace/espaces/99003/fermeture',
    ])
    expect(JSON.parse(String(init(spy, 0).body))).toEqual({ codeParticipant: '99004', correspondantId: 'c1' })
    expect(new Headers(init(spy, 0).headers).get('Idempotency-Key')).toBe('cle-ouverture-1')
    expect(new Headers(init(spy, 3).headers).get('Idempotency-Key')).toBe('cle-acte-3')
  })

  it('problème RFC 9457 : code du service repris', async () => {
    vi.spyOn(globalThis, 'fetch').mockImplementation(async () =>
      new Response(JSON.stringify({ type: 'https://gimac.cm/problemes/sbx-espace/sbx-esp-etat-incompatible', title: 'État incompatible', status: 409, code: 'SBX_ESP_ETAT_INCOMPATIBLE' }), {
        status: 409,
        headers: { 'Content-Type': 'application/problem+json' },
      }),
    )
    const e = await espacesHttp.agir('99001', 'fermeture', 'cle-acte-4').catch((x: unknown) => x)
    expect(e).toBeInstanceOf(ApiError)
    expect((e as ApiError).code).toBe('SBX_ESP_ETAT_INCOMPATIBLE')
  })
})
