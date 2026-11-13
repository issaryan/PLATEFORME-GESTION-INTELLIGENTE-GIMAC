import { ApiError } from '@gimac/socle'
import type { CoverageValue, TypeProjet } from '../../modele/commun'
import {
  ETAPES, type ApprobationSecurite, type Blocage, type DossierLiaison, type EtapeCircuit, type EtapeCode, type EtapeParcours, type EtapeVersion, type EtatOperationParcours, type FicheSuiviServices, type Fonction,
  type FonctionUtilisateur, type IndicateursPilotage, type IntervenantLiaison, type LiaisonResume, type LivrableAttendu, type OperationParcours, type Personne, type ProcesVerbalCertification,
  type Qualite, type ServicePerimetre, type Signature, type SignatureOperation, type Sla, type StatutParcours, type TableauActeur, type TacheActeur, type TypeAttestation,
} from '../../modele/parcours'
import type { FonctionCatalogue, Geste, Jalon, ReferenceOperation } from '../../modele/homologation'
import type { MesOperations, MonOperation } from '../../modele/parcours'
import { referentielDe, type OperationReferentiel } from '../../modele/referentielCR'
import { CATALOGUE, ETAPES_CANONIQUES, actionDuGeste, codesDeclenches, estDedie, gesteDe, jetonEtape, versionCR, vueDe } from './referentiels'
import { LIBELLES_CR } from './libellesCR'
import { empreinte } from './donnees'
import { chronologieDemo, livrablesAttendusDemo, type DepotsDemo } from './preuves'
import { executionsSandboxDemo, repartitionSandboxDemo, resultatsSandboxDemo } from './sandbox'
import { preuvesDe } from '../../modele/preuvesCR'
import { aujourdhui as aujourdhuiDouala } from '../../modele/jours'
import type { ServiceDecision, ServiceParcours, ServicePortailParcours } from '../ports'

// Serveur de substitution du parcours (étapes 3 à 10, D-56 à D-59) : il tient l'état en mémoire et
// calcule ce que calculeront les services (étape courante, couvertures, SLA, compteurs par fonction,
// actions permises). Données fictives : aucune institution, personne ni infrastructure réelle.

type Attendre = <T>(v: T | (() => T)) => Promise<T>

let seq = 0
const correlation = () => `sim-par-${Date.now().toString(36)}-${(++seq).toString(36)}`
const refus = (status: number, code: string, message: string) => new ApiError(status, code, message, correlation())
/**
 * DDR-15 : jour de référence de la démonstration = jour courant de Douala (même horloge que l'en-tête), jamais une date figée ;
 * toutes les dates du jeu (démarrage, échéances, clôtures, dépôts, blocages) s'en déduisent.
 */
const aujourdhuiDemo = () => new Date(`${aujourdhuiDouala()}T09:00:00Z`)
const jour = (decalage: number) => {
  const d = aujourdhuiDemo()
  d.setUTCDate(d.getUTCDate() + decalage)
  return d.toISOString().slice(0, 10)
}
const joursEntre = (a: string, b: string) => Math.round((Date.parse(b) - Date.parse(a)) / 86_400_000)
/** Horodatage sur l'horloge de la démonstration (jour simulé, heure courante) : une seule horloge pour toutes les pièces. */
const horodatage = () => `${jour(0)}T${new Date().toISOString().slice(11)}`
/** Empreinte de démonstration (graine entièrement mêlée), commune aux pièces simulées. */
const hex = empreinte

/**
 * Intervenants de démonstration, désignés par leur fonction (CR §1.5). Recette HOM-14 : nommés comme les comptes de
 * démonstration qui tiennent ces fonctions (annuaire simulé, ORGANISATION_DEMO), pour que l'acteur se reconnaisse.
 */
const P = (id: string, nom: string, fonction: Fonction): Personne => ({ id, nom, fonction })
export const PERSONNES: Personne[] = [
  P('U-DDRM-1', 'Fatimé Abakar Hissein', 'DDRM'), P('U-DDRM-2', 'Rodrigue Essono Mba', 'DDRM'), P('U-CDDRM', 'Bertrand Nguema Obiang', 'CHEF_DDRM'),
  P('U-CDES', 'Paul Mbarga Atangana', 'CHEF_DES'), P('U-COND-1', 'Aïcha Mahamat Nour', 'CONDUCTEUR'), P('U-COND-2', 'Brice Ondo Mba', 'CONDUCTEUR'),
  P('U-SNA-1', 'Rachel Biyong Mengue', 'SNA'), P('U-SNA-2', 'Joël Ekotto Mballa', 'SNA'),
  P('U-PM-1', 'Serge Ondoua Essomba', 'PRODUCTION_MONETIQUE'), P('U-PM-2', 'Lucien Ateba Owona', 'PRODUCTION_MONETIQUE'),
  P('U-RES-1', 'Idriss Mahamat Saleh', 'POLE_RESEAU'), P('U-SYS-1', 'Chantal Ngo Bassong', 'POLE_SYSTEME'), P('U-RSSI', 'RSSI', 'RSSI'),
  P('U-CONF', 'Responsable conformité', 'CONFORMITE'), P('U-DG', 'Directeur général', 'DG'), P('U-EXP', 'Exploitation et supervision', 'EXPLOITATION'),
]
/** Étape d'un circuit de signature du procès-verbal (EtapeSignature de MS-CERTIFICATION, D-94). */
type EtapePv = { code: string; libelle: string; fonction: Fonction; ordre: number; portail: boolean }
/** Circuit par défaut du procès-verbal (gimac.certification.circuits.proces-verbal de MS-CERTIFICATION, §10.3). */
const CIRCUIT_PV_DEFAUT: EtapePv[] = [
  { code: 'GIMAC', libelle: 'Signature du GIMAC (chef du DES)', fonction: 'CHEF_DES', ordre: 1, portail: false },
  { code: 'PARTICIPANT', libelle: 'Signature du participant', fonction: 'PARTICIPANT', ordre: 1, portail: true },
]
const PV_SIGNE = 'certification.pv-signed.v1'
const ouverteOp = (o: OperationParcours) => o.etat === 'A_FAIRE' || o.etat === 'EN_COURS'
/** État du procès-verbal établi selon les signatures du circuit (ProcesVerbalPolicy.etat). */
const etatPvDe = (c: EtapePv[], s: Record<string, unknown>): ProcesVerbalCertification['etat'] => {
  const des = c.filter((x) => !x.portail).every((x) => s[x.code])
  const portail = c.filter((x) => x.portail).every((x) => s[x.code])
  if (des && portail) return 'SIGNE'
  if (des && c.some((x) => !x.portail)) return 'SIGNE_GIMAC'
  if (portail && c.some((x) => x.portail)) return 'SIGNE_PARTICIPANT'
  return 'ETABLI'
}
/** Étapes que ce côté peut signer maintenant : non signées, du plus petit ordre restant (ProcesVerbalPolicy.signables). */
const signablesPv = (c: EtapePv[], s: Record<string, unknown>, portail: boolean) => {
  const prochain = Math.min(...c.filter((x) => !s[x.code]).map((x) => x.ordre))
  return c.filter((x) => x.portail === portail && !s[x.code] && x.ordre === prochain)
}
/** Refus faute d'étape signable : CER_SIGNATURE_ORDRE si ce côté attend encore après d'autres, sinon CER_PV_NOT_SIGNABLE. */
const nonSignablePv = (c: EtapePv[], s: Record<string, unknown>, portail: boolean) => {
  if (c.some((x) => x.portail === portail && !s[x.code])) {
    const prochain = Math.min(...c.filter((x) => !s[x.code]).map((x) => x.ordre))
    return new ApiError(409, 'CER_SIGNATURE_ORDRE', 'D\'autres signatures du procès-verbal doivent précéder la vôtre.', correlation(), { attendues: c.filter((x) => !s[x.code] && x.ordre === prochain).map((x) => x.code) })
  }
  return refus(409, 'CER_PV_NOT_SIGNABLE', portail ? 'Le procès-verbal n\'attend pas votre signature.' : 'Le procès-verbal n\'attend pas la signature du GIMAC.')
}

/** Fonction à affecter (D-93) : identifiant A_AFFECTER:<FONCTION>, tenue par personne tant que le conducteur ne l'a pas affectée. */
const aAffecter = (f: Fonction): Personne => ({ id: `A_AFFECTER:${f}`, nom: 'À affecter', fonction: f, aAffecter: true })
const personne = (f: Fonction, i = 0): Personne => {
  // DDR-10 : la fonction partagée des deux chefs reste CHEFS_DEPARTEMENT (jamais CHEF_DES, réservée au chef du DES).
  if (f === 'CHEFS_DEPARTEMENT') return { ...personne('CHEF_DES'), fonction: 'CHEFS_DEPARTEMENT' }
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
  /** D-104 : intitulé libre du projet et référence d'origine (reprise), facultatifs. */
  intituleProjet?: string | null
  referenceOrigine?: string | null
  /** État forcé du procès-verbal (démonstration des signatures ; A_ETABLIR : couverture complète constatée, procès-verbal à établir). */
  pv?: 'ETABLI' | 'SIGNE_GIMAC' | 'A_ETABLIR'
  /** Visas optionnels activés pour cette liaison (D-56 : désactivés par défaut). */
  visas?: boolean
  /** Attestations déjà fournies par le participant, en attente de vérification par la sécurité (démonstration). */
  attestationsFournies?: boolean
  /** État forcé de la fiche de suivi d'une liaison en production (démonstration de la remise et du constat). */
  fiche?: 'A_REMETTRE' | 'RENSEIGNEE'
  /** D-93 : fonctions encore « à affecter » sur la liaison (aucune attribution arbitraire). */
  aAffecter?: Fonction[]
  /** D-91 : opération faite en attente de ses signatures, dans l'ordre (démonstration de « Signer »). */
  enSignature?: { rang: string; signatures: Pick<SignatureOperation, 'code' | 'libelle' | 'signataire' | 'ordre' | 'porte'>[] }
  /**
   * D-98 : liaison instanciée depuis la version publiée d'un type créé dans la plateforme (modèle générique) : opérations,
   * gestes, jalons et déclencheurs lus sur la version ; aucune attestation imposée.
   */
  depuisVersion?: boolean
  /** DDR-24 : numéro de la version du référentiel instanciée (version publiée à l'ouverture). */
  versionNumero?: number
  /** DDR-15 : dates des étapes 1 et 2 tenues par MS-PARTICIPANT (prise de contact, adhésion prononcée). */
  amont?: { contact: string | null; adhesion: string | null }
  /**
   * Signatures configurées (D-91) de l'opération qui porte la signature du procès-verbal : circuit du procès-verbal de
   * MS-CERTIFICATION (D-94), complété de la signature du participant au portail.
   */
  circuitPv?: Pick<SignatureOperation, 'code' | 'libelle' | 'signataire' | 'ordre' | 'porte'>[]
}
const SERVICES_CARTE = [{ code: 'RETRAIT', libelle: 'Retrait' }, { code: 'PAIEMENT', libelle: 'Paiement marchand' }, { code: 'TRANSFERT', libelle: 'Transfert' }, { code: 'COMMERCE_ELECTRONIQUE', libelle: 'Commerce électronique' }]
const SERVICES_MOBILE = [{ code: 'PAIEMENT_MARCHAND', libelle: 'Paiement marchand mobile' }, { code: 'REMITTANCE', libelle: 'Rémittence entrante' }, { code: 'FACTURE', libelle: 'Paiement de facture' }, { code: 'CASH_IN', libelle: 'Cash-in agent' }]
const perimetre = (base: typeof SERVICES_CARTE, exclus: string[]) => base.map((s) => ({ ...s, inclus: !exclus.includes(s.code) }))

const SCENARIOS: Scenario[] = [
  { id: 'LIA-2026-041', institutionId: 'INST-0105', institutionNom: 'Fintech F', typeProjet: 'PAIEMENT_MOBILE', banque: 'Banque B', personnalisateur: null, fintech: true, avancement: '6', statut: 'CADREE', conducteur: 0, debut: -12, cible: 110, services: perimetre(SERVICES_MOBILE, ['CASH_IN']), aAffecter: ['POLE_RESEAU'] },
  { id: 'LIA-2026-036', institutionId: 'INST-0088', institutionNom: 'Établissement de microfinance C', typeProjet: 'PAIEMENT_MOBILE', banque: null, personnalisateur: null, fintech: false, avancement: '9', statut: 'CADREE', conducteur: 1, debut: -35, cible: 70, services: perimetre(SERVICES_MOBILE, []),
    enSignature: { rang: '11', signatures: [
      { code: 'VISA_NORMES', libelle: 'Visa des normes sur le compte rendu', signataire: 'SNA', ordre: 1, porte: ['L1'] },
      { code: 'VISA_CONDUITE', libelle: 'Visa du conducteur de projet', signataire: 'CONDUCTEUR', ordre: 2, porte: ['L1'] },
    ] } },
  { id: 'LIA-2026-030', institutionId: 'INST-0042', institutionNom: 'Établissement de paiement A', typeProjet: 'CARTE_INTERBANCAIRE', banque: 'Banque B', personnalisateur: null, fintech: false, avancement: '13', statut: 'EN_INTEGRATION', conducteur: 0, debut: -58, cible: 32, services: perimetre(SERVICES_CARTE, ['COMMERCE_ELECTRONIQUE']) },
  { id: 'LIA-2026-022', institutionId: 'INST-0017', institutionNom: 'Banque B', typeProjet: 'CARTE_INTERBANCAIRE', banque: null, personnalisateur: null, fintech: false, avancement: '16', statut: 'EN_TEST', conducteur: 1, debut: -80, cible: 12, services: perimetre(SERVICES_CARTE, []) },
  { id: 'LIA-2026-019', institutionId: 'INST-0120', institutionNom: 'Banque G', typeProjet: 'DELEGATAIRE', banque: 'Banque D', personnalisateur: 'Personnalisateur P', fintech: false, avancement: '20', statut: 'CERTIFIEE', conducteur: 0, debut: -96, cible: 20, services: perimetre(SERVICES_CARTE, ['TRANSFERT']), visas: true, attestationsFournies: true },
  { id: 'LIA-2026-012', institutionId: 'INST-0061', institutionNom: 'Banque D', typeProjet: 'CARTE_INTERBANCAIRE', banque: null, personnalisateur: null, fintech: false, avancement: '26', statut: 'APPROUVEE_SECURITE', conducteur: 1, debut: -120, cible: -2, services: perimetre(SERVICES_CARTE, ['COMMERCE_ELECTRONIQUE']) },
  { id: 'LIA-2025-088', institutionId: 'INST-0042', institutionNom: 'Établissement de paiement A', typeProjet: 'PAIEMENT_MOBILE', banque: null, personnalisateur: null, fintech: false, avancement: '31', statut: 'EN_PRODUCTION', conducteur: 0, debut: -210, cible: -30, services: perimetre(SERVICES_MOBILE, ['CASH_IN']) },
  { id: 'LIA-2026-046', institutionId: 'INST-0042', institutionNom: 'Établissement de paiement A', typeProjet: 'PAIEMENT_MOBILE', banque: null, personnalisateur: null, fintech: false, avancement: '11', statut: 'EN_INTEGRATION', conducteur: 1, debut: -36, cible: 48, services: [{ code: 'CASH_IN', libelle: 'Cash-in agent', inclus: true }], origine: 'LIA-2025-088' },
  { id: 'LIA-2026-027', institutionId: 'INST-0042', institutionNom: 'Établissement de paiement A', typeProjet: 'DELEGATAIRE', banque: 'Banque B', personnalisateur: 'Personnalisateur P', fintech: false, avancement: '18', statut: 'EN_TEST', conducteur: 0, debut: -74, cible: 25, services: perimetre(SERVICES_CARTE, ['COMMERCE_ELECTRONIQUE']), pv: 'ETABLI' },
  { id: 'LIA-2026-005', institutionId: 'INST-0073', institutionNom: 'Banque E', typeProjet: 'CARTE_INTERBANCAIRE', banque: null, personnalisateur: null, fintech: false, avancement: '27', statut: 'APPROUVEE_SECURITE', conducteur: 1, debut: -140, cible: 4, services: perimetre(SERVICES_CARTE, ['TRANSFERT', 'COMMERCE_ELECTRONIQUE']) },
  { id: 'LIA-2026-025', institutionId: 'INST-0151', institutionNom: 'Banque K', typeProjet: 'CARTE_INTERBANCAIRE', banque: null, personnalisateur: null, fintech: false, avancement: '17', statut: 'EN_TEST', conducteur: 0, debut: -70, cible: 28, services: perimetre(SERVICES_CARTE, ['TRANSFERT']), pv: 'A_ETABLIR',
    // Circuit configuré sur l'opération de la signature (D-94) : le participant signe d'abord (ordre 1), le GIMAC ensuite (ordre 2).
    circuitPv: [{ code: 'GIMAC', libelle: 'Signature du GIMAC (chef du DES)', signataire: 'CHEF_DES', ordre: 2, porte: ['L1'] }] },
  { id: 'LIA-2025-071', institutionId: 'INST-0144', institutionNom: 'Banque J', typeProjet: 'CARTE_INTERBANCAIRE', banque: null, personnalisateur: null, fintech: false, avancement: '31', statut: 'EN_PRODUCTION', conducteur: 1, debut: -190, cible: -12, services: perimetre(SERVICES_CARTE, ['TRANSFERT']), fiche: 'A_REMETTRE' },
  { id: 'LIA-2025-064', institutionId: 'INST-0133', institutionNom: 'Établissement de paiement H', typeProjet: 'PAIEMENT_MOBILE', banque: 'Banque D', personnalisateur: null, fintech: false, avancement: '32', statut: 'EN_PRODUCTION', conducteur: 0, debut: -230, cible: -45, services: perimetre(SERVICES_MOBILE, ['FACTURE']), fiche: 'RENSEIGNEE' },
  { id: 'LIA-2026-044', institutionId: 'INST-0017', institutionNom: 'Banque B', typeProjet: 'CARTE_INTERBANCAIRE', banque: null, personnalisateur: null, fintech: false, avancement: '3', statut: 'CADREE', conducteur: null, debut: -3, cible: null, services: [{ code: 'COMMERCE_ELECTRONIQUE', libelle: 'Commerce électronique', inclus: true }], origine: 'LIA-2026-022' },
  // Essais d'un projet mobile sur le simulateur (rang 16 : taux repris, non recalculé) ; lien d'essai à contrôler par le pôle réseau (rang 13).
  { id: 'LIA-2026-038', institutionId: 'INST-0131', institutionNom: 'Émetteur de monnaie électronique H', typeProjet: 'PAIEMENT_MOBILE', banque: 'Banque B', personnalisateur: null, fintech: false, avancement: '15', statut: 'EN_TEST', conducteur: 0, debut: -62, cible: 38, services: perimetre(SERVICES_MOBILE, ['FACTURE']) },
  { id: 'LIA-2026-040', institutionId: 'INST-0144', institutionNom: 'Banque H', typeProjet: 'CARTE_INTERBANCAIRE', banque: null, personnalisateur: null, fintech: false, avancement: '12', statut: 'EN_INTEGRATION', conducteur: 1, debut: -50, cible: 45, services: perimetre(SERVICES_CARTE, []) },
]

