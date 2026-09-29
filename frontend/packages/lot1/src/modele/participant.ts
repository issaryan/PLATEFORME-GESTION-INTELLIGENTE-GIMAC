// Registre des institutions et étapes 1 et 2 du parcours (MS-PARTICIPANT, D-57) : réponses des routes
// /bff/des/participant/institutions, champ pour champ.

export type StatutInstitution = 'CANDIDAT' | 'ADHERENT' | 'EN_HOMOLOGATION' | 'ACTIF' | 'A_REGULARISER' | 'SUSPENDU' | 'RETIRE' | 'CLOS_SANS_SUITE'
export type CategorieInstitution =
  | 'BANQUE' | 'MICROFINANCE' | 'ETABLISSEMENT_PAIEMENT' | 'EMETTEUR_MONNAIE_ELECTRONIQUE' | 'TRESOR_PUBLIC' | 'AGREGATEUR' | 'FINTECH' | 'EDITEUR_LOGICIEL'
export type ModeAdhesion = 'DIRECT' | 'INDIRECT'
export type EtatPiece = 'ATTENDUE' | 'RECUE' | 'VALIDEE' | 'REJETEE'
export type EtatTransition = 'PROPOSEE' | 'APPROUVEE' | 'REJETEE'

export type Personne = { id: string; nom: string }

export type Institution = {
  id: string
  nom: string
  sigle: string | null
  pays: string
  categorie: CategorieInstitution
  statut: StatutInstitution
  codeParticipant: string | null
  codeIdentification: string | null
  codeBanqueBeac: string | null
  modeAdhesion: ModeAdhesion | null
  datePriseDeContact: string | null
  originePriseDeContact: string | null
  projetEnvisage: string | null
  chargeDdrm: Personne | null
  conformite: string
  creeLe: string
}

export type Adhesion = {
  codeParticipant: string | null
  mode: ModeAdhesion | null
  banqueReglementId: string | null
  banqueMarraineId: string | null
  compteReglement: string | null
  compteCantonnement: string | null
  dateAdhesion: string | null
  prononceeLe: string | null
  prononceePar: string | null
  motifCloture: string | null
  closLe: string | null
}

export type PieceAdhesion = {
  id: string
  code: string
  libelle: string
  obligatoire: boolean
  etat: EtatPiece
  documentId: string | null
  documentSha256: string | null
  nomFichier: string | null
  deposeeLe: string | null
  examineeLe: string | null
  motifRejet: string | null
  /** Rattachement à l'institution, posé par l'interface pour les actions de ligne. */
  institutionId?: string
}

export type TransitionStatut = {
  id: string
  de: StatutInstitution
  vers: StatutInstitution
  motif: string
  etat: EtatTransition
  demandeurId: string
  demandeeLe: string
  approbationsRequises: number
  approbateurId: string | null
  secondApprobateurId: string | null
  decideeLe: string | null
  motifRejet: string | null
  pieceDecisionId: string | null
  institutionId?: string
}

export type Prerequis = { code: string; libelle: string; satisfait: boolean; reference: string | null }

/** Dossier numérique de l'institution (CR-CERT Fig. 7.3) pour les étapes 1 et 2. */
export type DossierInstitution = {
  institution: Institution
  adhesion: Adhesion
  pieces: PieceAdhesion[]
  transitions: TransitionStatut[]
  prerequisAdhesion: Prerequis[]
  transitionsPermises: StatutInstitution[]
}

export type IndicateursDdrm = {
  candidats: number
  piecesAExaminer: number
  propositionsEnAttente: number
  adherents: number
  enHomologation: number
  closSansSuite: number
}

export type Candidature = {
  nom: string
  sigle?: string | null
  pays: string
  categorie: CategorieInstitution
  codeIdentification?: string | null
  codeBanqueBeac?: string | null
  datePriseDeContact?: string | null
  originePriseDeContact?: string | null
  projetEnvisage?: string | null
}

export type ConditionsAdhesion = {
  codeParticipant: string
  mode: ModeAdhesion
  banqueReglementId?: string | null
  banqueMarraineId?: string | null
  compteReglement?: string | null
  compteCantonnement?: string | null
}

export type FiltreInstitutions = { statut?: StatutInstitution[]; recherche?: string; mesDossiers?: boolean; page: number; taille: number }
