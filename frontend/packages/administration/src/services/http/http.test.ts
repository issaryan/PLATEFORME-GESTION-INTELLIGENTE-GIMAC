import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { comptesHttp as fabriqueComptes } from './comptes'
import { configurationHttp as fabriqueConfiguration } from './configuration'
import { ouvrirFlux, plateformeHttp } from './plateforme'
import { servicesConsoleHttp } from './index'
import { infrastructureHttp } from './infrastructure'

// Adaptateurs HTTP de l'administration : routes /bff/console/iam ou /bff/des/iam (MS-IAM, D-69) et
// /api/plateforme (MS-GATEWAY).
const comptesHttp = fabriqueComptes('console')
const configurationHttp = fabriqueConfiguration('console')
const json = (corps: unknown, status = 200) =>
  status === 204 ? new Response(null, { status }) : new Response(JSON.stringify(corps), { status, headers: { 'Content-Type': 'application/json' } })
type Espion = { mock: { calls: unknown[][] } }
const appels = (spy: Espion) =>
  spy.mock.calls.map(([u, i]) => {
    const url = new URL(String(u))
    const init = i as RequestInit | undefined
    return `${init?.method} ${url.pathname}${url.search}`
  })
const entete = (spy: Espion, i: number, nom: string) => new Headers((spy.mock.calls[i][1] as RequestInit).headers).get(nom)

// WebAuthn n'existe que dans un contexte sécurisé : jsdom n'expose pas PublicKeyCredential.
beforeEach(() => {
  vi.stubGlobal('PublicKeyCredential', class {})
  vi.stubGlobal('isSecureContext', true)
  if (!navigator.credentials) Object.defineProperty(navigator, 'credentials', { value: {}, configurable: true })
})
afterEach(() => {
  vi.restoreAllMocks()
  vi.useRealTimers()
  vi.unstubAllGlobals()
})

describe('comptes (MS-IAM)', () => {
  it('lectures : pagination à partir de 0, filtres omis quand vides', async () => {
    const spy = vi.spyOn(globalThis, 'fetch').mockImplementation(async () => json({ contenu: [], total: 0 }))
    await comptesHttp.lister({ page: 2, taille: 25, q: 'ndong', profil: 'ROLE_RSSI', etat: 'SANS_2FA' })
    await comptesHttp.lister({ page: 1, taille: 50 })
    await comptesHttp.indicateurs()
    await comptesHttp.journal({ page: 1, taille: 10, compte: 'u-1' })
    await comptesHttp.journal({ page: 3, taille: 10 })
    await comptesHttp.lire('u 1')
    await comptesHttp.sessions('u-1')
    expect(appels(spy)).toEqual([
      'GET /bff/console/iam/comptes?page=1&taille=25&q=ndong&profil=ROLE_RSSI&etat=SANS_2FA',
      'GET /bff/console/iam/comptes?page=0&taille=50',
      'GET /bff/console/iam/comptes/indicateurs',
      'GET /bff/console/iam/comptes/journal?page=0&taille=10&compte=u-1',
      'GET /bff/console/iam/comptes/journal?page=2&taille=10',
      'GET /bff/console/iam/comptes/u%201',
      'GET /bff/console/iam/comptes/u-1/sessions',
    ])
  })

  it('écritures : corps, motif, assertion FIDO2 sur les actes sensibles', async () => {
    const spy = vi.spyOn(globalThis, 'fetch').mockImplementation(async (_u, i) => json({}, (i as RequestInit).method === 'DELETE' ? 204 : 200))
    await comptesHttp.creer({ identifiant: 'a.b', prenom: 'A', nom: 'B', email: 'a@b.cm', profils: ['ROLE_SNA'], envoyerInvitation: true, motDePasseProvisoire: false })
    await comptesHttp.modifier('u-1', { telephone: '+237 699 00 00 00' })
    await comptesHttp.definirProfils('u-1', ['ROLE_SNA'], 'Changement de fonction', 'ASSERTION')
    await comptesHttp.activer('u-1', 'Retour de congé')
    await comptesHttp.desactiver('u-1', 'Départ')
    await comptesHttp.supprimer('u-1', 'Doublon', 'ASSERTION-2')
    await comptesHttp.exigerActions('u-1', ['MOT_DE_PASSE'], true, 'Suspicion de fuite')
    await comptesHttp.reinitialiserSecondFacteur('u-1', 'Téléphone perdu')
    await comptesHttp.motDePasseProvisoire('u-1', 'Oubli')
    await comptesHttp.fermerSessions('u-1', 'Poste volé')
    expect(appels(spy)).toEqual([
      'POST /bff/console/iam/comptes',
      'PUT /bff/console/iam/comptes/u-1',
      'PUT /bff/console/iam/comptes/u-1/profils',
      'POST /bff/console/iam/comptes/u-1/activation',
      'POST /bff/console/iam/comptes/u-1/desactivation',
      'DELETE /bff/console/iam/comptes/u-1?motif=Doublon',
      'POST /bff/console/iam/comptes/u-1/actions-requises',
      'POST /bff/console/iam/comptes/u-1/second-facteur/reinitialisation',
      'POST /bff/console/iam/comptes/u-1/mot-de-passe-provisoire',
      'POST /bff/console/iam/comptes/u-1/sessions/fermeture',
    ])
    expect(entete(spy, 2, 'X-Reauth-Assertion')).toBe('ASSERTION')
    expect(entete(spy, 5, 'X-Reauth-Assertion')).toBe('ASSERTION-2')
    expect(entete(spy, 3, 'X-Reauth-Assertion')).toBeNull()
    expect(JSON.parse(String((spy.mock.calls[2][1] as RequestInit).body))).toEqual({ profils: ['ROLE_SNA'], motif: 'Changement de fonction' })
    expect(JSON.parse(String((spy.mock.calls[6][1] as RequestInit).body))).toEqual({ actions: ['MOT_DE_PASSE'], parCourriel: true, motif: 'Suspicion de fuite' })
  })

  it('428 : réauthentification exigée remontée telle quelle', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce(json({ code: 'IAM_REAUTH_REQUIRED', message: 'exigée' }, 428))
    await expect(comptesHttp.definirProfils('u-1', [], 'm', '')).rejects.toMatchObject({ status: 428, code: 'IAM_REAUTH_REQUIRED' })
  })
})

