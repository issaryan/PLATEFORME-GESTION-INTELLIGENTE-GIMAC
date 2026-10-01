import type { Page, Route } from '@playwright/test'

// Passerelle simulée pour l'application du département construite avec VITE_SERVICES=http : chaque appel
// /api et /bff est intercepté par Playwright et servi depuis un état en mémoire, aux formes de
// MS-PARTICIPANT (/bff/des/participant), de MS-HOMOLOGATION (/bff/des/homologation, LiaisonsCadrageResource)
// et de MS-IAM (/bff/des/iam). La clé de sécurité est simulée par navigator.credentials.

const LECTURE_DES = ['HOM_REFERENTIEL_READ', 'HOM_LIAISON_READ', 'HOM_LIAISON_LIST', 'HOM_DASHBOARD_READ', 'CER_CIRCUIT_READ', 'CER_DOSSIER_READ', 'CER_REGISTER_READ', 'PAR_INSTITUTION_READ']
export const PERMISSIONS_CHEF_DDRM = [...LECTURE_DES, 'PAR_INSTITUTION_CREATE', 'PAR_INSTITUTION_EDIT', 'PAR_ADHESION_INSTRUCT', 'PAR_TRANSITION_REQUEST', 'PAR_TRANSITION_APPROVE',
  'HOM_LIAISON_OPEN', 'HOM_PROJECT_DESIGNATE']

const il = (h: number) => new Date(Date.now() - h * 3_600_000).toISOString()
const couverture = (n: number, d: number) => ({ taux: d ? Math.round((n / d) * 100) : null, numerateur: n, denominateur: d, calculable: d > 0 })
const pieces = (etat: string) => ['DEMANDE_ADHESION', 'STATUTS', 'AGREMENT'].map((code, i) => ({
  id: `p-${i}`, code, libelle: code === 'STATUTS' ? "Statuts de l'établissement" : code === 'AGREMENT' ? "Agrément ou autorisation d'exercer" : "Lettre de demande d'adhésion",
  obligatoire: true, etat, documentId: `doc-${i}`, documentSha256: 'a'.repeat(64), nomFichier: `${code.toLowerCase()}.pdf`, deposeeLe: il(40), examineeLe: il(30), motifRejet: null,
}))
const institution = (id: string, nom: string, statut: string, categorie = 'BANQUE') => ({
  id, nom, sigle: null, pays: 'CM', categorie, statut, codeParticipant: statut === 'CANDIDAT' ? null : `CP-${id}`, codeIdentification: null, codeBanqueBeac: null,
  modeAdhesion: statut === 'CANDIDAT' ? null : 'DIRECT', datePriseDeContact: '2026-01-12', originePriseDeContact: 'Courrier', projetEnvisage: 'Carte interbancaire',
  chargeDdrm: { id: 'u-charge', nom: 'Fatimé Abakar Hissein' }, conformite: 'CONFORME', creeLe: il(2000),
})
type Etat = { institution: ReturnType<typeof institution>; transitions: Record<string, unknown>[] }

export type OptionsPasserelleDes = {
  permissions?: string[]
  /** Premier refus de l'ouverture d'une liaison, avant succès. */
  refusOuverture?: { status: number; code: string; details?: Record<string, unknown> }
  /** La liste des liaisons échoue (503). */
  listeEnErreur?: boolean
}

