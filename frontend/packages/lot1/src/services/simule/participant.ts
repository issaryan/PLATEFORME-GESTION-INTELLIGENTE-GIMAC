import { ApiError } from '@gimac/socle'
import type {
  Adhesion, Candidature, ConditionsAdhesion, DossierInstitution, FiltreInstitutions, FluxMensuel, IndicateursDdrm, Institution, PieceAdhesion, Prerequis, StatutInstitution,
  TransitionStatut,
} from '../../modele/participant'
import type { ServiceParticipant } from '../ports'
import { empreinte } from './donnees'

// Serveur de substitution de MS-PARTICIPANT pour le développement des écrans de la DDRM : mêmes formes
// de réponse et mêmes codes de refus que le service ; ses « règles » ne sont qu'une imitation.

type Etat = { institution: Institution; adhesion: Adhesion; pieces: PieceAdhesion[]; transitions: TransitionStatut[] }

const PIECES: [string, string, boolean][] = [
  ['DEMANDE_ADHESION', "Lettre de demande d'adhésion", true],
  ['STATUTS', "Statuts de l'établissement", true],
  ['AGREMENT', "Agrément ou autorisation d'exercer", true],
  ['ORGANIGRAMME', 'Organigramme et interlocuteurs désignés', false],
]
const DELTA: Record<StatutInstitution, StatutInstitution[]> = {
  CANDIDAT: ['ADHERENT', 'CLOS_SANS_SUITE'],
  ADHERENT: ['EN_HOMOLOGATION', 'CLOS_SANS_SUITE'],
  EN_HOMOLOGATION: ['ACTIF'],
  ACTIF: ['A_REGULARISER', 'SUSPENDU', 'RETIRE'],
  A_REGULARISER: ['ACTIF', 'SUSPENDU'],
  SUSPENDU: ['ACTIF', 'RETIRE'],
  RETIRE: [],
  CLOS_SANS_SUITE: [],
}

let n = 0
const id = (p: string) => `${p}-${(++n).toString().padStart(4, '0')}`
/**
 * DDR-02 : en démonstration continue, le registre relu du stockage porte des identifiants déjà attribués ; le compteur
 * repart après le plus grand d'entre eux, pour qu'aucun identifiant ne soit réattribué d'un chargement à l'autre.
 */
const reprendreCompteur = (garde: string) => {
  for (const m of garde.matchAll(/"(?:INS|PIE|DOC|TRA)-(\d+)"/g)) n = Math.max(n, Number(m[1]))
}
/** Chargé de la DDRM du jeu de démonstration (titulaire des dossiers initiaux, auteur des propositions historiques). */
export const CHARGE_DEMO = { id: 'sim-charge', nom: 'Fatimé Abakar Hissein' }
/** Chef de la DDRM du jeu de démonstration (approbateur des transitions historiques). */
export const CHEF_DEMO = { id: 'U-020', nom: 'Bertrand Nguema Obiang' }
/** Acteur authentifié (sujet du jeton) : auteur des écritures, comme ActeurCourant de MS-PARTICIPANT. */
export type ActeurParticipant = { id: string; nom: string }
const maintenant = () => new Date().toISOString()
const adhesionVide = (): Adhesion => ({
  codeParticipant: null, mode: null, banqueReglementId: null, banqueMarraineId: null, compteReglement: null, compteCantonnement: null,
  dateAdhesion: null, prononceePar: null, prononceeLe: null, motifCloture: null, closLe: null,
})

/** Groupe (D-104) comme le service : espaces réduits, vide ramené à l'absence de valeur. */
const groupe = (v: string | null | undefined): string | null => {
  const s = (v ?? '').replace(/\s+/g, ' ').trim()
  return s === '' ? null : s
}

