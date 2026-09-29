import type { Permitted } from './commun'

// MS-CERTIFICATION : circuit des quatre quitus, dossier de mise en service, attestation, registre (CER-01 à CER-15).

export type NatureQuitus = 'TECHNIQUE' | 'SECURITE' | 'CONFORMITE' | 'DIRECTION_GENERALE'
export type QuitusView = {
  nature: NatureQuitus; etat: string; signataireRole: string; dateSignature?: string; dateHorodatage?: string; motif?: string; actionsPermises: string[]
}
export type SocleEvaluation = { empreinte: string; operations: { code: string; libelle: string }[]; services: string[] }
export type Circuit = { id: string; quitus: QuitusView[]; socle: SocleEvaluation }
export type DecisionQuitus = { decisionFavorable: boolean; motif: string; operationsDesignees: string[]; servicesDesignes: string[]; socleEmpreinte: string }

export type ConditionCode = 'COUVERTURE_CERTIFICATION' | 'COUVERTURE_MISE_EN_SERVICE' | 'QUATRE_QUITUS' | 'FICHE_CHANGEMENT_APPROUVEE' | 'PROCES_VERBAL' | 'EMPREINTES_VERIFIEES'
export type EtatDossier = 'OUVERT' | 'AUTORISE' | 'MIS_EN_SERVICE' | 'COMPLET' | 'ATTESTE'
export type Dossier = Permitted & {
  id: string; liaisonId: string; circuitId: string; institutionNom: string; cycle: number; etat: EtatDossier; etatsParcourus: string[]
  etatScellement: 'EN_ATTENTE' | 'SCELLE' | 'REJETE' | null; socleEmpreinte: string; dateOuverture: string
  dateAutorisation: string | null; referenceFicheChangement: string | null; numeroAttestation: string | null
}
export type Condition = Permitted & { code: ConditionCode; satisfaite: boolean; source: string; detail: string | null; dateVerification: string | null }
export type FicheLue = { reference: string; etatApprobation: string; fenetreDebut: string; fenetreFin: string; dateLecture: string; aRelire: boolean }
export type Horodatage = { id: string; objetType: 'QUITUS' | 'DOSSIER'; empreinte: string; dateJeton: string; verifie: boolean; autoriteChaine: string }
export type ProcesVerbal = { empreinte: string; heureBascule: string; horsFenetre: boolean } | null

export type TypeDemande = 'REVOCATION' | 'SUSPENSION' | 'LEVEE'
export type Attestation = Permitted & {
  numero: string; institutionNom: string; typeProjet: string; servicesCertifies: string[]; dateEmission: string
  socleEmpreinte: string; empreinteDocument: string; etat: string
  demandeEnAttente: { id: string; typeDemande: TypeDemande; motif: string; empreinte: string; demandeeParMoi: boolean } | null
}
export type EntreeRegistre = {
  numero: string; institutionNom: string; typeProjet: string; services: string[]; dateEmission: string; echeanceRenouvellement: string; etat: string
}
export type Telechargement = { blob: Blob; empreinte: string | null }
