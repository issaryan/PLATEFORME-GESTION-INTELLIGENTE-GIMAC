import type { CoverageValue, Partie, Permitted, Segment, TypeProjet } from './commun'

// MS-HOMOLOGATION : référentiels (HOM-01 à HOM-03), liaisons (HOM-04 à HOM-24).

export type ReferentielVersion = Permitted & {
  id: string; typeProjet: TypeProjet; numero: number | null; libelle: string; etat: 'BROUILLON' | 'PUBLIEE' | 'RETIREE'
  dateCreation: string; datePublication: string | null; versionSourceId: string | null; versionActiveId: string | null
}
export type ReferenceOperation = Permitted & {
  versionId: string; code: string; libelle: string; phase: string; segment: Segment; partie: Partie; serviceCode: string
  natureCouverture: 'CERTIFICATION' | 'MISE_EN_SERVICE' | 'AUCUNE'; idScenario: string | null; predecesseurs: string[]
  fonctionResponsable: string; preuveAttendue: Record<string, string>
}
export type ImpactMigration = {
  liaisonId: string; institutionNom: string; conservees: number; ajoutees: number; retirees: number; modifiees: number
  exclue: boolean; motifExclusion: string | null
}
export type ControleResultat = {
  acyclique: boolean; ordreTopologique: string[]; cycle: string[]
  incoherences: { code: string; predecesseur: string; motif: string }[]; manquants: { code: string; attributs: string[] }[]
}

export type EtatLiaison = 'OUVERTE' | 'EN_HOMOLOGATION' | 'GELEE' | 'APTE_CERTIFICATION' | 'MISE_EN_SERVICE_AUTORISEE' | 'CERTIFIEE' | 'CLOSE'
export type Liaison = Permitted & {
  id: string; typeProjet: TypeProjet; institutionId: string; institutionNom: string; etat: EtatLiaison
  chefDeProjetId: string; chefDeProjetNom: string; banquePartenaireId: string | null; banquePartenaireNom: string | null
  dateOuverture: string; motifEtat: string | null; versionId: string; versionNumero: number; phaseCourante: string
  couvertureCertification: CoverageValue; couvertureMiseEnService: CoverageValue; operationsEnRetard: number; etatsParcourus: EtatLiaison[]
}
export type LiaisonPilotage = Liaison & { delaiConsomme: number; delaiStandard: number }
export type OuvertureLiaison = { institutionId: string; typeProjet: TypeProjet; chefDeProjetId: string; banquePartenaireId?: string | null }

export type ElementKey = 'responsable' | 'preuve' | 'horodatage' | 'validation'
export type ElementState = 'REUNI' | 'MANQUANT' | 'EN_ATTENTE'
export type OperationNodeData = {
  id: string; code: string; libelle: string; phase: string; partie: Partie; etat: string
  elements: Record<ElementKey, ElementState>; enRetard: boolean; aRevoir: boolean
}
export type Graphe = { noeuds: OperationNodeData[]; aretes: { from: string; to: string }[]; couches: string[][] }
export type ServiceCouvert = {
  serviceCode: string; serviceLibelle: string; etatPerimetre: string; certification: CoverageValue; miseEnService: CoverageValue
}
export type ExportPreuves = { id: string; etat: string; dateExport: string; empreinteManifeste: string }

export type OperationInstance = Permitted & {
  id: string; liaisonId: string; code: string; libelle: string; segment: Segment; partie: Partie; serviceCode: string; etat: string
  responsableId: string | null; responsableNom: string | null; echeance: string | null; joursDepassement: number
  predecesseursNonClos: string[]; aRevoir: boolean
}
export type PreuveVersion = { documentRef: string; version: number; typePiece: string; empreinte: string; dateDepot: string; scelle: boolean; valide: boolean }
export type Attribution = { event: string; at: string; actor?: string; correlationId?: string }
export type PointGrille = { code: string; libelle: string; reponse: 'CONFORME' | 'NON_CONFORME' | null; nonConformiteSignalee: boolean }

export type DepotResultat = {
  documentRef: string; empreinte: string; version: number; scelle: boolean
  controle: { resultat: 'ACCEPTEE' | 'MASQUEE' | 'REFUSEE'; motif?: string; sequencesMasquees?: number }
}
export type ElementCloture = { key: ElementKey; etat: ElementState; detail?: string; actionManquante?: { labelKey: string; href?: string } }
export type ClotureOperation = Permitted & {
  id: string; liaisonId: string; code: string; libelle: string; etat: string; preuveAttendue: string
  dernierDepot: DepotResultat | null; elements: ElementCloture[]; empreinteAValider: string | null
  restrictionValidation: 'EST_RESPONSABLE' | 'EST_DEPOSANT' | null
}

export type CampaignResult = {
  id: string; campagneId: string; executionId: string; idScenario: string; operationCode: string | null
  verdict: 'FAVORABLE' | 'DEFAVORABLE' | 'NON_EXECUTE'; dateExecution: string; versionScenario: string
  referenceTrace: string; empreinteReponse: string; dateReprise: string
}
export type ScenarioInconnu = Permitted & { id: string; liaisonId: string; idScenario: string; dateReception: string; qualification: 'A_DECLARER' | 'HORS_PARCOURS' | null }
export type EtatReprise = { etatDisjoncteur: 'FERME' | 'OUVERT' | 'SEMI_OUVERT'; depuis: string | null }