describe('configuration (MS-IAM)', () => {
  it('politique, messagerie, profils et permissions', async () => {
    const spy = vi.spyOn(globalThis, 'fetch').mockImplementation(async (_u, i) => json({}, (i as RequestInit).method === 'POST' ? 204 : 200))
    await configurationHttp.politique()
    await configurationHttp.appliquerPolitique({ longueurMinimale: 14 } as never, 'PSSI 2026', 'A1')
    await configurationHttp.messagerie()
    await configurationHttp.appliquerMessagerie({ hote: 'smtp' } as never, 'Nouveau relais')
    await configurationHttp.testerMessagerie('rssi@gimac-afr.org')
    await configurationHttp.profils()
    await configurationHttp.permissions()
    await configurationHttp.definirPermissions('ROLE_SNA', ['HOM_EVIDENCE_SUBMIT'], 'Revue annuelle', 'A2')
    expect(appels(spy)).toEqual([
      'GET /bff/console/iam/configuration/politique-securite',
      'PUT /bff/console/iam/configuration/politique-securite',
      'GET /bff/console/iam/configuration/messagerie',
      'PUT /bff/console/iam/configuration/messagerie',
      'POST /bff/console/iam/configuration/messagerie/test',
      'GET /bff/console/iam/configuration/profils',
      'GET /bff/console/iam/configuration/permissions',
      'PUT /bff/console/iam/configuration/profils/ROLE_SNA/permissions',
    ])
    expect(JSON.parse(String((spy.mock.calls[1][1] as RequestInit).body))).toEqual({ politique: { longueurMinimale: 14 }, motif: 'PSSI 2026' })
    expect(entete(spy, 1, 'X-Reauth-Assertion')).toBe('A1')
    expect(entete(spy, 7, 'X-Reauth-Assertion')).toBe('A2')
    expect(entete(spy, 3, 'X-Reauth-Assertion')).toBeNull()
  })
})

