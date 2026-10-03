import { afterEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from './errors'
import { depotsHttp, televerser, type Depot, type Progression, type ServiceDepots } from './televersement'

// Dépôts fragmentés (D-74) : parties envoyées dans l'ordre avec la progression, reprise des parties déjà
// reçues, nouvel essai sur une erreur passagère, abandon sur annulation, vérification suivie jusqu'au ticket.

const TP = 4
const fichier = (taille: number) => new File(['x'.repeat(taille)], 'atelier.mp4', { type: 'video/mp4' })

/** MS-DOCUMENT simulé : parties de 4 octets, vérification en deux relectures. */
function service(o: { recues?: number[]; echecs?: Record<number, ApiError[]>; verdict?: Partial<Depot>; relectures?: number } = {}) {
  const appels: string[] = []
  let depot: Depot
  let relectures = o.relectures ?? 2
  const s: ServiceDepots = {
    ouvrir: async (ouv) => {
      appels.push(`ouvrir ${ouv.nomFichier} ${ouv.taille} ${ouv.finalite} ${ouv.reference}`)
      depot = { id: 'D1', etat: 'OUVERT', nomFichier: ouv.nomFichier, typeMime: ouv.typeMime, taille: ouv.taille, taillePartie: TP, nombreParties: Math.ceil(ouv.taille / TP), partiesRecues: [...(o.recues ?? [])] }
      return { ...depot }
    },
    envoyerPartie: async (_id, n, partie, { surOctets }) => {
      const echec = o.echecs?.[n]?.shift()
      appels.push(`partie ${n} (${partie.size})${echec ? ` échec ${echec.status}` : ''}`)
      if (echec) throw echec
      surOctets?.(partie.size / 2)
      surOctets?.(partie.size)
      depot.partiesRecues.push(n)
    },
    etat: async () => {
      appels.push('etat')
      if (depot.etat === 'EN_VERIFICATION' && --relectures <= 0) depot = { ...depot, etat: 'DISPONIBLE', documentId: 'DOC-1', sha256: 'a'.repeat(64), dedoublonne: false, ...o.verdict }
      return { ...depot, partiesRecues: [...depot.partiesRecues] }
    },
    terminer: async () => {
      appels.push('fin')
      depot = { ...depot, etat: 'EN_VERIFICATION' }
      return { ...depot }
    },
    abandonner: async () => {
      appels.push('abandon')
    },
  }
  return { s, appels }
}

afterEach(() => vi.restoreAllMocks())

describe('televerser', () => {
  it('parties dans l’ordre, progression octet par octet, vérification suivie, ticket rendu', async () => {
    const { s, appels } = service()
    const vues: Progression[] = []
    const r = await televerser(s, fichier(10), { finalite: 'MESSAGERIE', reference: 'C1' }, { intervalleMs: 1, surProgression: (p) => vues.push(p) })
    expect(r).toEqual({ documentId: 'DOC-1', sha256: 'a'.repeat(64), dedoublonne: false, depotId: 'D1' })
    expect(appels).toEqual(['ouvrir atelier.mp4 10 MESSAGERIE C1', 'partie 1 (4)', 'partie 2 (4)', 'partie 3 (2)', 'fin', 'etat', 'etat'])
    expect(vues.filter((v) => v.phase === 'envoi').map((v) => v.envoye)).toEqual([0, 2, 4, 4, 6, 8, 8, 9, 10, 10])
    expect(vues.slice(-2).map((v) => v.phase)).toEqual(['verification', 'pret'])
  })
  it('reprise : les parties déjà reçues ne repartent pas', async () => {
    const { s, appels } = service({ recues: [1, 2] })
    const vues: Progression[] = []
    await televerser(s, fichier(10), { finalite: 'MESSAGERIE', reference: 'C1' }, { intervalleMs: 1, surProgression: (p) => vues.push(p) })
    expect(appels.filter((a) => a.startsWith('partie'))).toEqual(['partie 3 (2)'])
    expect(vues[0]).toEqual({ phase: 'envoi', envoye: 8, total: 10 })
  })
  it('erreur passagère : nouvel essai ; partie arrivée malgré l’erreur : pas de renvoi', async () => {
    const { s, appels } = service({ echecs: { 2: [new ApiError(503, 'X', '', '')] } })
    await televerser(s, fichier(10), { finalite: 'MESSAGERIE', reference: 'C1' }, { intervalleMs: 1 })
    expect(appels.slice(0, 5)).toEqual(['ouvrir atelier.mp4 10 MESSAGERIE C1', 'partie 1 (4)', 'partie 2 (4) échec 503', 'etat', 'partie 2 (4)'])
  })
  it('erreur définitive ou essais épuisés : échec sans abandon', async () => {
    const refus = new ApiError(400, 'DOC_PART_SIZE', '', '')
    await expect(televerser(service({ echecs: { 1: [refus] } }).s, fichier(10), { finalite: 'MESSAGERIE', reference: 'C1' }, { intervalleMs: 1 })).rejects.toBe(refus)
    const reseau = () => new ApiError(0, 'RESEAU_INDISPONIBLE', '', '')
    const { s, appels } = service({ echecs: { 1: [reseau(), reseau()] } })
    await expect(televerser(s, fichier(10), { finalite: 'MESSAGERIE', reference: 'C1' }, { intervalleMs: 1, essais: 2 })).rejects.toMatchObject({ code: 'RESEAU_INDISPONIBLE' })
    expect(appels).not.toContain('abandon')
  })
  it('annulation : dépôt abandonné côté serveur', async () => {
    const { s, appels } = service()
    const arret = new AbortController()
    const promesse = televerser(s, fichier(10), { finalite: 'MESSAGERIE', reference: 'C1' }, {
      intervalleMs: 1,
      signal: arret.signal,
      surProgression: (p) => {
        if (p.envoye >= 4) arret.abort()
      },
    })
    await expect(promesse).rejects.toMatchObject({ name: 'AbortError' })
    expect(appels).toContain('abandon')
    expect(appels).not.toContain('fin')
  })
  it('vérification refusée : erreur 422 portant le motif', async () => {
    const { s } = service({ verdict: { etat: 'REJETE', motif: 'DOC_PAN_DETECTED', documentId: null, sha256: null } })
    await expect(televerser(s, fichier(3), { finalite: 'MESSAGERIE', reference: 'C1' }, { intervalleMs: 1 })).rejects.toMatchObject({ status: 422, code: 'DOC_PAN_DETECTED' })
  })
})

/** XMLHttpRequest simulé : enregistre la requête, annonce la progression puis répond. */
class FausseXhr {
  static derniere: FausseXhr
  static reponse = { status: 204, texte: '' }
  methode = ''
  url = ''
  entetes: Record<string, string> = {}
  withCredentials = false
  status = 0
  responseText = ''
  corps: Blob | null = null
  upload: { onprogress: ((e: { loaded: number }) => void) | null } = { onprogress: null }
  onload: (() => void) | null = null
  onerror: (() => void) | null = null
  onabort: (() => void) | null = null
  constructor() {
    FausseXhr.derniere = this
  }
  open(m: string, u: string) {
    this.methode = m
    this.url = u
  }
  setRequestHeader(k: string, v: string) {
    this.entetes[k.toLowerCase()] = v
  }
  getResponseHeader() {
    return 'corr-1'
  }
  abort() {
    this.onabort?.()
  }
  send(corps: Blob) {
    this.corps = corps
    queueMicrotask(() => {
      if (FausseXhr.reponse.status === 0) return this.onerror?.()
      this.upload.onprogress?.({ loaded: 1 })
      this.status = FausseXhr.reponse.status
      this.responseText = FausseXhr.reponse.texte
      this.onload?.()
    })
  }
}

describe('depotsHttp', () => {
  it('routes du contrat, partie en octets bruts avec le jeton anti-CSRF et la progression', async () => {
    vi.stubGlobal('XMLHttpRequest', FausseXhr)
    document.cookie = 'XSRF-TOKEN=jeton-1'
    const appels: string[] = []
    vi.spyOn(globalThis, 'fetch').mockImplementation(async (u, i) => {
      appels.push(`${i?.method} ${new URL(String(u)).pathname} ${new Headers(i?.headers).get('X-Gimac-Fond') ?? ''}`.trim())
      return new Response(i?.method === 'DELETE' ? null : '{"id":"D 1"}', { status: i?.method === 'DELETE' ? 204 : 200, headers: { 'Content-Type': 'application/json' } })
    })
    const d = depotsHttp('des')
    await d.ouvrir({ nomFichier: 'a', typeMime: 't', taille: 1, finalite: 'MESSAGERIE', reference: 'C1' })
    await d.etat('D 1')
    await d.terminer('D 1')
    await d.abandonner('D 1')
    expect(appels).toEqual(['POST /bff/des/document/depots', 'GET /bff/des/document/depots/D%201 1', 'POST /bff/des/document/depots/D%201/fin', 'DELETE /bff/des/document/depots/D%201'])
    const octets: number[] = []
    FausseXhr.reponse = { status: 204, texte: '' }
    await d.envoyerPartie('D 1', 3, new Blob(['abcd']), { surOctets: (n) => octets.push(n) })
    const x = FausseXhr.derniere
    expect([x.methode, new URL(x.url).pathname, x.withCredentials]).toEqual(['PUT', '/bff/des/document/depots/D%201/parties/3', true])
    expect(x.entetes).toMatchObject({ 'content-type': 'application/octet-stream', 'x-xsrf-token': 'jeton-1' })
    expect(octets).toEqual([1, 4])
    vi.unstubAllGlobals()
  })
  it('refus du service en ApiError ; réseau coupé : RESEAU_INDISPONIBLE ; annulation : AbortError', async () => {
    vi.stubGlobal('XMLHttpRequest', FausseXhr)
    const d = depotsHttp('des')
    FausseXhr.reponse = { status: 409, texte: '{"code":"DOC_UPLOAD_CLOSED","message":"Dépôt clos"}' }
    await expect(d.envoyerPartie('D1', 1, new Blob(['a']), {})).rejects.toMatchObject({ status: 409, code: 'DOC_UPLOAD_CLOSED', message: 'Dépôt clos' })
    FausseXhr.reponse = { status: 502, texte: '<html>' }
    await expect(d.envoyerPartie('D1', 1, new Blob(['a']), {})).rejects.toMatchObject({ status: 502, code: 'HTTP_502' })
    FausseXhr.reponse = { status: 0, texte: '' }
    await expect(d.envoyerPartie('D1', 1, new Blob(['a']), {})).rejects.toMatchObject({ code: 'RESEAU_INDISPONIBLE' })
    const arret = new AbortController()
    FausseXhr.reponse = { status: 204, texte: '' }
    const p = d.envoyerPartie('D1', 1, new Blob(['a']), { signal: arret.signal })
    arret.abort()
    await expect(p).rejects.toMatchObject({ name: 'AbortError' })
    vi.unstubAllGlobals()
  })
})
