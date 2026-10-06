import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { configureHttp, http } from './client'
import { ApiError, SAISIE_INVALIDE, champsRefuses } from './errors'

const reply = (status: number, body: object | string, entetes: Record<string, string> = {}) =>
  vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce(
    new Response(typeof body === 'string' ? body : JSON.stringify(body), {
      status,
      headers: { 'X-Correlation-Id': 'cid-1', 'Content-Type': typeof body === 'string' ? 'text/html' : 'application/json', ...entetes },
    }),
  )
const entetes = (spy: ReturnType<typeof reply>, i = 0) => new Headers(spy.mock.calls[i][1]?.headers)

beforeEach(() => configureHttp({ onUnauthorized: async () => false, onForbidden: () => undefined, onCorrelation: () => undefined, csrf: { cookie: 'XSRF-TOKEN', header: 'X-XSRF-TOKEN' } }))
afterEach(() => {
  vi.restoreAllMocks()
  document.cookie = 'XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 GMT'
})

describe('F0 : client HTTP', () => {
  it('lecture avec en-têtes : corps et en-têtes de la réponse (X-Orchestrateur, D-78)', async () => {
    reply(200, [{ nom: 'iam' }], { 'X-Orchestrateur': 'AUCUN' })
    const r = await http.getAvecEntetes<{ nom: string }[]>('/api/plateforme/infrastructure/charges')
    expect(r.donnees).toEqual([{ nom: 'iam' }])
    expect(r.entetes.get('X-Orchestrateur')).toBe('AUCUN')
  })

  it('F0 : une erreur 409 est normalisée en ApiError avec son identifiant de corrélation', async () => {
    reply(409, { code: 'HOM_CLOSURE_ELEMENT_MISSING', message: 'preuve', correlationId: 'cid-409' })
    await expect(http.post('/bff/des/homologation/operations/o1/validation')).rejects.toMatchObject({ status: 409, code: 'HOM_CLOSURE_ELEMENT_MISSING', correlationId: 'cid-409' })
  })

  it('F0 : un 401 déclenche la réauthentification puis rejoue la requête une seule fois', async () => {
    const onUnauthorized = vi.fn(async () => true)
    configureHttp({ onUnauthorized })
    reply(401, {})
    reply(200, { ok: true })
    await expect(http.get('/bff/des/homologation/liaisons')).resolves.toEqual({ ok: true })
    expect(onUnauthorized).toHaveBeenCalledTimes(1)
  })

  it("F0 : l'assertion est transmise dans X-Reauth-Assertion et aucun en-tête Authorization n'est posé", async () => {
    const spy = reply(200, {})
    await http.post('/bff/des/homologation/operations/o1/validation', { body: {}, reauthAssertion: 'YXNzZXJ0' })
    expect(entetes(spy).get('X-Reauth-Assertion')).toBe('YXNzZXJ0')
    expect(entetes(spy).has('Authorization')).toBe(false)
  })

  it("F0 : une requête signée n'est jamais rejouée après un 401 (défi FIDO2 à usage unique)", async () => {
    const onUnauthorized = vi.fn(async () => true)
    configureHttp({ onUnauthorized })
    reply(401, { code: 'IAM_REAUTH_EXPIRED' })
    await expect(http.post('/bff/des/homologation/operations/o1/validation', { reauthAssertion: 'x' })).rejects.toBeInstanceOf(ApiError)
    expect(onUnauthorized).not.toHaveBeenCalled()
  })

  it('F0 : le jeton anti-CSRF de la passerelle accompagne les méthodes qui modifient, pas les lectures', async () => {
    document.cookie = 'XSRF-TOKEN=jeton-csrf'
    const spy = reply(200, {})
    reply(200, {})
    reply(200, { titre: 'G' })
    await http.post('/bff/des/homologation/liaisons', { body: {} })
    await http.get('/bff/des/homologation/liaisons')
    expect(await http.patch('/bff/des/collaboration/messagerie/conversations/C1', { body: { titre: 'G' } })).toEqual({ titre: 'G' })
    expect(entetes(spy, 0).get('X-XSRF-TOKEN')).toBe('jeton-csrf')
    expect(entetes(spy, 1).has('X-XSRF-TOKEN')).toBe(false)
    expect(spy.mock.calls[2][1]?.method).toBe('PATCH')
    expect(entetes(spy, 2).get('X-XSRF-TOKEN')).toBe('jeton-csrf')
  })

  it("F0 : une page HTML renvoyée par un relais mal configuré devient une erreur explicite", async () => {
    reply(200, '<!doctype html><html></html>')
    await expect(http.get('/bff/des/homologation/liaisons')).rejects.toMatchObject({ code: 'REPONSE_NON_JSON' })
  })

  it('F0 : une passerelle injoignable est normalisée', async () => {
    vi.spyOn(globalThis, 'fetch').mockRejectedValueOnce(new TypeError('Failed to fetch'))
    await expect(http.get('/bff/des/homologation/liaisons')).rejects.toMatchObject({ status: 0, code: 'RESEAU_INDISPONIBLE' })
  })

  it("F0 : un 403 est signalé à l'enveloppe (page Accès refusé)", async () => {
    const onForbidden = vi.fn()
    configureHttp({ onForbidden })
    reply(403, { code: 'OPA_DENIED', message: 'refus' })
    await expect(http.get('/bff/des/homologation/liaisons')).rejects.toBeInstanceOf(ApiError)
    expect(onForbidden).toHaveBeenCalledWith(expect.objectContaining({ code: 'OPA_DENIED', correlationId: 'cid-1' }))
  })

  it("F0 : l'empreinte d'un téléchargement vient de Repr-Digest", async () => {
    reply(200, { a: 1 }, { 'Repr-Digest': 'sha-256=:AAEC:' })
    const r = await http.blob('/bff/des/certification/attestations/A', 'application/pdf')
    expect(r.empreinte).toBe('000102')
  })

  it('D-65 : appel de fond marqué X-Gimac-Fond: 1 (ne prolonge pas la session) ; appel ordinaire sans marque', async () => {
    const spy = reply(200, {})
    reply(200, {})
    await http.get('/bff/des/collaboration/messagerie/non-lus', { fond: true })
    await http.get('/bff/des/collaboration/messagerie')
    expect(entetes(spy, 0).get('X-Gimac-Fond')).toBe('1')
    expect(entetes(spy, 1).has('X-Gimac-Fond')).toBe(false)
  })

  it('D-65 : route publique (anonyme) : un 401 est une réponse du formulaire, sans crochet de session', async () => {
    const onUnauthorized = vi.fn(async () => true)
    configureHttp({ onUnauthorized })
    reply(401, { code: 'CNX_IDENTIFIANTS_INVALIDES' })
    await expect(http.post('/api/connexion', { body: {}, anonyme: true })).rejects.toMatchObject({ status: 401, code: 'CNX_IDENTIFIANTS_INVALIDES' })
    expect(onUnauthorized).not.toHaveBeenCalled()
  })

  it('D-65 : le crochet 401 reçoit l’erreur (CNX_SESSION_EXPIREE) ; violations de premier niveau reprises dans details', async () => {
    const onUnauthorized = vi.fn(async (_e: ApiError) => false)
    configureHttp({ onUnauthorized })
    reply(401, { code: 'CNX_SESSION_EXPIREE' })
    await expect(http.get('/bff/des/homologation/liaisons')).rejects.toMatchObject({ code: 'CNX_SESSION_EXPIREE' })
    expect(onUnauthorized.mock.calls[0][0]).toMatchObject({ code: 'CNX_SESSION_EXPIREE' })
    reply(400, { code: 'IAM_PASSWORD_POLICY', violations: ['LONGUEUR'] })
    await expect(http.post('/api/connexion/nouveau-mot-de-passe', { anonyme: true })).rejects.toMatchObject({ details: { violations: ['LONGUEUR'] } })
  })
})

