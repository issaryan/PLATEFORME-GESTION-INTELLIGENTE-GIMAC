import { ApiError } from '@gimac/socle'
import type {
  Adhesion, Candidature, DossierInstitution, FiltreInstitutions, IndicateursDdrm, Institution, PieceAdhesion, Prerequis, StatutInstitution,
  TransitionStatut,
} from '../../modele/participant'
import type { ServiceParticipant } from '../ports'

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
const maintenant = () => new Date().toISOString()
const adhesionVide = (): Adhesion => ({
  codeParticipant: null, mode: null, banqueReglementId: null, banqueMarraineId: null, compteReglement: null, compteCantonnement: null,
  dateAdhesion: null, prononceePar: null, prononceeLe: null, motifCloture: null, closLe: null,
})

function nouvelEtat(c: Candidature, statut: StatutInstitution = 'CANDIDAT'): Etat {
  const institution: Institution = {
    id: id('INS'), nom: c.nom, sigle: c.sigle ?? null, pays: c.pays, categorie: c.categorie, statut, codeParticipant: null,
    codeIdentification: c.codeIdentification ?? null, codeBanqueBeac: c.codeBanqueBeac ?? null, modeAdhesion: null,
    datePriseDeContact: c.datePriseDeContact ?? maintenant().slice(0, 10), originePriseDeContact: c.originePriseDeContact ?? null,
    projetEnvisage: c.projetEnvisage ?? null, chargeDdrm: { id: 'sim-charge', nom: 'Chargé DDRM' }, conformite: 'A_REGULARISER', creeLe: maintenant(),
  }
  const pieces = PIECES.map(([code, libelle, obligatoire]) => ({
    id: id('PIE'), code, libelle, obligatoire, etat: 'ATTENDUE' as const, documentId: null, documentSha256: null, nomFichier: null,
    deposeeLe: null, examineeLe: null, motifRejet: null,
  }))
  return { institution, adhesion: adhesionVide(), pieces, transitions: [] }
}

