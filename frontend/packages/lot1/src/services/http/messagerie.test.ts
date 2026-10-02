import { afterEach, describe, expect, it, vi } from 'vitest'
import { messagerieHttp, messagerieHttpPour } from './messagerie'

// Contrat HTTP de la messagerie (docs/lot1/messagerie.md) : /bff/des/collaboration/messagerie, pagination
// à partir de 0, corps multipart (partie JSON « message » et parties « fichiers »).
const appels = () => (globalThis.fetch as unknown as { mock: { calls: [URL, RequestInit][] } }).mock.calls
afterEach(() => vi.restoreAllMocks())
const brancher = () => vi.spyOn(globalThis, 'fetch').mockImplementation(async () => new Response('{}', { status: 200, headers: { 'Content-Type': 'application/json' } }))
const url = (i: number) => new URL(String(appels()[i][0]))
const methode = (i: number) => appels()[i][1].method

describe('messagerieHttp', () => {
  it('lectures : annuaire, conversations filtrées, conversation, fil, non-lus', async () => {
    brancher()
    await messagerieHttp.annuaire('rachel')
    await messagerieHttp.annuaire('', 2, 10)
    await messagerieHttp.conversations({ recherche: 'essais', liaisonId: 'L1' })
    await messagerieHttp.conversations({ page: 1, taille: 5 })
    await messagerieHttp.conversation('C 1')
    await messagerieHttp.messages('C1', 'M9')
    await messagerieHttp.nonLus()
    expect([0, 1, 2, 3, 4, 5, 6].map((i) => url(i).pathname + url(i).search)).toEqual([
      '/bff/des/collaboration/messagerie/annuaire?recherche=rachel&page=0&taille=20',
      '/bff/des/collaboration/messagerie/annuaire?page=2&taille=10',
      '/bff/des/collaboration/messagerie/conversations?page=0&taille=20&recherche=essais&liaisonId=L1',
      '/bff/des/collaboration/messagerie/conversations?page=1&taille=5',
      '/bff/des/collaboration/messagerie/conversations/C%201',
      '/bff/des/collaboration/messagerie/conversations/C1/messages?avant=M9&taille=50',
      '/bff/des/collaboration/messagerie/non-lus',
    ])
    // Compteur périodique : appel de fond, sans prolonger la session d'inactivité (D-65 §6).
    // Liste, conversation et fil, relus par le sondage de repli (D-72) : appels de fond aussi.
    expect([2, 3, 4, 5, 6].map((i) => new Headers(appels()[i][1].headers).get('X-Gimac-Fond'))).toEqual(['1', '1', '1', '1', '1'])
    expect(new Headers(appels()[0][1].headers).has('X-Gimac-Fond')).toBe(false)
  })
  it('écritures : démarrage et envoi en multipart, lecture marquée, pièce téléchargée', async () => {
    brancher()
    const f = new File(['x'], 'rapport.pdf', { type: 'application/pdf' })
    await messagerieHttp.demarrer({ destinataires: ['u2'], titre: 'Essais', liaisonId: 'L1', contenu: 'Bonjour', etiquettes: [{ liaisonId: 'L1', reference: 'LIA-2026-030' }], fichiers: [f] })
    await messagerieHttp.envoyer('C1', { contenu: 'Suite', etiquettes: [], fichiers: [] })
    await messagerieHttp.marquerLu('C1')
    await messagerieHttp.telechargerPiece('P1')
    expect([0, 1, 2, 3].map((i) => `${methode(i)} ${url(i).pathname}`)).toEqual([
      'POST /bff/des/collaboration/messagerie/conversations',
      'POST /bff/des/collaboration/messagerie/conversations/C1/messages',
      'POST /bff/des/collaboration/messagerie/conversations/C1/lu',
      'GET /bff/des/collaboration/messagerie/pieces/P1',
    ])
    const corps = appels()[0][1].body as FormData
    expect(JSON.parse(await (corps.get('message') as Blob).text())).toEqual({ contenu: 'Bonjour', etiquettes: [{ liaisonId: 'L1', reference: 'LIA-2026-030' }], destinataires: ['u2'], titre: 'Essais', liaisonId: 'L1' })
    expect((corps.getAll('fichiers')[0] as File).name).toBe('rapport.pdf')
    const envoi = appels()[1][1].body as FormData
    expect(JSON.parse(await (envoi.get('message') as Blob).text())).toEqual({ contenu: 'Suite', etiquettes: [] })
    expect(envoi.getAll('fichiers')).toHaveLength(0)
  })
  it('groupes (D-68) : création nommée, renommage en PATCH JSON, ajout et retrait de membres', async () => {
    brancher()
    await messagerieHttp.demarrer({ destinataires: ['u2', 'u3'], titre: 'Cellule', liaisonId: null, contenu: '', etiquettes: [], fichiers: [], groupe: true })
    await messagerieHttp.renommerGroupe('C1', 'Cellule bis')
    await messagerieHttp.ajouterMembres('C1', ['u4'])
    await messagerieHttp.retirerMembre('C1', 'u 2')
    expect([0, 1, 2, 3].map((i) => `${methode(i)} ${url(i).pathname}`)).toEqual([
      'POST /bff/des/collaboration/messagerie/conversations',
      'PATCH /bff/des/collaboration/messagerie/conversations/C1',
      'POST /bff/des/collaboration/messagerie/conversations/C1/membres',
      'DELETE /bff/des/collaboration/messagerie/conversations/C1/membres/u%202',
    ])
    const creation = appels()[0][1].body as FormData
    expect(JSON.parse(await (creation.get('message') as Blob).text())).toMatchObject({ destinataires: ['u2', 'u3'], titre: 'Cellule', groupe: true })
    expect(JSON.parse(String(appels()[1][1].body))).toEqual({ titre: 'Cellule bis' })
    expect(JSON.parse(String(appels()[2][1].body))).toEqual({ membres: ['u4'] })
  })
  it('D-72 : présence servie par la passerelle, saisie signalée ; appels de fond', async () => {
    brancher()
    await messagerieHttp.presence()
    await messagerieHttp.signalerSaisie('C 1')
    await messagerieHttpPour('console').presence()
    expect([0, 1, 2].map((i) => `${methode(i)} ${url(i).pathname}`)).toEqual([
      'GET /bff/des/presence',
      'POST /bff/des/collaboration/messagerie/conversations/C%201/saisie',
      'GET /bff/console/presence',
    ])
    expect([0, 1, 2].map((i) => new Headers(appels()[i][1].headers).get('X-Gimac-Fond'))).toEqual(['1', '1', '1'])
  })
})
