import type { Page, Route } from '@playwright/test'

// Passerelle simulée de « Mon activité » (D-76, docs/lot1/mon-activite.md) pour l'application du département
// construite avec VITE_SERVICES=http : MS-PILOTAGE (/bff/des/pilotage/**) et l'organisation de MS-IAM
// (/bff/des/iam/organisation/**), aux formes du contrat. Session d'un collaborateur (PIL_ACTIVITY_OWN seul).
// Le premier rapport soumis est refusé (422 PIL_RAPPORT_INCOMPLET, message du service rendu) ; le suivant est remis au n+1.

const il = (h: number) => new Date(Date.now() - h * 3_600_000).toISOString()
const jour = () => new Date(Date.now() + 3_600_000).toISOString().slice(0, 10)
const moi = { id: 'u-collab', nomAffiche: 'Nadège Mvondo Assiga' }
const chef = { id: 'u-resp-info', nomAffiche: 'Gaston Ndzana Ebode' }

export class PasserellePilotage {
  appels: { methode: string; chemin: string; recherche: string; corps: unknown }[] = []
  soumissionsRefusees = 0
  activites: Record<string, unknown>[] = [
    { id: 'A-1', jour: jour(), titre: 'Configurer le VPN du participant', description: null, statut: 'EN_COURS', dureeMinutes: 90, ordre: 1, tacheId: null, reporteeAu: null, auteur: moi, modifieeLe: il(1) },
    { id: 'A-2', jour: jour(), titre: 'Classer les comptes rendus', description: null, statut: 'A_FAIRE', dureeMinutes: null, ordre: 2, tacheId: null, reporteeAu: null, auteur: moi, modifieeLe: il(1) },
  ]
  rapports: Record<string, unknown>[] = []

