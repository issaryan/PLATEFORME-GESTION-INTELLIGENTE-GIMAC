import { ApiError } from '@gimac/socle'
import type { CoverageValue, TypeProjet } from '../../modele/commun'
import { ETAPES, type ApprobationSecurite, type Blocage, type DossierLiaison, type EtapeCode, type EtapeParcours, type EtatOperationParcours, type FicheSuiviServices, type Fonction, type IndicateursPilotage, type LiaisonResume, type OperationParcours, type Personne, type ProcesVerbalCertification, type ServicePerimetre, type Sla, type StatutParcours, type TableauActeur, type TacheActeur, type TypeAttestation } from '../../modele/parcours'
import { referentielDe, type OperationReferentiel } from '../../modele/referentielCR'
import type { ServiceDecision, ServiceParcours, ServicePortailParcours } from '../ports'

// Serveur de substitution du parcours (étapes 3 à 10, D-56 à D-59) : il tient l'état en mémoire et
// calcule ce que calculeront les services (étape courante, couvertures, SLA, compteurs par fonction,
// actions permises). Données fictives : aucune institution, personne ni infrastructure réelle.

type Attendre = <T>(v: T | (() => T)) => Promise<T>

let seq = 0
const correlation = () => `sim-par-${Date.now().toString(36)}-${(++seq).toString(36)}`
const refus = (status: number, code: string, message: string) => new ApiError(status, code, message, correlation())
const AUJOURDHUI = new Date('2026-09-29T09:00:00Z')
const jour = (decalage: number) => {
  const d = new Date(AUJOURDHUI)
  d.setUTCDate(d.getUTCDate() + decalage)
  return d.toISOString().slice(0, 10)
}
const joursEntre = (a: string, b: string) => Math.round((Date.parse(b) - Date.parse(a)) / 86_400_000)
const hex = (graine: string) => {
  let h = 2166136261
  let s = ''
  for (let i = 0; s.length < 64; i++) {
    h ^= graine.charCodeAt(i % graine.length) + i
    h = Math.imul(h, 16777619) >>> 0
    s += h.toString(16).padStart(8, '0')
  }
  return s.slice(0, 64)
}

/** Intervenants fictifs, désignés par leur fonction (CR §1.5). */
const P = (id: string, nom: string, fonction: Fonction): Personne => ({ id, nom, fonction })
export const PERSONNES: Personne[] = [
  P('U-DDRM-1', 'Chargé DDRM 1', 'DDRM'), P('U-DDRM-2', 'Chargé DDRM 2', 'DDRM'), P('U-CDDRM', 'Chef du département DDRM', 'CHEF_DDRM'),
  P('U-CDES', 'Chef du département DES', 'CHEF_DES'), P('U-COND-1', 'Conducteur de projet 1', 'CONDUCTEUR'), P('U-COND-2', 'Conducteur de projet 2', 'CONDUCTEUR'),
  P('U-SNA-1', 'Ingénieur Normes et Agréments 1', 'SNA'), P('U-SNA-2', 'Ingénieur Normes et Agréments 2', 'SNA'),
  P('U-PM-1', 'Opérateur production monétique 1', 'PRODUCTION_MONETIQUE'), P('U-PM-2', 'Opérateur production monétique 2', 'PRODUCTION_MONETIQUE'),
  P('U-RES-1', 'Ingénieur pôle réseau', 'POLE_RESEAU'), P('U-SYS-1', 'Ingénieur pôle système', 'POLE_SYSTEME'), P('U-RSSI', 'RSSI', 'RSSI'),
  P('U-CONF', 'Responsable conformité', 'CONFORMITE'), P('U-DG', 'Directeur général', 'DG'), P('U-EXP', 'Exploitation et supervision', 'EXPLOITATION'),
]
const personne = (f: Fonction, i = 0): Personne => {
  if (f === 'CHEFS_DEPARTEMENT') return personne('CHEF_DES')
  const l = PERSONNES.filter((p) => p.fonction === f)
  return l[i % Math.max(1, l.length)] ?? P(`U-${f}`, f, f)
}

type Scenario = {
  id: string
  institutionId: string
  institutionNom: string
  typeProjet: TypeProjet
  banque: string | null
  personnalisateur: string | null
  fintech: boolean
  /** Rang (ou code de branche) de la dernière opération faite ; la suivante est en cours. */
  avancement: string
  statut: StatutParcours
  conducteur: number | null
  debut: number
  cible: number | null
  services: { code: string; libelle: string; inclus: boolean }[]
  origine?: string
  /** État forcé du procès-verbal (démonstration des signatures). */
  pv?: 'ETABLI' | 'SIGNE_GIMAC'
  /** Visas optionnels activés pour cette liaison (D-56 : désactivés par défaut). */
  visas?: boolean
}
const SERVICES_CARTE = [{ code: 'RETRAIT', libelle: 'Retrait' }, { code: 'PAIEMENT', libelle: 'Paiement marchand' }, { code: 'TRANSFERT', libelle: 'Transfert' }, { code: 'COMMERCE_ELECTRONIQUE', libelle: 'Commerce électronique' }]
const SERVICES_MOBILE = [{ code: 'PAIEMENT_MARCHAND', libelle: 'Paiement marchand' }, { code: 'REMITTANCE', libelle: 'Rémittence entrante' }, { code: 'FACTURE', libelle: 'Paiement de facture' }, { code: 'CASH_IN', libelle: 'Cash-in agent' }]
const perimetre = (base: typeof SERVICES_CARTE, exclus: string[]) => base.map((s) => ({ ...s, inclus: !exclus.includes(s.code) }))

const SCENARIOS: Scenario[] = [
  { id: 'LIA-2026-041', institutionId: 'INST-0105', institutionNom: 'Fintech F', typeProjet: 'PAIEMENT_MOBILE', banque: 'Banque B', personnalisateur: null, fintech: true, avancement: '6', statut: 'CADREE', conducteur: 0, debut: -12, cible: 110, services: perimetre(SERVICES_MOBILE, ['CASH_IN']) },
  { id: 'LIA-2026-036', institutionId: 'INST-0088', institutionNom: 'Établissement de microfinance C', typeProjet: 'PAIEMENT_MOBILE', banque: null, personnalisateur: null, fintech: false, avancement: '9', statut: 'CADREE', conducteur: 1, debut: -35, cible: 70, services: perimetre(SERVICES_MOBILE, []) },
  { id: 'LIA-2026-030', institutionId: 'INST-0042', institutionNom: 'Établissement de paiement A', typeProjet: 'CARTE_INTERBANCAIRE', banque: 'Banque B', personnalisateur: null, fintech: false, avancement: '13', statut: 'EN_INTEGRATION', conducteur: 0, debut: -58, cible: 32, services: perimetre(SERVICES_CARTE, ['COMMERCE_ELECTRONIQUE']) },
  { id: 'LIA-2026-022', institutionId: 'INST-0017', institutionNom: 'Banque B', typeProjet: 'CARTE_INTERBANCAIRE', banque: null, personnalisateur: null, fintech: false, avancement: '16', statut: 'EN_TEST', conducteur: 1, debut: -80, cible: 12, services: perimetre(SERVICES_CARTE, []) },
  { id: 'LIA-2026-019', institutionId: 'INST-0120', institutionNom: 'Banque G', typeProjet: 'DELEGATAIRE', banque: 'Banque D', personnalisateur: 'Personnalisateur P', fintech: false, avancement: '20', statut: 'CERTIFIEE', conducteur: 0, debut: -96, cible: 20, services: perimetre(SERVICES_CARTE, ['TRANSFERT']), visas: true },
  { id: 'LIA-2026-012', institutionId: 'INST-0061', institutionNom: 'Banque D', typeProjet: 'CARTE_INTERBANCAIRE', banque: null, personnalisateur: null, fintech: false, avancement: 'C9', statut: 'APPROUVEE_SECURITE', conducteur: 1, debut: -120, cible: -2, services: perimetre(SERVICES_CARTE, ['COMMERCE_ELECTRONIQUE']) },
  { id: 'LIA-2025-088', institutionId: 'INST-0042', institutionNom: 'Établissement de paiement A', typeProjet: 'PAIEMENT_MOBILE', banque: null, personnalisateur: null, fintech: false, avancement: '31', statut: 'EN_PRODUCTION', conducteur: 0, debut: -210, cible: -30, services: perimetre(SERVICES_MOBILE, ['CASH_IN']) },
  { id: 'LIA-2026-027', institutionId: 'INST-0042', institutionNom: 'Établissement de paiement A', typeProjet: 'DELEGATAIRE', banque: 'Banque B', personnalisateur: 'Personnalisateur P', fintech: false, avancement: '18', statut: 'EN_TEST', conducteur: 0, debut: -74, cible: 25, services: perimetre(SERVICES_CARTE, ['COMMERCE_ELECTRONIQUE']), pv: 'ETABLI' },
  { id: 'LIA-2026-005', institutionId: 'INST-0073', institutionNom: 'Banque E', typeProjet: 'CARTE_INTERBANCAIRE', banque: null, personnalisateur: null, fintech: false, avancement: '27', statut: 'APPROUVEE_SECURITE', conducteur: 1, debut: -140, cible: 4, services: perimetre(SERVICES_CARTE, ['TRANSFERT', 'COMMERCE_ELECTRONIQUE']) },
  { id: 'LIA-2026-044', institutionId: 'INST-0017', institutionNom: 'Banque B', typeProjet: 'CARTE_INTERBANCAIRE', banque: null, personnalisateur: null, fintech: false, avancement: '3', statut: 'CADREE', conducteur: null, debut: -3, cible: null, services: [{ code: 'COMMERCE_ELECTRONIQUE', libelle: 'Commerce électronique', inclus: true }], origine: 'LIA-2026-022' },
]

