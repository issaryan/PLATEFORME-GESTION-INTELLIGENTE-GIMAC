import { afterEach, describe, expect, it, vi } from 'vitest'
import { pouvoirsHttp } from './pouvoirs'
import { pilotageHttp } from './pilotage'

// Contrat HTTP des pouvoirs hiérarchiques (D-81, docs/lot1/pouvoirs-hierarchiques.md §3, §4, §6) : MS-IAM sous
// /bff/des/iam (pouvoirs, demandes « miennes » paginées à partir de 0, rattachement) et MS-PILOTAGE sous
// /bff/des/pilotage (groupe, prise, attribution, relance, création à plusieurs ou à une unité).
const appels = () => (globalThis.fetch as unknown as { mock: { calls: [URL, RequestInit][] } }).mock.calls
afterEach(() => vi.restoreAllMocks())
const brancher = () => vi.spyOn(globalThis, 'fetch').mockImplementation(async () => new Response('{}', { status: 200, headers: { 'Content-Type': 'application/json' } }))
const appel = (i: number) => {
  const u = new URL(String(appels()[i][0]))
  return `${appels()[i][1].method} ${u.pathname}${u.search}`
}
const corps = (i: number) => JSON.parse(String(appels()[i][1].body))

describe('pouvoirsHttp', () => {
  it('mes pouvoirs, demandes, annulation, rattachement', async () => {
    brancher()
    const d = { type: 'STATUT' as const, cibleId: 'u-1', contenu: { actif: false }, motif: 'Départ' }
    await pouvoirsHttp.moi()
    await pouvoirsHttp.demander(d)
    await pouvoirsHttp.demandes({})
    await pouvoirsHttp.demandes({ etat: 'EN_ATTENTE', page: 2, taille: 10 })
    await pouvoirsHttp.demande('D/1')
    await pouvoirsHttp.annuler('D1')
    await pouvoirsHttp.rattacher('u 1', { unite: 'POLE_RESEAU', responsableId: 'u-2' })
    await pouvoirsHttp.rattacher('u-3', { unite: null, responsableId: 'u-2', motif: '  Réorganisation du pôle  ' })
    expect([0, 1, 2, 3, 4, 5, 6, 7].map(appel)).toEqual([
      'GET /bff/des/iam/pouvoirs/moi',
      'POST /bff/des/iam/demandes',
      'GET /bff/des/iam/demandes?vue=miennes&page=0&taille=20',
      'GET /bff/des/iam/demandes?vue=miennes&etat=EN_ATTENTE&page=2&taille=10',
      'GET /bff/des/iam/demandes/D%2F1',
      'POST /bff/des/iam/demandes/D1/annulation',
      'PUT /bff/des/iam/pouvoirs/subordonnes/u%201/rattachement',
      'PUT /bff/des/iam/pouvoirs/subordonnes/u-3/rattachement',
    ])
    expect(corps(1)).toEqual(d)
    // Les deux valeurs partent toujours (null retire), le motif seulement s'il est donné.
    expect(corps(6)).toEqual({ unite: 'POLE_RESEAU', responsableId: 'u-2' })
    expect(corps(7)).toEqual({ unite: null, responsableId: 'u-2', motif: 'Réorganisation du pôle' })
  })
  it('tâches confiées : groupe, à une unité, prise, attribution, relance', async () => {
    brancher()
    const t = { titre: 'Vérifier', description: null, echeance: null, priorite: 'NORMALE' as const }
    await pilotageHttp.creerGroupe({ ...t, responsables: ['a', 'b'] })
    await pilotageHttp.creerTache({ ...t, uniteCible: 'POLE_RESEAU' })
    await pilotageHttp.groupe('G 1')
    await pilotageHttp.prendre('T1')
    await pilotageHttp.attribuer('T1', 'u-2')
    await pilotageHttp.relancer('T1')
    await pilotageHttp.taches({ vue: 'unite' })
    expect([0, 1, 2, 3, 4, 5, 6].map(appel)).toEqual([
      'POST /bff/des/pilotage/taches',
      'POST /bff/des/pilotage/taches',
      'GET /bff/des/pilotage/taches/groupes/G%201',
      'POST /bff/des/pilotage/taches/T1/prise',
      'POST /bff/des/pilotage/taches/T1/attribution',
      'POST /bff/des/pilotage/taches/T1/relance',
      'GET /bff/des/pilotage/taches?vue=unite&page=0&taille=20',
    ])
    expect(corps(0)).toEqual({ ...t, responsables: ['a', 'b'] })
    expect(corps(1)).toEqual({ ...t, uniteCible: 'POLE_RESEAU' })
    expect(corps(4)).toEqual({ responsableId: 'u-2' })
  })
})