describe('supervision (MS-GATEWAY)', () => {
  it('routes /api/plateforme', async () => {
    const spy = vi.spyOn(globalThis, 'fetch').mockImplementation(async () => json({}))
    await plateformeHttp.services()
    await plateformeHttp.serveurs()
    await plateformeHttp.alertes()
    await plateformeHttp.configuration()
    await plateformeHttp.journalisation('iam', 'cm.gimac')
    await plateformeHttp.journalisation('iam')
    await plateformeHttp.modifierJournalisation('iam', { logger: 'ROOT', niveau: 'DEBUG', motif: 'Analyse incident' })
    expect(appels(spy)).toEqual([
      'GET /api/plateforme/services',
      'GET /api/plateforme/serveurs',
      'GET /api/plateforme/alertes',
      'GET /api/plateforme/configuration',
      'GET /api/plateforme/journalisation/iam?filtre=cm.gimac',
      'GET /api/plateforme/journalisation/iam',
      'POST /api/plateforme/journalisation/iam',
    ])
  })

  it('services HTTP assemblés ; réauthentification sur la couche console', async () => {
    const spy = vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce(json({ code: 'IAM_NO_SECURITY_KEY' }, 409))
    const s = servicesConsoleHttp()
    expect(s.plateforme).toBe(plateformeHttp)
    await expect(s.reauth.assertion('ADMIN_SENSITIVE')).rejects.toMatchObject({ code: 'IAM_NO_SECURITY_KEY' })
    expect(String(spy.mock.calls[0][0])).toContain('/bff/console/iam/reauthentification/defi')
  })
  it('département (D-69) : comptes, politique et clé de sécurité sous /bff/des/iam', async () => {
    const spy = vi.spyOn(globalThis, 'fetch').mockImplementation(async () => json({ contenu: [], total: 0 }))
    const s = servicesConsoleHttp('des')
    await s.comptes.lister({ page: 1, taille: 20 })
    await s.configuration.politique()
    expect(appels(spy)).toEqual(['GET /bff/des/iam/comptes?page=0&taille=20', 'GET /bff/des/iam/configuration/politique-securite'])
    spy.mockImplementation(async () => json({ code: 'IAM_NO_SECURITY_KEY', title: 'Aucune clé' }, 409))
    await expect(s.reauth.assertion('ADMIN_SENSITIVE')).rejects.toBeTruthy()
    expect(String(spy.mock.calls[2][0])).toContain('/bff/des/iam/reauthentification/defi')
  })
})

// EventSource simulé : ouverture, événements nommés, erreur.
class FauxFlux {
  static instances: FauxFlux[] = []
  onopen: (() => void) | null = null
  onerror: (() => void) | null = null
  ecouteurs: Record<string, (m: MessageEvent) => void> = {}
  ferme = false
  url: string
  init?: EventSourceInit
  constructor(url: string, init?: EventSourceInit) {
    this.url = url
    this.init = init
    FauxFlux.instances.push(this)
  }
  addEventListener(nom: string, f: (m: MessageEvent) => void) {
    this.ecouteurs[nom] = f
  }
  close() {
    this.ferme = true
  }
  emettre(nom: string, data: string) {
    this.ecouteurs[nom]?.({ data } as MessageEvent)
  }
}

describe('flux temps réel /api/plateforme/flux', () => {
  it('connexion, impulsions, coupure puis reconnexion à délai croissant, fermeture', () => {
    vi.useFakeTimers()
    FauxFlux.instances = []
    vi.stubGlobal('EventSource', FauxFlux)
    const etats: string[] = []
    const impulsions: number[] = []
    const fermer = plateformeHttp.flux({ onEtat: (e) => etats.push(e), onImpulsion: (i) => impulsions.push(i.sequence) })
    const f1 = FauxFlux.instances[0]
    expect(f1.url).toBe('/api/plateforme/flux')
    expect(f1.init).toEqual({ withCredentials: true })
    f1.onopen?.()
    f1.emettre('plateforme', JSON.stringify({ sequence: 7, instantane: {}, nombreAlertes: 2 }))
    f1.emettre('plateforme', '{illisible')
    expect(impulsions).toEqual([7])
    f1.onerror?.()
    expect(f1.ferme).toBe(true)
    expect(etats).toEqual(['connexion', 'connecte', 'perdu'])
    vi.advanceTimersByTime(999)
    expect(FauxFlux.instances).toHaveLength(1)
    vi.advanceTimersByTime(1)
    expect(FauxFlux.instances).toHaveLength(2)
    const f2 = FauxFlux.instances[1]
    f2.onerror?.()
    vi.advanceTimersByTime(1999)
    expect(FauxFlux.instances).toHaveLength(2)
    vi.advanceTimersByTime(1)
    expect(FauxFlux.instances).toHaveLength(3)
    FauxFlux.instances[2].onopen?.()
    expect(etats.at(-1)).toBe('connecte')
    fermer()
    expect(FauxFlux.instances[2].ferme).toBe(true)
    FauxFlux.instances[2].onerror?.()
    vi.advanceTimersByTime(60_000)
    expect(FauxFlux.instances).toHaveLength(3)
    vi.unstubAllGlobals()
  })

  it('délai plafonné et adresse configurable', () => {
    vi.useFakeTimers()
    FauxFlux.instances = []
    vi.stubGlobal('EventSource', FauxFlux)
    const fermer = ouvrirFlux({ onEtat: () => undefined, onImpulsion: () => undefined }, { adresse: '/x', baseMs: 10, maxMs: 15 })
    for (let i = 0; i < 3; i++) {
      FauxFlux.instances.at(-1)!.onerror?.()
      vi.advanceTimersByTime(15)
    }
    expect(FauxFlux.instances).toHaveLength(4)
    expect(FauxFlux.instances[0].url).toBe('/x')
    fermer()
    vi.unstubAllGlobals()
  })
})