/** Action attendue d'une opération ouverte : livrable pour une phase et pour les réunions et ateliers (rangs 7 à 11), preuve sinon. */
const actionOuverte = (o: { grain: string; rang: string }) => (o.grain === 'PHASE' || ['7', '8', '9', '10', '11'].includes(o.rang) ? 'DEPOSER_LIVRABLE' : 'DEPOSER_PREUVE')

type Etat = {
  s: Scenario
  ops: OperationParcours[]
  blocages: Blocage[]
  pv: ProcesVerbalCertification
  approbation: ApprobationSecurite
  fiche: FicheSuiviServices
  conducteur: Personne | null
}

const couv = (n: number, d: number): CoverageValue => ({ taux: d ? Math.round((n / d) * 100) : null, numerateur: n, denominateur: d, calculable: d > 0, motifNonCalculable: d ? undefined : 'aucun service inclus' })

function construire(s: Scenario): Etat {
  const ref = referentielDe(s.typeProjet).filter((o) => Number(o.rang) >= 3 || isNaN(Number(o.rang)))
  const idxAvance = ref.findIndex((o) => o.rang === s.avancement)
  const conducteur = s.conducteur === null ? null : personne('CONDUCTEUR', s.conducteur)
  const ops: OperationParcours[] = ref.map((o: OperationReferentiel, i) => {
    const fait = i <= idxAvance
    const enAttente = o.predecesseurs.filter((p) => ref.findIndex((x) => x.rang === p) > idxAvance)
    const courant = !fait && enAttente.length === 0
    const echeance = jour(s.debut + 4 + i * 3)
    const etat: EtatOperationParcours = fait ? 'FAITE' : courant ? 'A_FAIRE' : 'VERROUILLEE'
    const R = o.porteurs[0] === 'PARTICIPANT' ? P(`EXT-${s.institutionId}`, `Correspondant ${s.institutionNom}`, 'PARTICIPANT') : o.porteurs[0] === 'CONDUCTEUR' ? conducteur : o.porteurs[0] === 'PERSONNALISATEUR' ? P('EXT-PERSO', s.personnalisateur ?? 'Personnalisateur', 'PERSONNALISATEUR') : personne(o.porteurs[0])
    const autres = o.porteurs.slice(1).map((f) => (f === 'PARTICIPANT' ? P(`EXT-${s.institutionId}`, `Correspondant ${s.institutionNom}`, 'PARTICIPANT') : personne(f)))
    const sla: Sla | null = fait ? null : courant ? (Date.parse(echeance) < AUJOURDHUI.getTime() ? 'DEPASSE' : joursEntre(AUJOURDHUI.toISOString(), echeance) <= 5 ? 'ECHEANCE_PROCHE' : 'DANS_LES_DELAIS') : null
    return {
      id: `${s.id}:${o.rang}`,
      liaisonId: s.id,
      rang: o.rang,
      etape: o.etape,
      segment: o.segment,
      plateforme: o.rang === '15' || o.rang === '16' ? (s.typeProjet === 'PAIEMENT_MOBILE' ? 'SIMULATEUR' : null) : o.plateforme,
      grain: o.grain,
      etat,
      raci: { R, C: autres.filter((p) => p.fonction !== 'PARTICIPANT'), V: o.porteurs[0] === 'CONDUCTEUR' ? personne('CHEF_DES') : conducteur, I: [personne('CHEF_DDRM')], E: autres.filter((p) => p.fonction === 'PARTICIPANT' || p.fonction === 'PERSONNALISATEUR') },
      echeance,
      sla,
      faiteLe: fait ? jour(s.debut + 3 + i * 3) : null,
      preuve: fait
        ? {
            documentRef: `DOC-${s.id.slice(-3)}-${o.rang}`,
            nom: o.grain === 'PHASE' ? `document-parametrage-${s.id}.pdf` : `preuve-${o.rang}.png`,
            empreinte: hex(`${s.id}-${o.rang}`),
            deposeLe: jour(s.debut + 3 + i * 3),
            deposePar: R?.nom ?? '',
            valeurReference: o.valeurReference ? { libelle: `parcours.valeur.${o.rang}`, valeur: o.rang === '12' ? `participant-${s.id.slice(-3)}.essai:8443` : `REF-${o.rang}-${s.id.slice(-3)}` } : undefined,
            codeControle: o.cle ? hex(`kcv-${s.id}-${o.rang}`).slice(0, 6).toUpperCase() : undefined,
          }
        : null,
      cle: Boolean(o.cle),
      valeurReference: Boolean(o.valeurReference),
      oubliSansSymptome: Boolean(o.oubliSansSymptome),
      pivot: Boolean(o.pivot),
      predecesseursNonFaits: fait ? [] : enAttente,
      transitionsPermises: [],
      actionsPermises: courant ? [actionOuverte(o)] : fait ? ['CONSULTER'] : [],
    }
  })
  const inclus = s.services.filter((x) => x.inclus)
  const certifies = ['CERTIFIEE', 'APPROUVEE_SECURITE', 'EN_PRODUCTION'].includes(s.statut) || s.pv ? inclus.length : s.statut === 'EN_TEST' ? inclus.length - 1 : 0
  const pvEtat = s.pv ?? (['APPROUVEE_SECURITE', 'EN_PRODUCTION', 'CERTIFIEE'].includes(s.statut) ? 'SIGNE' : 'NON_ETABLI')
  const signe = (f: Fonction, nom: string, d: number) => ({ signataire: nom, fonction: f, signeLe: jour(d), horodatage: `${jour(d)}T10:15:00Z` })
  const pv: ProcesVerbalCertification = {
    liaisonId: s.id,
    etat: pvEtat,
    couverture: couv(certifies, inclus.length),
    servicesCouverts: inclus.slice(0, certifies).map((x) => x.code),
    etabliPar: pvEtat !== 'NON_ETABLI' ? (conducteur?.nom ?? null) : null,
    etabliLe: pvEtat !== 'NON_ETABLI' ? jour(s.debut + 60) : null,
    empreinte: pvEtat !== 'NON_ETABLI' ? hex(`pv-${s.id}`) : null,
    signatureGimac: pvEtat === 'SIGNE' || pvEtat === 'SIGNE_GIMAC' ? signe('CHEF_DES', personne('CHEF_DES').nom, s.debut + 61) : null,
    signatureParticipant: pvEtat === 'SIGNE' ? signe('PARTICIPANT', `Représentant de ${s.institutionNom}`, s.debut + 62) : null,
    transitionsPermises: [],
    actionsPermises: [],
  }
  const typesAttestation: TypeAttestation[] = s.typeProjet === 'PAIEMENT_MOBILE' ? ['ISO_27001', 'GSMA'] : ['PCI_DSS']
  const approuve = ['APPROUVEE_SECURITE', 'EN_PRODUCTION'].includes(s.statut)
  const approbation: ApprobationSecurite = {
    liaisonId: s.id,
    etat: approuve ? 'APPROUVEE' : s.statut === 'CERTIFIEE' ? 'DOSSIER_CONSTITUE' : 'EN_ATTENTE_DOSSIER',
    dossierChangement: approuve || s.statut === 'CERTIFIEE' ? { documentRef: `CHG-DOS-${s.id.slice(-3)}`, nom: `dossier-changement-${s.id}.pdf`, empreinte: hex(`chg-${s.id}`), deposeLe: jour(s.debut + 64), deposePar: personne('PRODUCTION_MONETIQUE').nom } : null,
    ficheChangement: approuve ? { reference: `CHG-2026-${s.id.slice(-3)}`, etatApprobation: 'APPROUVEE', lueLe: jour(-1), aRelire: false } : null,
    documentParametrageRecu: approuve ? jour(s.debut + 66) : null,
    attestations: [...typesAttestation, ...(s.fintech ? (['COBAC'] as TypeAttestation[]) : [])].map((type) => ({
      type,
      requise: true,
      preuve: approuve || (s.statut === 'CERTIFIEE' && type !== 'PCI_DSS') ? { documentRef: `ATT-${type}-${s.id.slice(-3)}`, nom: `${type.toLowerCase()}-${s.institutionId}.pdf`, empreinte: hex(`att-${type}-${s.id}`), deposeLe: jour(s.debut + 65), deposePar: `Correspondant ${s.institutionNom}` } : null,
      verifieeLe: approuve ? jour(s.debut + 67) : null,
      verifieePar: approuve ? personne('RSSI').nom : null,
      archiveeLe: approuve ? jour(s.debut + 67) : null,
      motifRejet: null,
    })),
    fintech: s.fintech,
    visas: [
      { nature: 'CONFORMITE', actif: Boolean(s.visas), etat: 'ATTENDU', signature: null, motif: null },
      { nature: 'DIRECTION_GENERALE', actif: Boolean(s.visas), etat: 'ATTENDU', signature: null, motif: null },
    ],
    approuveeLe: approuve ? jour(s.debut + 68) : null,
    transitionsPermises: [],
    actionsPermises: [],
  }
  const enProd = s.statut === 'EN_PRODUCTION'
  const fiche: FicheSuiviServices = {
    liaisonId: s.id,
    etat: enProd ? 'REMISE' : 'A_REMETTRE',
    lignes: inclus.map((x, i) => ({ serviceCode: x.code, libelle: x.libelle, ouvert: enProd && i === 0 ? true : null, commentaire: null })),
    renseigneeLe: null,
    constateeLe: null,
    transitionsPermises: [],
    actionsPermises: [],
  }
  const blocages: Blocage[] =
    s.id === 'LIA-2026-030'
      ? [{ id: 'BLQ-0007', liaisonId: s.id, motif: 'Élément de sécurité du participant à renouveler avant la reprise des essais', depuis: jour(-9), prochaineAction: 'Obtenir le nouvel élément auprès du participant', responsable: personne('POLE_RESEAU'), echeanceLevee: jour(-2), leveLe: null, etape: 'INTEGRATION', transitionsPermises: [], actionsPermises: ['LEVER'] }]
      : []
  if (blocages.length) ops.filter((o) => o.etat === 'A_FAIRE').forEach((o) => (o.etat = 'BLOQUEE'))
  return { s, ops, blocages, pv, approbation, fiche, conducteur }
}

