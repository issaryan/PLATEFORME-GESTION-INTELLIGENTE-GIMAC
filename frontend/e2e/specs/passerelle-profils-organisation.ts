import type { Page, Route } from '@playwright/test'

// Passerelle simulée des profils, des rôles par fonctionnalité et de l'organigramme (D-86 et D-90,
// docs/lot1/profils-organisation.md et docs/lot1/roles-fonctionnalites.md) pour la console construite avec
// VITE_SERVICES=http, en complément de la passerelle de la console (passerelle.ts : session, clé de sécurité, défi) :
// profils avec libellé, description et dernière modification (GET, PUT /configuration/profils/{code}, composition),
// matrice des pouvoirs aux colonnes PROCESSUS_DEFINIR et PROCESSUS_LANCER, unités et comptes de l'organigramme
// (POST et PUT /organisation/unites, PUT /comptes/{id}/rattachement ; D-102 : affectations d'une unité et organigramme en
// PDF). Toute autre route est rendue (route.fallback()).

const il = (h: number) => new Date(Date.now() - h * 3_600_000).toISOString()
const AUCUNE = { TACHE_CONFIER: 'AUCUNE', TACHE_SUIVRE: 'AUCUNE', ACTIVITE_LIRE: 'AUCUNE', RAPPORT_VISER: 'AUCUNE', RATTACHEMENT_MODIFIER: 'AUCUNE', COMPTE_DEMANDER: 'AUCUNE',
  PROFIL_DEMANDER: 'AUCUNE', STATUT_DEMANDER: 'AUCUNE', PROCESSUS_DEFINIR: 'AUCUNE', PROCESSUS_LANCER: 'AUCUNE' }
const compte = (id: string, nomAffiche: string, profils: string[], unite: string | null, responsableId: string | null, fonction: string) => {
  const [prenom, ...nom] = nomAffiche.split(' ')
  return {
    id, identifiant: id.replace(/^u-/, ''), prenom, nom: nom.join(' '), nomAffiche, email: `${id}@gimac-afr.org`, emailVerifie: true, actif: true, creeLe: il(900), telephone: null, fonction,
    langue: 'fr', institutionId: null, profilComplet: true, premierAccesEnAttente: false, profils, actionsRequises: [], secondFacteur: true, unite, responsableId,
  }
}

type Appel = { methode: string; chemin: string; recherche: string; entetes: Record<string, string>; corps: unknown }

