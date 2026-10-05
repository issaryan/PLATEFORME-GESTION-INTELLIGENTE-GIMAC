import type { Page, Route } from '@playwright/test'

// Passerelle simulée du modélisateur de processus (D-77, docs/lot1/modelisateur-processus.md §4) pour
// l'application du département construite avec VITE_SERVICES=http : MS-HOMOLOGATION sous
// /bff/des/homologation/referentiels/**, aux formes exactes du service (OperationVue, GrapheVue, CatalogueVue).
// Session d'un ingénieur d'homologation. L'arc 3 → 1 est refusé (409 HOM_REFERENTIAL_CYCLE).

const il = (h: number) => new Date(Date.now() - h * 3_600_000).toISOString()
const moi = { id: 'u-ing', nomAffiche: 'Serge Owono Ndong' }
const V = '7d0c2a2e-4a51-4c1e-9a55-5f1f0d2c0a01'
type Op = Record<string, unknown> & { code: string; predecesseurs: string[]; delaiJours: number; position: { x: number; y: number } | null }
const operation = (code: string, libelle: string, etape: string, predecesseurs: string[], o: Partial<Op> = {}): Op => ({
  id: `op-${code}`, code, libelle, description: null, etape, phase: null, segment: 'AMONT', ordre: Number(code), responsable: 'SNA', validateur: 'CONDUCTEUR', consultes: [],
  informes: ['CHEF_DDRM'], externes: ['PARTICIPANT'], predecesseurs, successeurs: [], delaiJours: 3, grain: 'OPERATION', plateforme: null, cle: false, secret: false,
  oubliSansSymptome: false, pivot: false, valeurReference: null, livrables: [], geste: 'DEPOSER_PREUVE', jalon: null, declencheur: null, position: null, particularite: null,
  partie: 'DES', serviceCode: 'TRANSVERSE', natureCouverture: 'AUCUNE', preuveAttendue: {}, idScenario: null, fonctionResponsable: 'SNA', grille: null, ...o,
})
const entree = (code: string, libelle: string) => ({ code, libelle })

export class PasserelleReferentiels {
  appels: { methode: string; chemin: string; corps: unknown }[] = []
  piloteId = 'u-ing'
  ops: Op[] = [
    operation('1', 'Arrêt du périmètre des services', 'CADRAGE', []),
    operation('2', 'Réunion de lancement', 'LANCEMENT', ['1'], { geste: 'DEPOSER_LIVRABLE', livrables: [{ code: 'L1', libelle: 'Compte rendu', nature: 'LIVRABLE', deposant: 'SNA', typesPieces: ['application/pdf'], obligatoire: true }] }),
    operation('3', 'Constat de la couverture', 'CERTIFICATION', ['2'], { geste: 'CONSTATER_COUVERTURE', jalon: 'APTE_CERTIFICATION' }),
  ]

