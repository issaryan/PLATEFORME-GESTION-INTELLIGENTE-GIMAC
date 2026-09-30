import type { Page, Route } from '@playwright/test'

// Passerelle simulée pour le portail des participants construit avec VITE_SERVICES=http : chaque appel
// /api et /bff est intercepté par Playwright et servi depuis un état en mémoire, aux formes de la
// projection du portail de MS-HOMOLOGATION (/bff/portail/homologation) et de MS-CERTIFICATION
// (/bff/portail/certification), et de MS-IAM (/bff/portail/iam). Deux liaisons de l'institution du jeton :
// LIA-2026-060 (procès-verbal signé des deux parties, attestations attendues : GSMA rejetée, COBAC à
// fournir) et LIA-2026-061 (paramétrage d'essai à déposer, rang 15).

export const PERMISSIONS_CORRESPONDANT = ['HOM_PORTAL_READ', 'HOM_PORTAL_SUBMIT', 'CER_PORTAL_READ', 'CER_PORTAL_SIGN']

const couverture = (n: number, d: number) => ({ taux: d ? Math.round((n / d) * 100) : null, numerateur: n, denominateur: d, calculable: d > 0 })
const ETAPES = ['PRISE_CONTACT', 'ADHESION', 'CADRAGE', 'LANCEMENT', 'ATELIERS', 'INTEGRATION', 'TESTS', 'CERTIFICATION', 'APPROBATION_SECURITE', 'MISE_EN_PRODUCTION', 'SUIVI']
const RANG: Record<string, number | null> = { PRISE_CONTACT: 1, ADHESION: 2, CADRAGE: 3, LANCEMENT: 4, ATELIERS: 5, INTEGRATION: 6, TESTS: 7, CERTIFICATION: 8, APPROBATION_SECURITE: null, MISE_EN_PRODUCTION: 9, SUIVI: 10 }

function dossier(id: string, courante: string, statut: string) {
  const i = ETAPES.indexOf(courante)
  return {
    id, reference: id, institutionId: 'INST-0042', institutionNom: 'Établissement de paiement A', typeProjet: 'PAIEMENT_MOBILE', banquePartenaireNom: null, personnalisateurNom: null,
    statut, etapeCourante: courante, conducteur: { id: 'CONDUCTEUR', nom: 'Conducteur de projet GIMAC', fonction: 'CONDUCTEUR' },
    responsableFonctionnel: { id: 'DDRM', nom: 'DDRM', fonction: 'DDRM' }, responsableTechnique: { id: 'SNA', nom: 'SNA', fonction: 'SNA' },
    dateDemarrage: '2026-05-04', dateCibleProduction: null, versionReferentiel: 2,
    perimetre: [
      { serviceCode: 'PAIEMENT_MARCHAND', libelle: 'Paiement marchand mobile', inclus: true, etat: statut === 'CERTIFIEE' ? 'CERTIFIE' : 'EN_TEST', entreeEssai: '2026-08-01', sortieEssai: null, certifieLe: null, avancementSimulateur: statut === 'CERTIFIEE' ? 100 : 40 },
      { serviceCode: 'CASH_IN', libelle: 'Cash-in agent', inclus: false, etat: 'NON_DEMARRE', entreeEssai: null, sortieEssai: null, certifieLe: null, avancementSimulateur: null },
    ],
    couvertureCertification: statut === 'CERTIFIEE' ? couverture(1, 1) : couverture(0, 1), couvertureMiseEnService: couverture(0, 1),
    etapes: ETAPES.map((code, k) => ({
      code, rang: RANG[code], etat: k < i ? 'CLOSE' : k === i ? 'EN_COURS' : 'A_VENIR', porteurs: code === courante ? ['PARTICIPANT', 'SNA'] : ['DDRM'], responsable: null,
      elements: { RESPONSABLE: true, ACTIONS: k < i, LIVRABLES: k < i, VALIDATION: k < i, PREUVE: k < i, DATE: k < i }, actionsFaites: k < i ? 1 : 0, actionsTotal: 1, livrables: [],
      validateur: null, valideeLe: null, debut: null, closeLe: k < i ? '2026-06-01' : null, echeance: k === i ? '2026-10-06' : null, sla: k === i ? 'ECHEANCE_PROCHE' : null,
    })),
    blocages: [], interlocuteursParticipant: [], liaisonOrigineId: null, coordonneesTechniques: null, transitionsPermises: [], actionsPermises: [],
  }
}
const vue = (id: string, etat: string, statut: string, etape: string) => ({
  id, typeProjet: 'PAIEMENT_MOBILE', institutionNom: 'Établissement de paiement A', etat, statut, etapeCourante: etape, chefDeProjetNom: 'Conducteur de projet GIMAC', dateOuverture: '2026-05-04',
  phaseCourante: etape, couvertureCertification: couverture(statut === 'CERTIFIEE' ? 1 : 0, 1), couvertureMiseEnService: couverture(0, 1), operationsEnRetard: 0, bloquee: false, prochaineEcheance: '2026-10-06', dateCibleProduction: null,
})