export class PasserelleProfilsOrganisation {
  appels: Appel[] = []
  profils: Record<string, unknown>[] = [
    { code: 'ROLE_POLE_RESEAU', libelle: 'Pôle réseau', description: 'Réalise les opérations réseau.', permissions: ['PIL_ACTIVITY_OWN'], privilegie: false, titulaires: 2, cree: false, modifieLe: null, modifiePar: null },
    { code: 'ROLE_SNA', libelle: 'Normes et Agréments', description: 'Vérifie le respect des normes.', permissions: ['HOM_EVIDENCE_SUBMIT', 'HOM_LIAISON_LIST'], privilegie: false, titulaires: 1, cree: false, modifieLe: il(48), modifiePar: { id: 'u-rssi', nomAffiche: 'Carine Ebongue Ndjock' } },
    { code: 'ROLE_CHEF_DES', libelle: 'Chef du DES', description: 'Dirige le département.', permissions: ['PIL_ACTIVITY_OWN', 'PIL_TEAM_READ', 'PIL_TASK_ASSIGN'], privilegie: false, titulaires: 1, cree: false, modifieLe: null, modifiePar: null },
  ]
  matrice = [
    { profil: 'ROLE_CHEF_DES', libelle: 'Chef du DES', actions: { ...AUCUNE, TACHE_CONFIER: 'UNITES', PROCESSUS_DEFINIR: 'UNITES', PROCESSUS_LANCER: 'UNITES' }, profilsDemandables: ['ROLE_POLE_RESEAU'] },
    { profil: 'ROLE_POLE_RESEAU', libelle: 'Pôle réseau', actions: { ...AUCUNE }, profilsDemandables: [] },
  ]
  unites: Record<string, unknown>[] = [
    { code: 'DG', libelle: 'Direction Générale', type: 'DIRECTION', parent: null, active: true, responsableId: null },
    { code: 'DES', libelle: 'Département Exploitation et Supervision', type: 'DEPARTEMENT', parent: 'DG', active: true, responsableId: 'u-chef-des' },
    { code: 'PROD_INFO', libelle: 'Service Production informatique', type: 'SERVICE', parent: 'DES', active: true, responsableId: 'u-resp-info' },
    { code: 'POLE_RESEAU', libelle: 'Pôle réseau', type: 'POLE', parent: 'PROD_INFO', active: true, responsableId: null },
    { code: 'SNA', libelle: 'Service Normes et Agréments', type: 'SERVICE', parent: 'DES', active: true, responsableId: null },
  ]
  comptes = [
    compte('u-super-admin', 'Serge Mouelle Kombi', ['ROLE_SUPER_ADMIN'], null, null, 'Super administrateur'),
    compte('u-chef-des', 'Christian Ndong Obiang', ['ROLE_CHEF_DES'], 'DES', null, 'Chef du département DES'),
    compte('u-resp-info', 'Gaston Ndzana Ebode', ['ROLE_RESPONSABLE_SERVICE'], 'PROD_INFO', 'u-chef-des', 'Chef du service Production informatique'),
    compte('u-res', 'Idriss Mahamat Saleh', ['ROLE_POLE_RESEAU'], 'POLE_RESEAU', 'u-resp-info', 'Ingénieur réseau'),
    compte('u-collab', 'Nadège Mvondo Assiga', ['ROLE_POLE_RESEAU'], 'POLE_RESEAU', 'u-resp-info', 'Technicienne réseau'),
    compte('u-sna', 'Rachel Biyong Mengue', ['ROLE_SNA'], 'SNA', 'u-chef-des', 'Analyste Normes et Agréments'),
  ]