function nouvelEtat(c: Candidature,statut: StatutInstitution = 'CANDIDAT', identifiant?: string, charge: ActeurParticipant = CHARGE_DEMO): Etat {
  const institution: Institution = {
    id: identifiant ?? id('INS'), nom: c.nom, sigle: c.sigle ?? null, pays: c.pays, categorie: c.categorie, statut, codeParticipant: null,
    codeIdentification: c.codeIdentification ?? null, codeBanqueBeac: c.codeBanqueBeac ?? null, modeAdhesion: null,
    datePriseDeContact: c.datePriseDeContact ?? maintenant().slice(0, 10), originePriseDeContact: c.originePriseDeContact ?? null,
    projetEnvisage: c.projetEnvisage ?? null, chargeDdrm: { id: charge.id, nom: charge.nom }, conformite: 'A_REGULARISER', creeLe: maintenant(),
    groupe: groupe(c.groupe),
  }
  const pieces = PIECES.map(([code, libelle, obligatoire]) => ({
    id: id('PIE'), code, libelle, obligatoire, etat: 'ATTENDUE' as const, documentId: null, documentSha256: null, nomFichier: null,
    deposeeLe: null, examineeLe: null, motifRejet: null,
  }))
  return { institution, adhesion: adhesionVide(), pieces, transitions: [] }
}

/** Transition déjà tranchée, pour l'historique du jeu de données. */
const historique = (de: StatutInstitution, vers: StatutInstitution, motif: string, le: string, automatique = false): TransitionStatut => ({
  id: id('TRA'), de, vers, motif, etat: 'APPROUVEE', demandeurId: automatique ? 'systeme' : CHARGE_DEMO.id, demandeeLe: le, approbationsRequises: automatique ? 0 : 1,
  approbateurId: automatique ? null : CHEF_DEMO.id, secondApprobateurId: null, decideeLe: le, motifRejet: null, pieceDecisionId: null, automatique,
})

/** Institution admise (et au-delà) : dossier complet, conditions d'adhésion, historique des transitions. */
function admise(nom: string, identifiant: string, categorie: Institution['categorie'], pays: string, statut: StatutInstitution, code: string, liaison?: string): Etat {
  // CER-17 : prise de contact et enregistrement antérieurs à l'adhésion, jamais datés du jour du chargement.
  const e = nouvelEtat({ nom, pays, categorie, datePriseDeContact: '2026-01-12' }, statut, identifiant)
  e.institution.creeLe = '2026-01-12T08:30:00Z'
  e.pieces.forEach((p) => Object.assign(p, { etat: p.obligatoire ? 'VALIDEE' : 'ATTENDUE', documentId: p.obligatoire ? id('DOC') : null }))
  Object.assign(e.adhesion, { codeParticipant: code, mode: 'DIRECT', compteReglement: `BEAC-${pays}-${code.slice(-3)}`, dateAdhesion: '2026-02-10', prononceeLe: '2026-02-10T10:00:00Z', prononceePar: CHEF_DEMO.id })
  Object.assign(e.institution, { codeParticipant: code, modeAdhesion: 'DIRECT', conformite: 'CONFORME' })
  e.transitions.push(historique('CANDIDAT', 'ADHERENT', 'Dossier complet', '2026-02-10T10:00:00Z'))
  if (liaison) e.transitions.unshift(historique('ADHERENT', 'EN_HOMOLOGATION', `Ouverture de la liaison ${liaison}`, '2026-03-03T09:12:00Z', true))
  // CER-17 : activation postérieure à l'adhésion et à l'ouverture de la liaison (ordre chronologique de l'historique).
  if (statut === 'ACTIF' || statut === 'SUSPENDU') e.transitions.unshift(historique('EN_HOMOLOGATION', 'ACTIF', 'Attestation de certification émise', '2026-07-15T09:00:00Z'))
  if (statut === 'SUSPENDU') e.transitions.unshift(historique('ACTIF', 'SUSPENDU', 'Suspension prononcée par la production monétique', '2026-09-21T17:40:00Z'))
  return e
}