export class PasserelleDes {
  appels: { methode: string; chemin: string; recherche: string; entetes: Record<string, string>; corps: unknown }[] = []
  approbationsRefusees = 0
  o: OptionsPasserelleDes
  etats: Etat[] = [
    { institution: institution('i-bdl', 'Banque de Libreville', 'CANDIDAT'), transitions: [{ id: 't-1', de: 'CANDIDAT', vers: 'ADHERENT', motif: 'Dossier complet et conditions définies', etat: 'PROPOSEE',
      demandeurId: 'u-charge', demandeeLe: il(3), approbationsRequises: 1, approbateurId: null, secondApprobateurId: null, decideeLe: null, motifRejet: null, pieceDecisionId: null, automatique: false }] },
    { institution: institution('i-mms', 'Mobile Money Sanaga', 'ADHERENT', 'EMETTEUR_MONNAIE_ELECTRONIQUE'), transitions: [] },
    { institution: institution('i-bqb', 'Banque B', 'ACTIF'), transitions: [] },
  ]
  // Liaison antérieure au cadrage : vue sans périmètre ni intervenants, conducteur à désigner,
  // couverture de mise en service non calculable.
  liaisons: Record<string, unknown>[] = [{
    id: 'LIA-2025-090', typeProjet: 'CARTE_INTERBANCAIRE', institutionId: 'i-bqb', institutionNom: 'Banque B', etat: 'EN_HOMOLOGATION', chefDeProjetId: '', chefDeProjetNom: '',
    banquePartenaireId: null, banquePartenaireNom: null, dateOuverture: '2025-11-02', motifEtat: null, versionId: 'REF-CI-3', versionNumero: 3, phaseCourante: 'Certification',
    couvertureCertification: couverture(2, 5), couvertureMiseEnService: { taux: null, numerateur: 0, denominateur: 0, calculable: false, motifNonCalculable: 'aucun service souscrit' },
    operationsEnRetard: 0, etatsParcourus: ['OUVERTE'], transitionsPermises: [], actionsPermises: [],
  }]
  refusOuverture: OptionsPasserelleDes['refusOuverture']

  constructor(o: OptionsPasserelleDes = {}) {
    this.o = o
    this.refusOuverture = o.refusOuverture
  }