export function participantSimule(attendre: <T>(v: T | (() => T)) => Promise<T>, refus: (s: number, c: string, m: string) => ApiError): ServiceParticipant {
  const base: Etat[] = [
    nouvelEtat({ nom: 'Fintech Kribi Pay', pays: 'CM', categorie: 'FINTECH', originePriseDeContact: 'Salon', projetEnvisage: 'Paiement mobile' }),
    nouvelEtat({ nom: 'Banque de Libreville', sigle: 'BDL', pays: 'GA', categorie: 'BANQUE', codeIdentification: 'BDLIGALX' }),
    nouvelEtat({ nom: 'Microfinance du Chari', pays: 'TD', categorie: 'MICROFINANCE' }),
  ]
  // Un dossier complet, prêt au prononcé, et un adhérent.
  base[1].pieces.forEach((p) => Object.assign(p, { etat: p.obligatoire ? 'VALIDEE' : 'ATTENDUE', documentId: p.obligatoire ? id('DOC') : null }))
  Object.assign(base[1].adhesion, { codeParticipant: 'BQ-BDL-001', mode: 'DIRECT', compteReglement: 'BEAC-GA-001' })
  base[1].institution.codeParticipant = 'BQ-BDL-001'
  base[1].institution.modeAdhesion = 'DIRECT'
  base[1].pieces[0].etat = 'VALIDEE'
  base[2].pieces[1].etat = 'RECUE'
  const etats = base
  const adherent = nouvelEtat({ nom: 'Établissement de paiement A', pays: 'CM', categorie: 'ETABLISSEMENT_PAIEMENT' }, 'ADHERENT')
  adherent.institution.conformite = 'CONFORME'
  etats.push(adherent)

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
    return r
  }
  const dossier = (e: Etat): DossierInstitution => ({
    institution: e.institution, adhesion: e.adhesion,
    pieces: e.pieces.map((p) => ({ ...p, institutionId: e.institution.id })),
    transitions: e.transitions.map((t) => ({ ...t, institutionId: e.institution.id })),
    prerequisAdhesion: prerequis(e, 'ADHERENT'),
    transitionsPermises: DELTA[e.institution.statut],
  })
  const exigerCandidat = (e: Etat) => {
    if (e.institution.statut !== 'CANDIDAT') throw refus(409, 'PAR_INSTITUTION_STATUS', "Action possible seulement à l'état CANDIDAT")
  }

  return {
    listerInstitutions: (f: FiltreInstitutions) =>
      attendre(() => {
        const r = (f.recherche ?? '').toLowerCase()
        const l = etats
          .map((e) => e.institution)
          .filter((i) => !f.statut?.length || f.statut.includes(i.statut))
          .filter((i) => !r || [i.nom, i.sigle, i.codeParticipant].some((v) => (v ?? '').toLowerCase().includes(r)))
          .filter((i) => !f.mesDossiers || i.chargeDdrm?.id === 'sim-charge')
        const debut = (f.page - 1) * f.taille
        return { contenu: l.slice(debut, debut + f.taille), total: l.length }
      }),
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
        }
      }),
    dossier: (i) => attendre(() => dossier(trouver(i))),
    prerequis: (i, etat) => attendre(() => prerequis(trouver(i), etat)),
    enregistrer: (c) =>
      attendre(() => {
        if (c.codeIdentification && etats.some((e) => e.institution.codeIdentification === c.codeIdentification)) {
          throw refus(409, 'PAR_CODE_ALREADY_USED', 'Code déjà attribué à une autre institution')
        }
        const e = nouvelEtat(c)
        etats.unshift(e)
        return e.institution
      }),
    modifier: (i, c) =>
      attendre(() => {
        const e = trouver(i)
        Object.assign(e.institution, { nom: c.nom, sigle: c.sigle ?? null, pays: c.pays, categorie: c.categorie, projetEnvisage: c.projetEnvisage ?? null })
        return e.institution
      }),
    definirConditions: (i, c) =>
      attendre(() => {
        const e = trouver(i)
        exigerCandidat(e)
        Object.assign(e.adhesion, { ...c })
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
        Object.assign(p, { etat: 'RECUE', documentId: id('DOC'), documentSha256: '0'.repeat(64), nomFichier: fichier.name, deposeeLe: maintenant(), motifRejet: null })
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
          id: id('TRA'), de: e.institution.statut, vers, motif, etat: 'PROPOSEE', demandeurId: 'sim-charge', demandeeLe: maintenant(),
          approbationsRequises: 1, approbateurId: null, secondApprobateurId: null, decideeLe: null, motifRejet: null, pieceDecisionId: null,
        }
        e.transitions.unshift(t)
        return { ...t, institutionId: i }
      }),
    approuverTransition: (i, t) =>
      attendre(() => {
        const e = trouver(i)
        const x = e.transitions.find((y) => y.id === t && y.etat === 'PROPOSEE')
        if (!x) throw refus(409, 'PAR_TRANSITION_NOT_PENDING', 'Cette proposition a déjà été tranchée')
        Object.assign(x, { etat: 'APPROUVEE', approbateurId: 'sim-chef', decideeLe: maintenant() })
        e.institution.statut = x.vers
        if (x.vers === 'ADHERENT') Object.assign(e.adhesion, { prononceeLe: maintenant(), prononceePar: 'sim-chef', dateAdhesion: maintenant().slice(0, 10) })
        if (x.vers === 'CLOS_SANS_SUITE') Object.assign(e.adhesion, { motifCloture: x.motif, closLe: maintenant() })
        return { ...x, institutionId: i }
      }),
    rejeterTransition: (i, t, motif) =>
      attendre(() => {
        const e = trouver(i)
        const x = e.transitions.find((y) => y.id === t && y.etat === 'PROPOSEE')
        if (!x) throw refus(409, 'PAR_TRANSITION_NOT_PENDING', 'Cette proposition a déjà été tranchée')
        Object.assign(x, { etat: 'REJETEE', approbateurId: 'sim-chef', decideeLe: maintenant(), motifRejet: motif })
        return { ...x, institutionId: i }
      }),
  }
}
