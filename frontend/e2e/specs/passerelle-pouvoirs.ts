import type { Page, Route } from '@playwright/test'

// Passerelle simulée des pouvoirs hiérarchiques (D-81, docs/lot1/pouvoirs-hierarchiques.md) pour les constructions
// VITE_SERVICES=http, aux formes du contrat :
// - application interne (chef du DES) : session, « Mon compte », organisation, MS-PILOTAGE (tâches confiées à une
//   unité ou à plusieurs personnes, à prendre, attribution, relance, groupe) et MS-IAM (pouvoirs/moi, demandes) ;
// - console (super administrateur), en complément de la passerelle de la console (passerelle.ts) : demandes à
//   traiter, matrice des pouvoirs, profils créés ; toute autre route lui est rendue (route.fallback()).
// Premier dépôt d'une demande refusé (409 ORG_DEMANDE_EXISTANTE), seconde relance refusée (429 PIL_RELANCE_RECENTE).

const il = (h: number) => new Date(Date.now() - h * 3_600_000).toISOString()
const jour = (j = 0) => new Date(Date.now() + 3_600_000 + j * 86_400_000).toISOString().slice(0, 10)
const chef = { id: 'U-001', nomAffiche: 'Paul Mbarga Atangana' }
const AUCUNE = { TACHE_CONFIER: 'AUCUNE', TACHE_SUIVRE: 'AUCUNE', ACTIVITE_LIRE: 'AUCUNE', RAPPORT_VISER: 'AUCUNE', RATTACHEMENT_MODIFIER: 'AUCUNE', COMPTE_DEMANDER: 'AUCUNE', PROFIL_DEMANDER: 'AUCUNE', STATUT_DEMANDER: 'AUCUNE' }
const RESPONSABLE = { TACHE_CONFIER: 'UNITES', TACHE_SUIVRE: 'UNITES', ACTIVITE_LIRE: 'EQUIPE', RAPPORT_VISER: 'EQUIPE', RATTACHEMENT_MODIFIER: 'DIRECTS', COMPTE_DEMANDER: 'UNITES', PROFIL_DEMANDER: 'EQUIPE', STATUT_DEMANDER: 'EQUIPE' }
const UNITES = [
  { code: 'DES', libelle: 'Département Exploitation et Supervision', type: 'DEPARTEMENT', parent: 'DG', active: true, responsableId: 'U-001' },
  { code: 'PROD_INFO', libelle: 'Service Production informatique', type: 'SERVICE', parent: 'DES', active: true, responsableId: 'u-resp-info' },
  { code: 'POLE_RESEAU', libelle: 'Pôle réseau', type: 'POLE', parent: 'PROD_INFO', active: true, responsableId: null },
]
const SUBORDONNES = [
  { id: 'u-resp-info', nomAffiche: 'Gaston Ndzana Ebode', fonction: 'Chef du service Production informatique', unite: 'PROD_INFO', responsableId: 'U-001', niveau: 1, actif: true },
  { id: 'u-res', nomAffiche: 'Idriss Mahamat Saleh', fonction: 'Ingénieur réseau', unite: 'POLE_RESEAU', responsableId: 'u-resp-info', niveau: 2, actif: true },
  { id: 'u-collab', nomAffiche: 'Nadège Mvondo Assiga', fonction: 'Technicienne réseau', unite: 'POLE_RESEAU', responsableId: 'u-resp-info', niveau: 2, actif: true },
]
/** Permissions de la session du chef du DES (sous-ensemble utile : Mon activité, équipe, tâches confiées). */
export const PERMISSIONS_CHEF_DES = ['PIL_ACTIVITY_OWN', 'PIL_TEAM_READ', 'PIL_TASK_ASSIGN']

type Appel = { methode: string; chemin: string; recherche: string; entetes: Record<string, string>; corps: unknown }