/**
 * Projet de certification d'un dossier simulé, pour le périmètre (D-133) : type, services du périmètre inclus et état
 * ouvert ; absent si le dossier n'existe pas.
 */
export function projetDuDossier(id: string): { id: string; typeProjet: TypeProjet; etat: 'OUVERTE'; perimetre: string[] } | undefined {
  const s = SCENARIOS.find((x) => x.id === id)
  return s ? { id: s.id, typeProjet: s.typeProjet, etat: 'OUVERTE', perimetre: ['TRANSVERSE', ...s.services.filter((x) => x.inclus).map((x) => x.code)] } : undefined
}

/** Opération ouverte ou renvoyée pendant la démonstration (homologation.operation-activated.v1, DDR-08). */
export type EvenementOperationDemo = { type: 'OUVERTE' | 'RENVOYEE' | 'VALIDATION'; operationId: string; motif?: string; le: string }
type Etat = {
  s: Scenario
  ops: OperationParcours[]
  /** DDR-08 : opérations ouvertes ou renvoyées pendant la démonstration, notifiées à leur responsable. */
  journal?: EvenementOperationDemo[]
  blocages: Blocage[]
  pv: ProcesVerbalCertification
  approbation: ApprobationSecurite
  fiche: FicheSuiviServices
  conducteur: Personne | null
  /** Circuit du procès-verbal figé à la première signature (D-94). */
  pvCircuit?: EtapePv[]
  /** Signatures du procès-verbal données, par code d'étape du circuit. */
  pvSignees?: Record<string, ProcesVerbalCertification['signatureGimac'] & object>
}

/** Opérations de la liaison closes par un sujet d'événement : déclencheur lu sur le référentiel simulé (D-77, §3). */
/** Déclencheur de l'opération (D-77 §3), lu sur la version simulée du référentiel. */
const codesDeclenchesParRang = (type: string, rang: string) => ['certification.pv-established.v1', 'certification.pv-signed.v1', 'certification.change-record-constituted.v1',
  'certification.attestations-archived.v1', 'certification.security-approved.v1', 'certification.change-record-referenced.v1'].find((d) => codesDeclenches(type, d).includes(rang)) ?? null
// D-98 : déclencheur lu sur l'opération instanciée (version publiée du type), jamais sur un rang.
const declenchees = (e: Etat, sujet: string) => e.ops.filter((o) => o.declencheur === sujet)
const declenchee = (e: Etat, sujet: string) => declenchees(e, sujet)[0]

/** Actions d'une opération ouverte : son geste, et l'attribution des qualités tant qu'elle n'est pas faite. */
/**
 * Rangs dont le geste a sa route propre (affectation des ressources, verrous de la décision tenus par
 * MS-CERTIFICATION, fiche de suivi et constat) : jamais clos par une pièce libre (D-56), comme MS-HOMOLOGATION.
 */
export const GESTES_DEDIES = (['CARTE_INTERBANCAIRE', 'PAIEMENT_MOBILE', 'DELEGATAIRE'] as TypeProjet[])
  .flatMap((t) => versionCR(t)).filter((o) => estDedie(o.geste)).map((o) => o.code).filter((c, i, l) => l.indexOf(c) === i)
// D-98 : geste lu sur l'opération (version publiée), celui des rangs de la version 1 à défaut.
const gesteOuvert = (o: { geste?: Geste | null; grain: string; rang: string }) => o.geste ?? gesteDe({ rang: o.rang, grain: o.grain as OperationReferentiel['grain'] })
const actionsOuvertes = (o: { geste?: Geste | null; grain: string; rang: string }) => (estDedie(gesteOuvert(o)) ? ['ATTRIBUER'] : [actionDuGeste(gesteOuvert(o)), 'ATTRIBUER'])
const refuserGesteLibre = (o: { geste?: Geste | null; grain: string; rang: string }) => {
  if (estDedie(gesteOuvert(o))) throw refus(409, 'HOM_OPERATION_DEDICATED_GESTURE', 'Cette opération se réalise par son geste propre (affectation, verrous de la décision ou fiche de suivi), jamais par une pièce libre.')
}
/** Onglet du dossier où se réalise le geste d'une opération : décision (verrous 1 et 2), mise en service (étape 10), sinon opérations. */
// D-93 : l'onglet suit la vue de l'opération (attribut servi), jamais son rang.
export const ongletDeLaVue = (vue: OperationParcours['vue']) => (vue === 'DECISION' || vue === 'ATTESTATIONS' ? 'decision' : vue === 'MISE_EN_SERVICE' ? 'mise-en-service' : 'operations')
const lienOperation = (o: Pick<OperationParcours, 'liaisonId' | 'id' | 'vue'>) => `/des/parcours/${o.liaisonId}?onglet=${ongletDeLaVue(o.vue)}&operation=${encodeURIComponent(o.id)}`

const couv = (n: number, d: number): CoverageValue => ({ taux: d ? Math.round((n / d) * 100) : null, numerateur: n, denominateur: d, calculable: d > 0, motifNonCalculable: d ? undefined : 'aucun service inclus' })

/** Jalon porté par une opération de la version 1 simulée d'un type repris (D-77 §3). */
const jalonsParType = new Map<string, Map<string, Jalon | null>>()
const jalonDuRang = (type: string, rang: string): Jalon | null => {
  if (!jalonsParType.has(type)) jalonsParType.set(type, new Map(versionCR(type).map((o) => [o.code, o.jalon])))
  return jalonsParType.get(type)?.get(rang) ?? null
}
/** Statut de la liaison qui résulte d'un jalon atteint (homologation.liaison-milestone-reached.v1). */
const STATUT_DU_JALON: Record<Jalon, StatutParcours | null> = { APTE_CERTIFICATION: null, CERTIFIEE: 'CERTIFIEE', APPROUVEE_SECURITE: 'APPROUVEE_SECURITE', EN_PRODUCTION: 'EN_PRODUCTION' }

/**
 * D-98 : opérations d'une liaison instanciée depuis la version publiée de son type (D-91), comme MS-HOMOLOGATION : RACI
 * des fonctions de la version (conducteur désigné, correspondant du participant, fonction à affecter sinon tenue par
 * son premier titulaire de démonstration), geste, jalon, déclencheur et vue de chaque opération.
 */
function operationsDepuisVersion(s: Scenario, version: ReferenceOperation[], conducteur: Personne | null): OperationParcours[] {
  const correspondant = P(`EXT-${s.institutionId}`, `Correspondant ${s.institutionNom}`, 'PARTICIPANT')
  const tenue = (f: Fonction | null): Personne | null => (!f ? null : f === 'PARTICIPANT' ? correspondant : f === 'CONDUCTEUR' ? conducteur : s.aAffecter?.includes(f) ? aAffecter(f) : personne(f))
  return [...version].sort((a, b) => (a.ordre ?? 0) - (b.ordre ?? 0)).map((o, i): OperationParcours => {
    const courant = o.predecesseurs.length === 0
    const geste = o.geste ?? 'DEPOSER_PREUVE'
    const echeance = jour(s.debut + (i + 1) * (o.delaiJours ?? 3))
    return {
      id: `${s.id}:${o.code}`, liaisonId: s.id, rang: o.code, etape: (o.etape ?? 'CADRAGE') as EtapeCode, libelle: o.libelle, description: o.description, etapeCode: o.etape,
      vue: o.vue ?? 'AUCUNE', geste, declencheur: o.declencheur, jalon: o.jalon, particularite: o.particularite, renvois: [],
      signatures: (o.signatures ?? []).filter((x) => x.signataire).map((x) => ({ ...x, signataire: x.signataire as Fonction, libelleSignataire: CATALOGUE.fonctions.find((f) => f.code === x.signataire)?.libelle ?? x.signataire, etat: 'ATTENDUE' as const, suivante: false })),
      segment: o.segment ?? 'AMONT', plateforme: o.plateforme, grain: o.grain ?? 'OPERATION', etat: courant ? 'A_FAIRE' : 'VERROUILLEE',
      raci: { R: tenue(o.responsable), V: tenue(o.validateur), C: o.consultes.map((f) => tenue(f)!).filter(Boolean), I: o.informes.map((f) => tenue(f)!).filter(Boolean), E: o.externes.map((f) => tenue(f)!).filter(Boolean) },
      echeance, sla: courant ? 'DANS_LES_DELAIS' : null, faiteLe: null, preuve: null, cle: o.cle, secret: o.secret, valeurReference: !!o.valeurReference,
      oubliSansSymptome: o.oubliSansSymptome, pivot: o.pivot, predecesseursNonFaits: [...o.predecesseurs], transitionsPermises: [],
      actionsPermises: courant ? actionsOuvertes({ geste, grain: o.grain ?? 'OPERATION', rang: o.code }) : ['ATTRIBUER'],
    }
  })
}

function construire(s: Scenario, version?: ReferenceOperation[] | null): Etat {
  const ref = referentielDe(s.typeProjet).filter((o) => Number(o.rang) >= 3 || isNaN(Number(o.rang)))
  const idxAvance = ref.findIndex((o) => o.rang === s.avancement)
  const conducteur = s.conducteur === null ? null : personne('CONDUCTEUR', s.conducteur)
  const ops: OperationParcours[] = version ? operationsDepuisVersion(s, version, conducteur) : ref.map((o: OperationReferentiel, i) => {
    const fait = i <= idxAvance
    const enAttente = o.predecesseurs.filter((p) => ref.findIndex((x) => x.rang === p) > idxAvance)
    const courant = !fait && enAttente.length === 0
    const echeance = jour(s.debut + 4 + i * 3)
    const etat: EtatOperationParcours = fait ? 'FAITE' : courant ? 'A_FAIRE' : 'VERROUILLEE'
    // D-93 : une fonction encore à affecter n'est pas attribuée au premier titulaire de l'annuaire.
    const tenue = (f: Fonction) => (s.aAffecter?.includes(f) ? aAffecter(f) : personne(f))
    const R = o.porteurs[0] === 'PARTICIPANT' ? P(`EXT-${s.institutionId}`, `Correspondant ${s.institutionNom}`, 'PARTICIPANT') : o.porteurs[0] === 'CONDUCTEUR' ? conducteur : o.porteurs[0] === 'PERSONNALISATEUR' ? P('EXT-PERSO', s.personnalisateur ?? 'Personnalisateur', 'PERSONNALISATEUR') : tenue(o.porteurs[0])
    const autres = o.porteurs.slice(1).map((f) => (f === 'PARTICIPANT' ? P(`EXT-${s.institutionId}`, `Correspondant ${s.institutionNom}`, 'PARTICIPANT') : tenue(f)))
    const sla: Sla | null = fait ? null : courant ? (Date.parse(echeance) < aujourdhuiDemo().getTime() ? 'DEPASSE' : joursEntre(aujourdhuiDemo().toISOString(), echeance) <= 5 ? 'ECHEANCE_PROCHE' : 'DANS_LES_DELAIS') : null
    const geste = gesteDe(o)
    return {
      id: `${s.id}:${o.rang}`,
      liaisonId: s.id,
      rang: o.rang,
      etape: o.etape,
      // D-93 : libellé, étape de la version, vue spécialisée, geste et particularité lus sur l'opération du référentiel publié.
      libelle: LIBELLES_CR[o.rang]?.[0] ?? o.rang,
      description: LIBELLES_CR[o.rang]?.[1] || null,
      etapeCode: o.etape,
      vue: vueDe(o.rang),
      geste,
      declencheur: codesDeclenchesParRang(s.typeProjet, o.rang),
      jalon: jalonDuRang(s.typeProjet, o.rang),
      particularite: o.rang === '15' ? 'ENTREE_ESSAIS' : o.rang === '16' ? 'FIN_ESSAIS' : o.rang === 'C1' ? 'CEREMONIE_CLES' : o.rang === '35' ? 'SERVICE_SUPPLEMENTAIRE' : null,
      renvois: [],
      signatures: [],
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
            ceremonie: o.rang === 'C1' ? { tenueLe: jour(s.debut + 70), porteurs: [personne('RSSI').nom, 'Dépositaire de composante 2', 'Dépositaire de composante 3'], codesControle: [1, 2, 3].map((k) => hex(`c1-${s.id}-${k}`).slice(0, 6).toUpperCase()) } : undefined,
          }
        : null,
      cle: Boolean(o.cle),
      secret: Boolean(o.secret),
      valeurReference: Boolean(o.valeurReference),
      oubliSansSymptome: Boolean(o.oubliSansSymptome),
      pivot: Boolean(o.pivot),
      predecesseursNonFaits: fait ? [] : enAttente,
      transitionsPermises: [],
      actionsPermises: courant ? actionsOuvertes(o) : fait ? ['CONSULTER'] : ['ATTRIBUER'],
    }
  })
  // HOM-05 : signatures configurées au référentiel (D-91) recopiées sur les opérations de la liaison, comme à l'instanciation
  // de MS-HOMOLOGATION : l'opération ne se clôt qu'après elles ; celles d'une opération déjà faite sont données.
  if (!version) {
    const configurees = new Map(versionCR(s.typeProjet).map((r) => [r.code, r.signatures ?? []]))
    ops.forEach((o) => {
      const l = (configurees.get(o.rang) ?? []).filter((x) => x.signataire)
      if (!l.length) return
      const faite = o.etat === 'FAITE'
      o.signatures = l.map((x): SignatureOperation => {
        const qui = x.signataire === 'CONDUCTEUR' && conducteur ? conducteur : personne(x.signataire as Fonction)
        return {
          ...x, signataire: x.signataire as Fonction, libelleSignataire: CATALOGUE.fonctions.find((f) => f.code === x.signataire)?.libelle ?? x.signataire!, etat: faite ? 'SIGNEE' : 'ATTENDUE', suivante: false,
          ...(faite ? { signePar: qui.id, signeParNom: qui.nom, signeLe: `${o.faiteLe}T10:00:00Z`, empreinte: hex(`${o.id}-${x.code}`) } : {}),
        }
      })
    })
  }
  const inclus = s.services.filter((x) => x.inclus)
  const certifies = ['CERTIFIEE', 'APPROUVEE_SECURITE', 'EN_PRODUCTION'].includes(s.statut) || s.pv ? inclus.length : s.statut === 'EN_TEST' ? inclus.length - 1 : 0
  const pvEtat = s.pv && s.pv !== 'A_ETABLIR' ? s.pv : (['APPROUVEE_SECURITE', 'EN_PRODUCTION', 'CERTIFIEE'].includes(s.statut) ? 'SIGNE' : 'NON_ETABLI')
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
    signatureParticipant: pvEtat === 'SIGNE' ? signe('PARTICIPANT', `Correspondant ${s.institutionNom}`, s.debut + 62) : null,
    transitionsPermises: [],
    actionsPermises: [],
  }
  // Modèle GENERIQUE (type créé dans la plateforme, D-91) : aucune attestation imposée.
  const typesAttestation: TypeAttestation[] = s.depuisVersion ? [] : s.typeProjet === 'PAIEMENT_MOBILE' ? ['ISO_27001', 'GSMA'] : ['PCI_DSS']
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
      preuve: approuve || (s.statut === 'CERTIFIEE' && (type !== 'PCI_DSS' || s.attestationsFournies)) ? { documentRef: `ATT-${type}-${s.id.slice(-3)}`, nom: `${type.toLowerCase()}-${s.institutionId}.pdf`, empreinte: hex(`att-${type}-${s.id}`), deposeLe: jour(s.debut + 65), deposePar: `Correspondant ${s.institutionNom}` } : null,
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
    etat: enProd ? (s.fiche ?? 'REMISE') : 'A_REMETTRE',
    lignes: inclus.map((x, i) => ({
      serviceCode: x.code, libelle: x.libelle,
      ouvert: s.fiche === 'RENSEIGNEE' ? i < inclus.length - 1 : enProd && s.fiche !== 'A_REMETTRE' && i === 0 ? true : null,
      commentaire: s.fiche === 'RENSEIGNEE' && i === inclus.length - 1 ? 'Ouverture reportée à la prochaine fenêtre de maintenance' : null,
    })),
    renseigneeLe: s.fiche === 'RENSEIGNEE' ? jour(-2) : null,
    constateeLe: null,
    transitionsPermises: [],
    actionsPermises: [],
  }
  const blocages: Blocage[] =
    s.id === 'LIA-2026-030'
      ? [{ id: 'BLQ-0007', liaisonId: s.id, motif: 'Élément de sécurité du participant à renouveler avant la reprise des essais', depuis: jour(-9), prochaineAction: 'Obtenir le nouvel élément auprès du participant', responsable: personne('POLE_RESEAU'), echeanceLevee: jour(-2), leveLe: null, etape: 'INTEGRATION', transitionsPermises: [], actionsPermises: ['LEVER'] }]
      : []
  if (blocages.length) ops.filter((o) => o.etat === 'A_FAIRE').forEach((o) => { o.etat = 'BLOQUEE'; o.actionsPermises = ['ATTRIBUER'] })
  // D-91 : opération dont le geste a abouti, en attente de ses signatures dans l'ordre.
  const sig = s.enSignature ? ops.find((o) => o.rang === s.enSignature!.rang) : undefined
  if (sig && s.enSignature) {
    sig.etat = 'EN_SIGNATURE'
    sig.sla = null
    sig.predecesseursNonFaits = []
    sig.preuve = { documentRef: `DOC-${s.id.slice(-3)}-${sig.rang}`, nom: `compte-rendu-${sig.rang}.pdf`, empreinte: hex(`${s.id}-${sig.rang}`), deposeLe: jour(-1), deposePar: sig.raci.R?.nom ?? '' }
    sig.signatures = s.enSignature.signatures.map((x) => ({ ...x, libelleSignataire: CATALOGUE.fonctions.find((f) => f.code === x.signataire)?.libelle ?? x.signataire, etat: 'ATTENDUE', suivante: false }))
    sig.actionsPermises = ['ATTRIBUER']
  }
  // D-94 : signatures configurées sur l'opération qui porte la signature du procès-verbal (circuit du procès-verbal).
  const opPv = s.circuitPv ? ops.find((o) => o.declencheur === PV_SIGNE) : undefined
  if (opPv && s.circuitPv) opPv.signatures = s.circuitPv.map((x) => ({ ...x, libelleSignataire: CATALOGUE.fonctions.find((f) => f.code === x.signataire)?.libelle ?? x.signataire, etat: 'ATTENDUE', suivante: false }))
  // Signatures déjà données du jeu de démonstration : circuit par défaut, figé.
  const pvSignees = { ...(pv.signatureGimac ? { GIMAC: pv.signatureGimac } : {}), ...(pv.signatureParticipant ? { PARTICIPANT: pv.signatureParticipant } : {}) }
  return { s, ops, blocages, pv, approbation, fiche, conducteur, ...(Object.keys(pvSignees).length ? { pvCircuit: CIRCUIT_PV_DEFAUT, pvSignees } : {}) }
}