describe('F0 : validation de l’entrée refusée champ par champ (fieldErrors)', () => {
  const PROBLEME = {
    type: 'https://www.jhipster.tech/problem/constraint-violation', title: 'Method argument not valid', status: 400, message: 'error.validation', path: '/api/v1/institutions',
    fieldErrors: [
      { objectName: 'candidatureRequest', field: 'codeIdentification', message: 'must match "^[A-Z]{4}[A-Z]{2}[A-Z0-9]{2}([A-Z0-9]{3})?$"' },
      { objectName: 'candidatureRequest', field: 'datePriseDeContact', message: 'must be a date in the past or in the present (Africa/Douala)' },
      { objectName: 'candidatureRequest', field: 'codeIdentification', message: 'second message ignoré' },
    ],
  }

  it('fieldErrors repris dans details.champs, code SAISIE_INVALIDE et message lisible au lieu de « error.validation »', async () => {
    reply(400, PROBLEME, { 'Content-Type': 'application/problem+json' })
    const e = await http.post('/bff/des/participant/institutions', { body: {} }).catch((x: unknown) => x)
    expect(e).toBeInstanceOf(ApiError)
    expect(e).toMatchObject({ status: 400, code: SAISIE_INVALIDE, correlationId: 'cid-1' })
    expect((e as ApiError).message).not.toBe('error.validation')
    const attendus = { codeIdentification: PROBLEME.fieldErrors[0].message, datePriseDeContact: PROBLEME.fieldErrors[1].message }
    expect((e as ApiError).details).toEqual({ champs: attendus })
    expect(champsRefuses(e)).toEqual(attendus)
  })

  it('un code métier du service est conservé ; ses détails sont complétés des champs', async () => {
    reply(400, { code: 'HOM_SAISIE', message: 'Refus', details: { liaison: 'L1' }, fieldErrors: [{ field: 'motif', message: 'must not be blank' }] })
    await expect(http.put('/bff/des/homologation/liaisons/L1')).rejects.toMatchObject({ code: 'HOM_SAISIE', message: 'Refus', details: { liaison: 'L1', champs: { motif: 'must not be blank' } } })
  })

  it('sans fieldErrors : comportement inchangé (HTTP_400, détail du service) et aucun champ refusé', async () => {
    reply(400, { title: 'Bad Request', detail: 'Failed to read request', fieldErrors: [] })
    const e = await http.post('/bff/des/participant/institutions', { body: {} }).catch((x: unknown) => x)
    expect(e).toMatchObject({ code: 'HTTP_400', message: 'Failed to read request' })
    expect(champsRefuses(e)).toEqual({})
    expect(champsRefuses(new Error('x'))).toEqual({})
  })
})