function etapesDe(e: Etat): EtapeParcours[] {
  const rangEtape: Record<EtapeCode, number | null> = { PRISE_CONTACT: 1, ADHESION: 2, CADRAGE: 3, LANCEMENT: 4, ATELIERS: 5, INTEGRATION: 6, TESTS: 7, CERTIFICATION: 8, APPROBATION_SECURITE: null, MISE_EN_PRODUCTION: 9, SUIVI: 10 }
  return ETAPES.map((code) => {
    const ops = e.ops.filter((o) => o.etape === code)
    if (code === 'PRISE_CONTACT' || code === 'ADHESION') {
      return { code, rang: rangEtape[code], etat: 'CLOSE', porteurs: ['DDRM'], responsable: personne('DDRM'), elements: { RESPONSABLE: true, ACTIONS: true, LIVRABLES: true, VALIDATION: true, PREUVE: true, DATE: true }, actionsFaites: 1, actionsTotal: 1, livrables: [{ libelleKey: code === 'ADHESION' ? 'parcours.livrable.dossierAdhesion' : 'parcours.livrable.enregistrementCandidat', fourni: true }], validateur: personne('CHEF_DDRM'), valideeLe: jour(e.s.debut - 20), debut: jour(e.s.debut - 40), closeLe: jour(e.s.debut - 20), echeance: null, sla: null }
    }
    const faites = ops.filter((o) => o.etat === 'FAITE').length
    const close = ops.length > 0 && faites === ops.length
    const enCours = !close && (faites > 0 || ops.some((o) => o.etat === 'A_FAIRE' || o.etat === 'BLOQUEE'))
    const bloquee = ops.some((o) => o.etat === 'BLOQUEE')
    const premier = ops[0]
    const dernier = ops[ops.length - 1]
    return {
      code,
      rang: rangEtape[code],
      etat: ops.length === 0 ? 'SANS_OBJET' : close ? 'CLOSE' : bloquee ? 'BLOQUEE' : enCours ? 'EN_COURS' : 'A_VENIR',
      porteurs: [...new Set(ops.flatMap((o) => (o.raci.R ? [o.raci.R.fonction] : [])))],
      responsable: premier?.raci.R ?? null,
      elements: { RESPONSABLE: Boolean(premier?.raci.R), ACTIONS: close, LIVRABLES: close, VALIDATION: close, PREUVE: ops.every((o) => o.etat !== 'FAITE' || o.preuve !== null) && faites > 0, DATE: close },
      actionsFaites: faites,
      actionsTotal: ops.length,
      livrables: ops.filter((o) => o.grain === 'PHASE' || o.pivot).map((o) => ({ libelleKey: `parcours.op.${o.rang}`, fourni: o.etat === 'FAITE' })),
      validateur: premier?.raci.V ?? null,
      valideeLe: close ? (dernier?.faiteLe ?? null) : null,
      debut: premier?.faiteLe ?? (enCours ? premier?.echeance ?? null : null),
      closeLe: close ? (dernier?.faiteLe ?? null) : null,
      echeance: dernier?.echeance ?? null,
      sla: close ? null : ops.some((o) => o.sla === 'DEPASSE') ? 'DEPASSE' : ops.some((o) => o.sla === 'ECHEANCE_PROCHE') ? 'ECHEANCE_PROCHE' : enCours ? 'DANS_LES_DELAIS' : null,
    }
  })
}

const etapeCourante = (e: Etat): EtapeCode => e.ops.find((o) => o.etat !== 'FAITE')?.etape ?? 'SUIVI'

function perimetreDe(e: Etat): ServicePerimetre[] {
  return e.s.services.map((x, i) => {
    const certifie = e.pv.servicesCouverts.includes(x.code)
    const ouvert = e.fiche.lignes.find((l) => l.serviceCode === x.code)?.ouvert
    const enTest = !certifie && ['EN_TEST'].includes(e.s.statut)
    return {
      serviceCode: x.code,
      libelle: x.libelle,
      inclus: x.inclus,
      etat: !x.inclus ? 'NON_DEMARRE' : ouvert === true ? 'OUVERT' : ouvert === false ? 'NON_OUVERT' : certifie ? 'CERTIFIE' : enTest ? 'EN_TEST' : 'NON_DEMARRE',
      entreeEssai: certifie || enTest ? jour(e.s.debut + 40 + i) : null,
      sortieEssai: certifie ? jour(e.s.debut + 52 + i * 2) : null,
      certifieLe: certifie ? jour(e.s.debut + 60) : null,
      avancementSimulateur: e.s.typeProjet === 'PAIEMENT_MOBILE' && x.inclus ? (certifie ? 100 : enTest ? 60 : e.s.statut === 'EN_INTEGRATION' ? 15 : null) : null,
    }
  })
}

function resume(e: Etat): LiaisonResume {
  const p = perimetreDe(e)
  const inclus = p.filter((x) => x.inclus)
  const prochaine = e.ops.filter((o) => o.etat !== 'FAITE' && o.echeance).map((o) => o.echeance as string).sort()[0] ?? null
  return {
    id: e.s.id,
    reference: e.s.id,
    institutionNom: e.s.institutionNom,
    typeProjet: e.s.typeProjet,
    statut: e.s.statut,
    etapeCourante: etapeCourante(e),
    conducteurNom: e.conducteur?.nom ?? null,
    couvertureCertification: couv(inclus.filter((x) => ['CERTIFIE', 'OUVERT', 'NON_OUVERT'].includes(x.etat)).length, inclus.length),
    couvertureMiseEnService: couv(inclus.filter((x) => x.etat === 'OUVERT').length, inclus.length),
    bloquee: e.blocages.some((b) => !b.leveLe),
    tachesEnRetard: e.ops.filter((o) => o.sla === 'DEPASSE').length,
    prochaineEcheance: prochaine,
    dateCibleProduction: e.s.cible === null ? null : jour(e.s.cible),
  }
}