/** Clôture des étapes 1 et 2 : dates de MS-PARTICIPANT pour une liaison ouverte dans la démonstration (DDR-15), sinon le jeu. */
const amont = (e: Etat, code: EtapeCode) =>
  (code === 'ADHESION' ? e.s.amont?.adhesion : e.s.amont?.contact) ?? jour(e.s.debut - (code === 'ADHESION' ? 20 : 30))

function etapesDe(e: Etat): EtapeParcours[] {
  const rangEtape: Record<EtapeCode, number | null> = { PRISE_CONTACT: 1, ADHESION: 2, CADRAGE: 3, LANCEMENT: 4, ATELIERS: 5, INTEGRATION: 6, TESTS: 7, CERTIFICATION: 8, APPROBATION_SECURITE: null, MISE_EN_PRODUCTION: 9, SUIVI: 10 }
  return ETAPES.map((code) => {
    const ops = e.ops.filter((o) => o.etape === code)
    if (code === 'PRISE_CONTACT' || code === 'ADHESION') {
      return { code, rang: rangEtape[code], etat: 'CLOSE', porteurs: ['DDRM'], responsable: personne('DDRM'), elements: { RESPONSABLE: true, ACTIONS: true, LIVRABLES: true, VALIDATION: true, PREUVE: true, DATE: true }, actionsFaites: 1, actionsTotal: 1, livrables: [{ libelleKey: code === 'ADHESION' ? 'parcours.livrable.dossierAdhesion' : 'parcours.livrable.enregistrementCandidat', fourni: true }], validateur: personne('CHEF_DDRM'), valideeLe: amont(e, code), debut: code === 'ADHESION' ? amont(e, 'PRISE_CONTACT') : (e.s.amont?.contact ?? jour(e.s.debut - 40)), closeLe: amont(e, code), echeance: null, sla: null }
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
      livrables: ops.filter((o) => o.grain === 'PHASE' || o.pivot).map((o) => ({ libelle: o.libelle ?? o.rang, operation: o.rang, code: 'L1', obligatoire: true, fourni: o.etat === 'FAITE' })),
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

function resumeBrut(e: Etat): LiaisonResume {
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
    ...(e.s.intituleProjet ? { intituleProjet: e.s.intituleProjet } : {}),
    ...(e.s.referenceOrigine ? { referenceOrigine: e.s.referenceOrigine } : {}),
  }
}

/** Opérations qu'une fonction réalise (R) ou valide (V), ou qui incombent au participant (E). */
/** Portail : geste du participant sur une opération ouverte et écran où il l'accomplit (qualité Externe, CR §3.4). */
function gesteParticipant(e: Etat, o: OperationParcours): { lien: string; actionsPermises: string[] } {
  const base = `/portail/liaisons/${e.s.id}`
  const ouverte = o.etat === 'A_FAIRE' || o.etat === 'EN_COURS'
  // Écran du portail selon la vue et le geste de l'opération (D-93), jamais selon son rang.
  if (o.vue === 'COORDONNEES') return { lien: `${base}/coordonnees`, actionsPermises: ouverte ? ['COMMUNIQUER_COORDONNEES'] : [] }
  if (o.vue === 'DECISION' && o.declencheur === 'certification.pv-signed.v1') return { lien: `${base}/proces-verbal`, actionsPermises: e.pv.etat === 'ETABLI' || e.pv.etat === 'SIGNE_GIMAC' ? ['SIGNER_PROCES_VERBAL'] : [] }
  if (o.vue === 'ATTESTATIONS') return { lien: `${base}/attestations`, actionsPermises: ouverte && e.pv.etat === 'SIGNE' && e.approbation.attestations.some((a) => !a.preuve) ? ['DEPOSER_ATTESTATION'] : [] }
  if (o.vue === 'MISE_EN_SERVICE' && o.geste === 'FICHE_SUIVI' && o.raci.R?.fonction === 'PARTICIPANT') return { lien: `${base}/fiche-suivi`, actionsPermises: e.fiche.etat === 'REMISE' ? ['RENSEIGNER_FICHE_SUIVI'] : [] }
  // Livrables qui reviennent au participant : ses contributions aux ateliers, le paramétrage d'essai, ses propres pièces.
  const livrable = o.grain === 'PHASE' || o.geste === 'DEPOSER_LIVRABLE' || o.raci.R?.fonction === 'PARTICIPANT'
  if (livrable && ouverte && o.actionsPermises.some((a) => a === 'DEPOSER_LIVRABLE' || a === 'DEPOSER_PREUVE')) return { lien: `${base}/livrables/${encodeURIComponent(o.id)}`, actionsPermises: ['DEPOSER_LIVRABLE'] }
  return { lien: base, actionsPermises: [] }
}

function tachesDe(etats: Etat[], fonctions: Fonction[]): TacheActeur[] {
  const participant = fonctions.includes('PARTICIPANT')
  return etats.flatMap((e) =>
    e.ops
      .filter((o) => o.etat === 'A_FAIRE' || o.etat === 'EN_COURS' || o.etat === 'BLOQUEE' || o.etat === 'EN_VALIDATION' || o.etat === 'EN_SIGNATURE')
      .flatMap((o): TacheActeur[] => {
        const r = o.raci.R?.fonction
        // Comme MS-HOMOLOGATION : le responsable n'a plus rien à faire pendant la validation et les signatures.
        const estR = o.etat !== 'EN_SIGNATURE' && o.etat !== 'EN_VALIDATION' && r !== undefined && fonctions.includes(r)
        const estE = o.etat !== 'EN_SIGNATURE' && participant && (r === 'PARTICIPANT' || o.raci.E.some((p) => p.fonction === 'PARTICIPANT'))
        const estV = o.etat === 'EN_VALIDATION' && o.raci.V !== null && fonctions.includes(o.raci.V.fonction)
        // D-91 : signature attendue (plus petit ordre restant) dont la fonction est tenue par l'acteur.
        const signature = o.etat === 'EN_SIGNATURE' ? suivantes(o).find((x) => fonctions.includes(x.signataire)) : undefined
        if (!estR && !estE && !estV && !signature) return []
        return [{
          operationId: o.id, liaisonId: e.s.id, liaisonReference: e.s.id, institutionNom: e.s.institutionNom, rang: o.rang, etape: o.etape,
          qualite: signature || estV ? 'V' : estE && !estR ? 'E' : 'R', echeance: o.echeance, sla: o.sla, oubliSansSymptome: o.oubliSansSymptome,
          // D-93 : libellé servi, tâche à prendre (fonction à affecter), signature attendue.
          libelle: signature ? `Signer : ${signature.libelle}` : o.libelle ?? o.rang, aPrendre: estR && !!o.raci.R?.aAffecter, signature: signature?.code ?? null,
          ...(participant ? gesteParticipant(e, o) : { lien: lienOperation(o) }),
        }]
      }).concat(participant ? [] : tachesVisa(e, fonctions)),
  )
}

/** Libellé du visa (circuit gimac.certification.circuits.visas de MS-CERTIFICATION). */
export const LIBELLE_VISA = { CONFORMITE: 'Visa de la conformité', DIRECTION_GENERALE: 'Visa de la Direction Générale' } as const
/**
 * Visas optionnels attendus (D-56), comme MS-HOMOLOGATION : tâche du titulaire de la fonction du visa (CONFORMITE, DG)
 * sur la première opération ouverte de l'approbation de la sécurité, où le visa se signe (onglet Décision).
 */
function tachesVisa(e: Etat, fonctions: Fonction[]): TacheActeur[] {
  const o = e.ops.find((x) => x.declencheur === 'certification.security-approved.v1' && (x.etat === 'A_FAIRE' || x.etat === 'EN_COURS'))
  if (!o) return []
  return e.approbation.visas
    .filter((v) => v.actif && v.etat === 'ATTENDU' && fonctions.includes(v.nature === 'CONFORMITE' ? 'CONFORMITE' : 'DG'))
    .map((v): TacheActeur => ({
      operationId: o.id, liaisonId: e.s.id, liaisonReference: e.s.id, institutionNom: e.s.institutionNom, rang: o.rang, etape: o.etape, qualite: 'V', echeance: o.echeance, sla: o.sla,
      oubliSansSymptome: false, libelle: `Signer : ${LIBELLE_VISA[v.nature]}`, aPrendre: false, signature: v.nature, lien: lienOperation(o),
    }))
}

/** Signatures restantes du plus petit ordre (D-91 : même ordre, indifférent). */
function suivantes(o: OperationParcours): SignatureOperation[] {
  const restantes = (o.signatures ?? []).filter((x) => x.etat === 'ATTENDUE')
  const ordre = Math.min(...restantes.map((x) => x.ordre))
  return restantes.filter((x) => x.ordre === ordre)
}

/** nomInstitution : nom tenu par le registre (MS-PARTICIPANT simulé), pour que la liaison ouverte porte le nom de l'adhérent choisi. */
/** Démonstration continue (D-98) : état du parcours simulé gardé d'un chargement à l'autre (changement d'acteur). */
export type PersistanceDemo = { lire(): string | null; ecrire(valeur: string): void }
/** DDR-08 : opération ouverte ou renvoyée, telle que MS-NOTIFICATION la notifie à son responsable. */
export type NotificationOperationDemo = { type: 'OUVERTE' | 'RENVOYEE' | 'VALIDATION'; operationId: string; liaisonId: string; institutionNom: string; libelle: string; motif: string | null; lien: string; le: string }
/** DDR-08 : opérations à notifier à l'utilisateur (responsable par sa fonction), relues de l'état gardé et poussées en direct. */
export type EvenementsParcoursDemo = {
  pourMoi(): NotificationOperationDemo[]; surEvenement(f: (n: NotificationOperationDemo) => void): void
  /** HOM-04 : fonctions internes à affecter offertes en tâches à prendre de MS-PILOTAGE (effet-reel-referentiels.md §8.8). */
  fonctionsAPrendre?(): OffreFonctionDemo[]
  /** HOM-04 : prise de la tâche (pilotage.task-taken.v1) : la personne est affectée à la fonction sur la liaison (PrisesFonctions). */
  prendreFonction?(liaisonId: string, fonction: Fonction, personne: { id: string; nom: string }): void
}
/** Tâche à prendre d'une fonction à affecter : référence <réf.>/AFFECTER/<FONCTION>, permissions de la fonction au catalogue. */
export type OffreFonctionDemo = { liaisonId: string; reference: string; fonction: Fonction; titre: string; description: string; permissions: string[]; lien: string }

export function parcoursSimule(attendreJeu: Attendre, institutionPortail: string, nomInstitution?: (id: string) => Promise<string | undefined>,
  u: {
    profils?: string[]; fonctions?: () => FonctionCatalogue[]
    /** D-98 : opérations de la version publiée d'un type (instanciation, D-91) ; null sans version publiée. */
    versionPubliee?: (type: string) => ReferenceOperation[] | null
    /** homologation.liaison-opened.v1 consommé par MS-PARTICIPANT : ADHERENT vers EN_HOMOLOGATION. */
    surLiaisonOuverte?: (institutionId: string, liaisonId: string) => void
    /** homologation.liaison-milestone-reached.v1 consommé par MS-PARTICIPANT (EN_PRODUCTION : institution ACTIF). */
    surJalon?: (institutionId: string, jalon: Jalon, liaisonId: string) => void
    persistance?: PersistanceDemo
    /** Utilisateur de la démonstration : auteur réel des dépôts (DDR-05) et des affectations. */
    utilisateur?: { id: string; nom: string }
    /** Permissions de l'utilisateur : supervision HOM_STEP_MANAGE admise hors RACI, jamais offerte (DDR-05). */
    permissions?: string[]
    /** DDR-24 : numéro de la version publiée d'un type (arbre des référentiels). */
    numeroPublie?: (type: string) => number | null
    /** DDR-15 : dates de prise de contact et d'adhésion d'une institution (MS-PARTICIPANT). */
    datesAmont?: (institutionId: string) => Promise<{ contact: string | null; adhesion: string | null } | undefined>
    /** HOM-04 : appelé après chaque geste (offre des fonctions à affecter à MS-PILOTAGE simulé). */
    apresEffet?: () => void
    /** CER-05 : libellé d'un type de projet (catalogue des types de MS-HOMOLOGATION), servi au DES et au portail. */
    libelleType?: (type: string) => string | null
  } = {}): { parcours: ServiceParcours; decision: ServiceDecision; portailParcours: ServicePortailParcours; evenements: EvenementsParcoursDemo } {
  // CER-05 : le résumé et le dossier portent le libellé du type, comme Vues.LiaisonResume et Vues.Dossier.
  const resume = (e: Etat): LiaisonResume => {
    const libelle = u.libelleType?.(e.s.typeProjet)
    return libelle ? { ...resumeBrut(e), typeProjetLibelle: libelle } : resumeBrut(e)
  }
  const etats = new Map(SCENARIOS.map((s) => [s.id, construire(structuredClone(s))]))
  // D-109 : dépôts de la démonstration par opération (livrable, service, sans objet motivé).
  const depotsDemo: DepotsDemo = new Map()
  const servicesInclus = (e: Etat | undefined) => (e?.s.services ?? []).filter((x) => x.inclus).map((x) => x.code)
  /** D-109 : livrables obligatoires encore attendus (un dépôt par service inclus au grain du service). */
  const manquants = (e: Etat, o: OperationParcours) =>
    livrablesAttendusDemo(o, e.s.typeProjet, servicesInclus(e), depotsDemo.get(o.id) ?? [], false).filter((l) => l.obligatoire && (l.etat === 'ATTENDUE' || l.etat === 'REFUSEE'))
  /** D-109 : dépôt inscrit pour un livrable visé (et un service), remplaçant le précédent. */
  const inscrire = (o: OperationParcours, d: OperationParcours['preuve'], livrable: string | undefined, service: string | undefined, auteur: string) => {
    if (!d) return
    const code = livrable ?? preuvesDe(trouver(o.liaisonId).s.typeProjet, o.rang)?.livrables[0]?.code ?? 'L1'
    const l = (depotsDemo.get(o.id) ?? []).filter((x) => !(x.livrable === code && (x.service ?? null) === (service ?? null)))
    depotsDemo.set(o.id, [...l, { livrable: code, documentRef: d.documentRef, nom: d.nom, empreinte: d.empreinte, deposeLe: d.deposeLe, deposePar: auteur, service: service ?? null }])
  }
  // Démonstration continue : l'état gardé remplace le jeu initial (liaisons ouvertes et gestes accomplis compris).
  try {
    const garde = u.persistance?.lire()
    if (garde) (JSON.parse(garde) as [string, Etat][]).forEach(([id, e]) => etats.set(id, e))
  } catch {
    // État illisible : jeu initial.
  }
  const garder = () => {
    try {
      u.persistance?.ecrire(JSON.stringify([...etats.entries()]))
    } catch {
      // Stockage indisponible : la démonstration continue en mémoire.
    }
  }
  /** Chaque réponse garde l'état (démonstration continue) une fois l'effet appliqué. */
  const attendre = (<T>(v: T | (() => T)) => attendreJeu(() => {
    const r = typeof v === 'function' ? (v as () => T)() : v
    garder()
    u.apresEffet?.()
    return r
  })) as Attendre
  // Fonctions du catalogue (D-91), et celles que confèrent les profils de l'utilisateur (GET /moi/fonctions, D-94 §6.5).
  const catalogueFonctions = (): FonctionCatalogue[] => u.fonctions?.() ?? CATALOGUE.fonctions.map((f) => ({ code: f.code, libelle: f.libelle, profils: f.profils ?? [], externe: !!f.externe, actif: true }))
  const libelleFonction = (f: Fonction) => catalogueFonctions().find((x) => x.code === f)?.libelle ?? f
  const mesFonctions = (): FonctionUtilisateur[] => {
    const profils = u.profils ?? []
    const l = catalogueFonctions().filter((f) => f.actif && f.profils.some((p) => profils.includes(p))).map((f) => ({ code: f.code, libelle: f.libelle, externe: f.externe, profils: f.profils.filter((p) => profils.includes(p)) }))
    // Un chef de département tient aussi CHEFS_DEPARTEMENT (§6.5).
    const chef = profils.filter((p) => p === 'ROLE_CHEF_DES' || p === 'ROLE_CHEF_DDRM')
    if (chef.length && !l.some((f) => f.code === 'CHEFS_DEPARTEMENT')) l.push({ code: 'CHEFS_DEPARTEMENT', libelle: libelleFonction('CHEFS_DEPARTEMENT'), externe: false, profils: chef })
    return l
  }
  const codesMiens = () => mesFonctions().map((f) => f.code)
  /** Titulaire d'une fonction sur la liaison : le conducteur désigné pour CONDUCTEUR, sinon le premier titulaire de démonstration. */
  const titulaire = (e: Etat | undefined, f: Fonction): Personne => (f === 'CONDUCTEUR' && e?.conducteur ? e.conducteur : personne(f))
  /** Signataire admis (OperationsLiaison.admetSignataire) : la personne tient la fonction ; sans identité connue, le porteur de la permission de la route. */
  const admetSignataire = (fonction: Fonction) => u.profils === undefined || codesMiens().includes(fonction)
  /**
   * Opérations ouvertes qui portent un geste de certification (OperationsLiaison.ouvertes, D-93 §4) : 409
   * CER_OPERATION_ABSENTE si la version n'en porte aucune, CER_OPERATION_NON_OUVERTE si aucune n'est ouverte.
   */
  const ouvertesGeste = (e: Etat, declencheur: string, geste: string) => {
    const portant = declenchees(e, declencheur)
    if (!portant.length) throw new ApiError(409, 'CER_OPERATION_ABSENTE', `Le référentiel de cette liaison ne porte aucune opération pour ce geste : ${geste}.`, correlation(), { geste, declencheur })
    const ouvertes = portant.filter(ouverteOp)
    if (!ouvertes.length) throw new ApiError(409, 'CER_OPERATION_NON_OUVERTE', `L'opération qui porte ce geste n'est pas ouverte : ${geste}.`, correlation(), { geste, operations: portant.map((o) => ({ code: o.rang, etat: o.etat })) })
    return ouvertes
  }
  const ouvertesSansRefus = (e: Etat, declencheur: string) => {
    try {
      return ouvertesGeste(e, declencheur, '')
    } catch {
      return null
    }
  }
  /** Signatures du procès-verbal données (un état gardé antérieur n'a que celles du GIMAC et du participant). */
  const signeesPv = (e: Etat): Record<string, Signature> =>
    e.pvSignees ?? { ...(e.pv.signatureGimac ? { GIMAC: e.pv.signatureGimac } : {}), ...(e.pv.signatureParticipant ? { PARTICIPANT: e.pv.signatureParticipant } : {}) }
  /**
   * Circuit du procès-verbal (CircuitsSignature.procesVerbal, D-94) : figé à la première signature ; sinon signatures
   * configurées des opérations ouvertes qui portent la signature, complétées de l'étape du portail ; à défaut, le défaut.
   */
  const circuitPv = (e: Etat): EtapePv[] => {
    if (e.pvCircuit?.length) return e.pvCircuit
    const configurees = (ouvertesSansRefus(e, PV_SIGNE) ?? []).flatMap((o) => o.signatures ?? []).map((x): EtapePv => ({ code: x.code, libelle: x.libelle, fonction: x.signataire, ordre: x.ordre, portail: false }))
    return configurees.length ? [...configurees, ...CIRCUIT_PV_DEFAUT.filter((p) => p.portail && !configurees.some((c) => c.code === p.code))] : CIRCUIT_PV_DEFAUT
  }
  /** Vue du circuit ; au portail, une signature du GIMAC est désignée par sa fonction, jamais par le nom de la personne. */
  const vueCircuit = (e: Etat, portail: boolean): EtapeCircuit[] => {
    const s = signeesPv(e)
    return circuitPv(e).map((x) => {
      const sg = s[x.code] ?? null
      return { ...x, etat: sg ? 'SIGNEE' : 'ATTENDUE', signature: sg && portail && !x.portail ? { ...sg, signataire: sg.fonction } : sg }
    })
  }
  /** Signature du procès-verbal (ProcesVerbalService.signer) : opération ouverte, ordre du circuit, signataire désigné. */
  const signerPv = (e: Etat, portail: boolean) => {
    if (e.pv.etat === 'SIGNE') throw refus(409, 'CER_PV_NOT_SIGNABLE', 'Le procès-verbal est déjà signé.')
    ouvertesGeste(e, PV_SIGNE, 'Signer le procès-verbal de certification')
    const c = circuitPv(e)
    const s = signeesPv(e)
    const signables = signablesPv(c, s, portail)
    if (!signables.length) throw nonSignablePv(c, s, portail)
    const etape = portail ? signables[0] : signables.find((x) => admetSignataire(x.fonction))
    if (!etape) throw new ApiError(403, 'CER_SIGNATAIRE_NON_DESIGNE', `Vous ne tenez pas la fonction signataire attendue : ${signables[0].libelle}.`, correlation(), { signature: signables[0].code, fonction: signables[0].fonction })
    const nom = portail ? `Correspondant ${e.s.institutionNom}` : etape.fonction === 'CONDUCTEUR' && e.conducteur ? e.conducteur.nom : personne(etape.fonction).nom
    const sig: Signature = { signataire: nom, fonction: etape.fonction, signeLe: jour(0), horodatage: horodatage() }
    const signees = { ...s, [etape.code]: sig }
    e.pvCircuit = c
    e.pvSignees = signees
    // D-91 : la signature configurée de l'opération est donnée avec celle du circuit.
    declenchees(e, PV_SIGNE).forEach((o) => (o.signatures ?? []).filter((x) => x.code === etape.code).forEach((x) => Object.assign(x, { etat: 'SIGNEE', signeParNom: nom, signeLe: sig.horodatage, empreinte: e.pv.empreinte })))
    const premiere = (cote: boolean) => c.filter((x) => x.portail === cote && signees[x.code]).map((x) => signees[x.code]).sort((a, b) => a.horodatage.localeCompare(b.horodatage))[0] ?? null
    e.pv = { ...e.pv, etat: etatPvDe(c, signees), signatureGimac: premiere(false), signatureParticipant: premiere(true) }
    if (e.pv.etat === 'SIGNE') {
      e.s.statut = 'CERTIFIEE'
      const o = declenchee(e, PV_SIGNE)
      if (o && o.etat !== 'FAITE') terminer(e, o, { documentRef: `PV-SIG-${e.s.id.slice(-3)}`, nom: 'proces-verbal-signe.pdf', empreinte: e.pv.empreinte ?? '', deposeLe: jour(0), deposePar: nom })
    }
  }
  /** Signatures que la personne peut donner maintenant (actions SIGNER_GIMAC, SIGNER_PARTICIPANT). */
  const peutSignerPv = (e: Etat, portail: boolean) => {
    if (e.pv.etat === 'NON_ETABLI' || e.pv.etat === 'SIGNE' || !ouvertesSansRefus(e, PV_SIGNE)) return false
    const l = signablesPv(circuitPv(e), signeesPv(e), portail)
    return portail ? l.length > 0 : l.some((x) => admetSignataire(x.fonction))
  }
  /** Vue servie d'une opération (D-93 §8.5) : livrables attendus, signatures suivantes, actions calculées pour la personne. */
  const vueOperation = (o: OperationParcours): OperationParcours => {
    const miens = codesMiens()
    const ouverte = o.etat === 'A_FAIRE' || o.etat === 'EN_COURS'
    const droit = droitDepot(o)
    const deposer = o.actionsPermises.some((a) => a === 'DEPOSER_LIVRABLE' || a === 'DEPOSER_PREUVE') && droit !== null && droit !== 'SUPERVISION'
    // D-109 : livrables attendus lus dans les données du référentiel (preuvesCR), avec leur état ; geste dédié : affichage seul.
    const e0 = etats.get(o.liaisonId)
    const dedie = estDedie(o.geste ?? null) || o.geste === 'AUCUN'
    const livrablesAttendus: LivrableAttendu[] = !e0 || (dedie && !preuvesDe(e0.s.typeProjet, o.rang))
      ? []
      : livrablesAttendusDemo(o, e0.s.typeProjet, servicesInclus(e0), depotsDemo.get(o.id) ?? [], !dedie && ouverte && deposer)
          .map((l) => ({ ...l, libelleDeposant: l.deposant ? libelleFonction(l.deposant) : null }))
    const suivante = o.etat === 'EN_SIGNATURE' ? suivantes(o) : []
    const signatures = (o.signatures ?? []).map((x) => ({ ...x, suivante: suivante.includes(x), attendu: x.etat === 'ATTENDUE' ? { id: titulaire(etats.get(o.liaisonId), x.signataire).id, nom: titulaire(etats.get(o.liaisonId), x.signataire).nom, fonction: x.signataire } : null }))
    // DDR-05 : dépôt offert au seul RACI (R par sa fonction, conducteur), comme ParcoursProjection.actions ; HOM-03 : validation
    // et renvoi offerts au seul validateur désigné.
    const offerts = o.actionsPermises.filter((a) => ((a !== 'DEPOSER_LIVRABLE' && a !== 'DEPOSER_PREUVE') || deposer) && ((a !== 'VALIDER' && a !== 'RENVOYER') || peutValider(o)))
    const actions = [...offerts, ...(o.etat === 'EN_SIGNATURE' && suivante.some((x) => miens.includes(x.signataire)) ? ['SIGNER'] : [])]
    return { ...o, livrablesAttendus, signatures, actionsPermises: actions }
  }
  /**
   * DDR-05 : droit de dépôt de l'utilisateur (Parcours.droitDepot de MS-HOMOLOGATION) : R par sa fonction, conducteur de la
   * liaison, sinon supervision par HOM_STEP_MANAGE (admise, journalisée, jamais offerte) ; null : refus. Sans profils
   * (tests du jeu seul), le dépôt reste ouvert comme avant.
   */
  function droitDepot(o: OperationParcours): 'RESPONSABLE' | 'CONDUCTEUR' | 'SUPERVISION' | null {
    if (!u.profils) return 'RESPONSABLE'
    const miens = codesMiens()
    if (o.raci.R && miens.includes(o.raci.R.fonction)) return 'RESPONSABLE'
    const e = etats.get(o.liaisonId)
    if (miens.includes('CONDUCTEUR') && e?.conducteur && e.conducteur.id === personne('CONDUCTEUR', 0).id) return 'CONDUCTEUR'
    return (u.permissions ?? []).includes('HOM_STEP_MANAGE') ? 'SUPERVISION' : null
  }
  /** Dépôt admis, sinon 403 HOM_DEPOT_NON_AUTORISE ; l'auteur tracé est l'utilisateur réel (jamais le responsable désigné). */
  const exigerDroitDepot = (o: OperationParcours): string => {
    if (!droitDepot(o)) throw refus(403, 'HOM_DEPOT_NON_AUTORISE', `Ce dépôt revient au responsable désigné (${o.raci.R?.nom ?? 'non désigné'}) ou au conducteur de la liaison.`)
    return u.utilisateur?.nom ?? o.raci.R?.nom ?? ''
  }
  /** DDR-08 : opération ouverte ou renvoyée, consignée pour la notification de son responsable. */
  const abonnesEvenements: ((n: NotificationOperationDemo) => void)[] = []
  const notificationDe = (e: Etat, j: EvenementOperationDemo): NotificationOperationDemo | null => {
    const o = e.ops.find((x) => x.id === j.operationId)
    if (!o) return null
    return { type: j.type, operationId: o.id, liaisonId: e.s.id, institutionNom: e.s.institutionNom, libelle: o.libelle ?? o.rang, motif: j.motif ?? null, lien: lienOperation(o), le: j.le }
  }
  /**
   * Destinataire de l'événement : le responsable (ouverture, renvoi) ; HOM-16 : le validateur désigné pour une validation
   * demandée (homologation.operation-validation-requested.v1, « Validation attendue » de MS-NOTIFICATION).
   */
  const pourMoi = (o: OperationParcours, type: EvenementOperationDemo['type'] = 'OUVERTE') => {
    if (!u.profils) return false
    if (type === 'VALIDATION') return peutValider(o)
    return !!o.raci.R && o.raci.R.fonction !== 'PARTICIPANT' && codesMiens().includes(o.raci.R.fonction)
  }
  const consigner = (e: Etat, o: OperationParcours, type: EvenementOperationDemo['type'], motif?: string) => {
    const j: EvenementOperationDemo = { type, operationId: o.id, le: horodatage(), ...(motif ? { motif } : {}) }
    e.journal = [...(e.journal ?? []), j]
    const n = notificationDe(e, j)
    if (n && pourMoi(o, type)) abonnesEvenements.forEach((f) => f(n))
  }
  /** Fonction tenue sur la liaison : par ses profils ; CONDUCTEUR seulement par le conducteur désigné de la liaison. */
  const tient = (o: OperationParcours, p: Personne | null) => {
    if (!p) return false
    if (!codesMiens().includes(p.fonction)) return false
    if (p.fonction !== 'CONDUCTEUR') return true
    const e = etats.get(o.liaisonId)
    return !e?.conducteur || e.conducteur.id === personne('CONDUCTEUR', 0).id
  }
  /**
   * HOM-03 : validateur admis (Parcours.peutValider de MS-HOMOLOGATION) : le V désigné, ou un porteur de sa fonction s'il est
   * à affecter, jamais le responsable (R) ; sans profils (tests du jeu seul), la validation reste ouverte comme avant.
   */
  function peutValider(o: OperationParcours): boolean {
    if (!u.profils) return true
    return !!o.raci.V && tient(o, o.raci.V) && !(o.raci.R && o.raci.R.fonction !== o.raci.V.fonction && tient(o, o.raci.R))
  }
  /** Validation ou renvoi admis (Parcours.exigerValidateur), sinon 403 HOM_VALIDATION_NON_VALIDATEUR ; R : 409 HOM_VALIDATOR_IS_RESPONSIBLE. */
  const exigerValidateur = (o: OperationParcours) => {
    if (!u.profils) return
    const estR = !!o.raci.R && tient(o, o.raci.R) && !(o.raci.V && tient(o, o.raci.V) && o.raci.V.fonction === o.raci.R.fonction)
    if (estR) throw refus(409, 'HOM_VALIDATOR_IS_RESPONSIBLE', 'Le validateur doit être distinct du responsable.')
    if (!peutValider(o)) {
      const v = o.raci.V
      throw refus(403, 'HOM_VALIDATION_NON_VALIDATEUR', `Cette validation revient au validateur désigné (${!v ? 'non désigné' : v.aAffecter ? `un porteur de la fonction ${libelleFonction(v.fonction)}` : v.nom}).`)
    }
  }
  /** Intervenants de la liaison lus du RACI (D-93 §8.5) : fonction, qualités, personne désignée ou à affecter. */
  const intervenantsDe = (e: Etat): IntervenantLiaison[] => {
    const parFonction = new Map<Fonction, IntervenantLiaison>()
    const ajouter = (q: Qualite, p: Personne | null) => {
      if (!p) return
      const i = parFonction.get(p.fonction) ?? { fonction: p.fonction, libelle: libelleFonction(p.fonction), qualites: [], personne: null, aAffecter: false, externe: catalogueFonctions().find((f) => f.code === p.fonction)?.externe ?? false }
      if (!i.qualites.includes(q)) i.qualites.push(q)
      if (p.aAffecter) i.aAffecter = true
      else if (!i.personne) i.personne = p
      parFonction.set(p.fonction, i)
    }
    e.ops.filter((o) => o.etat !== 'FAITE').forEach((o) => {
      ajouter('R', o.raci.R)
      ajouter('V', o.raci.V)
      o.raci.C.forEach((p) => ajouter('C', p))
      o.raci.I.forEach((p) => ajouter('I', p))
      o.raci.E.forEach((p) => ajouter('E', p))
    })
    return [...parFonction.values()].sort((a, b) => Number(b.aAffecter) - Number(a.aAffecter) || a.libelle.localeCompare(b.libelle, 'fr'))
  }
  /** Étapes réelles de la version (D-93 §8.5) : libellé et couleur configurés, avancement calculé. */
  const etapesVersionDe = (e: Etat): EtapeVersion[] => etapesDe(e).filter((x) => x.code !== 'PRISE_CONTACT' && x.code !== 'ADHESION').map((x, i) => {
    const c = ETAPES_CANONIQUES.find((k) => k.code === x.code)
    return {
      code: x.code, libelle: c?.libelle ?? x.code, ordre: i + 1, jeton: jetonEtape(x.code), canonique: x.code, etat: x.etat, actionsFaites: x.actionsFaites, actionsTotal: x.actionsTotal,
      responsable: x.responsable, validateur: x.validateur, debut: x.debut, closeLe: x.closeLe, echeance: x.echeance, sla: x.sla, livrables: x.livrables,
    }
  })
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
  /** Clôt une opération (après ses signatures, D-91) puis ouvre les suivantes dont tous les prédécesseurs sont faits. */
  const terminer = (e: Etat, o: OperationParcours, preuve: OperationParcours['preuve']) => {
    if ((o.signatures ?? []).some((x) => x.etat === 'ATTENDUE')) {
      o.etat = 'EN_SIGNATURE'
      o.sla = null
      o.preuve = preuve
      o.actionsPermises = ['ATTRIBUER']
      return
    }
    o.etat = 'FAITE'
    o.faiteLe = jour(0)
    o.sla = null
    o.preuve = preuve
    o.actionsPermises = ['CONSULTER']
    // Jalon atteint (homologation.liaison-milestone-reached.v1) : statut de la liaison, institution ACTIF en production.
    const statut = o.jalon ? STATUT_DU_JALON[o.jalon] : null
    if (o.jalon && statut) {
      e.s.statut = statut
      u.surJalon?.(e.s.institutionId, o.jalon, e.s.id)
    }
    e.ops.forEach((x) => {
      x.predecesseursNonFaits = x.predecesseursNonFaits.filter((r) => r !== o.rang)
      if (x.etat === 'VERROUILLEE' && x.predecesseursNonFaits.length === 0) {
        x.etat = 'A_FAIRE'
        x.actionsPermises = actionsOuvertes(x)
        consigner(e, x, 'OUVERTE')
      }
    })
    // Avant tout jalon, statut du CR déduit de l'étape courante (Parcours.statutCalcule de MS-HOMOLOGATION, D-57).
    if (!statut && (e.s.statut === 'CADREE' || e.s.statut === 'EN_INTEGRATION' || e.s.statut === 'EN_TEST')) {
      const c = etapeCourante(e)
      e.s.statut = c === 'TESTS' || c === 'CERTIFICATION' ? 'EN_TEST' : c === 'INTEGRATION' ? 'EN_INTEGRATION' : 'CADREE'
    }
  }
  const dossierDe = (e: Etat): DossierLiaison => {
    const r = resume(e)
    const peutConduire = e.conducteur !== null
    return {
      id: e.s.id, reference: e.s.id, institutionId: e.s.institutionId, institutionNom: e.s.institutionNom, typeProjet: e.s.typeProjet,
      ...(r.typeProjetLibelle ? { typeProjetLibelle: r.typeProjetLibelle } : {}),
      banquePartenaireNom: e.s.banque, personnalisateurNom: e.s.personnalisateur, statut: e.s.statut, etapeCourante: r.etapeCourante,
      conducteur: e.conducteur, etapesVersion: etapesVersionDe(e), intervenants: intervenantsDe(e),
      dateDemarrage: jour(e.s.debut), dateCibleProduction: r.dateCibleProduction, versionReferentiel: e.s.versionNumero ?? (e.s.depuisVersion ? 1 : u.numeroPublie?.(e.s.typeProjet) ?? (e.s.typeProjet === 'PAIEMENT_MOBILE' ? 2 : e.s.typeProjet === 'CARTE_INTERBANCAIRE' ? 3 : 1)),
      perimetre: perimetreDe(e), couvertureCertification: r.couvertureCertification, couvertureMiseEnService: r.couvertureMiseEnService,
      etapes: etapesDe(e), blocages: e.blocages, interlocuteursParticipant: [{ nom: `Correspondant technique ${e.s.institutionNom}`, fonction: 'Responsable technique' }, { nom: `Correspondant fonctionnel ${e.s.institutionNom}`, fonction: 'Responsable fonctionnel' }],
      liaisonOrigineId: e.s.origine ?? null,
      // D-104 : intitulé et référence d'origine, comme le dossier de MS-HOMOLOGATION.
      intituleProjet: e.s.intituleProjet ?? null, referenceOrigine: e.s.referenceOrigine ?? null,
      transitionsPermises: [],
      actionsPermises: [
        ...(peutConduire ? ['DECLARER_BLOCAGE'] : []),
        // D-93 : affectation d'une fonction de la liaison (HOM_STEP_MANAGE ou HOM_OPERATION_ASSIGN).
        'AFFECTER_FONCTION',
        'DESIGNER_CONDUCTEUR',
        ...(e.s.statut === 'EN_PRODUCTION' ? ['OUVRIR_SERVICE_SUPPLEMENTAIRE'] : []),
        ...(e.ops.some((o) => o.rang === '6' && o.etat === 'A_FAIRE') ? ['AFFECTER_RESSOURCES'] : []),
        ...(r.etapeCourante === 'CERTIFICATION' || r.etapeCourante === 'TESTS' ? ['CONSTATER_COUVERTURE'] : []),
      ],
    }
  }

  const compteur = (cle: string, valeur: number, ton: TableauActeur['compteurs'][number]['ton'], lien: string | null = null) => ({ cle, valeur, ton: valeur === 0 ? 'ok' as const : ton, lien })
  const aEtape = (codes: EtapeCode[]) => tous().filter((e) => codes.includes(etapeCourante(e)))
  /** Lien d'un compteur : l'opération même s'il n'en compte qu'une, sinon la liste des liaisons de l'étape. */
  const versOps = (ops: OperationParcours[], etape: EtapeCode) => (ops.length === 1 ? lienOperation(ops[0]) : `/des/parcours?etape=${etape}`)
  const opsOuvertes = (actives: Etat[], filtre: (o: OperationParcours) => boolean) => actives.flatMap((e) => e.ops.filter(filtre))
  /** Lien d'un compteur de liaisons : l'onglet du dossier s'il n'y en a qu'une, sinon la liste filtrée par étape. */
  const versLiaisons = (l: Etat[], onglet: string, etapes: string) => (l.length === 1 ? `/des/parcours/${l[0].s.id}?onglet=${onglet}` : `/des/parcours?etape=${etapes}`)

  const parcours: ServiceParcours = {
    listerLiaisons: (f) => attendre(() => {
      let l = tous().map(resume)
      if (f.recherche) l = l.filter((x) => [x.institutionNom, x.reference, x.intituleProjet ?? '', x.referenceOrigine ?? ''].join(' ').toLowerCase().includes(f.recherche!.toLowerCase()))
      if (f.statut) l = l.filter((x) => x.statut === f.statut)
      if (f.typeProjet) l = l.filter((x) => x.typeProjet === f.typeProjet)
      if (f.etape) l = l.filter((x) => f.etape!.split(',').includes(x.etapeCourante))
      if (f.sansConducteur) l = l.filter((x) => !x.conducteurNom)
      if (f.bloquees) l = l.filter((x) => x.bloquee)
      if (f.mesProjets) l = l.filter((x) => x.conducteurNom === personne('CONDUCTEUR', 0).nom)
      return { contenu: l.slice((f.page - 1) * f.taille, f.page * f.taille), total: l.length }
    }),
    conducteurs: () => attendre(PERSONNES.filter((p) => p.fonction === 'CONDUCTEUR')),
    ouvrirLiaison: async (o) => {
      const nom = await nomInstitution?.(o.institutionId)
      const banque = o.banquePartenaireId ? await nomInstitution?.(o.banquePartenaireId) : undefined
      const datesAmont = await u.datesAmont?.(o.institutionId)
      return attendre(() => {
        // D-106 (§12), D-146 : comme OpenLiaisonService, ouvrir exige HOM_LIAISON_OPEN et la désignation du conducteur
        // (HOM_PROJECT_DESIGNATE), ou la conduite de projet (HOM_PROJECT_CONDUCT) pour le projet que l'on conduit soi-même.
        if (u.profils?.length && u.permissions && !(u.permissions.includes('HOM_LIAISON_OPEN') && (u.permissions.includes('HOM_PROJECT_DESIGNATE') || u.permissions.includes('HOM_PROJECT_CONDUCT')))) {
          throw refus(403, 'HOM_PROJECT_DESIGNATION_FORBIDDEN', 'Ouvrir une liaison exige HOM_LIAISON_OPEN et la désignation du conducteur (HOM_PROJECT_DESIGNATE).')
        }
        // D-104 : plusieurs liaisons actives par institution, y compris du même type ; seule la référence d'origine
        // d'un projet repris reste unique (OpenLiaisonService).
        const referenceOrigine = o.referenceOrigine?.trim() || null
        if (referenceOrigine && tous().some((e) => e.s.referenceOrigine === referenceOrigine)) {
          throw refus(409, 'HOM_LIAISON_REFERENCE_ORIGINE_DUPLICATE', "Cette référence d'origine est déjà portée par une autre liaison.")
        }
        // D-145 : périmètre défini par le gestionnaire à la création, parmi les services proposables (PerimetreCreation).
        const catalogue = o.typeProjet === 'PAIEMENT_MOBILE' ? SERVICES_MOBILE : SERVICES_CARTE
        if (o.perimetre) {
          const retenus = o.perimetre.filter((c) => c !== 'TRANSVERSE')
          if (!retenus.length) throw refus(422, 'HOM_PERIMETRE_VIDE', 'Le périmètre porte au moins un service à certifier.')
          if (retenus.some((c) => !catalogue.some((x) => x.code === c))) throw refus(422, 'HOM_PERIMETRE_SERVICE_NON_SOUSCRIT', 'Un service non souscrit ne peut entrer au périmètre (RG-72).')
        }
        const cond = PERSONNES.findIndex((p) => p.id === o.chefDeProjetId)
        // D-91, D-98 : un type créé dans la plateforme s'instancie depuis la version publiée de son référentiel.
        const version = ['CARTE_INTERBANCAIRE', 'PAIEMENT_MOBILE', 'DELEGATAIRE'].includes(o.typeProjet) ? null : u.versionPubliee?.(o.typeProjet) ?? null
        const s: Scenario = { id: `LIA-2026-${String(50 + etats.size).padStart(3, '0')}`, institutionId: o.institutionId, institutionNom: nom ?? (o.institutionId === 'INST-0042' ? 'Établissement de paiement A' : o.institutionId === 'INST-0017' ? 'Banque B' : 'Nouvel adhérent'), typeProjet: o.typeProjet, banque: banque ?? null, personnalisateur: null, fintech: false, avancement: '2', statut: 'CADREE', conducteur: cond >= 0 ? PERSONNES.filter((p) => p.fonction === 'CONDUCTEUR').findIndex((p) => p.id === o.chefDeProjetId) : 0, debut: 0, cible: null, services: version ? [{ code: 'TRANSVERSE', libelle: 'Service du projet', inclus: true }] : perimetre(catalogue, o.perimetre ? catalogue.map((x) => x.code).filter((c) => !o.perimetre!.includes(c)) : []), ...(version ? { depuisVersion: true } : {}),
          versionNumero: u.numeroPublie?.(o.typeProjet) ?? undefined, amont: datesAmont, intituleProjet: o.intituleProjet?.trim() || null, referenceOrigine }
        const e = construire(s, version)
        etats.set(s.id, e)
        // DDR-08 : chaque opération ouverte parvient à son responsable (tâche et notification).
        e.ops.filter((x) => x.etat === 'A_FAIRE').forEach((x) => consigner(e, x, 'OUVERTE'))
        u.surLiaisonOuverte?.(o.institutionId, s.id)
        return resume(e)
      })
    },
    perimetreProposable: (_institutionId, typeProjet) => attendre(() => ({
      // Démonstration : aucun registre de souscriptions, le catalogue du type de projet est proposé.
      services: ['CARTE_INTERBANCAIRE', 'PAIEMENT_MOBILE', 'DELEGATAIRE'].includes(typeProjet) ? (typeProjet === 'PAIEMENT_MOBILE' ? SERVICES_MOBILE : SERVICES_CARTE).map((x) => ({ ...x, interfaceSandbox: null })) : [],
      souscrits: false,
    })),
    designerConducteur: (id, conducteurId, motif) => attendre(() => {
      const e = trouver(id)
      if (e.conducteur && !motif.trim()) throw refus(422, 'HOM_DESIGNATION_REASON_REQUIRED', 'Une redésignation doit être motivée.')
      const p = PERSONNES.find((x) => x.id === conducteurId && x.fonction === 'CONDUCTEUR')
      if (!p) throw refus(422, 'HOM_LEAD_NOT_ELIGIBLE', 'La personne désignée ne détient pas le rôle de conducteur de projet.')
      e.conducteur = p
      e.ops.forEach((o) => { if (o.raci.R?.fonction === 'CONDUCTEUR') o.raci.R = p; if (o.raci.V?.fonction === 'CONDUCTEUR') o.raci.V = p })
    }),
    dossier: (id) => attendre(() => dossierDe(trouver(id))),
    operations: (id, f) => attendre(() => trouver(id).ops.filter((o) => (!f?.etape || o.etape === f.etape) && (!f?.fonction || o.raci.R?.fonction === f.fonction)).map(vueOperation)),
    affecterRessources: (id, a) => attendre(() => {
      const e = trouver(id)
      const o = e.ops.find((x) => x.rang === '6')
      if (!o || o.etat !== 'A_FAIRE') throw refus(409, 'HOM_STEP_NOT_OPEN', 'L\'affectation des ressources n\'est pas ouverte.')
      if (a.length === 0) throw refus(422, 'HOM_RESOURCES_REQUIRED', 'Au moins une ressource doit être affectée.')
      // DDR-06 : comme MS-HOMOLOGATION (intervenants.pourFonction) : chaque personne existe et tient la fonction affectée ;
      // elle remplace la fonction sur les opérations non faites.
      const personnes = a.map((x) => {
        const p = PERSONNES.find((y) => y.id === x.personneId)
        if (!p) throw refus(422, 'HOM_PERSON_UNKNOWN', 'Personne inconnue.')
        if (p.fonction !== x.fonction) throw refus(422, 'HOM_PERSON_NOT_ELIGIBLE', 'Cette personne ne détient pas le profil de la fonction.')
        return p
      })
      const remplacer = (x: Personne | null) => personnes.find((p) => p.fonction === x?.fonction) ?? x
      e.ops.filter((x) => x.etat !== 'FAITE').forEach((x) => {
        x.raci = { R: remplacer(x.raci.R), V: remplacer(x.raci.V), C: x.raci.C.map((y) => remplacer(y)!), I: x.raci.I.map((y) => remplacer(y)!), E: x.raci.E }
      })
      const contenu = personnes.map((p) => `${p.fonction}=${p.id}`).join(';')
      terminer(e, o, { documentRef: `AFF-${id.slice(-3)}`, nom: 'designation-ressources', empreinte: hex(`aff-${id}-${contenu}`), deposeLe: jour(0), deposePar: u.utilisateur?.nom ?? personne('CHEF_DES').nom })
    }),
    deposerLivrable: (opId, fichier, commentaire, livrable, service) => attendre(() => {
      const { e, o } = operation(opId)
      refuserGesteLibre(o)
      if (!o.actionsPermises.includes('DEPOSER_LIVRABLE') && !o.actionsPermises.includes('DEPOSER_PREUVE')) throw refus(409, 'HOM_OPERATION_NOT_OPEN', 'Cette opération n\'attend pas de livrable.')
      const auteur = exigerDroitDepot(o)
      const piece = { documentRef: `DOC-${opId.replace(':', '-')}${livrable ? `-${livrable}` : ''}${service ? `-${service}` : ''}`, nom: fichier.name, empreinte: hex(`${opId}|${fichier.name}|${fichier.size}|${fichier.lastModified}|${commentaire}`), deposeLe: jour(0), deposePar: auteur }
      inscrire(o, piece, livrable, service, auteur)
      // D-109 : un livrable visé parmi plusieurs ; l'opération reste en cours tant qu'un livrable obligatoire manque.
      if (livrable && manquants(e, o).length) {
        o.etat = 'EN_COURS'
        o.preuve = piece
        return
      }
      terminer(e, o, piece)
    }),
    attribuer: (opId, qualite, personneId) => attendre(() => {
      const { o } = operation(opId)
      const p = PERSONNES.find((x) => x.id === personneId)
      if (!p) throw refus(422, 'HOM_PERSON_UNKNOWN', 'Personne inconnue.')
      if (qualite === 'R') o.raci.R = p
      else if (qualite === 'V') {
        if (o.raci.R?.id === p.id) throw refus(409, 'HOM_VALIDATOR_IS_RESPONSIBLE', 'Le validateur doit être distinct du responsable.')
        o.raci.V = p
      } else o.raci[qualite] = [...o.raci[qualite], p]
    }),
    deposerPreuve: (opId, fichier, c) => attendre(() => {
      const { e, o } = operation(opId)
      refuserGesteLibre(o)
      if (o.etat !== 'A_FAIRE' && o.etat !== 'EN_COURS') throw refus(409, 'HOM_OPERATION_NOT_OPEN', 'Cette opération n\'est pas ouverte.')
      const kcv = /^[0-9A-F]{4,8}$/
      if (o.rang === 'C1') {
        const cer = c.ceremonie
        if (!cer || !cer.tenueLe || cer.porteurs.filter((x) => x.trim()).length !== 3 || cer.codesControle.length !== 3) throw refus(422, 'HOM_KEY_CEREMONY_INCOMPLETE', 'La cérémonie exige sa date de tenue, les trois porteurs de composantes et leurs trois codes de contrôle.')
        if (!cer.codesControle.every((x) => kcv.test(x))) throw refus(422, 'HOM_KEY_CHECK_VALUE_INVALID', 'Un code de contrôle compte de 4 à 8 caractères hexadécimaux.')
      } else if (o.cle && !c.codeControle) throw refus(422, 'HOM_KEY_CHECK_VALUE_REQUIRED', 'Le code de contrôle de la clé est exigé ; aucune valeur de clé n\'est admise.')
      if (o.cle && c.codeControle && !/^[0-9A-F]{4,8}$/.test(c.codeControle)) throw refus(422, 'HOM_KEY_CHECK_VALUE_INVALID', 'Un code de contrôle compte de 4 à 8 caractères hexadécimaux.')
      if (o.valeurReference && !c.valeurReference) throw refus(422, 'HOM_REFERENCE_VALUE_REQUIRED', 'Cette preuve doit porter la valeur de référence relevée.')
      const auteur = exigerDroitDepot(o)
      o.preuve = { documentRef: `DOC-${opId.replace(':', '-')}`, nom: fichier.name, empreinte: hex(`${opId}|${fichier.name}|${fichier.size}|${fichier.lastModified}`), deposeLe: jour(0), deposePar: auteur, valeurReference: c.valeurReference ? { libelle: `parcours.valeur.${o.rang}`, valeur: c.valeurReference } : undefined, codeControle: o.cle ? c.codeControle : undefined, ceremonie: o.rang === 'C1' ? c.ceremonie : undefined }
      inscrire(o, o.preuve, c.livrable, c.service, auteur)
      // D-109 : un livrable visé parmi plusieurs ; l'opération reste en cours tant qu'un livrable obligatoire manque.
      if (c.livrable && manquants(e, o).length) {
        o.etat = 'EN_COURS'
        return
      }
      o.etat = 'EN_VALIDATION'
      o.actionsPermises = ['VALIDER', 'RENVOYER', 'ATTRIBUER']
      // HOM-16 : validation demandée, notifiée au validateur (homologation.operation-validation-requested.v1).
      consigner(e, o, 'VALIDATION')
    }),
    // D-109 : livrable sans objet, motivé, tracé ; l'opération passe en validation quand plus rien ne manque.
    declarerSansObjet: (opId, livrable, motif, service) => attendre(() => {
      const { e, o } = operation(opId)
      if (o.etat !== 'A_FAIRE' && o.etat !== 'EN_COURS' && o.etat !== 'EN_VALIDATION') throw refus(409, 'HOM_OPERATION_NOT_OPEN', 'Cette opération n\'est pas ouverte.')
      const m = motif.trim()
      if (m.length < 3 || m.length > 500) throw refus(422, 'HOM_MOTIF_REQUIS', 'Le motif est exigé (3 à 500 caractères).')
      const attendus = livrablesAttendusDemo(o, e.s.typeProjet, servicesInclus(e), depotsDemo.get(o.id) ?? [], false)
      if (!attendus.some((l) => l.code === livrable)) throw refus(422, 'HOM_LIVRABLE_INCONNU', 'Ce livrable n\'est pas attendu sur cette opération.')
      const auteur = exigerDroitDepot(o)
      const l = (depotsDemo.get(o.id) ?? []).filter((x) => !(x.livrable === livrable && (x.service ?? null) === (service ?? null)))
      depotsDemo.set(o.id, [...l, { livrable, deposeLe: jour(0), deposePar: auteur, service: service ?? null, sansObjet: true, motif: m }])
      if ((o.etat === 'A_FAIRE' || o.etat === 'EN_COURS') && !manquants(e, o).length) {
        o.etat = 'EN_VALIDATION'
        o.actionsPermises = ['VALIDER', 'RENVOYER', 'ATTRIBUER']
      }
    }),
    // D-109 : pièce de démonstration, empreinte inscrite au dépôt.
    telechargerPiece: (opId, documentId) => attendre(() => {
      const { o } = operation(opId)
      const d = [...(depotsDemo.get(opId) ?? []), ...(o.preuve ? [o.preuve] : [])].find((x) => x.documentRef === documentId)
      if (!d) throw refus(404, 'HOM_PIECE_INCONNUE', 'Cette pièce n\'est pas rattachée à l\'opération.')
      return { blob: new Blob([`Pièce de démonstration ${d.nom ?? documentId}`], { type: 'application/pdf' }), empreinte: d.empreinte ?? null }
    }),
    chronologie: (id) => attendre(() => {
      const e = trouver(id)
      return chronologieDemo(e.ops, e.blocages, depotsDemo)
    }),
    // US-EXI-17 : résultats de la sandbox repris (lecture seule), au contrat de MS-HOMOLOGATION.
    resultatsSandbox: (id) => attendre(() => resultatsSandboxDemo(trouver(id).s)),
    executionsSandbox: (id, campagneId, page, taille) => attendre(() => executionsSandboxDemo(trouver(id).s, campagneId, page, taille)),
    repartitionSandbox: (id, campagneId) => attendre(() => repartitionSandboxDemo(trouver(id).s, campagneId)),
    validerOperation: (opId) => attendre(() => {
      const { e, o } = operation(opId)
      if (o.etat !== 'EN_VALIDATION') throw refus(409, 'HOM_OPERATION_NOT_IN_VALIDATION', 'L\'opération n\'attend pas de validation.')
      // HOM-03 : RACI imposé, comme ParcoursCommandes.valider (exigerValidateur).
      exigerValidateur(o)
      terminer(e, o, o.preuve)
    }),
    // D-93 §8.3 : renvoi motivé, l'opération revient à faire (bloquée si un blocage est actif), la pièce est gardée.
    renvoyer: (opId, motif) => attendre(() => {
      const { e, o } = operation(opId)
      if (!motif.trim() || motif.length > 500) throw refus(422, 'HOM_MOTIF_REQUIS', 'Un motif (1 à 500 caractères) est exigé.')
      if (o.etat !== 'EN_VALIDATION') throw refus(409, 'HOM_OPERATION_NOT_IN_VALIDATION', 'L\'opération n\'attend pas de validation.')
      exigerValidateur(o)
      const bloquee = e.blocages.some((b) => !b.leveLe)
      o.etat = bloquee ? 'BLOQUEE' : 'A_FAIRE'
      o.renvois = [...(o.renvois ?? []), { motif: motif.trim(), parId: o.raci.V?.id ?? 'U-V', parNom: o.raci.V?.nom ?? 'Validateur', le: horodatage() }]
      o.actionsPermises = bloquee ? ['ATTRIBUER'] : actionsOuvertes(o)
      consigner(e, o, 'RENVOYEE', motif.trim())
    }),
    // D-91 §5.4 : signature d'une opération, dans l'ordre, par le porteur de la fonction (clé de sécurité exigée).
    signerOperation: (id, code, signature, assertion) => attendre(() => {
      const e = trouver(id)
      const o = e.ops.find((x) => x.rang === code)
      if (!o) throw refus(404, 'HOM_OPERATION_NOT_FOUND', 'Opération introuvable.')
      const x = (o.signatures ?? []).find((y) => y.code === signature)
      if (!x) throw refus(404, 'HOM_SIGNATURE_INCONNUE', 'Signature inconnue.')
      if (x.etat === 'SIGNEE') throw refus(409, 'HOM_SIGNATURE_DEJA_SIGNEE', 'Cette signature est déjà donnée.')
      if (o.etat !== 'EN_SIGNATURE') throw new ApiError(409, 'HOM_SIGNATURE_ETAT', 'L\'opération n\'attend pas de signature.', correlation(), { etat: o.etat, attendu: 'EN_SIGNATURE' })
      if (!suivantes(o).includes(x)) throw new ApiError(409, 'HOM_SIGNATURE_ORDRE', 'Une signature d\'un ordre inférieur est attendue d\'abord.', correlation(), { attendues: suivantes(o).map((y) => y.code) })
      // Signataire admis (OperationsLiaison.admetSignataire) : porteur de la fonction ; sans identité (tests du jeu seul), admis.
      if (!admetSignataire(x.signataire)) throw refus(403, 'HOM_SIGNATURE_NON_SIGNATAIRE', 'Vous ne tenez pas la fonction signataire attendue.')
      if (!assertion) throw refus(428, 'IAM_REAUTH_REQUIRED', 'Réauthentification exigée.')
      // Signataire de la liaison (le conducteur désigné pour CONDUCTEUR), horloge de la démonstration.
      const qui = titulaire(e, x.signataire)
      Object.assign(x, { etat: 'SIGNEE', signePar: qui.id, signeParNom: qui.nom, signeLe: horodatage(), empreinte: hex(`${o.id}-${x.code}`) })
      if ((o.signatures ?? []).every((y) => y.etat === 'SIGNEE')) {
        o.etat = 'A_FAIRE'
        terminer(e, o, o.preuve)
      }
    }),
    // D-93 §8.1 : affectation d'une fonction de la liaison, sur les opérations non faites.
    affecterFonction: (id, fonction, a) => attendre(() => {
      const e = trouver(id)
      const f = catalogueFonctions().find((x) => x.code === fonction)
      if (!f || fonction === 'PARTICIPANT' || fonction === 'CONDUCTEUR') throw refus(422, 'HOM_FONCTION_NON_AFFECTABLE', 'Cette fonction ne s\'affecte pas ainsi.')
      if (f.externe && !a.institutionId) throw refus(422, 'HOM_INSTITUTION_REQUISE', 'Une fonction externe est tenue par une institution.')
      const p = f.externe ? P(`EXT-${a.institutionId}`, a.institutionId!, fonction) : PERSONNES.find((x) => x.id === a.personneId)
      if (!p) throw refus(422, 'HOM_PERSON_UNKNOWN', 'Personne inconnue.')
      if (p.fonction !== fonction) throw refus(422, 'HOM_PERSON_NOT_ELIGIBLE', 'Cette personne ne détient pas le profil de la fonction.')
      const remplacer = (x: Personne | null) => (x?.fonction === fonction ? p : x)
      e.ops.filter((o) => o.etat !== 'FAITE').forEach((o) => {
        o.raci = { R: remplacer(o.raci.R), V: remplacer(o.raci.V), C: o.raci.C.map((x) => remplacer(x)!), I: o.raci.I.map((x) => remplacer(x)!), E: o.raci.E.map((x) => remplacer(x)!) }
      })
    }),
    mesFonctions: () => attendre(mesFonctions),
    declarerBlocage: (id, b) => attendre(() => {
      const e = trouver(id)
      const resp = PERSONNES.find((p) => p.id === b.responsableId) ?? personne('CONDUCTEUR')
      const blocage: Blocage = { id: `BLQ-${String(100 + e.blocages.length)}`, liaisonId: id, motif: b.motif, depuis: jour(0), prochaineAction: b.prochaineAction, responsable: resp, echeanceLevee: b.echeanceLevee, leveLe: null, etape: etapeCourante(e), transitionsPermises: [], actionsPermises: ['LEVER'] }
      e.blocages.push(blocage)
      return blocage
    }),
    leverBlocage: (bid) => attendre(() => {
      const e = tous().find((x) => x.blocages.some((b) => b.id === bid))
      const b = e?.blocages.find((x) => x.id === bid)
      if (!e || !b) throw refus(404, 'HOM_BLOCKAGE_NOT_FOUND', 'Blocage introuvable.')
      b.leveLe = jour(0)
      b.actionsPermises = []
      e.ops.filter((o) => o.etat === 'BLOQUEE').forEach((o) => { o.etat = 'A_FAIRE'; o.actionsPermises = actionsOuvertes(o) })
    }),
    constaterCouverture: (id) => attendre(() => {
      const e = trouver(id)
      // D-98 : l'opération du constat est celle qui porte le geste CONSTATER_COUVERTURE.
      const o = e.ops.find((x) => x.geste === 'CONSTATER_COUVERTURE' && (x.etat === 'A_FAIRE' || x.etat === 'EN_COURS')) ?? e.ops.find((x) => x.geste === 'CONSTATER_COUVERTURE')
      if (!o || (o.etat !== 'A_FAIRE' && o.etat !== 'EN_COURS')) throw refus(409, 'HOM_COVERAGE_NOT_READY', 'Les essais ne sont pas terminés : la couverture ne peut pas être constatée.')
      // Essais achevés : tous les services inclus sont certifiés, la couverture est complète.
      if (e.s.depuisVersion) {
        const inclus = e.s.services.filter((x) => x.inclus)
        e.pv = { ...e.pv, couverture: couv(inclus.length, inclus.length), servicesCouverts: inclus.map((x) => x.code) }
      }
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
      // Le participant ne voit que les liaisons de son institution : tâches, blocages et liaisons compris.
      const visibles = f('PARTICIPANT') ? actives.filter((e) => e.s.institutionId === institutionPortail) : actives
      const taches = tachesDe(visibles, fonctions)
      const compteurs: TableauActeur['compteurs'] = []
      if (f('CHEF_DDRM') || f('CHEF_DES')) compteurs.push(compteur('conducteursADesigner', actives.filter((e) => !e.conducteur).length, 'warn', '/des/parcours?sansConducteur=1'))
      if (f('CHEF_DDRM')) compteurs.push(compteur('liaisonsAmont', aEtape(['CADRAGE', 'LANCEMENT', 'ATELIERS']).length, 'info', '/des/parcours?etape=CADRAGE,LANCEMENT,ATELIERS'))
      if (f('DDRM') || f('CHEF_DDRM')) compteurs.push(compteur('ateliersATenir', opsOuvertes(actives, (o) => o.rang === '8' && o.etat === 'A_FAIRE').length, 'warn', versOps(opsOuvertes(actives, (o) => o.rang === '8' && o.etat === 'A_FAIRE'), 'ATELIERS')), compteur('perimetresAArreter', opsOuvertes(actives, (o) => ['3', '4', '5'].includes(o.rang) && o.etat === 'A_FAIRE').length, 'warn', versOps(opsOuvertes(actives, (o) => ['3', '4', '5'].includes(o.rang) && o.etat === 'A_FAIRE'), 'CADRAGE')))
      if (f('CHEF_DES')) compteurs.push(compteur('ressourcesAAffecter', opsOuvertes(actives, (o) => o.rang === '6' && o.etat === 'A_FAIRE').length, 'warn', versLiaisons(actives.filter((e) => e.ops.some((o) => o.rang === '6' && o.etat === 'A_FAIRE')), 'etapes', 'CADRAGE,LANCEMENT')), compteur('enIntegrationTests', aEtape(['INTEGRATION', 'TESTS']).length, 'info', '/des/parcours?etape=INTEGRATION,TESTS'), compteur('pvASigner', actives.filter((e) => e.pv.etat === 'ETABLI' || e.pv.etat === 'SIGNE_PARTICIPANT').length, 'warn', versLiaisons(actives.filter((e) => e.pv.etat === 'ETABLI' || e.pv.etat === 'SIGNE_PARTICIPANT'), 'decision', 'CERTIFICATION')))
      if (f('CONDUCTEUR')) compteurs.push(compteur('mesProjets', actives.filter((e) => e.conducteur?.id === personne('CONDUCTEUR', 0).id).length, 'info', '/des/parcours?mesProjets=1'), compteur('pvAEtablir', actives.filter((e) => e.ops.some((o) => o.rang === '18' && o.etat === 'A_FAIRE')).length, 'warn', versLiaisons(actives.filter((e) => e.ops.some((o) => o.rang === '18' && o.etat === 'A_FAIRE')), 'decision', 'CERTIFICATION')))
      if (f('SNA')) {
        const o15 = opsOuvertes(actives, (o) => o.rang === '15' && o.etat !== 'FAITE' && o.etat !== 'VERROUILLEE')
        const o17 = opsOuvertes(actives, (o) => o.rang === '17' && o.etat === 'A_FAIRE')
        const o16 = opsOuvertes(actives, (o) => o.rang === '16' && o.etat !== 'FAITE' && o.etat !== 'VERROUILLEE')
        const constats = actives.filter((e) => e.fiche.etat === 'RENSEIGNEE')
        compteurs.push(compteur('essaisEnCours', aEtape(['TESTS']).length, 'info', versOps(o16, 'TESTS')), compteur('documentsParametrage', o15.length, 'warn', versOps(o15, 'INTEGRATION')), compteur('couverturesAConstater', o17.length, 'warn', versOps(o17, 'CERTIFICATION')), compteur('constatsMiseEnService', constats.length, 'warn', constats.length === 1 ? `/des/parcours/${constats[0].s.id}?onglet=mise-en-service` : '/des/parcours?etape=SUIVI'))
      }
      if (f('PRODUCTION_MONETIQUE')) {
        const dossiers = actives.filter((e) => e.approbation.etat === 'EN_ATTENTE_DOSSIER' && e.pv.etat === 'SIGNE')
        const branche = opsOuvertes(actives, (o) => o.segment === 'BRANCHE' && o.raci.R?.fonction === 'PRODUCTION_MONETIQUE' && o.etat === 'A_FAIRE')
        const o31 = opsOuvertes(actives, (o) => o.rang === '31' && o.etat === 'A_FAIRE')
        compteurs.push(compteur('dossiersChangement', dossiers.length, 'warn', dossiers.length === 1 ? `/des/parcours/${dossiers[0].s.id}?onglet=decision` : '/des/parcours?etape=APPROBATION_SECURITE'), compteur('operationsBranche', branche.length, 'info', versOps(branche, 'MISE_EN_PRODUCTION')), compteur('rapportsAConsolider', o31.length, 'warn', versOps(o31, 'MISE_EN_PRODUCTION')))
      }
      if (f('POLE_RESEAU')) {
        const liens = opsOuvertes(actives, (o) => ['13', '14'].includes(o.rang) && o.etat !== 'FAITE' && o.etat !== 'VERROUILLEE')
        compteurs.push(compteur('liensEssai', liens.length, 'warn', versOps(liens, 'INTEGRATION')), compteur('operationsReseau', taches.length, 'info', taches.length === 1 ? taches[0].lien : null))
      }
      if (f('POLE_SYSTEME')) {
        const o28 = opsOuvertes(actives, (o) => o.rang === '28' && o.etat !== 'FAITE' && o.etat !== 'VERROUILLEE')
        compteurs.push(compteur('scriptsDepot', o28.length, 'crit', versOps(o28, 'MISE_EN_PRODUCTION')), compteur('operationsSysteme', taches.length, 'info', taches.length === 1 ? taches[0].lien : null))
      }
      if (f('RSSI')) compteurs.push(compteur('attestationsAVerifier', actives.flatMap((e) => e.approbation.attestations.filter((a) => a.preuve && !a.verifieeLe)).length, 'warn'), compteur('approbationsEnAttente', actives.filter((e) => ['DOSSIER_CONSTITUE', 'EN_VERIFICATION', 'VERIFIE_ARCHIVE'].includes(e.approbation.etat)).length, 'warn'), compteur('ceremoniesCles', actives.flatMap((e) => e.ops.filter((o) => (o.rang === 'C1' || o.rang === 'D3') && o.etat === 'A_FAIRE')).length, 'warn'))
      // Visas attendus : lien vers l'onglet Décision de la liaison s'il n'y en a qu'une, sinon la liste de l'étape (comme MS-HOMOLOGATION).
      const visas = (nature: 'CONFORMITE' | 'DIRECTION_GENERALE') => actives.filter((e) => e.approbation.visas.some((v) => v.nature === nature && v.actif && v.etat === 'ATTENDU'))
      if (f('CONFORMITE')) compteurs.push(compteur('visasAttendus', visas('CONFORMITE').length, 'warn', versLiaisons(visas('CONFORMITE'), 'decision', 'APPROBATION_SECURITE')), compteur('fintechsCobac', actives.filter((e) => e.approbation.fintech).length, 'info'))
      if (f('DG')) compteurs.push(compteur('visasAttendus', visas('DIRECTION_GENERALE').length, 'warn', versLiaisons(visas('DIRECTION_GENERALE'), 'decision', 'APPROBATION_SECURITE')))
      if (f('PARTICIPANT')) {
        const siennes = visibles
        compteurs.push(compteur('tachesARealiser', taches.length, 'warn'), compteur('pvASigner', siennes.filter((e) => e.pv.etat === 'ETABLI' || e.pv.etat === 'SIGNE_GIMAC').length, 'warn'), compteur('attestationsAFournir', siennes.flatMap((e) => e.approbation.attestations.filter((a) => !a.preuve && e.pv.etat === 'SIGNE')).length, 'warn'), compteur('fichesSuivi', siennes.filter((e) => e.fiche.etat === 'REMISE').length, 'warn'))
      }
      compteurs.push(compteur('tachesEnRetard', taches.filter((t) => t.sla === 'DEPASSE').length, 'crit'), compteur('echeancesProches', taches.filter((t) => t.sla === 'ECHEANCE_PROCHE').length, 'warn'))
      const liaisons = (f('PARTICIPANT') ? visibles : f('CONDUCTEUR') && fonctions.length === 1 ? actives.filter((e) => e.conducteur?.id === personne('CONDUCTEUR', 0).id) : actives).map(resume)
      return { fonction: fonctions[0] ?? 'DDRM', compteurs, taches: taches.sort((a, b) => (a.echeance ?? '9').localeCompare(b.echeance ?? '9')), liaisons, blocages: visibles.flatMap((e) => e.blocages.filter((b) => !b.leveLe)) }
    }),
    // D-98 : mes actions de certification : tâches de mes fonctions (même calcul que le tableau d'acteur), opérations
    // engagées des liaisons que je conduis, état et jalons de ces liaisons.
    mesOperations: () => attendre((): MesOperations => {
      const miens = codesMiens()
      const actives = tous().filter((e) => e.s.statut !== 'CLOSE_SANS_SUITE')
      const taches = tachesDe(actives, miens)
      const mon = (e: Etat, o: OperationParcours, t?: TacheActeur): MonOperation => {
        const v = vueOperation(o)
        return {
          operationId: o.id, liaisonId: e.s.id, liaisonReference: e.s.id, institutionNom: e.s.institutionNom, statutLiaison: e.s.statut, code: o.rang, libelle: t?.libelle ?? o.libelle ?? o.rang,
          etape: o.etape, etapeCode: o.etapeCode ?? o.etape, etat: o.etat, qualite: t?.signature ? null : t?.qualite ?? null, aPrendre: t?.aPrendre ?? null, signature: t?.signature ?? null, geste: o.geste ?? null,
          jalon: o.jalon ?? null, declencheur: o.declencheur ?? null, vue: o.vue ?? null, responsable: o.raci.R, validateur: o.raci.V, echeance: o.echeance, sla: o.sla,
          lien: lienOperation(o), actionsPermises: t ? v.actionsPermises : [],
        }
      }
      const aAgir = taches.map((t) => { const { e, o } = operation(t.operationId); return mon(e, o, t) })
      const faire = new Set(aAgir.map((x) => x.operationId))
      const moi = personne('CONDUCTEUR', 0).id
      const conduites = miens.includes('CONDUCTEUR') ? actives.filter((e) => e.conducteur?.id === moi) : []
      const aSuivre = conduites.flatMap((e) => e.ops.filter((o) => !faire.has(o.id) && ['A_FAIRE', 'EN_COURS', 'EN_VALIDATION', 'EN_SIGNATURE', 'BLOQUEE'].includes(o.etat)).map((o) => mon(e, o)))
      const suivies = [...conduites, ...actives.filter((e) => !conduites.includes(e) && aAgir.some((x) => x.liaisonId === e.s.id))]
      const liaisons = suivies.map((e) => ({
        liaisonId: e.s.id, reference: e.s.id, institutionNom: e.s.institutionNom, typeProjet: e.s.typeProjet, statut: e.s.statut, etapeCourante: etapeCourante(e), conducteur: conduites.includes(e),
        operationsFaites: e.ops.filter((o) => o.etat === 'FAITE').length, operationsTotal: e.ops.length, lien: `/des/parcours/${e.s.id}`,
        jalons: e.ops.filter((o) => o.jalon).map((o) => ({ jalon: o.jalon!, atteint: o.etat === 'FAITE', atteintLe: o.etat === 'FAITE' ? o.faiteLe : null, operationId: o.id })),
      })).sort((a, b) => a.reference.localeCompare(b.reference))
      return { aAgir, aSuivre, liaisons }
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
          projetsBloques: actives.flatMap((e) => e.blocages.filter((b) => !b.leveLe).map((b) => ({ liaisonId: e.s.id, reference: e.s.id, institutionNom: e.s.institutionNom, motif: b.motif, depuis: b.depuis, jours: joursEntre(b.depuis, aujourdhuiDemo().toISOString()) }))),
          lancementsParService12Mois: tri.map(([serviceCode, v]) => ({ serviceCode, libelle: v.libelle, nombre: v.nombre })),
        },
      }
    }),
    personnes: (fonction) => attendre(PERSONNES.filter((p) => p.fonction === fonction)),
  }

  const decision: ServiceDecision = {
    procesVerbal: (id) => attendre(() => {
      const e = trouver(id)
      const o17 = e.ops.find((o) => o.geste === 'CONSTATER_COUVERTURE')
      const o18 = declenchee(e, 'certification.pv-established.v1')
      const actions: string[] = []
      if (e.pv.etat === 'NON_ETABLI' && o17?.etat === 'FAITE' && o18?.etat === 'A_FAIRE') actions.push(e.pv.couverture.taux === 100 ? 'ETABLIR' : 'ETABLIR_BLOQUE')
      // D-94 : une signature du DES du circuit est signable maintenant par la personne (ordre, opération ouverte, signataire).
      if (peutSignerPv(e, false)) actions.push('SIGNER_GIMAC')
      return { ...e.pv, circuit: vueCircuit(e, false), actionsPermises: actions }
    }),
    etablirProcesVerbal: (id) => attendre(() => {
      const e = trouver(id)
      if (e.pv.couverture.taux !== 100) throw refus(409, 'CER_COVERAGE_INCOMPLETE', `La couverture de certification est de ${e.pv.couverture.taux ?? 0} % : le procès-verbal exige 100 %.`)
      const o = declenchee(e, 'certification.pv-established.v1')
      if (!o || o.etat !== 'A_FAIRE') throw refus(409, 'CER_PV_NOT_READY', 'Le constat de la couverture doit précéder le procès-verbal.')
      e.pv = { ...e.pv, etat: 'ETABLI', etabliPar: e.conducteur?.nom ?? null, etabliLe: jour(0), empreinte: hex(`pv-${id}-${Date.now()}`) }
      terminer(e, o, { documentRef: `PV-${id.slice(-3)}`, nom: `proces-verbal-certification-${id}.pdf`, empreinte: e.pv.empreinte ?? '', deposeLe: jour(0), deposePar: e.conducteur?.nom ?? '' })
    }),
    // D-94 : prochaine signature du circuit que la personne tient, dans l'ordre (CER_SIGNATURE_ORDRE, CER_SIGNATAIRE_NON_DESIGNE).
    signerProcesVerbalGimac: (id, assertion) => attendre(() => {
      if (!assertion) throw refus(401, 'IAM_REAUTH_REQUIRED', 'Réauthentification exigée.')
      const e = trouver(id)
      if (e.pv.etat === 'NON_ETABLI') throw refus(409, 'CER_PV_NOT_SIGNABLE', 'Le procès-verbal n\'est pas établi.')
      signerPv(e, false)
    }),
    approbation: (id) => attendre(() => {
      const e = trouver(id)
      const a = e.approbation
      const actions: string[] = []
      // Comme ApprobationPolicy.actionsDes : aucun geste une fois l'approbation close (approuvée ou refusée).
      const ouverte = e.pv.etat === 'SIGNE' && a.etat !== 'APPROUVEE' && a.etat !== 'REFUSEE'
      const archivee = a.etat === 'VERIFIE_ARCHIVE' || (a.attestations.length > 0 && a.attestations.every((x) => x.archiveeLe))
      if (ouverte && a.etat === 'EN_ATTENTE_DOSSIER') actions.push('CONSTITUER_DOSSIER')
      if (ouverte && !archivee && a.attestations.some((x) => x.preuve && !x.verifieeLe)) actions.push('VERIFIER_ATTESTATION')
      if (ouverte && !archivee && a.attestations.length && a.attestations.every((x) => x.verifieeLe)) actions.push('ARCHIVER')
      if (ouverte && a.dossierChangement && !a.ficheChangement) actions.push('REFERENCER_FICHE')
      if (ouverte && a.ficheChangement) actions.push('RELIRE_FICHE')
      // Visas optionnels (D-56) : signables avant l'approbation de la sécurité, une fois le procès-verbal signé, sur
      // l'opération ouverte qui les porte (CER-02, comme ApprobationSecuriteService.actionsDes).
      if (ouverte && ouvertesSansRefus(e, 'certification.security-approved.v1')) {
        a.visas.filter((v) => v.actif && v.etat === 'ATTENDU').forEach((v) => actions.push(v.nature === 'CONFORMITE' ? 'SIGNER_VISA_CONFORMITE' : 'SIGNER_VISA_DG'))
      }
      return { ...a, actionsPermises: actions }
    }),
    constituerDossierChangement: (id, fichier) => attendre(() => {
      const e = trouver(id)
      if (e.pv.etat !== 'SIGNE') throw refus(409, 'CER_PV_NOT_SIGNED', 'Le procès-verbal de certification doit être signé des deux parties.')
      // Aucune attestation exigée (modèle générique) : rien à vérifier ni à archiver avant la relecture de la fiche.
      e.approbation = { ...e.approbation, etat: e.approbation.attestations.length ? 'DOSSIER_CONSTITUE' : 'VERIFIE_ARCHIVE', dossierChangement: { documentRef: `CHG-DOS-${id.slice(-3)}`, nom: fichier.name, empreinte: hex(fichier.name + fichier.size), deposeLe: jour(0), deposePar: personne('PRODUCTION_MONETIQUE').nom } }
      const o = declenchee(e, 'certification.change-record-constituted.v1')
      if (o && o.etat !== 'FAITE') terminer(e, o, e.approbation.dossierChangement)
    }),
    referencerFicheChangement: (id, reference) => attendre(() => {
      if (!/^[A-Z]{2,5}-\d{4}-\d{1,6}$/.test(reference)) throw refus(422, 'CER_CHANGE_RECORD_INVALID', 'Référence de fiche de changement invalide (exemple : CHG-2026-0412).')
      const e = trouver(id)
      e.approbation = { ...e.approbation, ficheChangement: { reference, etatApprobation: 'EN_EXAMEN', lueLe: jour(0), aRelire: false } }
      const o = declenchee(e, 'certification.change-record-referenced.v1')
      if (o && o.etat === 'A_FAIRE') terminer(e, o, { documentRef: reference, nom: reference, empreinte: hex(reference), deposeLe: jour(0), deposePar: personne('PRODUCTION_MONETIQUE').nom })
      // D-98 : fiche référencée par le responsable, l'approbation attend la relecture de son validateur (sécurité).
      if (e.s.depuisVersion) declenchees(e, 'certification.security-approved.v1').filter((x) => x.etat === 'A_FAIRE' && x.raci.V).forEach((x) => { x.etat = 'EN_VALIDATION'; x.actionsPermises = ['ATTRIBUER'] })
    }),
    relireFicheChangement: (id) => attendre(() => {
      const e = trouver(id)
      const f = e.approbation.ficheChangement
      if (!f) throw refus(409, 'CER_CHANGE_RECORD_MISSING', 'Aucune fiche de changement n\'est référencée.')
      // La fiche est lue, jamais refaite (D-56) : l'approbation vient du dispositif de gestion des changements.
      // Les visas activés s'insèrent avant l'approbation (D-56) : tant qu'un visa actif n'est pas favorable, pas d'approbation.
      const visasOk = e.approbation.visas.every((v) => !v.actif || v.etat === 'SIGNE_FAVORABLE')
      const approuvee = e.approbation.etat === 'VERIFIE_ARCHIVE' && visasOk
      e.approbation = { ...e.approbation, ficheChangement: { ...f, lueLe: jour(0), aRelire: false, etatApprobation: approuvee ? 'APPROUVEE' : f.etatApprobation }, etat: approuvee ? 'APPROUVEE' : e.approbation.etat, approuveeLe: approuvee ? jour(0) : e.approbation.approuveeLe }
      if (approuvee) {
        e.s.statut = 'APPROUVEE_SECURITE'
        ;declenchees(e, 'certification.security-approved.v1').forEach((o) => { const r = o.rang; if (o.etat !== 'FAITE') { o.etat = 'A_FAIRE'; terminer(e, o, { documentRef: `SSI-${r}`, nom: `approbation-${r}`, empreinte: hex(`ssi-${id}-${r}`), deposeLe: jour(0), deposePar: personne('RSSI').nom }) } })
        // Rang 26 ouvert après l'approbation : la fiche, déjà référencée et lue, en est la preuve (comme MS-HOMOLOGATION).
        const o26 = declenchee(e, 'certification.change-record-referenced.v1')
        if (o26 && o26.etat === 'A_FAIRE') terminer(e, o26, { documentRef: f.reference, nom: f.reference, empreinte: hex(f.reference), deposeLe: jour(0), deposePar: personne('PRODUCTION_MONETIQUE').nom })
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
      // Rangs 21 et 22 (pièces du participant) : clos par l'archivage des attestations vérifiées, qui en sont la preuve.
      const piece = e.approbation.attestations[0]?.preuve
      ;declenchees(e, 'certification.attestations-archived.v1').forEach((o) => { const r = o.rang; if (o.etat !== 'FAITE') { o.etat = 'A_FAIRE'; terminer(e, o, piece ?? { documentRef: `ATT-${r}`, nom: `attestations-${r}`, empreinte: hex(`att-${id}-${r}`), deposeLe: jour(0), deposePar: personne('RSSI').nom }) } })
    }),
    signerVisa: (id, nature, favorable, motif, assertion) => attendre(() => {
      if (!assertion) throw refus(401, 'IAM_REAUTH_REQUIRED', 'Réauthentification exigée.')
      const v = trouver(id).approbation.visas.find((x) => x.nature === nature)
      if (!v?.actif) throw refus(409, 'CER_VISA_NOT_REQUIRED', 'Ce visa n\'est pas requis pour ce type de projet.')
      if (v.etat !== 'ATTENDU') throw refus(409, 'CER_VISA_ALREADY_SIGNED', 'Ce visa est déjà signé : une signature est immuable.')
      if (!favorable && !motif.trim()) throw refus(422, 'CER_REJECTION_REASON_REQUIRED', 'Un avis défavorable doit être motivé.')
      const e = trouver(id)
      if (e.approbation.etat === 'APPROUVEE' || e.approbation.etat === 'REFUSEE') throw refus(409, 'CER_SECURITY_APPROVAL_CLOSED', 'L\'approbation de la sécurité est close.')
      // Comme ApprobationSecuriteService.signerVisa : opération de l'approbation ouverte, signataire désigné du circuit du visa.
      ouvertesGeste(e, 'certification.security-approved.v1', 'Signer un visa')
      const fonction: Fonction = nature === 'CONFORMITE' ? 'CONFORMITE' : 'DG'
      if (!admetSignataire(fonction)) throw new ApiError(403, 'CER_SIGNATAIRE_NON_DESIGNE', `Vous ne tenez pas la fonction signataire attendue : ${nature === 'CONFORMITE' ? 'Visa de la conformité' : 'Visa de la Direction Générale'}.`, correlation(), { signature: nature, fonction })
      Object.assign(v, { etat: favorable ? 'SIGNE_FAVORABLE' : 'SIGNE_DEFAVORABLE', motif, signature: { signataire: personne(fonction).nom, fonction, signeLe: jour(0), horodatage: horodatage() } })
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
  const coordonneesCommuniquees = new Map<string, string | null>()
  const portailParcours: ServicePortailParcours = {
    // D-104 : toutes les liaisons de l'institution du jeton ; la référence d'origine (reprise) reste interne au GIMAC.
    mesLiaisons: () => attendre(() => tous().filter((e) => e.s.institutionId === institutionPortail).map((e) => {
      const r = resume(e)
      delete r.referenceOrigine
      return r
    })),
    dossier: (id) => attendre(() => {
      const d = dossierDe(sienne(id))
      // Projection du portail : aucune donnée interne (responsables internes nommés par leur seule fonction).
      const parFonction = <T extends Personne | null>(p: T): T => (p ? { ...p, id: p.fonction, nom: p.fonction } : p)
      const o12 = sienne(id).ops.find((o) => o.rang === '12')
      const valeur = o12?.etat === 'FAITE' ? o12.preuve?.valeurReference?.valeur ?? null : null
      const coordonnees = valeur ? { adresse: valeur.slice(0, valeur.lastIndexOf(':')), port: valeur.slice(valeur.lastIndexOf(':') + 1), pointsTerminaison: coordonneesCommuniquees.get(id) ?? null, communiqueesLe: o12?.faiteLe ?? '' } : null
      return {
        ...d, conducteur: d.conducteur ? { ...d.conducteur, id: 'CONDUCTEUR', nom: 'Conducteur de projet GIMAC' } : null,
        // Intervenants internes nommés par leur seule fonction ; aucune donnée interne au portail.
        intervenants: (d.intervenants ?? []).map((i) => ({ ...i, personne: i.fonction === 'PARTICIPANT' ? i.personne : parFonction(i.personne) })),
        etapes: d.etapes.map((x) => ({ ...x, responsable: x.responsable?.fonction === 'PARTICIPANT' ? x.responsable : parFonction(x.responsable), validateur: parFonction(x.validateur) })),
        etapesVersion: (d.etapesVersion ?? []).map((x) => ({ ...x, responsable: x.responsable?.fonction === 'PARTICIPANT' ? x.responsable : parFonction(x.responsable), validateur: parFonction(x.validateur) })),
        blocages: d.blocages.map((b) => ({ ...b, responsable: parFonction(b.responsable), actionsPermises: [] })), coordonneesTechniques: coordonnees, actionsPermises: [],
        // D-104 : la référence d'origine (reprise) reste interne au GIMAC.
        referenceOrigine: null,
      }
    }),
    mesTaches: () => parcours.tableauActeur(['PARTICIPANT']),
    communiquerCoordonnees: (id, c) => attendre(() => {
      const e = sienne(id)
      if (!/^[a-z0-9.-]+$/i.test(c.adresse) || !/^\d{2,5}$/.test(c.port)) throw refus(422, 'HOM_TECHNICAL_DATA_INVALID', 'Adresse ou port invalide.')
      const o = e.ops.find((x) => x.rang === '12')
      if (!o || o.etat !== 'A_FAIRE') throw refus(409, 'HOM_OPERATION_NOT_OPEN', 'Les coordonnées techniques ne sont pas attendues à cette étape.')
      coordonneesCommuniquees.set(id, c.pointsTerminaison?.trim() || null)
      terminer(e, o, { documentRef: `COORD-${id.slice(-3)}`, nom: 'fiche-coordonnees-techniques', empreinte: hex(c.adresse + c.port), deposeLe: jour(0), deposePar: `Correspondant ${e.s.institutionNom}`, valeurReference: { libelle: 'parcours.valeur.12', valeur: `${c.adresse}:${c.port}` } })
    }),
    deposerLivrable: async (opId, fichier, commentaire, livrable, service) => {
      sienne(opId.split(':')[0])
      return parcours.deposerLivrable(opId, fichier, commentaire, livrable, service)
    },
    procesVerbal: (id) => attendre(() => {
      const e = sienne(id)
      const pv = e.pv
      return { ...pv, circuit: vueCircuit(e, true), etabliPar: pv.etabliPar ? 'CONDUCTEUR' : null, signatureGimac: pv.signatureGimac ? { ...pv.signatureGimac, signataire: pv.signatureGimac.fonction } : null, actionsPermises: peutSignerPv(e, true) ? ['SIGNER_PARTICIPANT'] : [] }
    }),
    signerProcesVerbal: (id, assertion) => attendre(() => {
      if (!assertion) throw refus(401, 'IAM_REAUTH_REQUIRED', 'Réauthentification exigée.')
      const e = sienne(id)
      if (e.pv.etat === 'NON_ETABLI') throw refus(409, 'CER_PV_NOT_SIGNABLE', 'Le procès-verbal n\'attend pas votre signature.')
      signerPv(e, true)
    }),
    approbation: (id) => attendre(() => {
      const e = sienne(id)
      const a = e.approbation
      return {
        ...a, visas: [], dossierChangement: a.dossierChangement ? { ...a.dossierChangement, deposePar: 'PRODUCTION_MONETIQUE' } : null,
        attestations: a.attestations.map((x) => ({ ...x, verifieePar: x.verifieePar ? 'RSSI' : null })),
        actionsPermises: e.pv.etat === 'SIGNE' && a.attestations.some((x) => !x.preuve) ? ['DEPOSER_ATTESTATION'] : [],
      }
    }),
    deposerAttestation: (id, type, fichier) => attendre(() => {
      const e = sienne(id)
      if (e.pv.etat !== 'SIGNE') throw refus(409, 'CER_PV_NOT_SIGNED', 'Les attestations sont attendues une fois le procès-verbal signé des deux parties.')
      const a = e.approbation.attestations.find((x) => x.type === type)
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

  // DDR-08 : opérations ouvertes ou renvoyées dans la démonstration, encore à faire, dont l'utilisateur est responsable.
  const evenements: EvenementsParcoursDemo = {
    pourMoi: () => tous().flatMap((e) => (e.journal ?? []).flatMap((j) => {
      const o = e.ops.find((x) => x.id === j.operationId)
      const attente = j.type === 'VALIDATION' ? o?.etat === 'EN_VALIDATION' : o?.etat === 'A_FAIRE' || o?.etat === 'EN_COURS' || o?.etat === 'BLOQUEE'
      const n = o && attente && pourMoi(o, j.type) ? notificationDe(e, j) : null
      return n ? [n] : []
    })),
    surEvenement: (f) => { abonnesEvenements.push(f) },
    // HOM-04 : comme MS-HOMOLOGATION (§8.8) : fonction interne à affecter, R d'une opération engagée ou V d'une opération en
    // validation, dont le catalogue donne au moins une permission : tâche « Prendre la fonction <libellé> (<réf.>) ».
    fonctionsAPrendre: () => tous().filter((e) => e.s.statut !== 'CLOSE_SANS_SUITE').flatMap((e) => {
      const f = new Set<Fonction>()
      e.ops.forEach((o) => {
        if ((o.etat === 'A_FAIRE' || o.etat === 'EN_COURS' || o.etat === 'BLOQUEE') && o.raci.R?.aAffecter) f.add(o.raci.R.fonction)
        if (o.etat === 'EN_VALIDATION' && o.raci.V?.aAffecter) f.add(o.raci.V.fonction)
      })
      return [...f].flatMap((x): OffreFonctionDemo[] => {
        const c = catalogueFonctions().find((y) => y.code === x)
        if (!c || c.externe || !c.actif || !c.permissions?.length) return []
        return [{
          liaisonId: e.s.id, reference: `${e.s.id}/AFFECTER/${x}`, fonction: x, titre: `Prendre la fonction ${c.libelle} (${e.s.id})`, permissions: [...c.permissions],
          description: `La fonction ${c.libelle} est à affecter sur la liaison ${e.s.id} (${e.s.institutionNom}) : en prenant cette tâche, vous la tenez sur ses opérations non faites.`,
          lien: `/des/parcours/${e.s.id}?onglet=etapes&fonction=${x}`,
        }]
      })
    }),
    // Prise (PrisesFonctions) : la personne remplace la fonction à affecter sur les opérations non faites ; sa tâche de R suit.
    prendreFonction: (liaisonId, fonction, qui) => {
      const e = etats.get(liaisonId)
      if (!e) return
      const p: Personne = { id: qui.id, nom: qui.nom, fonction }
      const remplacer = (x: Personne | null) => (x?.aAffecter && x.fonction === fonction ? p : x)
      e.ops.filter((o) => o.etat !== 'FAITE').forEach((o) => {
        o.raci = { R: remplacer(o.raci.R), V: remplacer(o.raci.V), C: o.raci.C.map((x) => remplacer(x)!), I: o.raci.I.map((x) => remplacer(x)!), E: o.raci.E.map((x) => remplacer(x)!) }
      })
      e.s.aAffecter = (e.s.aAffecter ?? []).filter((x) => x !== fonction)
      garder()
    },
  }
  return { parcours, decision, portailParcours, evenements }
}
