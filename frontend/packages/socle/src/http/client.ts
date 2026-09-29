import { ApiError, type Problem } from './errors'

// Client HTTP, seul point d'appel réseau de l'interface (PI-GIMAC-01 F0 §A2).
// Le témoin de session HttpOnly part seul (credentials: "same-origin") : le client ne lit ni ne
// pose aucun jeton d'authentification. Seul le jeton anti-CSRF, que la passerelle publie dans un
// témoin lisible, est recopié dans l'en-tête attendu sur les méthodes qui modifient.

export type RequestOptions = {
  query?: Record<string, string | number | boolean | undefined>
  body?: unknown
  /** Assertion FIDO2 (IAM-07), transmise dans X-Reauth-Assertion ; une requête signée n'est jamais rejouée. */
  reauthAssertion?: string
  accept?: string
  signal?: AbortSignal
}

export type HttpHooks = {
  /** Session expirée : rétablit la session ; renvoie true si la requête peut être rejouée dans la page. */
  onUnauthorized: () => Promise<boolean>
  onForbidden: (e: ApiError) => void
  onCorrelation: (id: string) => void
}

export type CsrfConfig = { cookie: string; header: string } | null

let hooks: HttpHooks = { onUnauthorized: async () => false, onForbidden: () => undefined, onCorrelation: () => undefined }
// Valeurs de Spring Security (CookieServerCsrfTokenRepository), configuration actuelle de MS-GATEWAY.
let csrf: CsrfConfig = { cookie: 'XSRF-TOKEN', header: 'X-XSRF-TOKEN' }

export const configureHttp = (h: Partial<HttpHooks> & { csrf?: CsrfConfig }): void => {
  hooks = { ...hooks, ...h }
  if (h.csrf !== undefined) csrf = h.csrf
}

const MUTATIONS = new Set(['POST', 'PUT', 'PATCH', 'DELETE'])

function lireTemoin(nom: string): string | null {
  const m = document.cookie.match(new RegExp('(?:^|; )' + nom.replace(/[.$?*|{}()[\]\\/+^]/g, '\\$&') + '=([^;]*)'))
  return m ? decodeURIComponent(m[1]) : null
}

async function send(method: string, path: string, o: RequestOptions, rejoue = false): Promise<Response> {
  const url = new URL(path, window.location.origin)
  Object.entries(o.query ?? {}).forEach(([k, v]) => {
    if (v !== undefined) url.searchParams.set(k, String(v))
  })
  const headers = new Headers({ Accept: o.accept ?? 'application/json' })
  if (o.body !== undefined && !(o.body instanceof FormData)) headers.set('Content-Type', 'application/json')
  if (o.reauthAssertion) headers.set('X-Reauth-Assertion', o.reauthAssertion)
  if (csrf && MUTATIONS.has(method)) {
    const jeton = lireTemoin(csrf.cookie)
    if (jeton) headers.set(csrf.header, jeton)
  }

  let res: Response
  try {
    res = await fetch(url, {
      method,
      headers,
      credentials: 'same-origin',
      signal: o.signal,
      body: o.body === undefined ? undefined : o.body instanceof FormData ? o.body : JSON.stringify(o.body),
    })
  } catch (e) {
    if (e instanceof DOMException && e.name === 'AbortError') throw e
    throw new ApiError(0, 'RESEAU_INDISPONIBLE', 'La passerelle est injoignable.', '')
  }

  const correlationId = res.headers.get('X-Correlation-Id') ?? ''
  if (correlationId) hooks.onCorrelation(correlationId)

  // Une requête portant une assertion FIDO2 n'est pas rejouée : le défi est à usage unique.
  if (res.status === 401 && !rejoue && !o.reauthAssertion) {
    if (await hooks.onUnauthorized()) return send(method, path, o, true)
  }
  if (res.ok) return res

  const estJson = (res.headers.get('Content-Type') ?? '').includes('json')
  const p: Problem = estJson ? await res.json().catch(() => ({})) : {}
  const err = new ApiError(res.status, p.code ?? `HTTP_${res.status}`, p.message ?? p.detail ?? '', p.correlationId ?? correlationId, p.details)
  if (res.status === 403) hooks.onForbidden(err)
  throw err
}

async function json<R>(r: Response): Promise<R> {
  if (r.status === 204) return undefined as R
  // Un relais mal configuré renvoie la page de l'application : erreur explicite plutôt que JSON illisible.
  if (!(r.headers.get('Content-Type') ?? '').includes('json')) {
    throw new ApiError(r.status, 'REPONSE_NON_JSON', 'Réponse inattendue de la passerelle.', r.headers.get('X-Correlation-Id') ?? '')
  }
  return (await r.json()) as R
}

export function reprDigestToHex(h: string | null): string | null {
  const b64 = h?.match(/sha-256=:([^:]+):/)?.[1]
  return b64 ? Array.from(atob(b64), (c) => c.charCodeAt(0).toString(16).padStart(2, '0')).join('') : null
}

export const http = {
  get: <R>(path: string, o: RequestOptions = {}) => send('GET', path, o).then((r) => json<R>(r)),
  post: <R>(path: string, o: RequestOptions = {}) => send('POST', path, o).then((r) => json<R>(r)),
  put: <R>(path: string, o: RequestOptions = {}) => send('PUT', path, o).then((r) => json<R>(r)),
  /** Téléchargement journalisé côté service ; empreinte de l'en-tête Repr-Digest (RFC 9530), null si absente. */
  blob: async (path: string, accept: string) => {
    const r = await send('GET', path, { accept })
    return { blob: await r.blob(), empreinte: reprDigestToHex(r.headers.get('Repr-Digest')) }
  },
}

export { ApiError }
