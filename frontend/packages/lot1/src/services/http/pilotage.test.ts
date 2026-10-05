import { afterEach, describe, expect, it, vi } from 'vitest'
import { pilotageHttp } from './pilotage'

// Contrat HTTP de « Mon activité » (docs/lot1/mon-activite.md §2 et §1.3) : chemins exacts de MS-PILOTAGE sous
// /bff/des/pilotage et de l'organisation de MS-IAM sous /bff/des/iam/organisation, corps JSON.
const appels = () => (globalThis.fetch as unknown as { mock: { calls: [URL, RequestInit][] } }).mock.calls
afterEach(() => vi.restoreAllMocks())
const brancher = (corps = '{}', type = 'application/json') => vi.spyOn(globalThis, 'fetch').mockImplementation(async () => new Response(corps, { status: 200, headers: { 'Content-Type': type } }))
const appel = (i: number) => {
  const u = new URL(String(appels()[i][0]))
  return `${appels()[i][1].method} ${u.pathname}${u.search}`
}
const corps = (i: number) => JSON.parse(String(appels()[i][1].body))

describe('pilotageHttp', () => {
  it('agenda (§2.1) : intervalle, collaborateur, création, modification, suppression, réponse', async () => {
    brancher()
    const e = { titre: 'Revue', description: null, lieu: 'Salle 2', debut: '2026-10-06T09:00:00+01:00', fin: '2026-10-06T10:00:00+01:00', journeeEntiere: false, categorie: 'REUNION' as const, participants: ['u2'] }
    await pilotageHttp.agenda('2026-10-05', '2026-10-11')
    await pilotageHttp.agenda('2026-10-05', '2026-10-11', 'u 2')
    await pilotageHttp.creerEvenement(e)
    await pilotageHttp.modifierEvenement('E/1', e)
    await pilotageHttp.supprimerEvenement('E1')
    await pilotageHttp.repondre('E1', 'ACCEPTE')
    expect([0, 1, 2, 3, 4, 5].map(appel)).toEqual([
      'GET /bff/des/pilotage/agenda?du=2026-10-05&au=2026-10-11',
      'GET /bff/des/pilotage/agenda?du=2026-10-05&au=2026-10-11&collaborateur=u+2',
      'POST /bff/des/pilotage/agenda',
      'PUT /bff/des/pilotage/agenda/E%2F1',
      'DELETE /bff/des/pilotage/agenda/E1',
      'POST /bff/des/pilotage/agenda/E1/reponse',
    ])
    expect(corps(2)).toEqual(e)
    expect(corps(5)).toEqual({ reponse: 'ACCEPTE' })
  })
  it('activités (§2.2) : lecture, création, modification, statut (report avec son jour), suppression', async () => {
    brancher()
    const a = { jour: '2026-10-06', titre: 'Préparer', description: null, dureeMinutes: 90, ordre: 2 }
    await pilotageHttp.activites('2026-10-06', '2026-10-06')
    await pilotageHttp.activites('2026-10-06', '2026-10-06', 'u2')
    await pilotageHttp.creerActivite(a)
    await pilotageHttp.modifierActivite('A1', a)
    await pilotageHttp.changerStatut('A1', 'FAITE')
    await pilotageHttp.changerStatut('A1', 'REPORTEE', '2026-10-07')
    await pilotageHttp.supprimerActivite('A1')
    expect([0, 1, 2, 3, 4, 5, 6].map(appel)).toEqual([
      'GET /bff/des/pilotage/activites?du=2026-10-06&au=2026-10-06',
      'GET /bff/des/pilotage/activites?du=2026-10-06&au=2026-10-06&collaborateur=u2',
      'POST /bff/des/pilotage/activites',
      'PUT /bff/des/pilotage/activites/A1',
      'POST /bff/des/pilotage/activites/A1/statut',
      'POST /bff/des/pilotage/activites/A1/statut',
      'DELETE /bff/des/pilotage/activites/A1',
    ])
    expect(corps(3)).toEqual(a)
    expect(corps(4)).toEqual({ statut: 'FAITE' })
    expect(corps(5)).toEqual({ statut: 'REPORTEE', reporteeAu: '2026-10-07' })
  })
  it('tâches (§2.3) : vues paginées à partir de 0, détail, création, modification, transition motivée', async () => {
    brancher()
    const t = { titre: 'Mettre à jour', description: null, echeance: '2026-10-10', priorite: 'HAUTE' as const, responsableId: 'u2' }
    await pilotageHttp.taches({ vue: 'moi' })
    await pilotageHttp.taches({ vue: 'equipe', etat: 'BLOQUEE', page: 2, taille: 10 })
    await pilotageHttp.tache('T1')
    await pilotageHttp.creerTache(t)
    await pilotageHttp.modifierTache('T1', t)
    await pilotageHttp.transition('T1', 'EN_COURS')
    await pilotageHttp.transition('T1', 'BLOQUEE', 'Attente du participant')
    expect([0, 1, 2, 3, 4, 5, 6].map(appel)).toEqual([
      'GET /bff/des/pilotage/taches?vue=moi&page=0&taille=20',
      'GET /bff/des/pilotage/taches?vue=equipe&etat=BLOQUEE&page=2&taille=10',
      'GET /bff/des/pilotage/taches/T1',
      'POST /bff/des/pilotage/taches',
      'PUT /bff/des/pilotage/taches/T1',
      'POST /bff/des/pilotage/taches/T1/transition',
      'POST /bff/des/pilotage/taches/T1/transition',
    ])
    expect(corps(3)).toEqual(t)
    expect(corps(5)).toEqual({ etat: 'EN_COURS' })
    expect(corps(6)).toEqual({ etat: 'BLOQUEE', motif: 'Attente du participant' })
  })
  it('rapports (§2.4) : listes, détail, préremplissage, brouillon, soumission, visa', async () => {
    brancher()
    const r = { type: 'QUOTIDIEN' as const, periode: '2026-10-06', mode: 'REDIGE' as const, contenu: '# Rapport' }
    await pilotageHttp.rapports({ vue: 'moi' })
    await pilotageHttp.rapports({ vue: 'recus', type: 'HEBDOMADAIRE', etat: 'SOUMIS', page: 1, taille: 5 })
    await pilotageHttp.rapport('R1')
    await pilotageHttp.preremplir('HEBDOMADAIRE', '2026-W41')
    await pilotageHttp.creerRapport(r)
    await pilotageHttp.modifierRapport('R1', { ...r, document: { ticket: 'doc-1' } })
    await pilotageHttp.soumettre('R1')
    await pilotageHttp.viser('R1', 'Merci')
    expect([0, 1, 2, 3, 4, 5, 6, 7].map(appel)).toEqual([
      'GET /bff/des/pilotage/rapports?vue=moi&page=0&taille=20',
      'GET /bff/des/pilotage/rapports?vue=recus&type=HEBDOMADAIRE&etat=SOUMIS&page=1&taille=5',
      'GET /bff/des/pilotage/rapports/R1',
      'POST /bff/des/pilotage/rapports/preremplissage',
      'POST /bff/des/pilotage/rapports',
      'PUT /bff/des/pilotage/rapports/R1',
      'POST /bff/des/pilotage/rapports/R1/soumission',
      'POST /bff/des/pilotage/rapports/R1/visa',
    ])
    expect(corps(3)).toEqual({ type: 'HEBDOMADAIRE', periode: '2026-W41' })
    expect(corps(4)).toEqual(r)
    expect(corps(5)).toEqual({ ...r, document: { ticket: 'doc-1' } })
    expect(appels()[6][1].body).toBeUndefined()
    expect(corps(7)).toEqual({ commentaire: 'Merci' })
  })
  it('document d’un rapport : flux binaire ; équipe, synthèse, unités et rattachement (MS-IAM)', async () => {
    brancher('%PDF', 'application/pdf')
    const d = await pilotageHttp.documentRapport('R1')
    expect(await d.blob.text()).toBe('%PDF')
    vi.restoreAllMocks()
    brancher('[]')
    await pilotageHttp.equipe('2026-10-06')
    await pilotageHttp.synthese('2026-10-06')
    await pilotageHttp.unites()
    await pilotageHttp.organisation()
    expect([0, 1, 2, 3].map(appel)).toEqual([
      'GET /bff/des/pilotage/equipe?jour=2026-10-06',
      'GET /bff/des/pilotage/moi/synthese?jour=2026-10-06',
      'GET /bff/des/iam/organisation/unites',
      'GET /bff/des/iam/organisation/moi',
    ])
  })
  it('dépôt du document par morceaux (D-74) : finalité RAPPORT_ACTIVITE, référence = identifiant du rapport', async () => {
    const recus: { url: string; corps: unknown }[] = []
    vi.spyOn(globalThis, 'fetch').mockImplementation(async (u, o) => {
      const chemin = new URL(String(u)).pathname
      recus.push({ url: `${o?.method} ${chemin}`, corps: o?.body && typeof o.body === 'string' ? JSON.parse(o.body) : undefined })
      const depot = { id: 'dep-1', etat: chemin.endsWith('/fin') ? 'DISPONIBLE' : 'OUVERT', nomFichier: 'r.pdf', typeMime: 'application/pdf', taille: 0, taillePartie: 8, nombreParties: 0, partiesRecues: [], documentId: 'doc-9', sha256: 'a'.repeat(64) }
      return new Response(JSON.stringify(depot), { status: 200, headers: { 'Content-Type': 'application/json' } })
    })
    const r = await pilotageHttp.deposerDocument(new File([], 'r.pdf', { type: 'application/pdf' }), 'RAP-1')
    expect(r.documentId).toBe('doc-9')
    expect(recus[0]).toEqual({ url: 'POST /bff/des/document/depots', corps: { nomFichier: 'r.pdf', typeMime: 'application/pdf', taille: 0, finalite: 'RAPPORT_ACTIVITE', reference: 'RAP-1' } })
    expect(recus[1].url).toBe('POST /bff/des/document/depots/dep-1/fin')
  })
})