export type OptionsPasserellePortail = { permissions?: string[]; tachesEnErreur?: boolean }

export class PasserellePortail {
  appels: { methode: string; chemin: string; entetes: Record<string, string>; corps: string }[] = []
  o: OptionsPasserellePortail
  attestations = [
    { type: 'ISO_27001', requise: true, preuve: { documentRef: 'ATT-1', nom: 'iso-27001.pdf', empreinte: 'ab'.repeat(32), deposeLe: '2026-09-20', deposePar: 'Correspondant' }, verifieeLe: '2026-09-22', verifieePar: 'RSSI', archiveeLe: null, motifRejet: null },
    { type: 'GSMA', requise: true, preuve: null as null | Record<string, string>, verifieeLe: null, verifieePar: null, archiveeLe: null, motifRejet: 'Attestation expirée depuis le 30 juin 2026' as string | null },
    { type: 'COBAC', requise: true, preuve: null as null | Record<string, string>, verifieeLe: null, verifieePar: null, archiveeLe: null, motifRejet: null as string | null },
  ]
  livrableDepose = false

  constructor(o: OptionsPasserellePortail = {}) {
    this.o = o
  }

  async brancher(page: Page) {
    await page.route(/\/(api|bff)\//, (r) => this.servir(r))
  }

  private taches() {
    const t = [
      ...this.attestations.filter((a) => !a.preuve).slice(0, 1).map(() => ({
        operationId: 'LIA-2026-060:21', liaisonId: 'LIA-2026-060', liaisonReference: 'LIA-2026-060', institutionNom: 'Établissement de paiement A', rang: '21', etape: 'APPROBATION_SECURITE',
        qualite: 'R', echeance: '2026-10-03', sla: 'DEPASSE', oubliSansSymptome: false, lien: '/portail/liaisons/LIA-2026-060/attestations', actionsPermises: ['DEPOSER_ATTESTATION'],
      })),
      ...(this.livrableDepose ? [] : [{
        operationId: 'LIA-2026-061:15', liaisonId: 'LIA-2026-061', liaisonReference: 'LIA-2026-061', institutionNom: 'Établissement de paiement A', rang: '15', etape: 'INTEGRATION',
        qualite: 'E', echeance: '2026-10-02', sla: 'ECHEANCE_PROCHE', oubliSansSymptome: true, lien: `/portail/liaisons/LIA-2026-061/livrables/${encodeURIComponent('LIA-2026-061:15')}`, actionsPermises: ['DEPOSER_LIVRABLE'],
      }]),
    ]
    const c = (cle: string, valeur: number, ton: string) => ({ cle, valeur, ton: valeur ? ton : 'ok', lien: null })
    return {
      fonction: 'PARTICIPANT', taches: t, liaisons: [], blocages: [],
      compteurs: [c('tachesARealiser', t.length, 'warn'), c('pvASigner', 0, 'warn'), c('attestationsAFournir', this.attestations.filter((a) => !a.preuve).length, 'warn'), c('fichesSuivi', 0, 'warn'),
        c('tachesEnRetard', t.filter((x) => x.sla === 'DEPASSE').length, 'crit'), c('echeancesProches', t.filter((x) => x.sla === 'ECHEANCE_PROCHE').length, 'warn')],
    }
  }

  private async servir(route: Route) {
    const req = route.request()
    const url = new URL(req.url())
    const chemin = url.pathname
    const methode = req.method()
    const brut = req.postDataBuffer()?.toString('utf8') ?? ''
    this.appels.push({ methode, chemin, entetes: req.headers(), corps: brut })
    const json = (b: unknown, status = 200) => route.fulfill({ status, json: b, headers: { 'X-Correlation-Id': `e2e-portail-${this.appels.length}` } })
    const refus = (status: number, code: string, message: string) => json({ code, message, details: {}, correlationId: `e2e-${code}` }, status)

    if (chemin === '/api/session') {
      return json({ sujet: 'u-correspondant', nom: 'Brice Nzeng Mba', identifiant: 'correspondant-a', roles: ['ROLE_CORRESPONDANT', 'ROLE_USER'], permissions: this.o.permissions ?? PERMISSIONS_CORRESPONDANT,
        institutionId: 'INST-0042', acr: '2', doubleFacteur: true })
    }
    if (chemin === '/api/logout') return json({ logoutUrl: '/connexion' })
    if (chemin === '/bff/portail/live') return route.fulfill({ status: 200, headers: { 'Content-Type': 'text/event-stream' }, body: 'id: 1\ndata: {"id":"1","type":"ping"}\n\n' })
    if (chemin === '/bff/portail/iam/moi') {
      return json({ compte: { id: 'u-correspondant', identifiant: 'correspondant-a', prenom: 'Brice', nom: 'Nzeng Mba', nomAffiche: 'Brice Nzeng Mba', email: 'b.nzeng@participant-a.exemple',
        emailVerifie: true, actif: true, creeLe: '2026-01-10T08:00:00Z', telephone: null, fonction: 'Correspondant', langue: 'fr', institutionId: 'INST-0042', profilComplet: true,
        premierAccesEnAttente: false, profils: ['ROLE_CORRESPONDANT'], actionsRequises: [], secondFacteur: true }, clesSecurite: 1, sessions: 1, etapesRestantes: [] })
    }

    const H = '/bff/portail/homologation'
    const C = '/bff/portail/certification/liaisons'
    if (chemin === `${H}/taches`) return this.o.tachesEnErreur ? refus(503, 'HOM_UNAVAILABLE', 'Service indisponible.') : json(this.taches())
    if (chemin === `${H}/liaisons`) return json([vue('LIA-2026-060', 'CERTIFIEE', 'CERTIFIEE', 'APPROBATION_SECURITE'), vue('LIA-2026-061', 'EN_HOMOLOGATION', 'EN_INTEGRATION', 'INTEGRATION')])
    const d = chemin.match(/^\/bff\/portail\/homologation\/liaisons\/(LIA-2026-06[01])\/(dossier|fiche-suivi)$/)
    if (d) {
      if (d[2] === 'fiche-suivi') return json({ liaisonId: d[1], etat: 'A_REMETTRE', lignes: [], renseigneeLe: null, constateeLe: null, transitionsPermises: [], actionsPermises: [] })
      return json(d[1] === 'LIA-2026-060' ? dossier(d[1], 'APPROBATION_SECURITE', 'CERTIFIEE') : dossier(d[1], 'INTEGRATION', 'EN_INTEGRATION'))
    }
    if (chemin.startsWith(`${H}/liaisons/`)) return refus(404, 'HOM_LIAISON_NOT_FOUND', 'Ce parcours est introuvable.')
    if (chemin === `${H}/operations/LIA-2026-061:15/livrable` && methode === 'POST') {
      this.livrableDepose = true
      return json({}, 201)
    }

    const pv = chemin.match(/^\/bff\/portail\/certification\/liaisons\/(LIA-2026-06[01])\/proces-verbal$/)
    if (pv) {
      const signe = pv[1] === 'LIA-2026-060'
      const sig = (f: string) => ({ signataire: f, fonction: f, signeLe: '2026-09-18', horodatage: '2026-09-18T10:15:00Z' })
      return json({ liaisonId: pv[1], etat: signe ? 'SIGNE' : 'NON_ETABLI', couverture: signe ? couverture(1, 1) : couverture(0, 1), servicesCouverts: signe ? ['PAIEMENT_MARCHAND'] : [],
        etabliPar: signe ? 'CONDUCTEUR' : null, etabliLe: signe ? '2026-09-15' : null, empreinte: signe ? 'cd'.repeat(32) : null, signatureGimac: signe ? sig('CHEF_DES') : null,
        signatureParticipant: signe ? { ...sig('PARTICIPANT'), signataire: 'Représentant de Établissement de paiement A' } : null, transitionsPermises: [], actionsPermises: [] })
    }
    if (chemin === `${C}/LIA-2026-060/approbation-securite`) {
      return json({ liaisonId: 'LIA-2026-060', etat: 'EN_VERIFICATION', dossierChangement: null, ficheChangement: null, documentParametrageRecu: null, fintech: true, visas: [], approuveeLe: null,
        attestations: this.attestations, transitionsPermises: [], actionsPermises: this.attestations.some((a) => !a.preuve) ? ['DEPOSER_ATTESTATION'] : [] })
    }
    if (chemin === `${C}/LIA-2026-061/approbation-securite`) {
      return json({ liaisonId: 'LIA-2026-061', etat: 'EN_ATTENTE_DOSSIER', dossierChangement: null, ficheChangement: null, documentParametrageRecu: null, fintech: false, visas: [], approuveeLe: null,
        attestations: [{ type: 'ISO_27001', requise: true, preuve: null, verifieeLe: null, verifieePar: null, archiveeLe: null, motifRejet: null }], transitionsPermises: [], actionsPermises: [] })
    }
    const att = chemin.match(/^\/bff\/portail\/certification\/liaisons\/LIA-2026-060\/attestations\/([A-Z_0-9]+)$/)
    if (att && methode === 'POST') {
      const a = this.attestations.find((x) => x.type === att[1])
      if (!a) return refus(422, 'CER_ATTESTATION_NOT_REQUIRED', 'Cette attestation n\'est pas exigée pour votre activité.')
      const nom = /filename="([^"]+)"/.exec(brut)?.[1] ?? 'attestation.pdf'
      Object.assign(a, { preuve: { documentRef: `ATT-${a.type}`, nom, empreinte: 'ef'.repeat(32), deposeLe: '2026-09-30', deposePar: 'Correspondant' }, motifRejet: null })
      return json({}, 201)
    }
    return refus(404, 'HTTP_404', `Route non simulée : ${methode} ${chemin}`)
  }
}