export class PasserellePouvoirs {
  appels: Appel[] = []
  depotsRefuses = 0
  relances = 0
  rattachementsRefuses = 0
  demandes: Record<string, unknown>[] = [
    {
      id: 'DEM-1', type: 'CREATION_COMPTE', demandeur: chef, cible: null, motif: 'Renfort du pôle réseau.', etat: 'EN_ATTENTE', decision: null, creeeLe: il(30),
      contenu: { identifiant: 'i.fouda', prenom: 'Ingrid', nom: 'Fouda Ngono', email: 'i.fouda@gimac-afr.org', telephone: null, fonction: 'Technicienne réseau', unite: 'POLE_RESEAU', responsableId: 'u-resp-info', profils: ['ROLE_POLE_RESEAU'] },
    },
    {
      id: 'DEM-2', type: 'STATUT', demandeur: chef, cible: { id: 'u-collab', nomAffiche: 'Nadège Mvondo Assiga' }, contenu: { actif: false }, motif: 'Départ annoncé.', etat: 'EN_ATTENTE', decision: null, creeeLe: il(20),
    },
  ]
  taches: Record<string, unknown>[] = []
  matrice = [
    { profil: 'ROLE_CHEF_DES', actions: { ...RESPONSABLE }, profilsDemandables: ['ROLE_COLLABORATEUR', 'ROLE_POLE_RESEAU'] },
    { profil: 'ROLE_SNA', actions: { ...AUCUNE }, profilsDemandables: [] },
  ]
  profils = [
    { code: 'ROLE_SNA', description: 'Service Normes et Agréments', permissions: ['HOM_EVIDENCE_SUBMIT'], privilegie: false, titulaires: 1, cree: false },
    { code: 'ROLE_COLLABORATEUR', description: 'Collaborateur', permissions: ['PIL_ACTIVITY_OWN'], privilegie: false, titulaires: 4, cree: false },
  ]

  constructor(private o: { departement?: boolean } = {}) {}

