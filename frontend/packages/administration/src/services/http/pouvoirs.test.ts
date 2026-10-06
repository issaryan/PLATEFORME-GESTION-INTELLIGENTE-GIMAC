import { afterEach, describe, expect, it, vi } from 'vitest'
import { normaliserMatrice, pouvoirsAdministrationHttp } from './pouvoirs'
import { configurationHttp } from './configuration'
import { organisationHttp } from './organisation'

// Contrat HTTP de l'administration des pouvoirs (D-81) : matrice et demandes sous /bff/{des,console}/iam (vue
// « a-traiter », pages à partir de 0), profils créés, responsable d'unité ; assertion FIDO2 des actes sensibles.
const json = (status = 200) => (status === 204 ? new Response(null, { status }) : new Response('{}', { status, headers: { 'Content-Type': 'application/json' } }))
type Espion = { mock: { calls: unknown[][] } }
const appels = (spy: Espion) => spy.mock.calls.map(([u, i]) => {
  const url = new URL(String(u))
  return `${(i as RequestInit).method} ${url.pathname}${url.search}`
})
const entete = (spy: Espion, i: number) => new Headers((spy.mock.calls[i][1] as RequestInit).headers).get('X-Reauth-Assertion')
const corps = (spy: Espion, i: number) => JSON.parse(String((spy.mock.calls[i][1] as RequestInit).body))
afterEach(() => vi.restoreAllMocks())

describe('pouvoirs (MS-IAM)', () => {
  it('matrice : liste nue acceptée', () => {
    const l = [{ profil: 'ROLE_SNA', actions: {} as never, profilsDemandables: [] }]
    expect(normaliserMatrice(l)).toEqual({ profils: l })
    expect(normaliserMatrice({ profils: l })).toEqual({ profils: l })
  })
  it('console : matrice, demandes à traiter, acceptation signée, refus motivé', async () => {
    const spy = vi.spyOn(globalThis, 'fetch').mockImplementation(async () => json())
    const s = pouvoirsAdministrationHttp('console')
    const e = { actions: { TACHE_CONFIER: 'UNITES' } as never, profilsDemandables: ['ROLE_SNA'] }
    await s.matrice()
    await s.modifierMatrice('ROLE_CHEF_DES', e, 'Revue', 'A1')
    await s.demandes({ page: 0, taille: 20 })
    await s.demandes({ etat: 'EN_ATTENTE', page: 1, taille: 10 })
    await s.demande('D/1')
    await s.accepter('D1', 'Validée', 'A2')
    await s.refuser('D1', 'Hors budget')
    expect(appels(spy)).toEqual([
      'GET /bff/console/iam/pouvoirs/matrice',
      'PUT /bff/console/iam/pouvoirs/matrice/ROLE_CHEF_DES',
      'GET /bff/console/iam/demandes?vue=a-traiter&page=0&taille=20',
      'GET /bff/console/iam/demandes?vue=a-traiter&etat=EN_ATTENTE&page=1&taille=10',
      'GET /bff/console/iam/demandes/D%2F1',
      'POST /bff/console/iam/demandes/D1/acceptation',
      'POST /bff/console/iam/demandes/D1/refus',
    ])
    expect(entete(spy, 1)).toBe('A1')
    expect(entete(spy, 5)).toBe('A2')
    expect(entete(spy, 6)).toBeNull()
    expect(corps(spy, 1)).toEqual({ ...e, motif: 'Revue' })
    expect(corps(spy, 6)).toEqual({ motif: 'Hors budget' })
  })
  it('département : profils créés et supprimés, responsable d’une unité', async () => {
    const spy = vi.spyOn(globalThis, 'fetch').mockImplementation(async (_u, i) => json((i as RequestInit).method === 'DELETE' ? 204 : 200))
    await pouvoirsAdministrationHttp('des').matrice()
    await configurationHttp('des').creerProfil({ code: 'ROLE_X', description: 'X', permissions: [] }, 'Nouveau', 'A3')
    await configurationHttp('des').supprimerProfil('ROLE_X', 'Ménage', 'A4')
    await organisationHttp('des').modifierUnite('DES', { code: 'DES', libelle: 'DES', type: 'DEPARTEMENT', parent: 'DG', active: true, responsableId: 'u-1' })
    expect(appels(spy)).toEqual([
      'GET /bff/des/iam/pouvoirs/matrice',
      'POST /bff/des/iam/configuration/profils',
      'DELETE /bff/des/iam/configuration/profils/ROLE_X?motif=M%C3%A9nage',
      'PUT /bff/des/iam/organisation/unites/DES',
    ])
    expect(entete(spy, 1)).toBe('A3')
    expect(entete(spy, 2)).toBe('A4')
    expect(corps(spy, 1)).toEqual({ code: 'ROLE_X', description: 'X', permissions: [], motif: 'Nouveau' })
    expect(corps(spy, 3).responsableId).toBe('u-1')
  })
})