/** Accès du serveur fictif de MS-HOMOLOGATION à celui de MS-PARTICIPANT (consommation d'événements). */
export type PontParticipant = {
  institution(id: string): Institution | undefined
  /** homologation.liaison-opened.v1 : ADHERENT vers EN_HOMOLOGATION, transition automatique (D-57). */
  liaisonOuverte(institutionId: string, liaisonId: string): void
  /** homologation.liaison-milestone-reached.v1, jalon EN_PRODUCTION : EN_HOMOLOGATION vers ACTIF (D-93 §10.4). */
  enProduction(institutionId: string, liaisonId: string): void
  /** Propositions en attente d'approbation (registre gardé d'un chargement à l'autre en démonstration continue). */
  propositions(): TransitionObservee[]
  /** Propositions humaines déjà tranchées (prononcées ou refusées), pour aviser leur proposant (DDR-08). */
  tranchees(): TransitionObservee[]
  /**
   * participant.transition-requested.v1, transition-approved.v1 et transition-rejected.v1 (D-98), consommés par MS-PILOTAGE
   * simulé : tâche de l'approbateur.
   */
  surTransition: ((e: TransitionObservee) => void) | null
}
/** Proposition, approbation ou refus d'une transition, tels que publiés par MS-PARTICIPANT. */
export type TransitionObservee = { type: 'PROPOSEE' | 'APPROUVEE' | 'REJETEE'; institutionId: string; institutionNom: string; transition: TransitionStatut }
export type ParticipantSimule = ServiceParticipant & { pont: PontParticipant }

/**
 * 12 mois denses (mois courant compris), fond fictif déterministe : les institutions de démonstration sont
 * toutes créées au chargement, leurs dates ne disent rien du flux.
 */
function fluxParMois(): FluxMensuel[] {
  const maintenant = new Date()
  return Array.from({ length: 12 }, (_, n) => {
    const d = new Date(maintenant.getFullYear(), maintenant.getMonth() - (11 - n), 1)
    return { mois: `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`, contacts: 1 + ((n * 5) % 4), adhesions: n % 3 === 1 ? 1 : 0, clotures: n === 4 ? 1 : 0 }
  })
}

