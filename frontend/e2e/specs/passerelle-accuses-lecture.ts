import type { Page, Route } from '@playwright/test'

// Passerelle simulée des accusés de lecture (D-88, docs/lot1/accuses-lecture.md §4) pour l'application du
// département construite avec VITE_SERVICES=http : MS-COLLABORATION sous /bff/des/collaboration/messagerie, aux
// formes exactes du service (MessageVue.lecture, dernierMessage.lecture, GET …/messages/{messageId}/lectures avec
// `lu` et `luLe`), et le canal poussé /bff/des/live qui remet `collaboration.conversation-read.v1`
// ({ id, type, subject, topic: "collaboration", acteur, luJusquA }) à la connexion suivante.

const il = (minutes: number) => new Date(Date.now() - minutes * 60_000).toISOString()
const moi = { id: 'u-chef', nom: 'Paul Mbarga Atangana' }
const aicha = { id: 'u-aicha', nom: 'Aïcha Mahamat Nour' }
const rachel = { id: 'u-rachel', nom: 'Rachel Biyong Mengue' }
const herve = { id: 'u-herve', nom: 'Hervé Kamga Tchoupo' }
const M = '/bff/des/collaboration/messagerie'

type Lecture = { etat: 'ENVOYE' | 'LU_PAR_CERTAINS' | 'LU'; lus: number; destinataires: number }
type MessageE2E = { id: string; conversationId: string; auteur: { id: string; nom: string; fonction: null }; moi: boolean; contenu: string; envoyeLe: string; etiquettes: never[]; piecesJointes: never[]; lecture?: Lecture }
const message = (id: string, conversationId: string, auteur: { id: string; nom: string }, contenu: string, envoyeLe: string, lecture?: Lecture): MessageE2E => ({
  id, conversationId, auteur: { ...auteur, fonction: null }, moi: auteur.id === moi.id, contenu, envoyeLe, etiquettes: [], piecesJointes: [], ...(lecture ? { lecture } : {}),
})

export class PasserelleAccusesLecture {
  appels: { methode: string; chemin: string }[] = []
  /** Événements en attente de la prochaine connexion du canal poussé. */
  private evenements: Record<string, unknown>[] = []
  private sequence = 0
  messages: Record<string, MessageE2E[]> = {
    'c-direct': [
      message('d-1', 'c-direct', aicha, 'Le procès-verbal de LIA-2026-027 est prêt.', il(90)),
      message('d-2', 'c-direct', moi, 'Merci, je le signe ce soir.', il(30), { etat: 'ENVOYE', lus: 0, destinataires: 1 }),
    ],
    'c-groupe': [
      message('g-1', 'c-groupe', rachel, 'Les essais reprennent demain.', il(120)),
      message('g-2', 'c-groupe', moi, 'Bien noté, tenez-nous informés.', il(60), { etat: 'LU_PAR_CERTAINS', lus: 2, destinataires: 3 }),
    ],
  }
  /** Détail « Lu par » de g-2 : Rachel à l'heure connue, Hervé avant D-88 (heure inconnue), Aïcha pas encore. */
  lecturesGroupe: { membre: { id: string; nom: string }; lu: boolean; luLe: string | null }[] = [
    { membre: rachel, lu: true, luLe: il(45) },
    { membre: herve, lu: true, luLe: null },
    { membre: aicha, lu: false, luLe: null },
  ]