// Exploitation de l'infrastructure (D-78) : InfrastructureResource de MS-GATEWAY.
describe('infrastructure /api/plateforme/infrastructure', () => {
  const charge = { nom: 'iam', orchestrateur: 'KUBERNETES', etat: 'EN_COURS', sante: 'SAINE', repliques: { voulues: 1, pretes: 1 }, redemarrages: 0, image: 'iam:1', demarreeLe: null, protegee: false, gestes: [] }
  const avecEntete = (corps: unknown, orchestrateur?: string) =>
    new Response(JSON.stringify(corps), { status: 200, headers: { 'Content-Type': 'application/json', ...(orchestrateur ? { 'X-Orchestrateur': orchestrateur } : {}) } })

  it('charges : orchestrateur de l’en-tête X-Orchestrateur ; AUCUN : liste vide ; en-tête absent : déduit des charges', async () => {
    const spy = vi.spyOn(globalThis, 'fetch')
      .mockResolvedValueOnce(avecEntete([charge], 'KUBERNETES'))
      .mockResolvedValueOnce(avecEntete([], 'AUCUN'))
      .mockResolvedValueOnce(avecEntete([charge]))
      .mockResolvedValueOnce(avecEntete([]))
    expect(await infrastructureHttp.charges()).toEqual({ orchestrateur: 'KUBERNETES', charges: [charge] })
    expect(await infrastructureHttp.charges()).toEqual({ orchestrateur: 'AUCUN', charges: [] })
    expect((await infrastructureHttp.charges()).orchestrateur).toBe('KUBERNETES')
    expect(await infrastructureHttp.charges()).toEqual({ orchestrateur: 'AUCUN', charges: [] })
    expect(appels(spy)[0]).toBe('GET /api/plateforme/infrastructure/charges')
  })

  it('journaux, historique paginé et gestes : routes, corps, assertion FIDO2 dans X-Reauth-Assertion', async () => {
    const spy = vi.spyOn(globalThis, 'fetch').mockImplementation(async () => json({}, 200))
    await infrastructureHttp.journaux('iam', 200, 'PT10M')
    await infrastructureHttp.operations(2, 20)
    await infrastructureHttp.executer('iam', 'REDEMARRER', { motif: 'Motif suffisant' }, 'YXNz')
    await infrastructureHttp.executer('iam', 'ARRETER', { motif: 'Motif suffisant' }, 'YXNz')
    await infrastructureHttp.executer('iam', 'DEMARRER', { motif: 'Motif suffisant' }, 'YXNz')
    await infrastructureHttp.executer('iam', 'ECHELLE', { motif: 'Motif suffisant', repliques: 3 }, 'YXNz')
    await infrastructureHttp.executer('iam', 'RETABLIR', { motif: 'Motif suffisant' }, 'YXNz')
    expect(appels(spy)).toEqual([
      'GET /api/plateforme/infrastructure/charges/iam/journaux?lignes=200&depuis=PT10M',
      'GET /api/plateforme/infrastructure/operations?page=2&taille=20',
      'POST /api/plateforme/infrastructure/charges/iam/redemarrage',
      'POST /api/plateforme/infrastructure/charges/iam/arret',
      'POST /api/plateforme/infrastructure/charges/iam/demarrage',
      'PUT /api/plateforme/infrastructure/charges/iam/repliques',
      'POST /api/plateforme/infrastructure/charges/iam/retablissement',
    ])
    expect(entete(spy, 2, 'X-Reauth-Assertion')).toBe('YXNz')
    expect(JSON.parse(String((spy.mock.calls[2][1] as RequestInit).body))).toEqual({ motif: 'Motif suffisant' })
    expect(JSON.parse(String((spy.mock.calls[5][1] as RequestInit).body))).toEqual({ motif: 'Motif suffisant', repliques: 3 })
  })

  it('flux : opérations « operation-infrastructure » ; événement illisible ignoré', () => {
    FauxFlux.instances = []
    vi.stubGlobal('EventSource', FauxFlux)
    const ops: string[] = []
    const etats: string[] = []
    const fermer = infrastructureHttp.flux({ onEtat: (e) => etats.push(e), onOperation: (o) => ops.push(o.id) })
    const f = FauxFlux.instances[0]
    f.onopen?.()
    f.emettre('operation-infrastructure', JSON.stringify({ id: 'op-1' }))
    f.emettre('operation-infrastructure', '{illisible')
    f.emettre('plateforme', JSON.stringify({ sequence: 1 }))
    expect(ops).toEqual(['op-1'])
    expect(etats).toEqual(['connexion', 'connecte'])
    fermer()
    vi.unstubAllGlobals()
  })
})