/** Opérations qu'une fonction réalise (R) ou valide (V), ou qui incombent au participant (E). */
function tachesDe(etats: Etat[], fonctions: Fonction[]): TacheActeur[] {
  const participant = fonctions.includes('PARTICIPANT')
  return etats.flatMap((e) =>
    e.ops
      .filter((o) => o.etat === 'A_FAIRE' || o.etat === 'EN_COURS' || o.etat === 'BLOQUEE' || o.etat === 'EN_VALIDATION')
      .flatMap((o): TacheActeur[] => {
        const r = o.raci.R?.fonction
        const estR = r !== undefined && (fonctions.includes(r) || (r === 'CHEF_DES' && fonctions.includes('CHEFS_DEPARTEMENT')))
        const estE = participant && (r === 'PARTICIPANT' || o.raci.E.some((p) => p.fonction === 'PARTICIPANT'))
        const estV = o.etat === 'EN_VALIDATION' && o.raci.V !== null && fonctions.includes(o.raci.V.fonction)
        if (!estR && !estE && !estV) return []
        return [{
          operationId: o.id, liaisonId: e.s.id, liaisonReference: e.s.id, institutionNom: e.s.institutionNom, rang: o.rang, etape: o.etape,
          qualite: estV ? 'V' : estE && !estR ? 'E' : 'R', echeance: o.echeance, sla: o.sla, oubliSansSymptome: o.oubliSansSymptome,
          lien: participant ? `/portail/liaisons/${e.s.id}?onglet=a-faire` : `/des/liaisons/${e.s.id}?onglet=operations&operation=${encodeURIComponent(o.id)}`,
        }]
      }),
  )
}