  async brancher(page: Page) {
    await page.route(/\/(api|bff)\//, (r) => this.servir(r))
  }

  /**
   * Un membre lit la conversation (POST /lu côté MS-COLLABORATION) : position avancée, état des messages mis à
   * jour comme le service le servirait, accusé publié sur le canal poussé.
   */
  lire(conversationId: 'c-direct' | 'c-groupe', lecteur: 'u-aicha') {
    const l = this.messages[conversationId]
    const luJusquA = l[l.length - 1].envoyeLe
    if (conversationId === 'c-direct') l.filter((m) => m.lecture).forEach((m) => (m.lecture = { etat: 'LU', lus: 1, destinataires: 1 }))
    else {
      this.lecturesGroupe = [...this.lecturesGroupe.filter((x) => x.lu), { membre: aicha, lu: true, luLe: new Date().toISOString() }]
      l.filter((m) => m.lecture).forEach((m) => (m.lecture = { etat: 'LU', lus: 3, destinataires: 3 }))
    }
    this.evenements.push({ id: `lu-${++this.sequence}`, type: 'collaboration.conversation-read.v1', subject: conversationId, topic: 'collaboration', acteur: lecteur, luJusquA })
  }

  private conversation(id: string) {
    const l = this.messages[id]
    const d = l[l.length - 1]
    const groupe = id === 'c-groupe'
    return {
      id, type: groupe ? 'GROUPE' : 'DIRECTE', titre: groupe ? 'Essais de LIA-2026-030' : null, liaison: null,
      membres: (groupe ? [moi, rachel, herve, aicha] : [moi, aicha]).map((p, i) => ({ ...p, fonction: null, avatarVersion: null, administrateur: groupe && i === 0 })),
      dernierMessage: { id: d.id, auteurNom: d.auteur.nom, extrait: d.contenu, envoyeLe: d.envoyeLe, piecesJointes: 0, evenement: null, ...(d.lecture ? { lecture: d.lecture } : {}) },
      nonLus: 0, misAJourLe: d.envoyeLe, creeeLe: il(600),
    }
  }

  private async servir(route: Route) {
    const req = route.request()
    const url = new URL(req.url())
    const chemin = url.pathname
    const methode = req.method()
    this.appels.push({ methode, chemin })
    const json = (b: unknown, status = 200) => route.fulfill({ status, json: b, headers: { 'X-Correlation-Id': `e2e-lu-${this.appels.length}` } })
    const vide = () => route.fulfill({ status: 204, headers: { 'X-Correlation-Id': `e2e-lu-${this.appels.length}` } })
    const refus = (status: number, code: string, message: string) => json({ code, message, details: {}, correlationId: `e2e-${code}` }, status)

    if (chemin === '/api/session') {
      return json({ sujet: moi.id, nom: moi.nom, identifiant: 'p.mbarga', roles: ['ROLE_CHEF_DES', 'ROLE_USER'],
        permissions: ['COL_MESSAGE_READ', 'COL_MESSAGE_SEND', 'NOT_INBOX_READ', 'PIL_ACTIVITY_OWN'], institutionId: '', acr: '2', doubleFacteur: true })
    }
    if (chemin === '/api/logout') return json({ logoutUrl: '/connexion' })
    if (chemin === '/bff/des/live') {
      const corps = this.evenements.splice(0).map((e) => `id: ${e.id}\ndata: ${JSON.stringify(e)}\n\n`).join('')
      return route.fulfill({ status: 200, headers: { 'Content-Type': 'text/event-stream', 'Cache-Control': 'no-cache' }, body: `: battement\n\n${corps}` })
    }
    if (chemin === '/bff/des/presence') return json({ enLigne: [aicha.id], vusLe: {} })
    if (chemin === '/bff/des/iam/moi') {
      return json({ compte: { id: moi.id, identifiant: 'p.mbarga', prenom: 'Paul', nom: 'Mbarga Atangana', nomAffiche: moi.nom, email: 'p.mbarga@gimac-afr.org', emailVerifie: true, actif: true,
        creeLe: il(90_000), telephone: null, fonction: 'Chef du département DES', langue: 'fr', institutionId: null, profilComplet: true, premierAccesEnAttente: false,
        profils: ['ROLE_CHEF_DES'], actionsRequises: [], secondFacteur: true }, clesSecurite: 1, sessions: 1, etapesRestantes: [] })
    }
    const N = '/bff/des/notification'
    if (chemin === `${N}/notifications/compteur`) return json({ nonLues: 0 })
    if (chemin === `${N}/notifications`) return json({ contenu: [], total: 0, page: 0, taille: 20 })
    if (chemin === `${N}/notifications/lecture`) return vide()
    if (chemin === `${N}/preferences`) return json({ systeme: false, son: false, types: { MESSAGE: true, TACHE: true, RAPPORT: true, MENTION: true, REPONSE: true } })

    if (chemin === `${M}/non-lus`) return json({ total: 0 })
    if (chemin === `${M}/conversations` && methode === 'GET') return json({ contenu: ['c-groupe', 'c-direct'].map((c) => this.conversation(c)), total: 2, page: 0, taille: 20 })
    const c = chemin.match(/^\/bff\/des\/collaboration\/messagerie\/conversations\/(c-direct|c-groupe)(\/.*)?$/)
    if (c) {
      const [, id, suite = ''] = c
      if (suite === '' && methode === 'GET') return json(this.conversation(id))
      if (suite === '/messages' && methode === 'GET') return json({ contenu: this.messages[id], plusAnciens: false })
      if (suite === '/lu' && methode === 'POST') return vide()
      if (suite === '/saisie') return vide()
      const lectures = suite.match(/^\/messages\/([^/]+)\/lectures$/)
      if (lectures && methode === 'GET') {
        // Auteur seulement, sinon 404 indistinct (COL_MESSAGE_NOT_FOUND).
        if (id !== 'c-groupe' || lectures[1] !== 'g-2') return refus(404, 'COL_MESSAGE_NOT_FOUND', 'Message introuvable')
        return json(this.lecturesGroupe)
      }
    }
    return refus(404, 'HTTP_404', `Route non simulée : ${methode} ${chemin}`)
  }
}
