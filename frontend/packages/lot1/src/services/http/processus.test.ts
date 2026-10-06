import { afterEach, describe, expect, it, vi } from 'vitest'
import { processusHttp } from './processus'
import { servicesHttp } from '.'

// Contrat HTTP des processus d'unité (docs/lot1/processus-unite.md §3 et §5.1) : chemins exacts de MS-PILOTAGE sous
// /bff/des/pilotage/processus, corps JSON, en-tête X-Reauth-Assertion de la signature, dépôt fragmenté de MS-DOCUMENT
// (finalité PROCESSUS_UNITE, référence : l'instance).
const appels = () => (globalThis.fetch as unknown as { mock: { calls: [URL, RequestInit][] } }).mock.calls
afterEach(() => vi.restoreAllMocks())
const brancher = (corps = '{}') => vi.spyOn(globalThis, 'fetch').mockImplementation(async (_u, i) => new Response((i as RequestInit).method === 'DELETE' ? null : corps, { status: (i as RequestInit).method === 'DELETE' ? 204 : 200, headers: { 'Content-Type': 'application/json' } }))
const appel = (i: number) => {
  const u = new URL(String(appels()[i][0]))
  return `${appels()[i][1].method} ${u.pathname}${u.search}`
}
const corps = (i: number) => (appels()[i][1].body ? JSON.parse(String(appels()[i][1].body)) : undefined)
const entete = (i: number) => new Headers(appels()[i][1].headers).get('X-Reauth-Assertion')

