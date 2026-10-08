import { readFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import type { Page, Route } from '@playwright/test'

// Passerelle simulée des processus d'unité (D-87, docs/lot1/processus-unite.md §3 et §5.1) pour l'application du
// département construite avec VITE_SERVICES=http : MS-PILOTAGE (/bff/des/pilotage/processus/**), dépôt fragmenté de
// MS-DOCUMENT (finalité PROCESSUS_UNITE), défi de réauthentification de MS-IAM, annuaire de MS-COLLABORATION.
// Les définitions et les instances de départ (donnees-processus.json) sont celles de la démonstration simulée
// (« Ouverture d'un flux réseau » du pôle réseau). Première publication refusée : avertissements à lever (422).

type Personne = { id: string; nomAffiche: string }
type Appel = { methode: string; chemin: string; recherche: string; entetes: Record<string, string>; corps: unknown }
type Json = Record<string, any>

const ICI = dirname(fileURLToPath(import.meta.url))
const il = (h: number) => new Date(Date.now() - h * 3_600_000).toISOString()

export const PERSONNES_PROCESSUS = {
  resp: { id: 'u-resp-info', nomAffiche: 'Gaston Ndzana Ebode', identifiant: 'g.ndzana', profil: 'ROLE_RESPONSABLE_SERVICE', unite: 'PROD_INFO', fonction: 'Chef du service Production informatique' },
  membre: { id: 'u-res', nomAffiche: 'Idriss Mahamat Saleh', identifiant: 'i.mahamat', profil: 'ROLE_POLE_RESEAU', unite: 'POLE_RESEAU', fonction: 'Ingénieur réseau' },
}
const UNITES = [
  { code: 'DES', libelle: 'Département Exploitation et Supervision', type: 'DEPARTEMENT', parent: null, active: true, responsableId: 'U-001' },
  { code: 'PROD_INFO', libelle: 'Service Production informatique', type: 'SERVICE', parent: 'DES', active: true, responsableId: 'u-resp-info' },
  { code: 'POLE_RESEAU', libelle: 'Pôle réseau', type: 'POLE', parent: 'PROD_INFO', active: true, responsableId: null },
]

export class PasserelleProcessus {
  appels: Appel[] = []
  definitions: Json[]
  instances: Json[]
  annuaire: Json[]
  publicationsRefusees = 0
  private moi: Personne & { identifiant: string; profil: string; unite: string; fonction: string }

  constructor(qui: keyof typeof PERSONNES_PROCESSUS) {
    const d = JSON.parse(readFileSync(join(ICI, 'donnees-processus.json'), 'utf8'))
    this.moi = PERSONNES_PROCESSUS[qui]
    this.definitions = d.definitions
    this.instances = d.instances
    this.annuaire = d.annuaire.filter((p: Json) => p.id !== this.moi.id)
    // Vue de chaque personne : le membre exécute l'analyse à prendre ; le chef du service signe la recette.
    if (qui === 'membre') {
      for (const i of this.instances) {
        for (const e of i.etapes) {
          e.moi = { executer: false, prendre: e.etat === 'ACTIVE' && !!e.aPrendre, signer: [], valider: false, reaffecter: false }
        }
        i.droits = { suspendre: false, reprendre: false, annuler: false, reaffecter: false }
      }
      for (const p of this.definitions) p.droits = { modifier: false, publier: false, lancer: p.etat === 'PUBLIEE', retirer: false }
    }
  }

  async brancher(page: Page) {
    // Clé de sécurité simulée (WebAuthn) : assertion factice, sans authentificateur réel.
    await page.addInitScript(() => {
      const tampon = (...v: number[]) => new Uint8Array(v).buffer
      Object.defineProperty(navigator, 'credentials', {
        configurable: true,
        value: { get: async () => ({ id: 'cle-proc', type: 'public-key', response: { clientDataJSON: tampon(1, 2), authenticatorData: tampon(3), signature: tampon(4), userHandle: tampon(5) } }) },
      })
    })
    await page.route(/\/(api|bff)\//, (r) => this.servir(r))
  }

  /** Dernier appel d'une méthode sur un chemin. */
  dernier(methode: string, chemin: string) {
    return this.appels.filter((a) => a.methode === methode && a.chemin === chemin).at(-1)
  }

  private resume(p: Json) {
    const { etapes, creeLe: _c, creePar: _cp, publieePar: _pp, motifPublication: _m, avertissementsLeves: _a, retireeLe: _r, retireePar: _rp, motifRetrait: _mr, versions: _v, avertissements: _w, personnes: _pe, historique: _h, ...r } = p
    return { ...r, nombreEtapes: etapes.length }
  }

  private resumeInstance(i: Json) {
    return {
      id: i.id, processus: i.processus, unite: i.unite, intitule: i.intitule, etat: i.etat, lanceur: i.lanceur, lanceeLe: i.lanceeLe, closeLe: i.closeLe, avancement: i.avancement,
      etapesEnCours: i.etapes.filter((e: Json) => ['ACTIVE', 'A_SIGNER', 'A_VALIDER'].includes(e.etat)).map((e: Json) => ({ code: e.code, libelle: e.libelle, etat: e.etat, echeance: e.echeance })),
    }
  }

  /** Porteurs d'un profil (annuaire de démonstration) : ceux du pôle réseau dans l'unité, les autres ailleurs. */
  private candidats(unite: string, profil: string | null) {
    const DU_POLE = ['u-res', 'u-collab']
    const vers = (p: Json) => ({ id: p.id, nomAffiche: p.nom, fonction: p.fonction ?? null, unite: DU_POLE.includes(p.id) ? 'POLE_RESEAU' : p.id === 'u-resp-info' ? 'PROD_INFO' : 'DES' })
    const porteurs = [...this.annuaire, { id: this.moi.id, nom: this.moi.nomAffiche, fonction: this.moi.fonction, profils: [this.moi.profil] }]
      .filter((p) => (profil ? (p.profils ?? []).includes(profil) : DU_POLE.includes(p.id)))
    return { unite, profil, dansUnite: porteurs.filter((p) => DU_POLE.includes(p.id)).map(vers), ailleurs: porteurs.filter((p) => !DU_POLE.includes(p.id)).map(vers) }
  }

  private historiser(i: Json, action: string, etape: string | null, detail: string | null = null) {
    i.historique.unshift({ quand: il(0), acteur: { id: this.moi.id, nomAffiche: this.moi.nomAffiche }, action, etape, detail })
  }

  private async servir(route: Route) {
    const req = route.request()
    const url = new URL(req.url())
    const chemin = url.pathname
    const methode = req.method()
    const brut = req.postData()
    let corps: Json | undefined
    try {
      corps = brut ? JSON.parse(brut) : undefined
    } catch {
      corps = undefined
    }
    this.appels.push({ methode, chemin, recherche: url.search, entetes: req.headers(), corps })
    const json = (b: unknown, status = 200) => route.fulfill({ status, json: b, headers: { 'X-Correlation-Id': `e2e-proc-${this.appels.length}` } })
    const refus = (status: number, code: string, message: string, details: unknown = {}) => json({ code, message, details, correlationId: `e2e-${code}` }, status)
    const moi = { id: this.moi.id, nomAffiche: this.moi.nomAffiche }

    // ------------------------------------------------ Session, Mon compte, organisation, pouvoirs, annuaire
    if (chemin === '/api/session') {
      const permissions = ['PIL_ACTIVITY_OWN', 'COL_MESSAGE_READ', ...(this.moi.profil === 'ROLE_RESPONSABLE_SERVICE' ? ['PIL_TEAM_READ', 'PIL_TASK_ASSIGN'] : [])]
      return json({ sujet: this.moi.id, nom: this.moi.nomAffiche, identifiant: this.moi.identifiant, roles: [this.moi.profil, 'ROLE_USER'], permissions, institutionId: '', acr: '2', doubleFacteur: true })
    }
    if (chemin === '/api/logout') return json({ logoutUrl: '/connexion' })
    if (chemin === '/bff/des/live') return route.fulfill({ status: 200, headers: { 'Content-Type': 'text/event-stream', 'Cache-Control': 'no-cache' }, body: ': battement\n\n' })
    if (chemin === '/bff/des/iam/moi') {
      const [prenom, ...nom] = this.moi.nomAffiche.split(' ')
      return json({ compte: { id: this.moi.id, identifiant: this.moi.identifiant, prenom, nom: nom.join(' '), nomAffiche: this.moi.nomAffiche, email: `${this.moi.identifiant}@gimac-afr.org`, emailVerifie: true,
        actif: true, creeLe: il(900), telephone: null, fonction: this.moi.fonction, langue: 'fr', institutionId: null, profilComplet: true, premierAccesEnAttente: false, profils: [this.moi.profil],
        actionsRequises: [], secondFacteur: true, unite: this.moi.unite, responsableId: 'U-001' }, clesSecurite: 1, sessions: 1, etapesRestantes: [] })
    }
    if (chemin === '/bff/des/iam/organisation/unites') return json(UNITES)
    if (chemin === '/bff/des/iam/organisation/moi') {
      const u = UNITES.find((x) => x.code === this.moi.unite)!
      return json({ id: this.moi.id, nomAffiche: this.moi.nomAffiche, unite: u, chemin: [u], responsable: null, chaine: [], equipeDirecte: 0, unitesDirigees: this.moi.profil === 'ROLE_RESPONSABLE_SERVICE' ? ['PROD_INFO', 'POLE_RESEAU'] : [] })
    }
    if (chemin === '/bff/des/iam/pouvoirs/moi') {
      const chef = this.moi.profil === 'ROLE_RESPONSABLE_SERVICE'
      return json({ actions: { PROCESSUS_DEFINIR: chef ? 'UNITES' : 'AUCUNE', PROCESSUS_LANCER: chef ? 'UNITES' : 'AUCUNE' }, profilsDemandables: [], subordonnes: [], unitesDirigees: chef ? ['PROD_INFO', 'POLE_RESEAU'] : [],
        libellesProfils: { ROLE_POLE_RESEAU: 'Pôle réseau', ROLE_RESPONSABLE_SERVICE: 'Responsable de service' } })
    }
    if (chemin === '/bff/des/iam/reauthentification/defi') return json({ challengeId: 'defi-proc', challenge: 'AQIDBA', rpId: 'localhost', allowCredentials: ['AQ'], timeoutMs: 60000 })
    if (chemin === '/bff/des/collaboration/messagerie/annuaire') return json({ contenu: this.annuaire, total: this.annuaire.length, page: 0, taille: 50 })

    // ------------------------------------------------ MS-DOCUMENT : dépôt fragmenté (une partie)
    if (chemin === '/bff/des/document/depots' && methode === 'POST') return json({ id: 'DEP-1', taillePartie: 5_242_880, nombreParties: 1, etat: 'OUVERT', partiesRecues: [] }, 201)
    if (/^\/bff\/des\/document\/depots\/DEP-1\/parties\/\d+$/.test(chemin)) return route.fulfill({ status: 204 })
    if (chemin === '/bff/des/document/depots/DEP-1/fin') return json({ id: 'DEP-1', etat: 'DISPONIBLE', documentId: 'DOC-E2E-1', sha256: 'ab'.repeat(32), dedoublonne: false, partiesRecues: [1] })
    if (chemin === '/bff/des/document/depots/DEP-1') return json({ id: 'DEP-1', etat: 'DISPONIBLE', documentId: 'DOC-E2E-1', sha256: 'ab'.repeat(32), dedoublonne: false, partiesRecues: [1] })

    // ------------------------------------------------ MS-PILOTAGE : prise d'une tâche d'étape
    const prise = chemin.match(/^\/bff\/des\/pilotage\/taches\/([^/]+)\/prise$/)
    if (prise) {
      const i = this.instances.find((x) => x.etapes.some((e: Json) => e.tacheId === decodeURIComponent(prise[1])))!
      const e = i.etapes.find((x: Json) => x.tacheId === decodeURIComponent(prise[1]))
      Object.assign(e, { executant: moi, aPrendre: null, moi: { ...e.moi, prendre: false, executer: true } })
      this.historiser(i, 'ETAPE_PRISE', e.code, moi.nomAffiche)
      return json({ id: e.tacheId, titre: `${i.intitule} : ${e.libelle}`, etat: 'OUVERTE', responsable: moi, origine: { type: 'PROCESSUS', reference: i.id, service: 'pilotage', lien: `/des/processus/instances/${i.id}` } })
    }

    // ------------------------------------------------ MS-PILOTAGE : définitions
    const P = '/bff/des/pilotage/processus'
    // Désignation hors de l'unité : candidats d'un intervenant (porteurs du profil dans l'unité, puis ailleurs).
    if (chemin === `${P}/candidats` && methode === 'GET') return json(this.candidats(url.searchParams.get('unite') ?? '', url.searchParams.get('profil')))
    if (chemin === P && methode === 'GET') return json(this.definitions.map((p) => this.resume(p)))
    if (chemin === `${P}/instances` && methode === 'GET') {
      const l = this.instances.filter((i) => url.searchParams.get('vue') !== 'lancees' || i.lanceur.id === this.moi.id)
      return json({ contenu: l.map((i) => this.resumeInstance(i)), total: l.length, page: Number(url.searchParams.get('page')), taille: Number(url.searchParams.get('taille')) })
    }
    const inst = chemin.match(/^\/bff\/des\/pilotage\/processus\/instances\/([^/]+)(\/.*)?$/)
    if (inst) {
      const i = this.instances.find((x) => x.id === decodeURIComponent(inst[1]))
      if (!i) return refus(404, 'PIL_PROCESSUS_INSTANCE_INTROUVABLE', 'Instance introuvable')
      const suite = inst[2] ?? ''
      if (!suite) return json(i)
      const cand = suite.match(/^\/etapes\/([^/]+)\/candidats$/)
      if (cand && methode === 'GET') {
        const e = i.etapes.find((x: Json) => x.code === cand[1])
        return json(this.candidats(e?.aPrendre?.unite ?? i.unite, e?.aPrendre?.profil ?? 'ROLE_POLE_RESEAU'))
      }
      const ec = suite.match(/^\/etapes\/([^/]+)\/(livrables|signature|fin|validation|reaffectation|champs)$/)
      const e = ec ? i.etapes.find((x: Json) => x.code === ec[1]) : null
      if (ec && ec[2] === 'livrables') {
        const l = e.livrables.find((x: Json) => x.code === corps!.livrable)
        l.depot = { documentId: corps!.documentId, nomFichier: 'note-analyse.pdf', typeMime: 'application/pdf', taille: 4, sha256: 'ab'.repeat(32), deposePar: moi, deposeLe: il(0) }
        this.historiser(i, 'LIVRABLE_DEPOSE', e.code, `${l.code} : note-analyse.pdf`)
        return json(i)
      }
      if (ec && ec[2] === 'signature') {
        if (!req.headers()['x-reauth-assertion']) return refus(428, 'PIL_PROCESSUS_REAUTH_REQUISE', 'Réauthentification forte exigée')
        const s = e.signatures.find((x: Json) => x.code === corps!.signature)
        Object.assign(s, { etat: 'SIGNEE', signePar: moi, signeLe: il(0), empreinte: 'cd'.repeat(32) })
        Object.assign(e, { etat: 'TERMINEE', termineeLe: il(0), moi: { ...e.moi, signer: [] } })
        Object.assign(i, { etat: 'CLOSE', closeLe: il(0), avancement: { ...i.avancement, terminees: i.avancement.total }, droits: { suspendre: false, reprendre: false, annuler: false, reaffecter: false } })
        this.historiser(i, 'SIGNATURE_APPOSEE', e.code, s.libelle)
        return json(i)
      }
      if (ec && ec[2] === 'fin') {
        Object.assign(e, { etat: 'A_VALIDER', soumiseLe: il(0), moi: { ...e.moi, executer: false } })
        this.historiser(i, 'ETAPE_SOUMISE', e.code)
        return json(i)
      }
      if (ec && ec[2] === 'reaffectation') {
        const p = this.annuaire.find((x) => x.id === corps!.personneId)
        Object.assign(e, { executant: { id: p!.id, nomAffiche: p!.nom }, aPrendre: null })
        this.historiser(i, 'ETAPE_REAFFECTEE', e.code, `${p!.nom} : ${corps!.motif}`)
        return json(i)
      }
      if (suite === '/suspension') {
        Object.assign(i, { etat: 'SUSPENDUE', droits: { ...i.droits, suspendre: false, reprendre: true } })
        this.historiser(i, 'INSTANCE_SUSPENDUE', null, corps!.motif)
        return json(i)
      }
      if (suite === '/reprise') {
        Object.assign(i, { etat: 'EN_COURS', droits: { ...i.droits, suspendre: true, reprendre: false } })
        this.historiser(i, 'INSTANCE_REPRISE', null)
        return json(i)
      }
      return refus(404, 'HTTP_404', `Route non simulée : ${methode} ${chemin}`)
    }
    const def = chemin.match(/^\/bff\/des\/pilotage\/processus\/([^/]+)(\/.*)?$/)
    if (def) {
      const p = this.definitions.find((x) => x.id === decodeURIComponent(def[1]))
      if (!p) return refus(404, 'PIL_PROCESSUS_INTROUVABLE', 'Processus introuvable')
      const suite = def[2] ?? ''
      if (!suite && methode === 'GET') return json(p)
      if (!suite && methode === 'PUT') return json(Object.assign(p, { ...corps, modifieLe: il(0), modifiePar: moi }))
      if (suite === '/controle') {
        return json({ publiable: true, anomalies: [{ code: 'CHEF_ABSENT', gravite: 'LEVABLE', etape: 'CONFIGURATION', element: 'validateur', message: 'L’unité n’a pas de responsable.' }] })
      }
      if (suite === '/publication') {
        if (!corps?.leverAvertissements) {
          this.publicationsRefusees++
          return refus(422, 'PIL_PROCESSUS_AVERTISSEMENTS', 'Avertissements à lever', { anomalies: [{ code: 'CHEF_ABSENT', gravite: 'LEVABLE', etape: 'CONFIGURATION', element: 'validateur', message: 'L’unité n’a pas de responsable.' }] })
        }
        return json(Object.assign(p, { etat: 'PUBLIEE', publieeLe: il(0), publieePar: moi, motifPublication: corps.motif, avertissementsLeves: true, droits: { modifier: false, publier: false, lancer: true, retirer: true } }))
      }
      if (suite === '/instances' && methode === 'POST') {
        const base = structuredClone(this.instances.find((x) => x.processus.id === p.id)!)
        const n = {
          ...base, id: 'INS-E2E-1', intitule: corps!.intitule, etat: 'EN_COURS', lanceur: moi, lanceeLe: il(0), closeLe: null, avancement: { total: base.etapes.length, terminees: 0 },
          etapes: base.etapes.map((e: Json, k: number) => ({ ...e, etat: k === 0 ? 'ACTIVE' : 'EN_ATTENTE', executant: k === 0 ? moi : null, aPrendre: null, termineeLe: null,
            moi: { executer: k === 0, prendre: false, signer: [], valider: false, reaffecter: false } })),
          historique: [{ quand: il(0), acteur: moi, action: 'INSTANCE_LANCEE', etape: null, detail: null }],
        }
        this.instances.unshift(n)
        return json(n, 201)
      }
    }
    return refus(404, 'HTTP_404', `Route non simulée : ${methode} ${chemin}`)
  }
}