  async brancher(page: Page) {
    await page.route(/\/(api|bff)\//, (r) => this.servir(r))
  }

  private version() {
    return { id: V, typeProjet: 'DELEGATAIRE', numero: null, libelle: 'Délégataire, brouillon 2', etat: 'BROUILLON', dateCreation: il(30), datePublication: null, versionSourceId: null,
      versionActiveId: null, nombreOperations: this.ops.length, ordreTopologique: [], actionsPermises: ['CONTROLER', 'PUBLIER', 'MODIFIER'], piloteId: this.piloteId, transitionsPermises: [] }
  }

  private vue(o: Op) {
    return { ...o, successeurs: this.ops.filter((x) => x.predecesseurs.includes(o.code)).map((x) => x.code).sort() }
  }

  /** GrapheVue : couches (plus long chemin), dates au plus tôt, chemin critique (chaîne la plus longue). */
  private graphe() {
    const fin = new Map<string, number>()
    const couche = new Map<string, number>()
    const calcul = (o: Op): number => {
      if (fin.has(o.code)) return fin.get(o.code)!
      const preds = o.predecesseurs.map((p) => this.ops.find((x) => x.code === p)!).filter(Boolean)
      const debut = Math.max(0, ...preds.map(calcul))
      couche.set(o.code, preds.length ? Math.max(...preds.map((p) => couche.get(p.code)!)) + 1 : 0)
      fin.set(o.code, debut + o.delaiJours)
      return fin.get(o.code)!
    }
    this.ops.forEach(calcul)
    const duree = Math.max(0, ...fin.values())
    const chemin: string[] = []
    let c = [...fin.entries()].find(([, f]) => f === duree)?.[0]
    while (c) {
      chemin.unshift(c)
      const o = this.ops.find((x) => x.code === c)!
      c = o.predecesseurs.find((p) => fin.get(p) === fin.get(o.code)! - o.delaiJours)
    }
    return {
      versionId: V, typeProjet: 'DELEGATAIRE', numero: null, etat: 'BROUILLON', piloteId: this.piloteId,
      noeuds: this.ops.map((o) => ({ ...this.vue(o), couche: couche.get(o.code), debutAuPlusTot: fin.get(o.code)! - o.delaiJours, finAuPlusTot: fin.get(o.code), critique: chemin.includes(o.code), anomalies: [] })),
      arcs: this.ops.flatMap((o) => o.predecesseurs.map((p) => ({ de: p, vers: o.code, critique: chemin.includes(p) && chemin.includes(o.code) }))),
      anomalies: [], manquants: [], cheminCritique: { codes: chemin, dureeJours: duree }, actionsPermises: ['CONTROLER', 'PUBLIER', 'MODIFIER'],
    }
  }

  private async servir(route: Route) {
    const req = route.request()
    const url = new URL(req.url())
    const chemin = url.pathname
    const methode = req.method()
    const corps = req.postData() ? JSON.parse(req.postData()!) : undefined
    this.appels.push({ methode, chemin, corps })
    const json = (b: unknown, status = 200) => route.fulfill({ status, json: b, headers: { 'X-Correlation-Id': `e2e-ref-${this.appels.length}` } })
    const vide = () => route.fulfill({ status: 204, headers: { 'X-Correlation-Id': `e2e-ref-${this.appels.length}` } })
    const refus = (status: number, code: string, message: string) => json({ code, message, details: {}, correlationId: `e2e-${code}` }, status)

    if (chemin === '/api/session') {
      return json({ sujet: moi.id, nom: moi.nomAffiche, identifiant: 's.owono', roles: ['ROLE_INGENIEUR_HOMOLOGATION', 'ROLE_USER'],
        permissions: ['HOM_REFERENTIEL_READ', 'HOM_REFERENTIEL_EDIT', 'HOM_REFERENTIEL_PUBLISH'], institutionId: '', acr: '2', doubleFacteur: true })
    }
    if (chemin === '/api/logout') return json({ logoutUrl: '/connexion' })
    if (chemin === '/bff/des/live') return route.fulfill({ status: 200, headers: { 'Content-Type': 'text/event-stream', 'Cache-Control': 'no-cache' }, body: ': battement\n\n' })
    if (chemin === '/bff/des/iam/moi') {
      return json({ compte: { id: moi.id, identifiant: 's.owono', prenom: 'Serge', nom: 'Owono Ndong', nomAffiche: moi.nomAffiche, email: 's.owono@gimac-afr.org', emailVerifie: true, actif: true,
        creeLe: il(900), telephone: null, fonction: 'Ingénieur d\'homologation', langue: 'fr', institutionId: null, profilComplet: true, premierAccesEnAttente: false,
        profils: ['ROLE_INGENIEUR_HOMOLOGATION'], actionsRequises: [], secondFacteur: true }, clesSecurite: 1, sessions: 1, etapesRestantes: [] })
    }

    // ------------------------------------------------ MS-HOMOLOGATION : référentiels et modélisateur
    const R = '/bff/des/homologation/referentiels'
    if (chemin === R && methode === 'GET') return json([this.version()])
    if (chemin === `${R}/catalogue`) {
      return json({
        fonctions: [{ code: 'SNA', libelle: 'Normes et Agréments', profils: ['ROLE_SNA'], externe: false }, { code: 'CONDUCTEUR', libelle: 'Conducteur de projet', profils: ['ROLE_CONDUCTEUR_PROJET'], externe: false },
          { code: 'CHEF_DDRM', libelle: 'Chef du département DDRM', profils: ['ROLE_CHEF_DDRM'], externe: false }, { code: 'RSSI', libelle: 'Sécurité des systèmes d\'information', profils: ['ROLE_RSSI'], externe: false },
          { code: 'PARTICIPANT', libelle: 'Participant', profils: ['ROLE_CORRESPONDANT'], externe: true }],
        gestes: [{ code: 'DEPOSER_LIVRABLE', libelle: 'Déposer un livrable', dedie: false }, { code: 'DEPOSER_PREUVE', libelle: 'Déposer une preuve', dedie: false },
          { code: 'CONSTATER_COUVERTURE', libelle: 'Constater la couverture du périmètre', dedie: false }, { code: 'EVENEMENT_EXTERNE', libelle: 'Événement d\'un autre service', dedie: true }],
        jalons: [{ code: 'APTE_CERTIFICATION', libelle: 'Apte à la certification', rang: 1 }, { code: 'CERTIFIEE', libelle: 'Certifiée', rang: 2 }],
        declencheurs: [entree('certification.pv-signed.v1', 'Procès-verbal signé des deux parties')],
        etapes: [{ code: 'CADRAGE', libelle: 'Cadrage', rang: 3 }, { code: 'LANCEMENT', libelle: 'Lancement', rang: 4 }, { code: 'TESTS', libelle: 'Tests', rang: 7 }, { code: 'CERTIFICATION', libelle: 'Certification', rang: 8 }],
        segments: [entree('AMONT', 'Tronc commun amont'), entree('BRANCHE', 'Branche du type de projet'), entree('AVAL', 'Tronc commun aval')],
        plateformes: [entree('SWITCH_DELEGATAIRE', 'Switch délégataire')], grains: [entree('PHASE', 'Pièce de phase'), entree('OPERATION', 'Preuve par opération')],
        naturesLivrable: [entree('LIVRABLE', 'Livrable'), entree('PREUVE', 'Preuve')], particularites: [entree('FIN_ESSAIS', 'Fin des essais et certification des services')],
        typesPieces: ['application/pdf', 'application/zip'], pilotes: [{ id: 'u-ing', nom: moi.nomAffiche }, { id: 'u-ing-2', nom: 'Clarisse Mengue Obame' }],
      })
    }
    const m = chemin.match(/^\/bff\/des\/homologation\/referentiels\/([^/]+)(\/.*)?$/)
    if (m && m[1] === V) {
      const suite = m[2] ?? ''
      if (suite === '') return json(this.version())
      if (suite === '/historique') return json([{ event: 'Version créée', at: il(30), actor: null, correlationId: null }])
      if (suite === '/graphe') return json(this.graphe())
      if (suite === '/operations' && methode === 'GET') return json(this.ops.map((o) => this.vue(o)))
      if (suite === '/operations' && methode === 'POST') {
        if (this.ops.some((o) => o.code === corps.code)) return refus(409, 'HOM_OPERATION_CODE_DUPLICATE', 'Code déjà employé')
        const o = { ...operation(corps.code, '', 'TESTS', []), ...corps, id: `op-${corps.code}` } as Op
        this.ops.push(o)
        return json(this.vue(o), 201)
      }
      const op = suite.match(/^\/operations\/([^/]+)$/)
      if (op) {
        const code = decodeURIComponent(op[1])
        const o = this.ops.find((x) => x.code === code)
        if (!o) return refus(404, 'HOM_OPERATION_NOT_FOUND', 'Opération introuvable')
        if (methode === 'DELETE') {
          this.ops = this.ops.filter((x) => x.code !== code).map((x) => ({ ...x, predecesseurs: x.predecesseurs.filter((p) => p !== code) }))
          return vide()
        }
        Object.assign(o, corps, { position: corps.position === undefined ? o.position : corps.position })
        return json(this.vue(o))
      }
      if (suite === '/positions') {
        for (const p of corps as { code: string; x: number; y: number }[]) {
          const o = this.ops.find((x) => x.code === p.code)
          if (o) o.position = { x: p.x, y: p.y }
        }
        return vide()
      }
      if (suite === '/arcs') {
        if (corps.de === '3' && corps.vers === '1') return refus(409, 'HOM_REFERENTIAL_CYCLE', 'Cycle 1 → 2 → 3 → 1')
        const o = this.ops.find((x) => x.code === corps.vers)!
        o.predecesseurs = corps.present ? [...new Set([...o.predecesseurs, corps.de])] : o.predecesseurs.filter((p) => p !== corps.de)
        return json(this.vue(o))
      }
      if (suite === '/pilote') {
        if (corps.compteId !== 'u-ing' && corps.compteId !== 'u-ing-2') return refus(422, 'HOM_PILOTE_NOT_ELIGIBLE', 'Pilote non éligible')
        this.piloteId = corps.compteId
        return json(this.version())
      }
    }
    if (chemin === '/bff/des/homologation/reprise/etat') return json({ etatDisjoncteur: 'FERME', depuis: null })
    return refus(404, 'HTTP_404', `Route non simulée : ${methode} ${chemin}`)
  }
}