  async brancher(page: Page) {
    await page.route(/\/(api|bff)\//, (r) => this.servir(r))
  }

  /** Dernier appel d'une méthode sur un chemin. */
  dernier(methode: string, chemin: string) {
    return this.appels.filter((a) => a.methode === methode && a.chemin === chemin).at(-1)
  }

  private async servir(route: Route) {
    const req = route.request()
    const url = new URL(req.url())
    const chemin = url.pathname
    const methode = req.method()
    const corps = req.postData() ? JSON.parse(req.postData()!) : undefined
    const json = (b: unknown, status = 200) => route.fulfill({ status, json: b, headers: { 'X-Correlation-Id': `e2e-d86-${this.appels.length}` } })
    const refus = (status: number, code: string, message: string) => json({ code, message, details: {}, correlationId: `e2e-${code}` }, status)
    const noter = () => this.appels.push({ methode, chemin, recherche: url.search, entetes: req.headers(), corps })
    const signe = () => !!req.headers()['x-reauth-assertion']
    const moi = { id: 'u-super-admin', nomAffiche: 'Serge Mouelle Kombi' }

    // ------------------------------------------------ Profils (D-86 §2) et composition (D-90 §3)
    const K = '/bff/console/iam/configuration/profils'
    if (chemin === K && methode === 'GET') {
      noter()
      return json(this.profils)
    }
    const pf = chemin.match(/^\/bff\/console\/iam\/configuration\/profils\/([^/]+)(\/permissions)?$/)
    if (pf && methode === 'PUT') {
      noter()
      if (!signe()) return refus(428, 'IAM_REAUTH_REQUIRED', 'Réauthentification forte exigée')
      const p = this.profils.find((x) => x.code === decodeURIComponent(pf[1]))
      if (!p) return refus(404, 'IAM_PROFILE_NOT_FOUND', 'Profil introuvable')
      if (pf[2]) return json(Object.assign(p, { permissions: corps.permissions, modifieLe: il(0), modifiePar: moi }))
      return json(Object.assign(p, { libelle: corps.libelle, description: corps.description, modifieLe: il(0), modifiePar: moi }))
    }
    // ------------------------------------------------ Matrice des pouvoirs (D-81, colonnes processus de D-87)
    if (chemin === '/bff/console/iam/pouvoirs/matrice' && methode === 'GET') {
      noter()
      return json({ profils: this.matrice })
    }
    const mx = chemin.match(/^\/bff\/console\/iam\/pouvoirs\/matrice\/([^/]+)$/)
    if (mx && methode === 'PUT') {
      noter()
      if (!signe()) return refus(428, 'IAM_REAUTH_REQUIRED', 'Réauthentification forte exigée')
      const l = this.matrice.find((x) => x.profil === decodeURIComponent(mx[1]))!
      return json(Object.assign(l, { actions: corps.actions, profilsDemandables: corps.profilsDemandables }))
    }
    // ------------------------------------------------ Organigramme (D-86 §3) : routes d'organisation existantes
    const U = '/bff/console/iam/organisation/unites'
    if (chemin === U && methode === 'GET') {
      noter()
      return json(this.unites)
    }
    if (chemin === U && methode === 'POST') {
      noter()
      const u = { ...corps, active: true }
      this.unites.push(u)
      return json(u, 201)
    }
    const un = chemin.match(/^\/bff\/console\/iam\/organisation\/unites\/([^/]+)$/)
    if (un && methode === 'PUT') {
      noter()
      const u = this.unites.find((x) => x.code === decodeURIComponent(un[1]))
      if (!u) return refus(404, 'ORG_UNITE_INTROUVABLE', 'Unité introuvable')
      return json(Object.assign(u, corps))
    }
    if (chemin === '/bff/console/iam/comptes' && methode === 'GET') {
      noter()
      const page = Number(url.searchParams.get('page') ?? 0)
      const taille = Number(url.searchParams.get('taille') ?? 20)
      return json({ contenu: this.comptes.slice(page * taille, (page + 1) * taille), total: this.comptes.length, page, taille })
    }
    const ra = chemin.match(/^\/bff\/console\/iam\/comptes\/([^/]+)\/rattachement$/)
    if (ra && methode === 'PUT') {
      noter()
      const c = this.comptes.find((x) => x.id === decodeURIComponent(ra[1]))
      if (!c) return refus(404, 'IAM_ACCOUNT_NOT_FOUND', 'Ressource introuvable')
      return json(Object.assign(c, { unite: corps.unite, responsableId: corps.responsableId }))
    }
    // ------------------------------------------------ D-102 (docs/lot1/organigramme.md) : affectations et PDF
    const af = chemin.match(/^\/bff\/console\/iam\/organisation\/unites\/([^/]+)\/affectations(?:\/([^/]+))?$/)
    if (af && methode === 'POST') {
      noter()
      const code = decodeURIComponent(af[1])
      const chef = (c: string | null, id: string): string | null => {
        const u = this.unites.find((x) => x.code === c)
        if (!u) return null
        return u.responsableId && u.responsableId !== id ? (u.responsableId as string) : chef(u.parent as string | null, id)
      }
      return json((corps.comptes as string[]).map((id) => {
        const c = this.comptes.find((x) => x.id === id)!
        const avant = { unite: c.unite, responsableId: c.responsableId }
        const apres = { unite: code, responsableId: 'responsableId' in corps ? corps.responsableId : chef(code, id) }
        Object.assign(c, apres)
        return { id, nomAffiche: c.nomAffiche, avant, apres }
      }))
    }
    if (af && af[2] && methode === 'DELETE') {
      noter()
      const c = this.comptes.find((x) => x.id === decodeURIComponent(af[2]))!
      const avant = { unite: c.unite, responsableId: c.responsableId }
      Object.assign(c, { unite: null })
      return json({ id: c.id, nomAffiche: c.nomAffiche, avant, apres: { unite: null, responsableId: c.responsableId } })
    }
    if (chemin === '/bff/console/iam/organisation/organigramme.pdf' && methode === 'GET') {
      noter()
      return route.fulfill({
        status: 200,
        body: Buffer.from('%PDF-1.7\n% organigramme composé par MS-IAM (simulé)\n'),
        headers: { 'Content-Type': 'application/pdf', 'Content-Disposition': 'attachment; filename="organigramme-moabi-2026-10-09.pdf"' },
      })
    }
    return route.fallback()
  }
}
