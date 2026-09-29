import type { SignPurpose } from '@gimac/socle'
import type { EvenementChronologie, ListQuery, Option, Page } from '../modele/commun'
import type {
  Attribution, CampaignResult, ClotureOperation, ControleResultat, DepotResultat, EtatReprise, ExportPreuves, Graphe, ImpactMigration,
  Liaison, LiaisonPilotage, OperationInstance, OuvertureLiaison, PointGrille, PreuveVersion, ReferenceOperation, ReferentielVersion,
  ScenarioInconnu, ServiceCouvert,
} from '../modele/homologation'
import type {
  Attestation, Circuit, Condition, DecisionQuitus, Dossier, EntreeRegistre, FicheLue, Horodatage, NatureQuitus, ProcesVerbal,
  Telechargement, TypeDemande,
} from '../modele/certification'
import type { PortailCertification, PortailLiaison, PortailOperation } from '../modele/portail'
import type {
  Candidature, ConditionsAdhesion, DossierInstitution, FiltreInstitutions, IndicateursDdrm, Institution, PieceAdhesion, Prerequis,
  StatutInstitution, TransitionStatut,
} from '../modele/participant'

// Ports de l'interface du lot 1 : le seul contrat entre les écrans et les données. Les composants et
// les pages n'appellent jamais un service directement ; les hooks de données appellent le port injecté
// (ServicesProvider), dont l'implémentation est HTTP (passerelle /bff) ou simulée (développement).
// Les règles (transitions, permissions, couvertures, impartialité) sont toujours décidées par le service.

/** Assertion FIDO2 obtenue par réauthentification (IAM-07), transmise telle quelle au service. */
export type Assertion = string

export interface ServiceHomologation {
  // Référentiels d'opérations (HOM-01 à HOM-03)
  listerReferentiels(): Promise<ReferentielVersion[]>
  lireReferentiel(id: string): Promise<ReferentielVersion>
  operationsReferentiel(id: string): Promise<ReferenceOperation[]>
  controlerReferentiel(id: string): Promise<ControleResultat>
  simulerMigration(id: string): Promise<ImpactMigration[]>
  liaisonsImpactees(versionId: string): Promise<{ total: number }>
  creerBrouillon(entree: { typeProjet: string; libelle: string }): Promise<ReferentielVersion>
  deriverBrouillon(versionSourceId: string): Promise<ReferentielVersion>
  publierReferentiel(id: string): Promise<void>
  migrerLiaisons(id: string, liaisonsRetenues: string[]): Promise<void>
  historiqueReferentiel(id: string): Promise<EvenementChronologie[]>

  // Liaisons (HOM-04 à HOM-06, HOM-20, HOM-22)
  listerLiaisons(q: ListQuery): Promise<Page<Liaison>>
  tableauDeBordLiaisons(q: ListQuery): Promise<Page<LiaisonPilotage>>
  lireLiaison(id: string): Promise<Liaison>
  historiqueLiaison(id: string): Promise<EvenementChronologie[]>
  ouvrirLiaison(entree: OuvertureLiaison): Promise<Liaison>
  designerChefDeProjet(id: string, chefDeProjetId: string): Promise<void>
  degelerLiaison(id: string, motif: string): Promise<void>
  exporterDossierDePreuves(id: string): Promise<void>
  dernierExportPreuves(id: string): Promise<ExportPreuves | null>
  grapheLiaison(id: string): Promise<Graphe>
  couverturesLiaison(id: string): Promise<ServiceCouvert[]>

  // Opérations (HOM-07 à HOM-12)
  listerOperations(liaisonId: string, filtres?: Record<string, string | boolean | undefined>): Promise<OperationInstance[]>
  lireOperation(id: string): Promise<OperationInstance>
  preuvesOperation(id: string): Promise<PreuveVersion[]>
  attributionsOperation(id: string): Promise<Attribution[]>
  grilleOperation(id: string): Promise<PointGrille[]>
  renseignerGrille(id: string, reponses: Record<string, 'CONFORME' | 'NON_CONFORME'>): Promise<void>
  attribuerOperation(id: string, responsableId: string, motif: string): Promise<void>
  rouvrirOperation(id: string, motif: string): Promise<void>
  elementsDeCloture(id: string): Promise<ClotureOperation>
  deposerPreuve(id: string, fichier: File, typePiece: string): Promise<DepotResultat>
  validerOperation(id: string, entree: { motif: string; empreinte: string }, assertion: Assertion): Promise<void>