  async brancher(page: Page) {
    await page.route(/\/(api|bff)\//, (r) => this.servir(r))
  }

  private rapport(corps: Record<string, unknown>, id = `R-${this.rapports.length + 1}`) {
    return {
      id, type: corps.type, periode: corps.periode, mode: corps.mode, contenu: corps.contenu ?? null, document: null, etat: 'BROUILLON', auteur: moi, destinataire: null,
      soumisLe: null, viseLe: null, visa: null, indicateurs: { activitesFaites: 1, activitesPrevues: 2, minutes: 90, tachesTerminees: 0 },
    }
  }

  private async servir(route: Route) {
    const req = route.request()
    const url = new URL(req.url())
    const chemin = url.pathname
    const methode = req.method()
    const corps = req.postData() ? JSON.parse(req.postData()!) : undefined
    this.appels.push({ methode, chemin, recherche: url.search, corps })
    const json = (b: unknown, status = 200) => route.fulfill({ status, json: b, headers: { 'X-Correlation-Id': `e2e-pil-${this.appels.length}` } })
    const refus = (status: number, code: string, message: string) => json({ code, message, details: {}, correlationId: `e2e-${code}` }, status)

    if (chemin === '/api/session') {
      return json({ sujet: moi.id, nom: moi.nomAffiche, identifiant: 'n.mvondo', roles: ['ROLE_COLLABORATEUR', 'ROLE_USER'], permissions: ['PIL_ACTIVITY_OWN'], institutionId: '', acr: '2', doubleFacteur: true })
    }
    if (chemin === '/api/logout') return json({ logoutUrl: '/connexion' })
    if (chemin === '/bff/des/live') return route.fulfill({ status: 200, headers: { 'Content-Type': 'text/event-stream', 'Cache-Control': 'no-cache' }, body: ': battement\n\n' })
    if (chemin === '/bff/des/iam/moi') {
      return json({ compte: { id: moi.id, identifiant: 'n.mvondo', prenom: 'Nadège', nom: 'Mvondo Assiga', nomAffiche: moi.nomAffiche, email: 'n.mvondo@gimac-afr.org', emailVerifie: true, actif: true,
        creeLe: il(900), telephone: '+237 699 00 11 22', fonction: 'Technicienne réseau', langue: 'fr', institutionId: null, profilComplet: true, premierAccesEnAttente: false,
        profils: ['ROLE_COLLABORATEUR'], actionsRequises: [], secondFacteur: true, unite: 'POLE_RESEAU', responsableId: chef.id }, clesSecurite: 1, sessions: 1, etapesRestantes: [] })
    }
    // ------------------------------------------------ MS-IAM : organisation
    const pole = { code: 'POLE_RESEAU', libelle: 'Pôle réseau', type: 'POLE', parent: 'PROD_INFO', active: true }
    if (chemin === '/bff/des/iam/organisation/unites') return json([pole])
    if (chemin === '/bff/des/iam/organisation/moi') {
      return json({ id: moi.id, nomAffiche: moi.nomAffiche, unite: pole, chemin: [pole], responsable: { ...chef, fonction: 'Chef du service Production informatique' }, chaine: [chef.id], equipeDirecte: 0 })
    }
    // ------------------------------------------------ MS-PILOTAGE
    const P = '/bff/des/pilotage'
    if (chemin === `${P}/moi/synthese`) {
      return json({ activites: { prevues: 2, faites: 0, enCours: 1 }, evenements: [], taches: { ouvertes: 0, enRetard: 0, echeanceProche: 0 }, rapportDuJour: 'ABSENT', rapportsARecevoir: 0 })
    }
    if (chemin === `${P}/agenda`) return json([])
    if (chemin === `${P}/taches`) return json({ contenu: [], total: 0 })
    if (chemin === `${P}/activites` && methode === 'GET') return json(this.activites)
    if (chemin === `${P}/activites` && methode === 'POST') {
      const a = { id: `A-${this.activites.length + 1}`, ...corps, statut: 'A_FAIRE', ordre: this.activites.length + 1, reporteeAu: null, auteur: moi, modifieeLe: il(0), tacheId: null }
      this.activites.push(a)
      return json(a, 201)
    }
    const statut = chemin.match(/^\/bff\/des\/pilotage\/activites\/([^/]+)\/statut$/)
    if (statut) {
      const a = this.activites.find((x) => x.id === statut[1])!
      return json(Object.assign(a, { statut: corps.statut }))
    }
    if (chemin === `${P}/rapports` && methode === 'GET') return json({ contenu: this.rapports, total: this.rapports.length })
    if (chemin === `${P}/rapports/preremplissage`) return json({ contenu: `# Rapport quotidien du ${corps.periode}\n\n## Réalisé\n- Néant\n\n## En cours\n- Configurer le VPN du participant` })
    if (chemin === `${P}/rapports` && methode === 'POST') {
      const r = this.rapport(corps)
      this.rapports.push(r)
      return json(r, 201)
    }
    const r = chemin.match(/^\/bff\/des\/pilotage\/rapports\/([^/]+)(\/.*)?$/)
    if (r) {
      const x = this.rapports.find((y) => y.id === r[1]) as Record<string, unknown> | undefined
      if (!x) return refus(404, 'PIL_RAPPORT_INTROUVABLE', 'Rapport introuvable')
      if (!r[2] && methode === 'GET') return json(x)
      if (!r[2] && methode === 'PUT') return json(Object.assign(x, { mode: corps.mode, contenu: corps.contenu }))
      // Destinataire annoncé avant la soumission (§2.4) : n+1, puis les chefs des unités au-dessus.
      if (r[2] === '/destinataires' && methode === 'GET') {
        return json({ destinataire: chef, motif: 'N_PLUS_UN', depot: false, choix: [{ ...chef, lien: 'N_PLUS_UN', unite: null }, { id: 'U-001', nomAffiche: 'Paul Mbarga Atangana', lien: 'CHEF_UNITE_PARENTE', unite: 'DES' }] })
      }
      if (r[2] === '/soumission') {
        // Premier essai refusé : rapport vide côté service (422 expliqué, plus jamais muet).
        if (this.soumissionsRefusees++ === 0) return refus(422, 'PIL_RAPPORT_INCOMPLET', 'Le rapport est vide')
        return json(Object.assign(x, { etat: 'SOUMIS', destinataire: chef, destinataireMotif: 'N_PLUS_UN', soumisLe: il(0) }))
      }
    }
    return refus(404, 'HTTP_404', `Route non simulée : ${methode} ${chemin}`)
  }
}