export function parcoursSimule(attendre: Attendre, institutionPortail: string): { parcours: ServiceParcours; decision: ServiceDecision; portailParcours: ServicePortailParcours } {
  // Copie des scénarios : chaque instance a son propre état (le statut d'une liaison ne fuit pas d'une instance à l'autre).
  const etats = new Map(SCENARIOS.map((s) => [s.id, construire({ ...s })]))
  const tous = () => [...etats.values()]
  const trouver = (id: string) => {
    const e = etats.get(id)
    if (!e) throw refus(404, 'HOM_LIAISON_NOT_FOUND', 'Liaison introuvable.')
    return e
  }
  const operation = (id: string) => {
    const [liaisonId] = id.split(':')
    const e = trouver(liaisonId)
    const o = e.ops.find((x) => x.id === id)
    if (!o) throw refus(404, 'HOM_OPERATION_NOT_FOUND', 'Opération introuvable.')
    return { e, o }
  }
  /** Clôt une opération puis ouvre les suivantes dont tous les prédécesseurs sont faits. */
  const terminer = (e: Etat, o: OperationParcours, preuve: OperationParcours['preuve']) => {
    o.etat = 'FAITE'
    o.faiteLe = jour(0)
    o.sla = null
    o.preuve = preuve
    o.actionsPermises = ['CONSULTER']
    e.ops.forEach((x) => {
      x.predecesseursNonFaits = x.predecesseursNonFaits.filter((r) => r !== o.rang)
      if (x.etat === 'VERROUILLEE' && x.predecesseursNonFaits.length === 0) {
        x.etat = 'A_FAIRE'
        x.actionsPermises = [actionOuverte(x)]
      }
    })
  }
  const dossierDe = (e: Etat): DossierLiaison => {
    const r = resume(e)
    const peutConduire = e.conducteur !== null
    return {
      id: e.s.id, reference: e.s.id, institutionId: e.s.institutionId, institutionNom: e.s.institutionNom, typeProjet: e.s.typeProjet,
      banquePartenaireNom: e.s.banque, personnalisateurNom: e.s.personnalisateur, statut: e.s.statut, etapeCourante: r.etapeCourante,
      conducteur: e.conducteur, responsableFonctionnel: personne('DDRM'), responsableTechnique: personne('SNA'),
      dateDemarrage: jour(e.s.debut), dateCibleProduction: r.dateCibleProduction, versionReferentiel: e.s.typeProjet === 'PAIEMENT_MOBILE' ? 2 : 4,
      perimetre: perimetreDe(e), couvertureCertification: r.couvertureCertification, couvertureMiseEnService: r.couvertureMiseEnService,
      etapes: etapesDe(e), blocages: e.blocages, interlocuteursParticipant: [{ nom: `Correspondant technique ${e.s.institutionNom}`, fonction: 'Responsable technique' }, { nom: `Correspondant fonctionnel ${e.s.institutionNom}`, fonction: 'Responsable fonctionnel' }],
      liaisonOrigineId: e.s.origine ?? null,
      transitionsPermises: [],
      actionsPermises: [
        ...(peutConduire ? ['DECLARER_BLOCAGE'] : []),
        'DESIGNER_CONDUCTEUR',
        ...(e.s.statut === 'EN_PRODUCTION' ? ['OUVRIR_SERVICE_SUPPLEMENTAIRE'] : []),
        // Actions offertes seulement si le service les accepterait : rang 6 ouvert, rang 17 ouvert.
        ...(e.ops.some((o) => o.rang === '6' && o.etat === 'A_FAIRE') ? ['AFFECTER_RESSOURCES'] : []),
        ...(e.ops.some((o) => o.rang === '17' && (o.etat === 'A_FAIRE' || o.etat === 'EN_COURS')) ? ['CONSTATER_COUVERTURE'] : []),
      ],
    }
  }

  const compteur = (cle: string, valeur: number, ton: TableauActeur['compteurs'][number]['ton'], lien: string | null = null) => ({ cle, valeur, ton: valeur === 0 ? 'ok' as const : ton, lien })
  const aEtape = (codes: EtapeCode[]) => tous().filter((e) => codes.includes(etapeCourante(e)))

  const parcours: ServiceParcours = {
    listerLiaisons: (f) => attendre(() => {
      let l = tous().map(resume)
      if (f.recherche) l = l.filter((x) => (x.institutionNom + x.reference).toLowerCase().includes(f.recherche!.toLowerCase()))
      if (f.statut) l = l.filter((x) => x.statut === f.statut)
      if (f.typeProjet) l = l.filter((x) => x.typeProjet === f.typeProjet)
      if (f.etape) l = l.filter((x) => x.etapeCourante === f.etape)
      if (f.bloquees) l = l.filter((x) => x.bloquee)
      if (f.mesProjets) l = l.filter((x) => x.conducteurNom === personne('CONDUCTEUR', 0).nom)
      return { contenu: l.slice((f.page - 1) * f.taille, f.page * f.taille), total: l.length }
    }),
    conducteurs: () => attendre(PERSONNES.filter((p) => p.fonction === 'CONDUCTEUR')),
    ouvrirLiaison: (o) => attendre(() => {
      if (tous().some((e) => e.s.institutionId === o.institutionId && e.s.typeProjet === o.typeProjet && e.s.statut !== 'CLOSE_SANS_SUITE' && e.s.statut !== 'EN_PRODUCTION')) {
        throw refus(409, 'HOM_LIAISON_DUPLICATE', 'Une liaison active existe déjà pour cette institution et ce type de projet.')
      }
      // Même règle que la désignation : seul un conducteur de projet habilité conduit la liaison (D-58).
      const cond = PERSONNES.filter((p) => p.fonction === 'CONDUCTEUR').findIndex((p) => p.id === o.chefDeProjetId)
      if (cond < 0) throw refus(422, 'HOM_LEAD_NOT_ELIGIBLE', 'La personne désignée ne détient pas le rôle de conducteur de projet.')
      const s: Scenario = { id: `LIA-2026-${String(50 + etats.size).padStart(3, '0')}`, institutionId: o.institutionId, institutionNom: o.institutionId === 'INST-0042' ? 'Établissement de paiement A' : o.institutionId === 'INST-0017' ? 'Banque B' : 'Nouvel adhérent', typeProjet: o.typeProjet, banque: null, personnalisateur: null, fintech: false, avancement: '2', statut: 'CADREE', conducteur: cond, debut: 0, cible: null, services: perimetre(o.typeProjet === 'PAIEMENT_MOBILE' ? SERVICES_MOBILE : SERVICES_CARTE, []) }
      const e = construire(s)
      etats.set(s.id, e)
      return resume(e)
    }),
    designerConducteur: (id, conducteurId, motif) => attendre(() => {
      const e = trouver(id)
      if (e.conducteur && !motif.trim()) throw refus(422, 'HOM_DESIGNATION_REASON_REQUIRED', 'Une redésignation doit être motivée.')
      const p = PERSONNES.find((x) => x.id === conducteurId && x.fonction === 'CONDUCTEUR')
      if (!p) throw refus(422, 'HOM_LEAD_NOT_ELIGIBLE', 'La personne désignée ne détient pas le rôle de conducteur de projet.')
      e.conducteur = p
      // Sans conducteur, ses places (R des opérations qu'il porte, V des autres) sont vides : la première désignation les pourvoit.
      e.ops.forEach((o) => { if (o.raci.R === null || o.raci.R.fonction === 'CONDUCTEUR') o.raci.R = p; if (o.raci.V === null || o.raci.V.fonction === 'CONDUCTEUR') o.raci.V = p })
    }),
    dossier: (id) => attendre(() => dossierDe(trouver(id))),
    operations: (id, f) => attendre(() => trouver(id).ops.filter((o) => (!f?.etape || o.etape === f.etape) && (!f?.fonction || o.raci.R?.fonction === f.fonction))),
    affecterRessources: (id, a) => attendre(() => {
      const e = trouver(id)
      const o = e.ops.find((x) => x.rang === '6')
      if (!o || o.etat !== 'A_FAIRE') throw refus(409, 'HOM_STEP_NOT_OPEN', 'L\'affectation des ressources n\'est pas ouverte.')
      if (a.length === 0) throw refus(422, 'HOM_RESOURCES_REQUIRED', 'Au moins une ressource doit être affectée.')
      terminer(e, o, { documentRef: `AFF-${id.slice(-3)}`, nom: 'designation-ressources', empreinte: hex(`aff-${id}`), deposeLe: jour(0), deposePar: personne('CHEF_DES').nom })
    }),
    deposerLivrable: (opId, fichier, commentaire) => attendre(() => {
      const { e, o } = operation(opId)
      if (!o.actionsPermises.includes('DEPOSER_LIVRABLE') && !o.actionsPermises.includes('DEPOSER_PREUVE')) throw refus(409, 'HOM_OPERATION_NOT_OPEN', 'Cette opération n\'attend pas de livrable.')
      // Un livrable ne dispense ni du code de contrôle d'une clé ni de la valeur de référence (D-59) : dépôt de preuve exigé.
      if (o.cle) throw refus(422, 'HOM_KEY_CHECK_VALUE_REQUIRED', 'Le code de contrôle de la clé est exigé ; aucune valeur de clé n\'est admise.')
      if (o.valeurReference) throw refus(422, 'HOM_REFERENCE_VALUE_REQUIRED', 'Cette preuve doit porter la valeur de référence relevée.')
      terminer(e, o, { documentRef: `DOC-${opId.replace(':', '-')}`, nom: fichier.name, empreinte: hex(fichier.name + fichier.size + commentaire), deposeLe: jour(0), deposePar: o.raci.R?.nom ?? '' })
    }),
    attribuer: (opId, qualite, personneId) => attendre(() => {
      const { o } = operation(opId)
      const p = PERSONNES.find((x) => x.id === personneId)
      if (!p) throw refus(422, 'HOM_PERSON_UNKNOWN', 'Personne inconnue.')
      if (qualite === 'R') {
        if (o.raci.V?.id === p.id) throw refus(409, 'HOM_VALIDATOR_IS_RESPONSIBLE', 'Le validateur doit être distinct du responsable.')
        o.raci.R = p
      } else if (qualite === 'V') {
        if (o.raci.R?.id === p.id) throw refus(409, 'HOM_VALIDATOR_IS_RESPONSIBLE', 'Le validateur doit être distinct du responsable.')
        o.raci.V = p
      } else o.raci[qualite] = [...o.raci[qualite], p]
    }),
    deposerPreuve: (opId, fichier, c) => attendre(() => {
      const { e, o } = operation(opId)
      if (o.etat !== 'A_FAIRE' && o.etat !== 'EN_COURS') throw refus(409, 'HOM_OPERATION_NOT_OPEN', 'Cette opération n\'est pas ouverte.')
      if (o.cle && !c.codeControle) throw refus(422, 'HOM_KEY_CHECK_VALUE_REQUIRED', 'Le code de contrôle de la clé est exigé ; aucune valeur de clé n\'est admise.')
      if (o.cle && c.codeControle && !/^[0-9A-F]{4,8}$/.test(c.codeControle)) throw refus(422, 'HOM_KEY_CHECK_VALUE_INVALID', 'Un code de contrôle compte de 4 à 8 caractères hexadécimaux.')
      if (o.valeurReference && !c.valeurReference) throw refus(422, 'HOM_REFERENCE_VALUE_REQUIRED', 'Cette preuve doit porter la valeur de référence relevée.')
      o.etat = 'EN_VALIDATION'
      o.preuve = { documentRef: `DOC-${opId.replace(':', '-')}`, nom: fichier.name, empreinte: hex(fichier.name + fichier.size), deposeLe: jour(0), deposePar: o.raci.R?.nom ?? '', valeurReference: c.valeurReference ? { libelle: `parcours.valeur.${o.rang}`, valeur: c.valeurReference } : undefined, codeControle: c.codeControle }
      o.actionsPermises = ['VALIDER']
      void e
    }),
    validerOperation: (opId) => attendre(() => {
      const { e, o } = operation(opId)
      if (o.etat !== 'EN_VALIDATION') throw refus(409, 'HOM_OPERATION_NOT_IN_VALIDATION', 'L\'opération n\'attend pas de validation.')
      terminer(e, o, o.preuve)
    }),
    declarerBlocage: (id, b) => attendre(() => {
      const e = trouver(id)
      const resp = PERSONNES.find((p) => p.id === b.responsableId) ?? personne('CONDUCTEUR')
      // Identifiant unique sur toutes les liaisons : la levée retrouve le blocage par son seul identifiant.
      const blocage: Blocage = { id: `BLQ-${String(100 + tous().reduce((n, x) => n + x.blocages.length, 0))}`, liaisonId: id, motif: b.motif, depuis: jour(0), prochaineAction: b.prochaineAction, responsable: resp, echeanceLevee: b.echeanceLevee, leveLe: null, etape: etapeCourante(e), transitionsPermises: [], actionsPermises: ['LEVER'] }
      e.blocages.push(blocage)
      return blocage
    }),
    leverBlocage: (bid) => attendre(() => {
      const e = tous().find((x) => x.blocages.some((b) => b.id === bid))
      const b = e?.blocages.find((x) => x.id === bid)
      if (!e || !b) throw refus(404, 'HOM_BLOCKAGE_NOT_FOUND', 'Blocage introuvable.')
      b.leveLe = jour(0)
      b.actionsPermises = []
      e.ops.filter((o) => o.etat === 'BLOQUEE').forEach((o) => (o.etat = 'A_FAIRE'))
    }),
    constaterCouverture: (id) => attendre(() => {
      const e = trouver(id)
      const o = e.ops.find((x) => x.rang === '17')
      if (!o || (o.etat !== 'A_FAIRE' && o.etat !== 'EN_COURS')) throw refus(409, 'HOM_COVERAGE_NOT_READY', 'Les essais ne sont pas terminés : la couverture ne peut pas être constatée.')
      terminer(e, o, { documentRef: `COUV-${id.slice(-3)}`, nom: 'constat-couverture', empreinte: hex(`couv-${id}`), deposeLe: jour(0), deposePar: personne('SNA').nom })
    }),
    ouvrirServiceSupplementaire: (id, services) => attendre(() => {
      const e = trouver(id)
      if (e.s.statut !== 'EN_PRODUCTION') throw refus(409, 'HOM_LIAISON_NOT_IN_PRODUCTION', 'Un service supplémentaire s\'ouvre sur une liaison en production.')
      const base = e.s.typeProjet === 'PAIEMENT_MOBILE' ? SERVICES_MOBILE : SERVICES_CARTE
      const s: Scenario = { ...e.s, id: `LIA-2026-${String(50 + etats.size).padStart(3, '0')}`, avancement: '2', statut: 'CADREE', conducteur: null, debut: 0, cible: null, services: base.filter((x) => services.includes(x.code)).map((x) => ({ ...x, inclus: true })), origine: id }
      const n = construire(s)
      etats.set(s.id, n)
      return resume(n)
    }),
    tableauActeur: (fonctions) => attendre((): TableauActeur => {
      const f = (x: Fonction) => fonctions.includes(x)
      const actives = tous().filter((e) => e.s.statut !== 'CLOSE_SANS_SUITE')
      // Le participant ne voit que les liaisons de son institution : tâches et blocages compris.
      const visibles = f('PARTICIPANT') ? actives.filter((e) => e.s.institutionId === institutionPortail) : actives
      const taches = tachesDe(visibles, fonctions)
      const compteurs: TableauActeur['compteurs'] = []
      if (f('CHEF_DDRM')) compteurs.push(compteur('conducteursADesigner', actives.filter((e) => !e.conducteur).length, 'warn', '/des/liaisons?sansConducteur=1'), compteur('liaisonsAmont', aEtape(['CADRAGE', 'LANCEMENT', 'ATELIERS']).length, 'info', '/des/liaisons?etape=ATELIERS'))
      if (f('DDRM') || f('CHEF_DDRM')) compteurs.push(compteur('ateliersATenir', actives.flatMap((e) => e.ops.filter((o) => o.rang === '8' && o.etat === 'A_FAIRE')).length, 'warn'), compteur('perimetresAArreter', actives.flatMap((e) => e.ops.filter((o) => ['3', '4', '5'].includes(o.rang) && o.etat === 'A_FAIRE')).length, 'warn'))
      if (f('CHEF_DES')) compteurs.push(compteur('ressourcesAAffecter', actives.flatMap((e) => e.ops.filter((o) => o.rang === '6' && o.etat === 'A_FAIRE')).length, 'warn'), compteur('enIntegrationTests', aEtape(['INTEGRATION', 'TESTS']).length, 'info', '/des/liaisons?etape=TESTS'), compteur('pvASigner', actives.filter((e) => e.pv.etat === 'ETABLI' || e.pv.etat === 'SIGNE_PARTICIPANT').length, 'warn'))
      if (f('CONDUCTEUR')) compteurs.push(compteur('mesProjets', actives.filter((e) => e.conducteur?.id === personne('CONDUCTEUR', 0).id).length, 'info', '/des/liaisons?mesProjets=1'), compteur('pvAEtablir', actives.filter((e) => e.ops.some((o) => o.rang === '18' && o.etat === 'A_FAIRE')).length, 'warn'))
      if (f('SNA')) compteurs.push(compteur('essaisEnCours', aEtape(['TESTS']).length, 'info'), compteur('documentsParametrage', actives.flatMap((e) => e.ops.filter((o) => o.rang === '15' && o.etat !== 'FAITE' && o.etat !== 'VERROUILLEE')).length, 'warn'), compteur('couverturesAConstater', actives.flatMap((e) => e.ops.filter((o) => o.rang === '17' && o.etat === 'A_FAIRE')).length, 'warn'), compteur('constatsMiseEnService', actives.filter((e) => e.fiche.etat === 'RENSEIGNEE').length, 'warn'))
      if (f('PRODUCTION_MONETIQUE')) compteurs.push(compteur('dossiersChangement', actives.filter((e) => e.approbation.etat === 'EN_ATTENTE_DOSSIER' && e.pv.etat === 'SIGNE').length, 'warn'), compteur('operationsBranche', actives.flatMap((e) => e.ops.filter((o) => o.segment === 'BRANCHE' && o.raci.R?.fonction === 'PRODUCTION_MONETIQUE' && o.etat === 'A_FAIRE')).length, 'info'), compteur('rapportsAConsolider', actives.flatMap((e) => e.ops.filter((o) => o.rang === '31' && o.etat === 'A_FAIRE')).length, 'warn'))
      if (f('POLE_RESEAU')) compteurs.push(compteur('liensEssai', actives.flatMap((e) => e.ops.filter((o) => ['13', '14'].includes(o.rang) && o.etat !== 'FAITE' && o.etat !== 'VERROUILLEE')).length, 'warn'), compteur('operationsReseau', taches.length, 'info'))
      if (f('POLE_SYSTEME')) compteurs.push(compteur('scriptsDepot', actives.flatMap((e) => e.ops.filter((o) => o.rang === '28' && o.etat !== 'FAITE' && o.etat !== 'VERROUILLEE')).length, 'crit'), compteur('operationsSysteme', taches.length, 'info'))
      if (f('RSSI')) compteurs.push(compteur('attestationsAVerifier', actives.flatMap((e) => e.approbation.attestations.filter((a) => a.preuve && !a.verifieeLe)).length, 'warn'), compteur('approbationsEnAttente', actives.filter((e) => ['DOSSIER_CONSTITUE', 'EN_VERIFICATION', 'VERIFIE_ARCHIVE'].includes(e.approbation.etat)).length, 'warn'), compteur('ceremoniesCles', actives.flatMap((e) => e.ops.filter((o) => (o.rang === 'C1' || o.rang === 'D3') && o.etat === 'A_FAIRE')).length, 'warn'))
      if (f('CONFORMITE')) compteurs.push(compteur('visasAttendus', actives.filter((e) => e.approbation.visas.some((v) => v.nature === 'CONFORMITE' && v.actif && v.etat === 'ATTENDU')).length, 'warn'), compteur('fintechsCobac', actives.filter((e) => e.approbation.fintech).length, 'info'))
      if (f('DG')) compteurs.push(compteur('visasAttendus', actives.filter((e) => e.approbation.visas.some((v) => v.nature === 'DIRECTION_GENERALE' && v.actif && v.etat === 'ATTENDU')).length, 'warn'))
      if (f('PARTICIPANT')) {
        const siennes = actives.filter((e) => e.s.institutionId === institutionPortail)
        compteurs.push(compteur('tachesARealiser', taches.length, 'warn'), compteur('pvASigner', siennes.filter((e) => e.pv.etat === 'ETABLI' || e.pv.etat === 'SIGNE_GIMAC').length, 'warn'), compteur('attestationsAFournir', siennes.flatMap((e) => e.approbation.attestations.filter((a) => !a.preuve && e.pv.etat === 'SIGNE')).length, 'warn'), compteur('fichesSuivi', siennes.filter((e) => e.fiche.etat === 'REMISE').length, 'warn'))
      }
      compteurs.push(compteur('tachesEnRetard', taches.filter((t) => t.sla === 'DEPASSE').length, 'crit'), compteur('echeancesProches', taches.filter((t) => t.sla === 'ECHEANCE_PROCHE').length, 'warn'))
      const liaisons = (f('PARTICIPANT') ? actives.filter((e) => e.s.institutionId === institutionPortail) : f('CONDUCTEUR') && fonctions.length === 1 ? actives.filter((e) => e.conducteur?.id === personne('CONDUCTEUR', 0).id) : actives).map(resume)
      return { fonction: fonctions[0] ?? 'DDRM', compteurs, taches: taches.sort((a, b) => (a.echeance ?? '9').localeCompare(b.echeance ?? '9')), liaisons, blocages: visibles.flatMap((e) => e.blocages.filter((b) => !b.leveLe)) }
    }),
    pilotage: () => attendre((): IndicateursPilotage => {
      const actives = tous().filter((e) => e.s.statut !== 'CLOSE_SANS_SUITE')
      const r = actives.map(resume)
      const demandes = new Map<string, { libelle: string; nombre: number }>()
      actives.forEach((e) => e.s.services.filter((x) => x.inclus).forEach((x) => demandes.set(x.code, { libelle: x.libelle, nombre: (demandes.get(x.code)?.nombre ?? 0) + 1 })))
      const tri = [...demandes.entries()].sort((a, b) => b[1].nombre - a[1].nombre)
      return {
        projetsActifs: r.filter((x) => x.statut !== 'EN_PRODUCTION').length,
        projetsBloques: r.filter((x) => x.bloquee).length,
        tachesEnRetard: r.reduce((n, x) => n + x.tachesEnRetard, 0),
        echeancesProches: actives.flatMap((e) => e.ops.filter((o) => o.sla === 'ECHEANCE_PROCHE')).length,
        repartitionParEtape: ETAPES.filter((x) => x !== 'PRISE_CONTACT' && x !== 'ADHESION').map((etape) => ({ etape, nombre: r.filter((x) => x.etapeCourante === etape).length })),
        delaiMoyenCertificationJours: 46,
        tauxRespectDelais: 78,
        questions: {
          enCertification: r.filter((x) => ['INTEGRATION', 'TESTS', 'CERTIFICATION'].includes(x.etapeCourante)).length,
          servicesPlusDemandes: tri.slice(0, 4).map(([serviceCode, v]) => ({ serviceCode, libelle: v.libelle, nombre: v.nombre })),
          servicesPlusLongs: [{ serviceCode: 'REMITTANCE', libelle: 'Rémittence entrante', delaiMoyenJours: 38 }, { serviceCode: 'PAIEMENT', libelle: 'Paiement marchand', delaiMoyenJours: 31 }, { serviceCode: 'RETRAIT', libelle: 'Retrait', delaiMoyenJours: 22 }],
          delaiMoyenAdhesionProductionJours: 174,
          projetsBloques: actives.flatMap((e) => e.blocages.filter((b) => !b.leveLe).map((b) => ({ liaisonId: e.s.id, reference: e.s.id, institutionNom: e.s.institutionNom, motif: b.motif, depuis: b.depuis, jours: joursEntre(b.depuis, AUJOURDHUI.toISOString()) }))),
          lancementsParService12Mois: tri.map(([serviceCode, v]) => ({ serviceCode, libelle: v.libelle, nombre: v.nombre })),
        },
      }
    }),
    personnes: (fonction) => attendre(PERSONNES.filter((p) => p.fonction === fonction)),
  }

  const decision: ServiceDecision = {
    procesVerbal: (id) => attendre(() => {
      const e = trouver(id)
      const o17 = e.ops.find((o) => o.rang === '17')
      const o18 = e.ops.find((o) => o.rang === '18')
      const actions: string[] = []
      if (e.pv.etat === 'NON_ETABLI' && o17?.etat === 'FAITE' && o18?.etat === 'A_FAIRE') actions.push(e.pv.couverture.taux === 100 ? 'ETABLIR' : 'ETABLIR_BLOQUE')
      if (e.pv.etat === 'ETABLI' || e.pv.etat === 'SIGNE_PARTICIPANT') actions.push('SIGNER_GIMAC')
      return { ...e.pv, actionsPermises: actions }
    }),
    etablirProcesVerbal: (id) => attendre(() => {
      const e = trouver(id)
      if (e.pv.couverture.taux !== 100) throw refus(409, 'CER_COVERAGE_INCOMPLETE', `La couverture de certification est de ${e.pv.couverture.taux ?? 0} % : le procès-verbal exige 100 %.`)
      const o = e.ops.find((x) => x.rang === '18')
      if (!o || o.etat !== 'A_FAIRE') throw refus(409, 'CER_PV_NOT_READY', 'Le constat de la couverture doit précéder le procès-verbal.')
      e.pv = { ...e.pv, etat: 'ETABLI', etabliPar: e.conducteur?.nom ?? null, etabliLe: jour(0), empreinte: hex(`pv-${id}-${Date.now()}`) }
      terminer(e, o, { documentRef: `PV-${id.slice(-3)}`, nom: `proces-verbal-certification-${id}.pdf`, empreinte: e.pv.empreinte ?? '', deposeLe: jour(0), deposePar: e.conducteur?.nom ?? '' })
    }),
    signerProcesVerbalGimac: (id, assertion) => attendre(() => {
      if (!assertion) throw refus(401, 'IAM_REAUTH_REQUIRED', 'Réauthentification exigée.')
      const e = trouver(id)
      if (e.pv.etat !== 'ETABLI' && e.pv.etat !== 'SIGNE_PARTICIPANT') throw refus(409, 'CER_PV_NOT_SIGNABLE', 'Le procès-verbal n\'attend pas la signature du GIMAC.')
      const sig = { signataire: personne('CHEF_DES').nom, fonction: 'CHEF_DES' as Fonction, signeLe: jour(0), horodatage: new Date().toISOString() }
      e.pv = { ...e.pv, signatureGimac: sig, etat: e.pv.signatureParticipant ? 'SIGNE' : 'SIGNE_GIMAC' }
      if (e.pv.etat === 'SIGNE') {
        e.s.statut = 'CERTIFIEE'
        const o = e.ops.find((x) => x.rang === '19')
        if (o) terminer(e, o, { documentRef: `PV-SIG-${id.slice(-3)}`, nom: 'proces-verbal-signe.pdf', empreinte: e.pv.empreinte ?? '', deposeLe: jour(0), deposePar: sig.signataire })
      }
    }),
    approbation: (id) => attendre(() => {
      const e = trouver(id)
      const a = e.approbation
      const actions: string[] = []
      if (e.pv.etat === 'SIGNE' && a.etat === 'EN_ATTENTE_DOSSIER') actions.push('CONSTITUER_DOSSIER')
      if (a.attestations.some((x) => x.preuve && !x.verifieeLe)) actions.push('VERIFIER_ATTESTATION')
      if (a.attestations.every((x) => x.verifieeLe) && a.etat !== 'APPROUVEE' && a.etat !== 'VERIFIE_ARCHIVE') actions.push('ARCHIVER')
      if (a.dossierChangement && !a.ficheChangement) actions.push('REFERENCER_FICHE')
      if (a.ficheChangement) actions.push('RELIRE_FICHE')
      return { ...a, actionsPermises: actions }
    }),
    constituerDossierChangement: (id, fichier) => attendre(() => {
      const e = trouver(id)
      if (e.pv.etat !== 'SIGNE') throw refus(409, 'CER_PV_NOT_SIGNED', 'Le procès-verbal de certification doit être signé des deux parties.')
      e.approbation = { ...e.approbation, etat: 'DOSSIER_CONSTITUE', dossierChangement: { documentRef: `CHG-DOS-${id.slice(-3)}`, nom: fichier.name, empreinte: hex(fichier.name + fichier.size), deposeLe: jour(0), deposePar: personne('PRODUCTION_MONETIQUE').nom } }
      const o = e.ops.find((x) => x.rang === '20')
      if (o && o.etat !== 'FAITE') terminer(e, o, e.approbation.dossierChangement)
    }),
    referencerFicheChangement: (id, reference) => attendre(() => {
      if (!/^[A-Z]{2,5}-\d{4}-\d{1,6}$/.test(reference)) throw refus(422, 'CER_CHANGE_RECORD_INVALID', 'Référence de fiche de changement invalide (exemple : CHG-2026-0412).')
      const e = trouver(id)
      e.approbation = { ...e.approbation, ficheChangement: { reference, etatApprobation: 'EN_EXAMEN', lueLe: jour(0), aRelire: false } }
      const o = e.ops.find((x) => x.rang === '26')
      if (o && o.etat === 'A_FAIRE') terminer(e, o, { documentRef: reference, nom: reference, empreinte: hex(reference), deposeLe: jour(0), deposePar: personne('PRODUCTION_MONETIQUE').nom })
    }),
    relireFicheChangement: (id) => attendre(() => {
      const e = trouver(id)
      const f = e.approbation.ficheChangement
      if (!f) throw refus(409, 'CER_CHANGE_RECORD_MISSING', 'Aucune fiche de changement n\'est référencée.')
      // La fiche est lue, jamais refaite (D-56) : l'approbation vient du dispositif de gestion des changements.
      const approuvee = e.approbation.etat === 'VERIFIE_ARCHIVE'
      e.approbation = { ...e.approbation, ficheChangement: { ...f, lueLe: jour(0), aRelire: false, etatApprobation: approuvee ? 'APPROUVEE' : f.etatApprobation }, etat: approuvee ? 'APPROUVEE' : e.approbation.etat, approuveeLe: approuvee ? jour(0) : e.approbation.approuveeLe }
      if (approuvee) {
        e.s.statut = 'APPROUVEE_SECURITE'
        // L'approbation clôt aussi la fourniture des attestations (rangs 21 et 22), vérifiées et archivées.
        ;['21', '22', '23', '24', '25'].forEach((r) => { const o = e.ops.find((x) => x.rang === r); if (o && o.etat !== 'FAITE') { o.etat = 'A_FAIRE'; terminer(e, o, { documentRef: `SSI-${r}`, nom: `approbation-${r}`, empreinte: hex(`ssi-${id}-${r}`), deposeLe: jour(0), deposePar: personne('RSSI').nom }) } })
      }
    }),
    verifierAttestation: (id, type, conforme, motif) => attendre(() => {
      const e = trouver(id)
      const a = e.approbation.attestations.find((x) => x.type === type)
      if (!a?.preuve) throw refus(409, 'CER_ATTESTATION_MISSING', 'L\'attestation n\'a pas été fournie par le participant.')
      if (!conforme && !motif.trim()) throw refus(422, 'CER_REJECTION_REASON_REQUIRED', 'Un rejet doit être motivé.')
      Object.assign(a, conforme ? { verifieeLe: jour(0), verifieePar: personne('RSSI').nom, motifRejet: null } : { preuve: null, motifRejet: motif })
      e.approbation.etat = 'EN_VERIFICATION'
    }),
    archiverAttestations: (id) => attendre(() => {
      const e = trouver(id)
      if (!e.approbation.attestations.every((a) => a.verifieeLe)) throw refus(409, 'CER_ATTESTATIONS_INCOMPLETE', 'Toutes les attestations exigées doivent être vérifiées avant l\'archivage.')
      e.approbation.attestations.forEach((a) => (a.archiveeLe = jour(0)))
      e.approbation.etat = 'VERIFIE_ARCHIVE'
    }),
    signerVisa: (id, nature, favorable, motif, assertion) => attendre(() => {
      if (!assertion) throw refus(401, 'IAM_REAUTH_REQUIRED', 'Réauthentification exigée.')
      const v = trouver(id).approbation.visas.find((x) => x.nature === nature)
      if (!v?.actif) throw refus(409, 'CER_VISA_NOT_REQUIRED', 'Ce visa n\'est pas requis pour ce type de projet.')
      Object.assign(v, { etat: favorable ? 'SIGNE_FAVORABLE' : 'SIGNE_DEFAVORABLE', motif, signature: { signataire: personne(nature === 'CONFORMITE' ? 'CONFORMITE' : 'DG').nom, fonction: nature === 'CONFORMITE' ? 'CONFORMITE' : 'DG', signeLe: jour(0), horodatage: new Date().toISOString() } })
    }),
    ficheSuivi: (id) => attendre(() => {
      const e = trouver(id)
      return { ...e.fiche, actionsPermises: [...(e.fiche.etat === 'A_REMETTRE' && e.s.statut === 'EN_PRODUCTION' ? ['REMETTRE'] : []), ...(e.fiche.etat === 'RENSEIGNEE' ? ['CONSTATER'] : [])] }
    }),
    remettreFicheSuivi: (id) => attendre(() => {
      trouver(id).fiche.etat = 'REMISE'
    }),
    constaterMiseEnService: (id) => attendre(() => {
      const e = trouver(id)
      if (e.fiche.etat !== 'RENSEIGNEE') throw refus(409, 'HOM_SERVICE_SHEET_INCOMPLETE', 'La fiche de suivi n\'est pas encore renseignée par le participant.')
      e.fiche = { ...e.fiche, etat: 'CONSTATEE', constateeLe: jour(0) }
      const o = e.ops.find((x) => x.rang === '33')
      if (o && o.etat !== 'FAITE') { o.etat = 'A_FAIRE'; terminer(e, o, { documentRef: `CST-${id.slice(-3)}`, nom: 'constat-mise-en-service', empreinte: hex(`cst-${id}`), deposeLe: jour(0), deposePar: personne('SNA').nom }) }
    }),
  }

  const sienne = (id: string) => {
    const e = trouver(id)
    if (e.s.institutionId !== institutionPortail) throw refus(404, 'HOM_LIAISON_NOT_FOUND', 'Ce parcours est introuvable.')
    return e
  }
  const portailParcours: ServicePortailParcours = {
    mesLiaisons: () => attendre(() => tous().filter((e) => e.s.institutionId === institutionPortail).map(resume)),
    dossier: (id) => attendre(() => {
      const d = dossierDe(sienne(id))
      // Projection du portail : aucune donnée interne (responsables internes nommés par leur seule fonction).
      return { ...d, conducteur: d.conducteur ? { ...d.conducteur, nom: 'Conducteur de projet GIMAC' } : null, blocages: d.blocages.map((b) => ({ ...b, responsable: { ...b.responsable, nom: b.responsable.fonction } })), actionsPermises: [] }
    }),
    mesTaches: () => parcours.tableauActeur(['PARTICIPANT']),
    communiquerCoordonnees: (id, c) => attendre(() => {
      const e = sienne(id)
      if (!/^[a-z0-9.-]+$/i.test(c.adresse) || !/^\d{2,5}$/.test(c.port)) throw refus(422, 'HOM_TECHNICAL_DATA_INVALID', 'Adresse ou port invalide.')
      const o = e.ops.find((x) => x.rang === '12')
      if (!o || o.etat !== 'A_FAIRE') throw refus(409, 'HOM_OPERATION_NOT_OPEN', 'Les coordonnées techniques ne sont pas attendues à cette étape.')
      terminer(e, o, { documentRef: `COORD-${id.slice(-3)}`, nom: 'fiche-coordonnees-techniques', empreinte: hex(c.adresse + c.port), deposeLe: jour(0), deposePar: `Correspondant ${e.s.institutionNom}`, valeurReference: { libelle: 'parcours.valeur.12', valeur: `${c.adresse}:${c.port}` } })
    }),
    // Refus rendu en promesse rejetée, comme toute réponse du service (et non en exception immédiate).
    deposerLivrable: (opId, fichier, commentaire) => attendre(() => { sienne(opId.split(':')[0]) }).then(() => parcours.deposerLivrable(opId, fichier, commentaire)),
    procesVerbal: (id) => attendre(() => {
      const e = sienne(id)
      return { ...e.pv, actionsPermises: e.pv.etat === 'ETABLI' || e.pv.etat === 'SIGNE_GIMAC' ? ['SIGNER_PARTICIPANT'] : [] }
    }),
    signerProcesVerbal: (id, assertion) => attendre(() => {
      if (!assertion) throw refus(401, 'IAM_REAUTH_REQUIRED', 'Réauthentification exigée.')
      const e = sienne(id)
      if (e.pv.etat !== 'ETABLI' && e.pv.etat !== 'SIGNE_GIMAC') throw refus(409, 'CER_PV_NOT_SIGNABLE', 'Le procès-verbal n\'attend pas votre signature.')
      const sig = { signataire: `Représentant de ${e.s.institutionNom}`, fonction: 'PARTICIPANT' as Fonction, signeLe: jour(0), horodatage: new Date().toISOString() }
      e.pv = { ...e.pv, signatureParticipant: sig, etat: e.pv.signatureGimac ? 'SIGNE' : 'SIGNE_PARTICIPANT' }
      if (e.pv.etat === 'SIGNE') {
        // Quel que soit l'ordre des signatures, la seconde clôt le rang 19 (pièce pivot, D-56).
        e.s.statut = 'CERTIFIEE'
        const o = e.ops.find((x) => x.rang === '19')
        if (o && o.etat !== 'FAITE') terminer(e, o, { documentRef: `PV-SIG-${id.slice(-3)}`, nom: 'proces-verbal-signe.pdf', empreinte: e.pv.empreinte ?? '', deposeLe: jour(0), deposePar: sig.signataire })
      }
    }),
    approbation: (id) => attendre(() => {
      const a = sienne(id).approbation
      return { ...a, visas: [], actionsPermises: a.attestations.some((x) => !x.preuve) ? ['DEPOSER_ATTESTATION'] : [] }
    }),
    deposerAttestation: (id, type, fichier) => attendre(() => {
      const a = sienne(id).approbation.attestations.find((x) => x.type === type)
      if (!a) throw refus(422, 'CER_ATTESTATION_NOT_REQUIRED', 'Cette attestation n\'est pas exigée pour votre activité.')
      if (!/\.pdf$/i.test(fichier.name)) throw refus(422, 'CER_ATTESTATION_FORMAT', 'L\'attestation doit être déposée au format PDF.')
      Object.assign(a, { preuve: { documentRef: `ATT-${type}-${id.slice(-3)}`, nom: fichier.name, empreinte: hex(fichier.name + fichier.size), deposeLe: jour(0), deposePar: 'Correspondant' }, motifRejet: null })
    }),
    ficheSuivi: (id) => attendre(() => {
      const f = sienne(id).fiche
      return { ...f, actionsPermises: f.etat === 'REMISE' ? ['RENSEIGNER'] : [] }
    }),
    renseignerFicheSuivi: (id, lignes) => attendre(() => {
      const e = sienne(id)
      if (e.fiche.etat !== 'REMISE') throw refus(409, 'HOM_SERVICE_SHEET_NOT_OPEN', 'La fiche de suivi n\'est pas ouverte à la saisie.')
      if (lignes.length !== e.fiche.lignes.length) throw refus(422, 'HOM_SERVICE_SHEET_INCOMPLETE', 'Chaque service du périmètre doit être renseigné.')
      e.fiche = { ...e.fiche, etat: 'RENSEIGNEE', renseigneeLe: jour(0), lignes: e.fiche.lignes.map((l) => ({ ...l, ...lignes.find((x) => x.serviceCode === l.serviceCode) })) }
      const o = e.ops.find((x) => x.rang === '32')
      if (o && o.etat !== 'FAITE') { o.etat = 'A_FAIRE'; terminer(e, o, { documentRef: `FSS-${id.slice(-3)}`, nom: 'fiche-suivi-services', empreinte: hex(`fss-${id}`), deposeLe: jour(0), deposePar: `Correspondant ${e.s.institutionNom}` }) }
    }),
  }

  return { parcours, decision, portailParcours }
}