  async brancher(page: Page) {
    // Clé de sécurité simulée (WebAuthn) : assertion factice, sans authentificateur réel.
    await page.addInitScript(() => {
      const tampon = (...v: number[]) => new Uint8Array(v).buffer
      Object.defineProperty(navigator, 'credentials', {
        configurable: true,
        value: { get: async () => ({ id: 'cle-des', type: 'public-key', response: { clientDataJSON: tampon(1, 2), authenticatorData: tampon(3), signature: tampon(4), userHandle: tampon(5) } }) },
      })
    })
    await page.route(/\/(api|bff)\//, (r) => this.servir(r))
  }

  private dossier(e: Etat) {
    const i = e.institution
    return {
      institution: i,
      adhesion: { codeParticipant: i.codeParticipant, mode: i.modeAdhesion, banqueReglementId: null, banqueMarraineId: null, compteReglement: 'BEAC-CM-001', compteCantonnement: null,
        dateAdhesion: null, prononceeLe: null, prononceePar: null, motifCloture: null, closLe: null },
      pieces: pieces('VALIDEE'),
      transitions: e.transitions,
      prerequisAdhesion: [{ code: 'CODE_PARTICIPANT', libelle: 'Code participant attribué', satisfait: true, reference: i.codeParticipant }],
      transitionsPermises: i.statut === 'CANDIDAT' ? ['ADHERENT', 'CLOS_SANS_SUITE'] : i.statut === 'ADHERENT' ? ['EN_HOMOLOGATION', 'CLOS_SANS_SUITE'] : [],
    }
  }

  private async servir(route: Route) {
    const req = route.request()
    const url = new URL(req.url())
    const chemin = url.pathname
    const methode = req.method()
    const corps = req.postData() ? JSON.parse(req.postData()!) : undefined
    this.appels.push({ methode, chemin, recherche: url.search, entetes: req.headers(), corps })
    const json = (b: unknown, status = 200) => route.fulfill({ status, json: b, headers: { 'X-Correlation-Id': `e2e-des-${this.appels.length}` } })
    const refus = (status: number, code: string, message: string, details: Record<string, unknown> = {}) => json({ code, message, details, correlationId: `e2e-${code}` }, status)

    if (chemin === '/api/session') {
      return json({ sujet: 'u-chef-ddrm', nom: 'Bertrand Nguema Obiang', identifiant: 'b.nguema', roles: ['ROLE_CHEF_DDRM', 'ROLE_USER'], permissions: this.o.permissions ?? PERMISSIONS_CHEF_DDRM,
        institutionId: '', acr: '2', doubleFacteur: true })
    }
    if (chemin === '/api/logout') return json({ logoutUrl: '/connexion' })
    if (chemin === '/bff/des/live') {
      // Comme la passerelle, la reprise (« depuis ») ne rejoue que les événements postérieurs : aucun ici.
      // Rejouer l'événement à chaque reconnexion relançait sans fin les listes du sujet homologation.
      const rejeu = url.searchParams.get('depuis') ? '' : `id: 1\ndata: ${JSON.stringify({ id: '1', type: 'liaison-opened', subject: 'liaison', topic: 'homologation' })}\n\n`
      return route.fulfill({ status: 200, headers: { 'Content-Type': 'text/event-stream', 'Cache-Control': 'no-cache' }, body: rejeu })
    }

    // ------------------------------------------------ MS-IAM : Mon compte, réauthentification
    if (chemin === '/bff/des/iam/moi') {
      return json({ compte: { id: 'u-chef-ddrm', identifiant: 'b.nguema', prenom: 'Bertrand', nom: 'Nguema Obiang', nomAffiche: 'Bertrand Nguema Obiang', email: 'b.nguema@gimac-afr.org',
        emailVerifie: true, actif: true, creeLe: il(900), telephone: '+237 699 00 11 22', fonction: 'Chef de la DDRM', langue: 'fr', institutionId: null, profilComplet: true,
        premierAccesEnAttente: false, profils: ['ROLE_CHEF_DDRM'], actionsRequises: [], secondFacteur: true }, clesSecurite: 1, sessions: 1, etapesRestantes: [] })
    }
    if (chemin === '/bff/des/iam/reauthentification/defi') return json({ challengeId: 'defi-des', challenge: 'AQIDBA', rpId: 'localhost', allowCredentials: ['AQ'], timeoutMs: 60000 })

    // ------------------------------------------------ MS-PARTICIPANT : registre des institutions
    const I = '/bff/des/participant/institutions'
    if (chemin === I) {
      const statuts = (url.searchParams.get('statut') ?? '').split(',').filter(Boolean)
      const l = this.etats.map((e) => e.institution).filter((i) => !statuts.length || statuts.includes(i.statut))
      return json({ contenu: l, total: l.length })
    }
    if (chemin === `${I}/indicateurs`) {
      const n = (s: string) => this.etats.filter((e) => e.institution.statut === s).length
      return json({ candidats: n('CANDIDAT'), piecesAExaminer: 0, propositionsEnAttente: this.etats.flatMap((e) => e.transitions).filter((t) => t.etat === 'PROPOSEE').length,
        adherents: n('ADHERENT'), enHomologation: n('EN_HOMOLOGATION'), closSansSuite: 0 })
    }
    const m = chemin.match(/^\/bff\/des\/participant\/institutions\/([^/]+)(\/.*)?$/)
    if (m) {
      const e = this.etats.find((x) => x.institution.id === m[1])
      if (!e) return refus(404, 'PAR_INSTITUTION_NOT_FOUND', 'Ressource introuvable')
      const suite = m[2] ?? ''
      if (suite === '') return json(this.dossier(e))
      const t = suite.match(/^\/transitions\/([^/]+)\/(approbation|rejet)$/)
      if (t) {
        const x = e.transitions.find((y) => y.id === t[1])!
        if (t[2] === 'rejet') return json(Object.assign(x, { etat: 'REJETEE', motifRejet: corps.motif, decideeLe: il(0) }))
        const assertion = req.headers()['x-reauth-assertion']
        const a = assertion ? JSON.parse(Buffer.from(assertion, 'base64').toString('utf8')) : null
        if (!a || a.challengeId !== 'defi-des' || a.id !== 'cle-des') return refus(403, 'PAR_REAUTH_INVALID', 'Réauthentification forte invalide')
        Object.assign(x, { etat: 'APPROUVEE', approbateurId: 'u-chef-ddrm', decideeLe: il(0) })
        e.institution.statut = String(x.vers)
        return json(x)
      }
    }

    // ------------------------------------------------ MS-HOMOLOGATION : cadrage (LiaisonsCadrageResource)
    const L = '/bff/des/homologation/liaisons'
    if (chemin === `${L}/conducteurs`) return json([{ id: 'U-201', nom: 'Aïcha Mahamat Nour' }, { id: 'U-202', nom: 'Brice Ondo Mba' }])
    if (chemin === L && methode === 'GET' && this.o.listeEnErreur) return refus(503, 'HOM_INDISPONIBLE', 'Service indisponible')
    if (chemin === L && methode === 'GET') {
      const institutionId = url.searchParams.get('institutionId')
      const l = this.liaisons.filter((x) => !institutionId || x.institutionId === institutionId)
      return json({ contenu: l, total: l.length })
    }
    if (chemin === L && methode === 'POST') {
      if (this.refusOuverture) {
        const r = this.refusOuverture
        this.refusOuverture = undefined
        return refus(r.status, r.code, 'Refus du service', r.details)
      }
      const e = this.etats.find((x) => x.institution.id === corps.institutionId)!
      const chef = corps.chefDeProjetId === 'U-201' ? 'Aïcha Mahamat Nour' : 'Brice Ondo Mba'
      const l = {
        id: 'LIA-2026-101', typeProjet: corps.typeProjet, institutionId: e.institution.id, institutionNom: e.institution.nom, etat: 'OUVERTE', chefDeProjetId: corps.chefDeProjetId,
        chefDeProjetNom: chef, banquePartenaireId: corps.banquePartenaireId, banquePartenaireNom: corps.banquePartenaireId ? 'Banque B' : null, dateOuverture: '2026-09-29', motifEtat: null,
        versionId: 'REF-PM-2', versionNumero: 2, phaseCourante: 'CADRAGE', couvertureCertification: couverture(0, 3), couvertureMiseEnService: couverture(0, 2), operationsEnRetard: 0,
        etatsParcourus: [], transitionsPermises: ['EN_HOMOLOGATION'], actionsPermises: ['DESIGNER_CHEF_DE_PROJET'], perimetre: ['TRANSVERSE', 'TRANSFERT', 'PAIEMENT'],
        intervenants: [{ role: 'INSTITUTION', id: e.institution.id, nom: e.institution.nom }, { role: 'CHEF_DE_PROJET', id: corps.chefDeProjetId, nom: chef }],
      }
      this.liaisons.push(l)
      // homologation.liaison-opened.v1 : MS-PARTICIPANT fait passer l'institution en homologation.
      e.institution.statut = 'EN_HOMOLOGATION'
      e.transitions.unshift({ id: 't-auto', de: 'ADHERENT', vers: 'EN_HOMOLOGATION', motif: `Ouverture de la liaison ${l.id}`, etat: 'APPROUVEE', demandeurId: 'systeme', demandeeLe: il(0),
        approbationsRequises: 0, approbateurId: null, secondApprobateurId: null, decideeLe: il(0), motifRejet: null, pieceDecisionId: null, automatique: true })
      return json(l, 201)
    }
    const lm = chemin.match(/^\/bff\/des\/homologation\/liaisons\/([^/]+)(\/.*)?$/)
    if (lm) {
      const l = this.liaisons.find((x) => x.id === lm[1])
      if (!l) return refus(404, 'HOM_LIAISON_NOT_FOUND', 'Liaison introuvable')
      const suite = lm[2] ?? ''
      if (suite === '') return json(l)
      if (suite === '/chef-de-projet') {
        const nom = corps.chefDeProjetId === 'U-201' ? 'Aïcha Mahamat Nour' : 'Brice Ondo Mba'
        Object.assign(l, { chefDeProjetId: corps.chefDeProjetId, chefDeProjetNom: nom,
          intervenants: [(l.intervenants as unknown[])[0], { role: 'CHEF_DE_PROJET', id: corps.chefDeProjetId, nom }] })
        return json(l)
      }
      if (suite === '/couvertures') return json([])
      if (suite === '/historique') return json([{ event: 'Liaison ouverte en phase de cadrage', at: il(0), actor: 'Bertrand Nguema Obiang', correlationId: 'e2e-ouverture' }])
    }
    if (chemin === '/bff/des/homologation/reprise/etat') return json({ etatDisjoncteur: 'FERME', depuis: null })
    return refus(404, 'HTTP_404', `Route non simulée : ${methode} ${chemin}`)
  }
}