describe('processusHttp', () => {
  it('définitions : catalogue filtré, lecture, création, modification, suppression, contrôle, publication, versions, retrait', async () => {
    brancher()
    const s = { code: 'P', unite: 'POLE_RESEAU', libelle: 'P', description: '', categorie: '', delaiCibleJours: null, etapes: [] }
    await processusHttp.lister({})
    await processusHttp.lister({ unite: 'POLE_RESEAU', etat: 'PUBLIEE', q: ' flux ' })
    await processusHttp.lire('P/1')
    await processusHttp.creer(s)
    await processusHttp.modifier('P1', s)
    await processusHttp.supprimer('P1')
    await processusHttp.controler('P1')
    await processusHttp.publier('P1', {})
    await processusHttp.publier('P1', { motif: ' Levée ', leverAvertissements: true })
    await processusHttp.nouvelleVersion('P1')
    await processusHttp.retirer('P1', 'Obsolète')
    expect(appels().map((_, i) => appel(i))).toEqual([
      'GET /bff/des/pilotage/processus',
      'GET /bff/des/pilotage/processus?unite=POLE_RESEAU&etat=PUBLIEE&q=flux',
      'GET /bff/des/pilotage/processus/P%2F1',
      'POST /bff/des/pilotage/processus',
      'PUT /bff/des/pilotage/processus/P1',
      'DELETE /bff/des/pilotage/processus/P1',
      'POST /bff/des/pilotage/processus/P1/controle',
      'POST /bff/des/pilotage/processus/P1/publication',
      'POST /bff/des/pilotage/processus/P1/publication',
      'POST /bff/des/pilotage/processus/P1/nouvelle-version',
      'POST /bff/des/pilotage/processus/P1/retrait',
    ])
    expect(corps(3)).toEqual(s)
    expect(corps(7)).toEqual({})
    expect(corps(8)).toEqual({ motif: 'Levée', leverAvertissements: true })
    expect(corps(10)).toEqual({ motif: 'Obsolète' })
  })

  it('instances : liste, lancement, lecture, livrables, champs, signature signée, fin, validation, réaffectation, suspension, reprise, annulation', async () => {
    brancher()
    const l = { intitule: 'Flux', champs: { PORT: 22 }, designations: { MISE_EN_OEUVRE: 'u-collab', 'RECETTE.signature.S': 'u-x' } }
    await processusHttp.instances({ vue: 'miennes' })
    await processusHttp.instances({ vue: 'unite', etat: 'EN_COURS', page: 2, taille: 50 })
    await processusHttp.lancer('P1', l)
    await processusHttp.instance('I/1')
    await processusHttp.deposerLivrable('I1', 'ANALYSE', 'NOTE', 'DOC-1')
    await processusHttp.saisirChamps('I1', 'DEMANDE', { PORT: 22, DATE: null })
    await processusHttp.signer('I1', 'RECETTE', 'S', 'ASSERTION')
    await processusHttp.terminer('I1', 'ANALYSE')
    await processusHttp.valider('I1', 'ANALYSE', 'VALIDER')
    await processusHttp.valider('I1', 'ANALYSE', 'RENVOYER', ' Incomplet ')
    await processusHttp.reaffecter('I1', 'ANALYSE', 'u-res', 'Absence')
    await processusHttp.suspendre('I1', 'Gel')
    await processusHttp.reprendre('I1')
    await processusHttp.reprendre('I1', 'Fin du gel')
    await processusHttp.annuler('I1', 'Abandon')
    expect(appels().map((_, i) => appel(i))).toEqual([
      'GET /bff/des/pilotage/processus/instances?vue=miennes&page=0&taille=20',
      'GET /bff/des/pilotage/processus/instances?vue=unite&etat=EN_COURS&page=2&taille=50',
      'POST /bff/des/pilotage/processus/P1/instances',
      'GET /bff/des/pilotage/processus/instances/I%2F1',
      'POST /bff/des/pilotage/processus/instances/I1/etapes/ANALYSE/livrables',
      'PUT /bff/des/pilotage/processus/instances/I1/etapes/DEMANDE/champs',
      'POST /bff/des/pilotage/processus/instances/I1/etapes/RECETTE/signature',
      'POST /bff/des/pilotage/processus/instances/I1/etapes/ANALYSE/fin',
      'POST /bff/des/pilotage/processus/instances/I1/etapes/ANALYSE/validation',
      'POST /bff/des/pilotage/processus/instances/I1/etapes/ANALYSE/validation',
      'POST /bff/des/pilotage/processus/instances/I1/etapes/ANALYSE/reaffectation',
      'POST /bff/des/pilotage/processus/instances/I1/suspension',
      'POST /bff/des/pilotage/processus/instances/I1/reprise',
      'POST /bff/des/pilotage/processus/instances/I1/reprise',
      'POST /bff/des/pilotage/processus/instances/I1/annulation',
    ])
    expect(corps(2)).toEqual(l)
    expect(corps(4)).toEqual({ livrable: 'NOTE', documentId: 'DOC-1' })
    expect(corps(5)).toEqual({ valeurs: { PORT: 22, DATE: null } })
    expect(corps(6)).toEqual({ signature: 'S' })
    expect(entete(6)).toBe('ASSERTION')
    expect(entete(5)).toBeNull()
    expect(corps(8)).toEqual({ decision: 'VALIDER' })
    expect(corps(9)).toEqual({ decision: 'RENVOYER', motif: 'Incomplet' })
    expect(corps(10)).toEqual({ personneId: 'u-res', motif: 'Absence' })
    expect(corps(11)).toEqual({ motif: 'Gel' })
    expect(corps(12)).toEqual({})
    expect(corps(13)).toEqual({ motif: 'Fin du gel' })
    expect(corps(14)).toEqual({ motif: 'Abandon' })
  })

  it('livrable : dépôt fragmenté de MS-DOCUMENT, finalité PROCESSUS_UNITE, référence de l’instance ; services de la couche interne', async () => {
    const spy = vi.spyOn(globalThis, 'fetch').mockImplementation(async (u, i) => {
      const chemin = new URL(String(u)).pathname
      const m = (i as RequestInit).method
      const json = (b: unknown, status = 200) => new Response(JSON.stringify(b), { status, headers: { 'Content-Type': 'application/json' } })
      // Partie déjà reçue (reprise) : aucun envoi d'octets, fin du dépôt, document disponible.
      if (m === 'POST' && chemin.endsWith('/depots')) return json({ id: 'D1', taillePartie: 1024, nombreParties: 1, etat: 'OUVERT', partiesRecues: [1] }, 201)
      return json({ id: 'D1', etat: 'DISPONIBLE', documentId: 'DOC-9', sha256: 'abc', dedoublonne: false, partiesRecues: [1] })
    })
    const r = await processusHttp.televerserLivrable(new File(['x'], 'pv.pdf', { type: 'application/pdf' }), 'INS-1', { intervalleMs: 1 })
    expect(r).toMatchObject({ documentId: 'DOC-9' })
    const ouverture = spy.mock.calls.find(([, i]) => (i as RequestInit).method === 'POST')!
    expect(new URL(String(ouverture[0])).pathname).toBe('/bff/des/document/depots')
    expect(JSON.parse(String((ouverture[1] as RequestInit).body))).toMatchObject({ finalite: 'PROCESSUS_UNITE', reference: 'INS-1', nomFichier: 'pv.pdf' })
    expect(servicesHttp('des').processus).toBe(processusHttp)
  })
})