export function participantSimule(attendreJeu: <T>(v: T | (() => T)) => Promise<T>, refus: (s: number, c: string, m: string, details?: Record<string, unknown>) => ApiError,
  persistance?: { lire(): string | null; ecrire(valeur: string): void },
  /** Utilisateur de la démonstration : auteur des propositions et des décisions (maker-checker, DDR-01). */
  acteur: ActeurParticipant = CHARGE_DEMO): ParticipantSimule {
  const base: Etat[] = [
    nouvelEtat({ nom: 'Fintech Kribi Pay', pays: 'CM', categorie: 'FINTECH', originePriseDeContact: 'Salon', projetEnvisage: 'Paiement mobile', datePriseDeContact: '2026-09-14' }),
    nouvelEtat({ nom: 'Banque de Libreville', sigle: 'BDL', pays: 'GA', categorie: 'BANQUE', codeIdentification: 'BDLIGALX', datePriseDeContact: '2026-06-02' }),
    nouvelEtat({ nom: 'Microfinance du Chari', pays: 'TD', categorie: 'MICROFINANCE', datePriseDeContact: '2026-08-21' }),
  ]
  // Un dossier complet, prêt au prononcé, et un dossier dont une pièce attend l'examen.
  base[1].pieces.forEach((p) => Object.assign(p, { etat: p.obligatoire ? 'VALIDEE' : 'ATTENDUE', documentId: p.obligatoire ? id('DOC') : null }))
  Object.assign(base[1].adhesion, { codeParticipant: 'BQ-BDL-001', mode: 'DIRECT', compteReglement: 'BEAC-GA-001' })
  base[1].institution.codeParticipant = 'BQ-BDL-001'
  base[1].institution.modeAdhesion = 'DIRECT'
  base[1].pieces[0].etat = 'VALIDEE'
  base[2].pieces[1].etat = 'RECUE'
  // DDR-15 : dossiers de démonstration enregistrés le jour de leur prise de contact, jamais au chargement de la page.
  base.forEach((e) => { e.institution.creeLe = `${e.institution.datePriseDeContact}T08:30:00Z` })
  let etats = base
  // Adhérents prêts pour le cadrage (étape 3), puis institutions déjà engagées dans une liaison, sous les
  // identifiants des liaisons du jeu de données de MS-HOMOLOGATION.
  etats.push(
    admise('Mobile Money Sanaga', 'INST-0105', 'EMETTEUR_MONNAIE_ELECTRONIQUE', 'CM', 'ADHERENT', 'EM-MMS-105'),
    admise('Crédit rural du Woleu', 'INST-0107', 'MICROFINANCE', 'GA', 'ADHERENT', 'MF-CRW-107'),
    admise('Établissement de paiement A', 'INST-0042', 'ETABLISSEMENT_PAIEMENT', 'CM', 'EN_HOMOLOGATION', 'EP-A-042', 'LIA-2026-017'),
    admise('Établissement de microfinance C', 'INST-0088', 'MICROFINANCE', 'TD', 'EN_HOMOLOGATION', 'MF-C-088', 'LIA-2026-030'),
    admise('Banque B', 'INST-0017', 'BANQUE', 'CM', 'ACTIF', 'BQ-B-017', 'LIA-2025-102'),
    admise('Banque D', 'INST-0061', 'BANQUE', 'CG', 'SUSPENDU', 'BQ-D-061', 'LIA-2026-021'),
  )
  // D-104 : deux filiales d'un même groupe, chacune avec son code membre.
  etats.filter((e) => e.institution.id === 'INST-0017' || e.institution.id === 'INST-0061').forEach((e) => { e.institution.groupe = 'Groupe Banque BD' })

  // Démonstration continue (D-98) : registre gardé d'un chargement à l'autre.
  try {
    const garde = persistance?.lire()
    if (garde) {
      etats = JSON.parse(garde) as Etat[]
      reprendreCompteur(garde)
    }
  } catch {
    // État illisible : jeu initial.
  }
  const sauver = () => {
    try {
      persistance?.ecrire(JSON.stringify(etats))
    } catch {
      // Stockage indisponible : la démonstration continue en mémoire.
    }
  }
  const attendre = <T,>(v: T | (() => T)) => attendreJeu(() => {
    const r = typeof v === 'function' ? (v as () => T)() : v
    sauver()
    return r
  })
  const trouver = (i: string) => {
    const e = etats.find((x) => x.institution.id === i)
    if (!e) throw refus(404, 'PAR_INSTITUTION_NOT_FOUND', 'Ressource introuvable')
    return e
  }
  const piece = (e: Etat, code: string) => {
    const p = e.pieces.find((x) => x.code === code)
    if (!p) throw refus(404, 'PAR_PIECE_NOT_FOUND', 'Ressource introuvable')
    return { ...p, institutionId: e.institution.id }
  }
  const prerequis = (e: Etat, vers: StatutInstitution): Prerequis[] => {
    if (vers !== 'ADHERENT') return []
    const r: Prerequis[] = e.pieces.filter((p) => p.obligatoire).map((p) => ({ code: `PIECE_${p.code}`, libelle: p.libelle, satisfait: p.etat === 'VALIDEE', reference: p.documentId ?? p.etat }))
    r.push({ code: 'CODE_PARTICIPANT', libelle: 'Code participant attribué', satisfait: !!e.adhesion.codeParticipant, reference: e.adhesion.codeParticipant })
    r.push({ code: 'MODE_ADHESION', libelle: "Mode d'adhésion défini", satisfait: !!e.adhesion.mode, reference: e.adhesion.mode })
    // Comme PrerequisitesEvaluator : compte de règlement en directe (RG-11), marraine membre direct actif et compte de cantonnement en indirecte (RG-12).
    if (e.adhesion.mode === 'DIRECT') {
      r.push({ code: 'COMPTE_REGLEMENT', libelle: 'Compte de règlement à la BEAC (RG-11)', satisfait: !!e.adhesion.compteReglement, reference: e.adhesion.compteReglement })
    } else if (e.adhesion.mode === 'INDIRECT') {
      const m = etats.find((x) => x.institution.id === e.adhesion.banqueMarraineId)?.institution
      r.push({ code: 'BANQUE_MARRAINE', libelle: 'Banque marraine membre direct actif', satisfait: m?.modeAdhesion === 'DIRECT' && m.statut === 'ACTIF', reference: m?.codeParticipant ?? null })
      r.push({ code: 'COMPTE_CANTONNEMENT', libelle: 'Compte de cantonnement (RG-12)', satisfait: !!e.adhesion.compteCantonnement, reference: e.adhesion.compteCantonnement })
    }
    return r
  }
  const dossier = (e: Etat): DossierInstitution => ({
    institution: e.institution, adhesion: e.adhesion,
    pieces: e.pieces.map((p) => ({ ...p, institutionId: e.institution.id })),
    transitions: e.transitions.map((t) => ({ ...t, institutionId: e.institution.id })),
    prerequisAdhesion: prerequis(e, 'ADHERENT'),
    transitionsPermises: DELTA[e.institution.statut],
  })
  /** DDR-04 : codes BIC et BEAC uniques dans le registre (RG-10), hors l'institution elle-même. */
  const verifierUnicite = (c: Candidature, soi?: string) => {
    const pris = (champ: 'codeIdentification' | 'codeBanqueBeac') =>
      !!c[champ] && etats.some((e) => e.institution.id !== soi && e.institution[champ] === c[champ])
    for (const champ of ['codeIdentification', 'codeBanqueBeac'] as const) {
      if (pris(champ)) throw refus(409, 'PAR_CODE_ALREADY_USED', 'Code déjà attribué à une autre institution', { champ, valeur: c[champ] })
    }
  }
  /** DDR-03 : mêmes contrôles que ConditionsRequest et definirConditionsAdhesion de MS-PARTICIPANT (RG-11, RG-12). */
  const verifierConditions = (c: ConditionsAdhesion) => {
    const champs: Record<string, string> = {}
    const code = c.codeParticipant ?? ''
    if (!code.trim()) champs.codeParticipant = 'Champ obligatoire'
    else if (code.length > 20 || !/^[A-Z0-9-]+$/.test(code)) champs.codeParticipant = 'Majuscules, chiffres et tirets, 20 caractères au plus'
    if (!c.mode) champs.mode = 'Champ obligatoire'
    else if (c.mode === 'DIRECT' && !c.compteReglement?.trim()) champs.compteReglement = 'Compte de règlement exigé en adhésion directe (RG-11)'
    else if (c.mode === 'INDIRECT') {
      if (!c.banqueMarraineId) champs.banqueMarraineId = 'Banque marraine exigée en adhésion indirecte (RG-12)'
      if (!c.compteCantonnement?.trim()) champs.compteCantonnement = 'Compte de cantonnement exigé en adhésion indirecte (RG-12)'
    }
    if (Object.keys(champs).length) throw refus(422, 'PAR_CONDITIONS_INVALID', 'Conditions d’adhésion incomplètes ou invalides', { champs })
  }
  const exigerCandidat = (e: Etat) => {
    if (e.institution.statut !== 'CANDIDAT') throw refus(409, 'PAR_INSTITUTION_STATUS', "Action possible seulement à l'état CANDIDAT")
  }

  const pont: PontParticipant = {
    institution: (i) => etats.find((x) => x.institution.id === i)?.institution,
    liaisonOuverte: (i, liaisonId) => {
      const e = etats.find((x) => x.institution.id === i)
      if (!e || e.institution.statut !== 'ADHERENT') return
      e.transitions.unshift(historique('ADHERENT', 'EN_HOMOLOGATION', `Ouverture de la liaison ${liaisonId}`, maintenant(), true))
      e.institution.statut = 'EN_HOMOLOGATION'
      sauver()
    },
    enProduction: (i, liaisonId) => {
      const e = etats.find((x) => x.institution.id === i)
      // Seule une institution en homologation passe à ACTIF ; une réactivation reste une décision humaine.
      if (!e || e.institution.statut !== 'EN_HOMOLOGATION') return
      e.transitions.unshift(historique('EN_HOMOLOGATION', 'ACTIF', `Jalon EN_PRODUCTION de la liaison ${liaisonId}`, maintenant(), true))
      e.institution.statut = 'ACTIF'
      sauver()
    },
    propositions: () => etats.flatMap((e) => e.transitions.filter((t) => t.etat === 'PROPOSEE')
      .map((t) => ({ type: 'PROPOSEE' as const, institutionId: e.institution.id, institutionNom: e.institution.nom, transition: { ...t } }))),
    tranchees: () => etats.flatMap((e) => e.transitions.filter((t) => !t.automatique && (t.etat === 'APPROUVEE' || t.etat === 'REJETEE'))
      .map((t) => ({ type: t.etat === 'REJETEE' ? 'REJETEE' as const : 'APPROUVEE' as const, institutionId: e.institution.id, institutionNom: e.institution.nom, transition: { ...t } }))),
    surTransition: null,
  }
  const publier = (type: TransitionObservee['type'], e: Etat, t: TransitionStatut) =>
    pont.surTransition?.({ type, institutionId: e.institution.id, institutionNom: e.institution.nom, transition: { ...t } })

  return {
    pont,
    listerInstitutions: (f: FiltreInstitutions) =>
      attendre(() => {
        const r = (f.recherche ?? '').toLowerCase()
        const l = etats
          .map((e) => e.institution)
          .filter((i) => !f.statut?.length || f.statut.includes(i.statut))
          .filter((i) => !r || [i.nom, i.sigle, i.codeParticipant, i.groupe].some((v) => (v ?? '').toLowerCase().includes(r)))
          .filter((i) => !f.mesDossiers || i.chargeDdrm?.id === acteur.id)
          .filter((i) => !groupe(f.groupe) || (i.groupe ?? '').toLowerCase() === groupe(f.groupe)?.toLowerCase())
        const debut = (f.page - 1) * f.taille
        return { contenu: l.slice(debut, debut + f.taille), total: l.length }
      }),
    groupes: () => attendre(() => [...new Set(etats.map((e) => e.institution.groupe).filter((g): g is string => !!g))].sort((a, b) => a.localeCompare(b))),
    indicateurs: () =>
      attendre((): IndicateursDdrm => {
        const compte = (s: StatutInstitution) => etats.filter((e) => e.institution.statut === s).length
        return {
          candidats: compte('CANDIDAT'),
          piecesAExaminer: etats.filter((e) => e.institution.statut === 'CANDIDAT').flatMap((e) => e.pieces).filter((p) => p.etat === 'RECUE').length,
          propositionsEnAttente: etats.flatMap((e) => e.transitions).filter((t) => t.etat === 'PROPOSEE').length,
          adherents: compte('ADHERENT'),
          enHomologation: compte('EN_HOMOLOGATION'),
          closSansSuite: compte('CLOS_SANS_SUITE'),
          fluxParMois: fluxParMois(),
        }
      }),
    dossier: (i) => attendre(() => dossier(trouver(i))),
    prerequis: (i, etat) => attendre(() => prerequis(trouver(i), etat)),
    enregistrer: (c) =>
      attendre(() => {
        verifierUnicite(c)
        // Le chargé de la DDRM est l'agent qui enregistre la prise de contact (ddrmOfficerId du service).
        const e = nouvelEtat(c, 'CANDIDAT', undefined, acteur)
        etats.unshift(e)
        return e.institution
      }),
    modifier: (i, c) =>
      attendre(() => {
        const e = trouver(i)
        if (e.institution.statut === 'RETIRE' || e.institution.statut === 'CLOS_SANS_SUITE') {
          throw refus(409, 'PAR_INSTITUTION_CLOSED', 'Institution close : fiche en lecture seule', { statut: e.institution.statut })
        }
        verifierUnicite(c, i)
        // DDR-04 : tous les champs de la fiche, comme appliquerFiche du service ; la date de prise de contact si elle est fournie.
        Object.assign(e.institution, {
          nom: c.nom, sigle: c.sigle ?? null, pays: c.pays, categorie: c.categorie, codeIdentification: c.codeIdentification ?? null,
          codeBanqueBeac: c.codeBanqueBeac ?? null, originePriseDeContact: c.originePriseDeContact ?? null, projetEnvisage: c.projetEnvisage ?? null,
          groupe: groupe(c.groupe),
          ...(c.datePriseDeContact ? { datePriseDeContact: c.datePriseDeContact } : {}),
        })
        return e.institution
      }),
    definirConditions: (i, c) =>
      attendre(() => {
        const e = trouver(i)
        exigerCandidat(e)
        verifierConditions(c)
        if (etats.some((x) => x.institution.id !== i && x.institution.codeParticipant === c.codeParticipant)) {
          throw refus(409, 'PAR_CODE_ALREADY_USED', 'Code déjà attribué à une autre institution', { champ: 'codeParticipant', valeur: c.codeParticipant })
        }
        if (c.banqueMarraineId === i) throw refus(409, 'PAR_SPONSOR_INVALID', 'Une institution ne peut être sa propre banque de règlement ou marraine')
        // Comme le service : compte de règlement en directe, compte de cantonnement et marraine en indirecte.
        Object.assign(e.adhesion, {
          ...c, compteReglement: c.mode === 'DIRECT' ? c.compteReglement ?? null : null,
          compteCantonnement: c.mode === 'INDIRECT' ? c.compteCantonnement ?? null : null,
          banqueMarraineId: c.mode === 'INDIRECT' ? c.banqueMarraineId ?? null : null,
        })
        e.institution.codeParticipant = c.codeParticipant
        e.institution.modeAdhesion = c.mode
        return e.institution
      }),
    deposerPiece: (i, code, fichier) =>
      attendre(() => {
        const e = trouver(i)
        exigerCandidat(e)
        const p = e.pieces.find((x) => x.code === code)
        if (!p) throw refus(404, 'PAR_PIECE_NOT_FOUND', 'Ressource introuvable')
        if (p.etat === 'VALIDEE') throw refus(409, 'PAR_PIECE_ALREADY_VALIDATED', 'Pièce déjà validée')
        // DDR-13 : empreinte propre à la pièce déposée (MS-DOCUMENT la calcule), jamais une suite de zéros.
        const documentId = id('DOC')
        Object.assign(p, { etat: 'RECUE', documentId, documentSha256: empreinte(`${documentId}|${fichier.name}|${fichier.size}|${fichier.lastModified}`), nomFichier: fichier.name, deposeeLe: maintenant(), examineeLe: null, motifRejet: null })
        return piece(e, code)
      }),
    validerPiece: (i, code) =>
      attendre(() => {
        const e = trouver(i)
        const p = e.pieces.find((x) => x.code === code)
        if (!p || p.etat !== 'RECUE') throw refus(409, 'PAR_PIECE_NOT_RECEIVED', 'Seule une pièce reçue peut être examinée')
        Object.assign(p, { etat: 'VALIDEE', examineeLe: maintenant() })
        return piece(e, code)
      }),
    rejeterPiece: (i, code, motif) =>
      attendre(() => {
        const e = trouver(i)
        const p = e.pieces.find((x) => x.code === code)
        if (!p || p.etat !== 'RECUE') throw refus(409, 'PAR_PIECE_NOT_RECEIVED', 'Seule une pièce reçue peut être examinée')
        Object.assign(p, { etat: 'REJETEE', examineeLe: maintenant(), motifRejet: motif })
        return piece(e, code)
      }),
    proposerTransition: (i, vers, motif) =>
      attendre(() => {
        const e = trouver(i)
        if (!DELTA[e.institution.statut].includes(vers)) throw refus(409, 'PAR_TRANSITION_NOT_ALLOWED', 'Transition non autorisée')
        if (e.transitions.some((t) => t.etat === 'PROPOSEE')) throw refus(409, 'PAR_TRANSITION_PENDING', 'Une proposition est déjà en attente')
        if (prerequis(e, vers).some((p) => !p.satisfait)) throw refus(409, 'PAR_PREREQUISITES_MISSING', 'Prérequis non réunis')
        const t: TransitionStatut = {
          id: id('TRA'), de: e.institution.statut, vers, motif, etat: 'PROPOSEE', demandeurId: acteur.id, demandeeLe: maintenant(),
          approbationsRequises: 1, approbateurId: null, secondApprobateurId: null, decideeLe: null, motifRejet: null, pieceDecisionId: null,
        }
        e.transitions.unshift(t)
        publier('PROPOSEE', e, t)
        return { ...t, institutionId: i }
      }),
    approuverTransition: (i, t) =>
      attendre(() => {
        const e = trouver(i)
        const x = e.transitions.find((y) => y.id === t && y.etat === 'PROPOSEE')
        if (!x) throw refus(409, 'PAR_TRANSITION_NOT_PENDING', 'Cette proposition a déjà été tranchée')
        // DDR-01 (maker-checker, PAR-02 critère 4) : l'approbateur est distinct du demandeur, comme InstitutionLifecycleService.
        if (x.demandeurId === acteur.id || x.approbateurId === acteur.id) {
          throw refus(403, 'PAR_SEPARATION_OF_DUTIES', 'L’approbateur doit être distinct du demandeur et du premier approbateur')
        }
        if (prerequis(e, x.vers).some((p) => !p.satisfait)) throw refus(409, 'PAR_PREREQUISITES_MISSING', 'Prérequis non réunis')
        Object.assign(x, { etat: 'APPROUVEE', approbateurId: acteur.id, decideeLe: maintenant() })
        e.institution.statut = x.vers
        if (x.vers === 'ADHERENT') {
          Object.assign(e.adhesion, { prononceeLe: maintenant(), prononceePar: acteur.id, dateAdhesion: maintenant().slice(0, 10) })
          // DDR-29 : dossier complet au prononcé, conformité documentaire CONFORME (comme le service).
          e.institution.conformite = 'CONFORME'
        }
        if (x.vers === 'CLOS_SANS_SUITE') Object.assign(e.adhesion, { motifCloture: x.motif, closLe: maintenant() })
        publier('APPROUVEE', e, x)
        return { ...x, institutionId: i }
      }),
    rejeterTransition: (i, t, motif) =>
      attendre(() => {
        const e = trouver(i)
        const x = e.transitions.find((y) => y.id === t && y.etat === 'PROPOSEE')
        if (!x) throw refus(409, 'PAR_TRANSITION_NOT_PENDING', 'Cette proposition a déjà été tranchée')
        if (x.demandeurId === acteur.id) throw refus(403, 'PAR_SEPARATION_OF_DUTIES', 'Le demandeur ne peut pas statuer sur sa propre proposition')
        Object.assign(x, { etat: 'REJETEE', approbateurId: acteur.id, decideeLe: maintenant(), motifRejet: motif })
        publier('REJETEE', e, x)
        return { ...x, institutionId: i }
      }),
  }
}