  async brancher(page: Page) {
    await page.route(/\/(api|bff)\//, (r) => this.servir(r))
  }

  /** Dernier appel d'une méthode sur un chemin. */
  dernier(methode: string, chemin: string | RegExp) {
    return this.appels.filter((a) => a.methode === methode && (typeof chemin === 'string' ? a.chemin === chemin : chemin.test(a.chemin))).at(-1)
  }

  private tache(corps: Record<string, unknown>, o: Record<string, unknown>) {
    const t = {
      id: `T-${this.taches.length + 1}`, titre: corps.titre, description: corps.description ?? null, echeance: corps.echeance ?? null, priorite: corps.priorite ?? 'NORMALE', etat: 'OUVERTE',
      responsable: null, createur: chef, origine: { type: 'INTERNE', reference: null, service: null, lien: null }, enRetard: false, creeeLe: il(0), termineeLe: null,
      groupeId: null, uniteCible: null, historique: [{ quand: il(0), acteur: chef, action: 'CREEE', detail: null }], ...o,
    }
    this.taches.push(t)
    return t
  }

  private groupe(id: string) {
    const l = this.taches.filter((t) => t.groupeId === id)
    return { groupeId: id, titre: l[0]?.titre, createur: chef, taches: l, avancement: { total: l.length, terminees: 0, enRetard: 0 } }
  }

  private async servir(route: Route) {
    const req = route.request()
    const url = new URL(req.url())
    const chemin = url.pathname
    const methode = req.method()
    const brut = req.postData()
    const corps = brut ? JSON.parse(brut) : undefined
    const json = (b: unknown, status = 200) => route.fulfill({ status, json: b, headers: { 'X-Correlation-Id': `e2e-pvr-${this.appels.length}` } })
    const vide = () => route.fulfill({ status: 204 })
    const refus = (status: number, code: string, message: string) => json({ code, message, details: {}, correlationId: `e2e-${code}` }, status)
    const noter = () => this.appels.push({ methode, chemin, recherche: url.search, entetes: req.headers(), corps })

    // ------------------------------------------------ Console : demandes à traiter, matrice, profils créés
    const C = '/bff/console/iam'
    if (chemin === `${C}/demandes` && methode === 'GET') {
      noter()
      const etat = url.searchParams.get('etat')
      const l = this.demandes.filter((d) => !etat || d.etat === etat)
      return json({ contenu: l, total: l.length, page: Number(url.searchParams.get('page')), taille: Number(url.searchParams.get('taille')) })
    }
    const dc = chemin.match(/^\/bff\/(console|des)\/iam\/demandes\/([^/]+)(\/[a-z]+)?$/)
    if (dc) {
      noter()
      const d = this.demandes.find((x) => x.id === decodeURIComponent(dc[2]))
      if (!d) return refus(404, 'ORG_DEMANDE_INTROUVABLE', 'Demande introuvable')
      if (!dc[3]) return json(d)
      if (d.etat !== 'EN_ATTENTE') return refus(409, 'ORG_DEMANDE_TRAITEE', 'Cette demande n’est plus en attente')
      if (dc[3] === '/acceptation') {
        if (!req.headers()['x-reauth-assertion']) return refus(428, 'IAM_REAUTH_REQUIRED', 'Réauthentification forte exigée')
        return json(Object.assign(d, { etat: 'ACCEPTEE', decision: { par: { id: 'u-super-admin', nomAffiche: 'Serge Mouelle Kombi' }, motif: corps?.motif ?? null, le: il(0) } }))
      }
      if (dc[3] === '/refus') return json(Object.assign(d, { etat: 'REFUSEE', decision: { par: { id: 'u-super-admin', nomAffiche: 'Serge Mouelle Kombi' }, motif: corps.motif, le: il(0) } }))
      if (dc[3] === '/annulation') return json(Object.assign(d, { etat: 'ANNULEE' }))
    }
    if (chemin === `${C}/pouvoirs/matrice`) {
      noter()
      return json({ profils: this.matrice })
    }
    const mx = chemin.match(/^\/bff\/console\/iam\/pouvoirs\/matrice\/([^/]+)$/)
    if (mx && methode === 'PUT') {
      noter()
      if (!req.headers()['x-reauth-assertion']) return refus(428, 'IAM_REAUTH_REQUIRED', 'Réauthentification forte exigée')
      const l = this.matrice.find((x) => x.profil === decodeURIComponent(mx[1]))!
      return json(Object.assign(l, { actions: corps.actions, profilsDemandables: corps.profilsDemandables }))
    }
    if (chemin === `${C}/configuration/profils` && methode === 'GET') {
      noter()
      return json(this.profils)
    }
    if (chemin === `${C}/configuration/profils` && methode === 'POST') {
      noter()
      if (!req.headers()['x-reauth-assertion']) return refus(428, 'IAM_REAUTH_REQUIRED', 'Réauthentification forte exigée')
      const p = { code: corps.code, description: corps.description, permissions: corps.permissions, privilegie: false, titulaires: 0, cree: true }
      this.profils.push(p)
      return json(p, 201)
    }
    const pf = chemin.match(/^\/bff\/console\/iam\/configuration\/profils\/([^/]+)$/)
    if (pf && methode === 'DELETE') {
      noter()
      if (!req.headers()['x-reauth-assertion']) return refus(428, 'IAM_REAUTH_REQUIRED', 'Réauthentification forte exigée')
      const code = decodeURIComponent(pf[1])
      if (!this.profils.find((p) => p.code === code)?.cree) return refus(409, 'IAM_PROFIL_SYSTEME', 'Profil livré')
      this.profils = this.profils.filter((p) => p.code !== code)
      return vide()
    }
    if (!this.o.departement) return route.fallback()

    // ------------------------------------------------ Application interne : session du chef du DES
    noter()
    if (chemin === '/api/session') {
      return json({ sujet: chef.id, nom: chef.nomAffiche, identifiant: 'p.mbarga', roles: ['ROLE_CHEF_DES', 'ROLE_USER'], permissions: PERMISSIONS_CHEF_DES, institutionId: '', acr: '2', doubleFacteur: true })
    }
    if (chemin === '/api/logout') return json({ logoutUrl: '/connexion' })
    if (chemin === '/bff/des/live') return route.fulfill({ status: 200, headers: { 'Content-Type': 'text/event-stream', 'Cache-Control': 'no-cache' }, body: ': battement\n\n' })
    if (chemin === '/bff/des/iam/moi') {
      return json({ compte: { id: chef.id, identifiant: 'p.mbarga', prenom: 'Paul', nom: 'Mbarga Atangana', nomAffiche: chef.nomAffiche, email: 'p.mbarga@gimac-afr.org', emailVerifie: true, actif: true,
        creeLe: il(900), telephone: '+237 699 00 11 22', fonction: 'Chef du département DES', langue: 'fr', institutionId: null, profilComplet: true, premierAccesEnAttente: false,
        profils: ['ROLE_CHEF_DES'], actionsRequises: [], secondFacteur: true, unite: 'DES', responsableId: 'u-dg' }, clesSecurite: 1, sessions: 1, etapesRestantes: [] })
    }
    if (chemin === '/bff/des/iam/organisation/unites') return json(UNITES)
    if (chemin === '/bff/des/iam/organisation/moi') {
      return json({ id: chef.id, nomAffiche: chef.nomAffiche, unite: UNITES[0], chemin: [UNITES[0]], responsable: null, chaine: [], equipeDirecte: 1, unitesDirigees: ['DES', 'PROD_INFO', 'POLE_RESEAU'] })
    }
    if (chemin === '/bff/des/iam/pouvoirs/moi') return json({ actions: RESPONSABLE, profilsDemandables: ['ROLE_COLLABORATEUR', 'ROLE_POLE_RESEAU'], subordonnes: SUBORDONNES, unitesDirigees: ['DES', 'PROD_INFO', 'POLE_RESEAU'] })
    // Rattachement d'un subordonné par son responsable (D-81) : les deux valeurs toujours reçues, motif facultatif ;
    // premier essai refusé (boucle hiérarchique), puis la fiche du compte.
    const rattachement = chemin.match(/^\/bff\/des\/iam\/pouvoirs\/subordonnes\/([^/]+)\/rattachement$/)
    if (rattachement && methode === 'PUT') {
      const s = SUBORDONNES.find((x) => x.id === decodeURIComponent(rattachement[1]))
      if (!s) return refus(403, 'ORG_HORS_PERIMETRE', 'Hors de votre périmètre')
      if (this.rattachementsRefuses++ === 0) return refus(409, 'ORG_CYCLE', 'Boucle hiérarchique')
      return json({ id: s.id, identifiant: s.id, nomAffiche: s.nomAffiche, unite: corps.unite, responsableId: corps.responsableId, actif: true, profils: [] })
    }
    if (chemin === '/bff/des/iam/demandes' && methode === 'GET') {
      const l = this.demandes.filter((d) => (d.demandeur as { id: string }).id === chef.id)
      return json({ contenu: l, total: l.length, page: 0, taille: 20 })
    }
    if (chemin === '/bff/des/iam/demandes' && methode === 'POST') {
      if (this.depotsRefuses++ === 0) return refus(409, 'ORG_DEMANDE_EXISTANTE', 'Une demande de ce type est déjà en attente')
      const d = { id: `DEM-${this.demandes.length + 1}`, ...corps, demandeur: chef, cible: corps.cibleId ? SUBORDONNES.find((s) => s.id === corps.cibleId) ?? null : null, etat: 'EN_ATTENTE', decision: null, creeeLe: il(0) }
      this.demandes.unshift(d)
      return json(d, 201)
    }
    // ------------------------------------------------ MS-PILOTAGE
    const P = '/bff/des/pilotage'
    if (chemin === `${P}/equipe`) return json([])
    if (chemin === `${P}/taches` && methode === 'GET') {
      const vue = url.searchParams.get('vue')
      const l = this.taches.filter((t) => (vue === 'unite' ? t.etat === 'A_PRENDRE' : vue === 'creees' ? true : false))
      return json({ contenu: l, total: l.length, page: 0, taille: 20 })
    }
    if (chemin === `${P}/taches` && methode === 'POST') {
      if (corps.responsables) {
        corps.responsables.forEach((r: string) => this.tache(corps, { responsable: SUBORDONNES.find((s) => s.id === r), groupeId: 'G-1' }))
        return json(this.groupe('G-1'), 201)
      }
      if (corps.uniteCible) return json(this.tache(corps, { etat: 'A_PRENDRE', uniteCible: corps.uniteCible }), 201)
      return json(this.tache(corps, { responsable: SUBORDONNES.find((s) => s.id === corps.responsableId) ?? chef }), 201)
    }
    const gr = chemin.match(/^\/bff\/des\/pilotage\/taches\/groupes\/([^/]+)$/)
    if (gr) return json(this.groupe(decodeURIComponent(gr[1])))
    const tc = chemin.match(/^\/bff\/des\/pilotage\/taches\/([^/]+)(\/[a-z]+)?$/)
    if (tc) {
      const t = this.taches.find((x) => x.id === decodeURIComponent(tc[1]))
      if (!t) return refus(404, 'PIL_TACHE_INTROUVABLE', 'Tâche introuvable')
      if (!tc[2]) return json(t)
      if (tc[2] === '/attribution') return json(Object.assign(t, { etat: 'OUVERTE', responsable: SUBORDONNES.find((s) => s.id === corps.responsableId) }))
      if (tc[2] === '/relance') return this.relances++ === 0 ? vide() : refus(429, 'PIL_RELANCE_RECENTE', 'Relance déjà envoyée')
    }
    return refus(404, 'HTTP_404', `Route non simulée : ${methode} ${chemin}`)
  }
}

export const JOUR_E2E = jour
