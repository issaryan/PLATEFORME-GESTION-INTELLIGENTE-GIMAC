import { afterEach, describe, expect, it, vi } from 'vitest'
import { empreinteSha256, participantHttp } from './participant'

// Contrat HTTP de la tranche DDRM : chemins /bff/des/participant, corps, pagination, réauthentification.
const reponses: unknown[] = []
const appels = () => (globalThis.fetch as unknown as { mock: { calls: [URL, RequestInit][] } }).mock.calls
afterEach(() => {
  vi.restoreAllMocks()
  reponses.length = 0
})
const brancher = (...r: unknown[]) => {
  reponses.push(...r)
  vi.spyOn(globalThis, 'fetch').mockImplementation(async () => new Response(JSON.stringify(reponses.shift() ?? {}), { status: 200, headers: { 'Content-Type': 'application/json' } }))
}
const url = (i: number) => new URL(String(appels()[i][0]))
const corps = (i: number) => JSON.parse(String(appels()[i][1].body ?? 'null'))

describe('participantHttp', () => {
  it('liste : pagination à partir de 0, statuts joints, recherche et mes dossiers', async () => {
    brancher({ contenu: [], total: 0 })
    await participantHttp.listerInstitutions({ page: 2, taille: 25, statut: ['CANDIDAT', 'ADHERENT'], recherche: 'banque', mesDossiers: true })
    const u = url(0)
    expect(u.pathname).toBe('/bff/des/participant/institutions')
    expect(Object.fromEntries(u.searchParams)).toEqual({ page: '1', taille: '25', q: 'banque', mesDossiers: 'true', statut: 'CANDIDAT,ADHERENT' })
  })
  it('lectures : indicateurs, dossier, prérequis', async () => {
    brancher({}, {}, [])
    await participantHttp.indicateurs()
    await participantHttp.dossier('i1')
    await participantHttp.prerequis('i1', 'ADHERENT')
    expect([0, 1, 2].map((i) => url(i).pathname + url(i).search)).toEqual([
      '/bff/des/participant/institutions/indicateurs',
      '/bff/des/participant/institutions/i1',
      '/bff/des/participant/institutions/i1/prerequis?etat=ADHERENT',
    ])
  })
  it('écritures : prise de contact, fiche, conditions, examen des pièces et transitions', async () => {
    brancher({}, {}, {}, {}, {}, {}, {}, {})
    await participantHttp.enregistrer({ nom: 'B', pays: 'CM', categorie: 'BANQUE' })
    await participantHttp.modifier('i1', { nom: 'B', pays: 'CM', categorie: 'BANQUE' })
    await participantHttp.definirConditions('i1', { codeParticipant: 'BQ-1', mode: 'DIRECT' })
    await participantHttp.validerPiece('i1', 'STATUTS')
    await participantHttp.rejeterPiece('i1', 'STATUTS', 'illisible')
    await participantHttp.proposerTransition('i1', 'ADHERENT', 'complet')
    await participantHttp.approuverTransition('i1', 't1', 'assertion')
    await participantHttp.rejeterTransition('i1', 't1', 'non')
    const c = appels()
    expect(c.map(([u, o]) => `${o.method} ${new URL(String(u)).pathname}`)).toEqual([
      'POST /bff/des/participant/institutions',
      'PUT /bff/des/participant/institutions/i1',
      'PUT /bff/des/participant/institutions/i1/conditions-adhesion',
      'POST /bff/des/participant/institutions/i1/pieces/STATUTS/validation',
      'POST /bff/des/participant/institutions/i1/pieces/STATUTS/rejet',
      'POST /bff/des/participant/institutions/i1/transitions',
      'POST /bff/des/participant/institutions/i1/transitions/t1/approbation',
      'POST /bff/des/participant/institutions/i1/transitions/t1/rejet',
    ])
    expect(corps(4)).toEqual({ motif: 'illisible' })
    expect(corps(5)).toEqual({ vers: 'ADHERENT', motif: 'complet' })
    expect(new Headers(c[6][1].headers).get('X-Reauth-Assertion')).toBe('assertion')
  })
  it('dépôt : MS-DOCUMENT puis rattachement de la référence et de l’empreinte', async () => {
    brancher({ id: 'doc-1', sha256: 'a'.repeat(64) }, { code: 'STATUTS' })
    const fichier = new File(['contenu'], 'statuts.pdf', { type: 'application/pdf' })
    await participantHttp.deposerPiece('i1', 'STATUTS', fichier)
    expect(url(0).pathname).toBe('/bff/des/document/documents')
    expect(appels()[0][1].body).toBeInstanceOf(FormData)
    expect(url(1).pathname).toBe('/bff/des/participant/institutions/i1/pieces/STATUTS/depot')
    expect(corps(1)).toEqual({ documentId: 'doc-1', documentSha256: 'a'.repeat(64), nomFichier: 'statuts.pdf' })
  })
  it('dépôt : empreinte calculée dans le navigateur si MS-DOCUMENT ne la renvoie pas', async () => {
    brancher({ id: 'doc-2', sha256: '' }, {})
    await participantHttp.deposerPiece('i1', 'STATUTS', new File(['abc'], 'a.pdf'))
    expect(corps(1).documentSha256).toBe('ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad')
  })
  it('empreinte SHA-256 en hexadécimal minuscule', async () => {
    expect(await empreinteSha256(new Blob(['abc']))).toBe('ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad')
  })
})