  // Résultats du simulateur (HOM-13 à HOM-15) et reprise
  campagnesLiaison(liaisonId: string): Promise<CampaignResult[]>
  scenariosInconnus(liaisonId: string): Promise<ScenarioInconnu[]>
  qualifierScenario(id: string, qualification: 'A_DECLARER' | 'HORS_PARCOURS', motif: string): Promise<void>
  etatReprise(): Promise<EtatReprise>

  // Recherches pour les champs de formulaire (MS-PARTICIPANT, MS-IAM, relayées par la passerelle)
  rechercherInstitutions(recherche: string): Promise<Option[]>
  rechercherCollaborateurs(recherche: string): Promise<Option[]>
}

export interface ServiceCertification {
  dossierParLiaison(liaisonId: string): Promise<Dossier | null>
  lireDossier(id: string): Promise<Dossier>
  historiqueDossier(id: string): Promise<EvenementChronologie[]>
  circuit(id: string): Promise<Circuit>
  signerQuitus(circuitId: string, nature: NatureQuitus, decision: DecisionQuitus, assertion: Assertion): Promise<void>
  conditions(dossierId: string): Promise<Condition[]>
  verifierDossier(dossierId: string, condition?: string): Promise<void>
  ficheDeChangement(dossierId: string): Promise<FicheLue | null>
  lierFicheDeChangement(dossierId: string, reference: string): Promise<void>
  procesVerbal(dossierId: string): Promise<ProcesVerbal>
  deposerProcesVerbal(dossierId: string, fichier: File, heureBascule: string): Promise<void>
  horodatages(dossierId: string): Promise<Horodatage[]>
  emettreAttestation(dossierId: string): Promise<void>
  lireAttestation(numero: string): Promise<Attestation>
  telechargerAttestation(numero: string): Promise<Telechargement>
  demanderStatut(numero: string, entree: { typeDemande: TypeDemande; motif: string; piece: File; conditionLevee?: string }): Promise<void>
  approuverStatut(demandeId: string, motif: string, assertion: Assertion): Promise<void>
  registre(q: ListQuery): Promise<Page<EntreeRegistre>>
  exporterRegistre(q: ListQuery): Promise<Telechargement>
}

export interface ServicePortail {
  mesLiaisons(): Promise<PortailLiaison[]>
  maLiaison(id: string): Promise<PortailLiaison>
  aFaire(liaisonId: string): Promise<PortailOperation[]>
  operationAFaire(liaisonId: string, id: string): Promise<PortailOperation>
  deposerPreuve(operationId: string, fichier: File, typePiece: string): Promise<void>
  certifications(): Promise<PortailCertification[]>
  telechargerAttestation(certificationId: string): Promise<Telechargement>
}

/** Registre des institutions et étapes 1 et 2 du parcours (MS-PARTICIPANT, D-57). */
export interface ServiceParticipant {
  listerInstitutions(f: FiltreInstitutions): Promise<Page<Institution>>
  indicateurs(): Promise<IndicateursDdrm>
  dossier(id: string): Promise<DossierInstitution>
  prerequis(id: string, etat: StatutInstitution): Promise<Prerequis[]>
  enregistrer(c: Candidature): Promise<Institution>
  modifier(id: string, c: Candidature): Promise<Institution>
  definirConditions(id: string, c: ConditionsAdhesion): Promise<Institution>
  /** Dépose la pièce dans MS-DOCUMENT puis rattache sa référence et son empreinte au dossier. */
  deposerPiece(id: string, code: string, fichier: File): Promise<PieceAdhesion>
  validerPiece(id: string, code: string): Promise<PieceAdhesion>
  rejeterPiece(id: string, code: string, motif: string): Promise<PieceAdhesion>
  proposerTransition(id: string, vers: StatutInstitution, motif: string, pieceDecisionId?: string): Promise<TransitionStatut>
  approuverTransition(id: string, transitionId: string, assertion: Assertion): Promise<TransitionStatut>
  rejeterTransition(id: string, transitionId: string, motif: string): Promise<TransitionStatut>
}

export interface ServiceReauthentification {
  /** Lève ReauthCancelled si l'utilisateur annule ; aucune requête d'effet n'est alors envoyée. */
  assertion(purpose: SignPurpose): Promise<Assertion>
}

export interface Services {
  participant: ServiceParticipant
  homologation: ServiceHomologation
  certification: ServiceCertification
  portail: ServicePortail
  reauth: ServiceReauthentification
}
