import type { SignPurpose } from '@gimac/socle'
import type { EvenementChronologie, ListQuery, Option, Page } from '../modele/commun'
import type {
  Attribution, CampaignResult, ClotureOperation, Conducteur, ControleResultat, DepotResultat, EtatReprise, ExportPreuves, Graphe, ImpactMigration,
  CatalogueModelisateur, CreationFonction, CreationTypeProjet, FonctionCatalogue, ModificationFonction, ModificationTypeProjet, TypeProjetVue, EtapeReferentiel, GrapheReferentiel, Liaison, OperationsReferentiel, SaisieEtape, LiaisonPilotage, OperationInstance, OuvertureLiaison, PointGrille, PreuveVersion, ReferenceOperation, ReferentielVersion, SaisieOperation,
  ScenarioInconnu, ServiceCouvert,
  PerimetreProjet,
  ModificationPerimetre,
} from '../modele/homologation'
import type {
  Attestation, Circuit, Condition, DecisionQuitus, Dossier, EntreeRegistre, FicheLue, Horodatage, NatureQuitus, ProcesVerbal,
  Telechargement, TypeDemande,
} from '../modele/certification'
import type { PortailCertification, PortailLiaison, PortailOperation } from '../modele/portail'
import type { PageExecutionsSandbox, RepartitionSandbox, ResultatsSandbox } from '../modele/sandbox'
import type { EtatPresence, OptionsTeleversement, Televersement } from '@gimac/socle'
import type { Conversation, Correspondant, Envoi, FilMessages, LecteurMessage, Message, NouvelleConversation, PageMessagerie } from '../modele/messagerie'
import type {
  Activite, DestinatairesRapport, EntreeActivite, EntreeEvenement, EntreeRapport, EntreeTache, EtatRapport, EtatTache, Evenement, GroupeTaches, MembreEquipe, PagePilotage, Rapport, Rattachement,
  ReponseInvitation, StatutActivite, Synthese, Tache, TypeRapport, Unite, VueRapports, VueTaches,
} from '../modele/pilotage'
import type { Demande, EtatDemande, MesPouvoirs, NouvelleDemande, PageDemandes } from '../modele/pouvoirs'
import type {
  CandidatsProcessus, ControleProcessus, DecisionValidation, EtatInstance, EtatProcessus, InstanceProcessus, Lancables, Lancement, PageDemandesProcessus, PageInstances, PorteeProcessus, Processus, ResumeProcessus, SaisieProcessus, SaisiePublic, VueInstances,
} from '../modele/processus'
import type {
  Candidature, ConditionsAdhesion, DossierInstitution, FiltreInstitutions, IndicateursDdrm, Institution, PieceAdhesion, Prerequis,
  StatutInstitution, TransitionStatut,
} from '../modele/participant'
import type {
  ApprobationSecurite, Blocage, DossierLiaison, EvenementPreuve, Fonction, FonctionUtilisateur, FicheSuiviServices, IndicateursPilotage, LiaisonResume, MesOperations, OperationParcours,
  Ceremonie, OuvertureLiaison as OuvertureCadrage, Personne, ProcesVerbalCertification, TableauActeur, TypeAttestation,
} from '../modele/parcours'

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
  /** Opérations de la version, filtrées pour un lecteur concerné (fantômes compris, D-89) ; filtre lu dans X-Gimac-Filtre. */
  operationsReferentiel(id: string): Promise<OperationsReferentiel>
  controlerReferentiel(id: string): Promise<ControleResultat>
  simulerMigration(id: string): Promise<ImpactMigration[]>
  liaisonsImpactees(versionId: string): Promise<{ total: number }>
  /** Brouillon ; pilote facultatif (par défaut le créateur, D-77 §5). */
  creerBrouillon(entree: { typeProjet: string; libelle: string; piloteId?: string }): Promise<ReferentielVersion>
  deriverBrouillon(versionSourceId: string): Promise<ReferentielVersion>
  publierReferentiel(id: string): Promise<void>
  migrerLiaisons(id: string, liaisonsRetenues: string[]): Promise<void>
  historiqueReferentiel(id: string): Promise<EvenementChronologie[]>

  // Modélisateur de processus (D-77, §4) : toute écriture hors brouillon est refusée (409 HOM_REFERENTIAL_FROZEN).
  /** Graphe étendu : nœuds complets, couches, échéances au plus tôt, chemin critique, anomalies. */
  grapheReferentiel(id: string): Promise<GrapheReferentiel>
  /** Listes fermées du modélisateur et pilotes possibles d'un brouillon. */
  catalogueReferentiels(versionId?: string): Promise<CatalogueModelisateur>
  /** Export CSV des opérations de la version, filtré comme la liste (D-89) ; nom du fichier du service. */
  exporterReferentiel(id: string): Promise<Telechargement & { nom: string }>
  // Étapes configurables (D-89 §2), brouillon seulement (HOM_REFERENTIEL_EDIT).
  ajouterEtape(id: string, etape: SaisieEtape & { code: string }): Promise<EtapeReferentiel>
  modifierEtape(id: string, code: string, etape: SaisieEtape): Promise<EtapeReferentiel>
  supprimerEtape(id: string, code: string): Promise<void>
  ordonnerEtapes(id: string, codes: string[]): Promise<EtapeReferentiel[]>
  /** Création d'une opération au code fourni (409 HOM_OPERATION_CODE_DUPLICATE, 422 HOM_OPERATION_CODE_INVALID). */
  creerOperation(id: string, operation: SaisieOperation & { code: string }): Promise<ReferenceOperation>
  /** Enregistrement complet d'une opération ; position et particularité absentes sont conservées. */
  enregistrerOperation(id: string, code: string, operation: SaisieOperation): Promise<ReferenceOperation>
  /** Retrait d'une opération ; son code disparaît des prédécesseurs des autres. */
  supprimerOperation(id: string, code: string): Promise<void>
  /** Positions des opérations dans le canevas. */
  placerOperations(id: string, positions: { code: string; x: number; y: number }[]): Promise<void>
  /** Arc de → vers ajouté ou retiré (409 HOM_REFERENTIAL_CYCLE) ; rend l'opération « vers ». */
  arcReferentiel(id: string, arc: { de: string; vers: string; present: boolean }): Promise<ReferenceOperation>
  /** Pilote du brouillon (422 HOM_PILOTE_NOT_ELIGIBLE) ; rend la version. */
  designerPilote(id: string, compteId: string): Promise<ReferentielVersion>
  // Types de projet et catalogue des fonctions (D-91) : lecture HOM_REFERENTIEL_READ, écriture HOM_REFERENTIEL_EDIT.
  typesProjet(actif?: boolean): Promise<TypeProjetVue[]>
  creerTypeProjet(c: CreationTypeProjet): Promise<TypeProjetVue>
  modifierTypeProjet(code: string, m: ModificationTypeProjet): Promise<TypeProjetVue>
  fonctionsReferentiel(actif?: boolean): Promise<FonctionCatalogue[]>
  creerFonction(c: CreationFonction): Promise<FonctionCatalogue>
  modifierFonction(code: string, m: ModificationFonction): Promise<FonctionCatalogue>

  // Liaisons (HOM-04 à HOM-06, HOM-20, HOM-22) et cadrage (étape 3 du parcours, D-57, D-58)
  /** Filtres du service : etat, typeProjet, institutionId, mesProjets ; recherche libre ; pagination. */
  listerLiaisons(q: ListQuery): Promise<Page<Liaison>>
  tableauDeBordLiaisons(q: ListQuery): Promise<Page<LiaisonPilotage>>
  lireLiaison(id: string): Promise<Liaison>
  historiqueLiaison(id: string): Promise<EvenementChronologie[]>
  /** Ouvre la liaison en phase de cadrage ; MS-PARTICIPANT fait ensuite passer l'institution en homologation. */
  ouvrirLiaison(entree: OuvertureLiaison): Promise<Liaison>
  /** Désigne (ou remplace, motif à l'appui) le conducteur de projet ; renvoie la liaison à jour. */
  designerChefDeProjet(id: string, chefDeProjetId: string, motif?: string): Promise<Liaison>
  /** Conducteurs de projet habilités (titulaires de ROLE_CONDUCTEUR_PROJET). */
  conducteurs(): Promise<Conducteur[]>
  degelerLiaison(id: string, motif: string): Promise<void>
  exporterDossierDePreuves(id: string): Promise<void>
  dernierExportPreuves(id: string): Promise<ExportPreuves | null>
  grapheLiaison(id: string): Promise<Graphe>
  couverturesLiaison(id: string): Promise<ServiceCouvert[]>
  /** D-133 : périmètre du projet de certification, modifiable par son gestionnaire, un super administrateur ou un chargé SNA. */
  perimetreLiaison(id: string): Promise<PerimetreProjet>
  modifierPerimetre(id: string, services: string[], motif: string): Promise<PerimetreProjet>
  historiquePerimetre(id: string, page: number, taille: number): Promise<Page<ModificationPerimetre>>

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

  // Recherche pour les champs de formulaire (MS-IAM, relayée par la passerelle)
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
  /** Groupes ou maisons mères déclarés (D-104), triés : filtre de la liste. */
  groupes(): Promise<string[]>
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

/** Filtre des liaisons du parcours (étape 3 : « mes projets » du conducteur). */
export type FiltreLiaisons = { page: number; taille: number; recherche?: string; statut?: string; typeProjet?: string; etape?: string; mesProjets?: boolean; bloquees?: boolean; sansConducteur?: boolean }

/**
 * Parcours de la liaison, étapes 3 à 10 (D-56 à D-59) : dossier numérique, étapes à six éléments,
 * opérations du référentiel de l'annexe A avec leurs preuves, blocages datés, tableaux de bord par acteur.
 * Étape 3 : routes réelles de MS-HOMOLOGATION (/api/v1/liaisons) ; étapes 4 à 10 : routes à livrer.
 */
export interface ServiceParcours {
  // Étape 3 : cadrage (réel)
  listerLiaisons(f: FiltreLiaisons): Promise<Page<LiaisonResume>>
  conducteurs(): Promise<Personne[]>
  ouvrirLiaison(o: OuvertureCadrage): Promise<LiaisonResume>
  designerConducteur(liaisonId: string, conducteurId: string, motif: string): Promise<void>
  // Dossier numérique et progression
  dossier(liaisonId: string): Promise<DossierLiaison>
  operations(liaisonId: string, filtre?: { etape?: string; fonction?: Fonction }): Promise<OperationParcours[]>
  // Étapes 4 et 5 : lancement et ateliers
  affecterRessources(liaisonId: string, affectations: { fonction: Fonction; personneId: string }[]): Promise<void>
  /** Clôt une opération à livrable ; `livrable` (D-93) : code du livrable attendu visé (absent : l'unique livrable). */
  deposerLivrable(operationId: string, fichier: File, commentaire: string, livrable?: string, service?: string): Promise<void>
  attribuer(operationId: string, qualite: 'R' | 'C' | 'V' | 'I' | 'E', personneId: string, motif: string): Promise<void>
  // Exécution des opérations (preuve au grain de l'opération en production)
  deposerPreuve(operationId: string, fichier: File, complement: { valeurReference?: string; codeControle?: string; ceremonie?: Ceremonie; livrable?: string; service?: string }): Promise<void>
  /** D-109 : livrable déclaré sans objet, motivé (tous les services, ou un service pour un livrable au grain du service). */
  declarerSansObjet(operationId: string, livrable: string, motif: string, service?: string): Promise<void>
  /** D-109 : pièce d'une opération (preuve ou dépôt), servie avec l'empreinte inscrite au dépôt, revérifiée par le service. */
  telechargerPiece(operationId: string, documentId: string): Promise<{ blob: Blob; empreinte: string | null }>
  /** D-109 : chronologie réelle de la liaison (dépôts, sans objet, renvois, signatures, clôtures, reprises, blocages). */
  chronologie(liaisonId: string): Promise<EvenementPreuve[]>
  /** US-EXI-17 : résultats de la sandbox repris par MS-HOMOLOGATION (lecture seule, jamais recalculés). */
  resultatsSandbox(liaisonId: string): Promise<ResultatsSandbox>
  /** US-EXI-17 : exécutions retenues d'une campagne reprise, paginées (page à partir de 0, 1 à 100 par page). */
  executionsSandbox(liaisonId: string, campagneId: string, page: number, taille: number): Promise<PageExecutionsSandbox>
  /** D-121 : répartition d'une campagne reprise (par cas, par jour du fuseau du GIMAC), comptée par MS-HOMOLOGATION. */
  repartitionSandbox(liaisonId: string, campagneId: string): Promise<RepartitionSandbox>
  validerOperation(operationId: string, assertion?: Assertion): Promise<void>
  /** D-93 : renvoi motivé par le validateur (l'opération revient à faire, tâche rouverte à R). */
  renvoyer(operationId: string, motif: string): Promise<void>
  /** D-91 : signature attendue d'une opération (finalité SIGNATURE_OPERATION) ; `codeOperation` : code de l'opération du référentiel. */
  signerOperation(liaisonId: string, codeOperation: string, signature: string, assertion: Assertion): Promise<void>
  /** D-93 : affectation d'une fonction de la liaison à une personne (interne) ou une institution (externe). */
  affecterFonction(liaisonId: string, fonction: Fonction, a: { personneId?: string; institutionId?: string; motif?: string }): Promise<void>
  /** D-94 §6.5 : fonctions du catalogue que confèrent les profils du jeton (remplace toute table rôle → fonction). */
  mesFonctions(): Promise<FonctionUtilisateur[]>
  // Blocages datés (CR annexe B)
  declarerBlocage(liaisonId: string, b: { motif: string; prochaineAction: string; responsableId: string; echeanceLevee: string }): Promise<Blocage>
  leverBlocage(blocageId: string, commentaire: string): Promise<void>
  // Étape 7 : constat de la couverture (rang 17)
  constaterCouverture(liaisonId: string): Promise<void>
  // Étape 10 : service supplémentaire (rang 35) : nouvelle liaison, sans repasser par l'adhésion
  ouvrirServiceSupplementaire(liaisonId: string, services: string[]): Promise<LiaisonResume>
  // Tableaux de bord
  tableauActeur(fonctions: Fonction[]): Promise<TableauActeur>
  /** D-98 : mes actions de certification (à faire, à prendre, à suivre) et l'état des liaisons suivies, sans fonction exigée. */
  mesOperations(): Promise<MesOperations>
  pilotage(): Promise<IndicateursPilotage>
  personnes(fonction: Fonction): Promise<Personne[]>
}

/** Décision de mise en production, modèle hybride de D-56 : verrou 1, verrou 2, visas optionnels. */
export interface ServiceDecision {
  procesVerbal(liaisonId: string): Promise<ProcesVerbalCertification>
  etablirProcesVerbal(liaisonId: string): Promise<void>
  signerProcesVerbalGimac(liaisonId: string, assertion: Assertion): Promise<void>
  approbation(liaisonId: string): Promise<ApprobationSecurite>
  constituerDossierChangement(liaisonId: string, fichier: File): Promise<void>
  referencerFicheChangement(liaisonId: string, reference: string): Promise<void>
  relireFicheChangement(liaisonId: string): Promise<void>
  verifierAttestation(liaisonId: string, type: TypeAttestation, conforme: boolean, motif: string): Promise<void>
  archiverAttestations(liaisonId: string): Promise<void>
  signerVisa(liaisonId: string, nature: 'CONFORMITE' | 'DIRECTION_GENERALE', favorable: boolean, motif: string, assertion: Assertion): Promise<void>
  ficheSuivi(liaisonId: string): Promise<FicheSuiviServices>
  remettreFicheSuivi(liaisonId: string): Promise<void>
  constaterMiseEnService(liaisonId: string): Promise<void>
}

/** Portail du participant : les tâches qui lui incombent (qualité Externe, CR §3.4). */
export interface ServicePortailParcours {
  mesLiaisons(): Promise<LiaisonResume[]>
  dossier(liaisonId: string): Promise<DossierLiaison>
  mesTaches(): Promise<TableauActeur>
  communiquerCoordonnees(liaisonId: string, c: { adresse: string; port: string; pointsTerminaison?: string }): Promise<void>
  /** `livrable` et `service` (D-109) : livrable attendu visé et service du périmètre pour un livrable au grain du service. */
  deposerLivrable(operationId: string, fichier: File, commentaire: string, livrable?: string, service?: string): Promise<void>
  procesVerbal(liaisonId: string): Promise<ProcesVerbalCertification>
  signerProcesVerbal(liaisonId: string, assertion: Assertion): Promise<void>
  approbation(liaisonId: string): Promise<ApprobationSecurite>
  deposerAttestation(liaisonId: string, type: TypeAttestation, fichier: File): Promise<void>
  ficheSuivi(liaisonId: string): Promise<FicheSuiviServices>
  renseignerFicheSuivi(liaisonId: string, lignes: { serviceCode: string; ouvert: boolean; commentaire: string | null }[]): Promise<void>
}

export interface ServiceReauthentification {
  /** Lève ReauthCancelled si l'utilisateur annule ; aucune requête d'effet n'est alors envoyée. */
  assertion(purpose: SignPurpose): Promise<Assertion>
}

/** Messagerie instantanée de travail (D-64) : MS-COLLABORATION, /bff/des/collaboration/messagerie. */
export interface ServiceMessagerie {
  annuaire(recherche: string, page?: number, taille?: number): Promise<PageMessagerie<Correspondant>>
  conversations(q: { page?: number; taille?: number; recherche?: string; liaisonId?: string }): Promise<PageMessagerie<Conversation>>
  conversation(id: string): Promise<Conversation>
  /** Fil d'une conversation, du plus ancien au plus récent ; `avant` remonte vers le passé. */
  messages(id: string, avant?: string): Promise<FilMessages>
  /** Crée la conversation (ou réutilise la conversation directe existante) et y dépose le premier message. */
  demarrer(c: NouvelleConversation): Promise<Conversation>
  envoyer(conversationId: string, e: Envoi): Promise<Message>
  marquerLu(conversationId: string): Promise<void>
  /** Détail « Lu par » d'un de mes messages (D-88) : destinataires présents à l'envoi, lecteurs d'abord. */
  lectures(conversationId: string, messageId: string): Promise<LecteurMessage[]>
  nonLus(): Promise<{ total: number }>
  telechargerPiece(pieceId: string): Promise<{ blob: Blob; empreinte: string | null }>
  /** Groupes de discussion (D-68), réservés à l'administrateur du groupe ; le départ est ouvert à tout membre. */
  renommerGroupe(conversationId: string, titre: string): Promise<Conversation>
  ajouterMembres(conversationId: string, membres: string[]): Promise<Conversation>
  /** Retrait d'un membre, ou départ quand membreId est l'utilisateur courant. */
  retirerMembre(conversationId: string, membreId: string): Promise<void>
  /**
   * Dépôt fragmenté d'une pièce (claim check, D-74) : envoi par morceaux avec progression, puis ticket du
   * document à porter par le message (`documents`). `reference` : la conversation, si elle existe déjà.
   */
  deposer(fichier: File, reference: string, o?: OptionsTeleversement): Promise<Televersement>
  /** Adresse de téléchargement direct d'une pièce volumineuse (flux du navigateur), null si indisponible. */
  lienPiece(pieceId: string): string | null
  /** Présence des comptes internes (D-72) : état initial, tenu à jour par le canal poussé. */
  presence(): Promise<EtatPresence>
  /** « En train d'écrire » (D-72) : signal éphémère aux autres membres. */
  signalerSaisie(conversationId: string): Promise<void>
}

/**
 * Mon activité (D-76, docs/lot1/mon-activite.md) : MS-PILOTAGE derrière /bff/des/pilotage (agenda, activités,
 * tâches, rapports, équipe, synthèse) et l'organisation de MS-IAM (/bff/des/iam/organisation) pour les
 * libellés des unités et le rattachement de l'utilisateur. Le sujet du jeton est l'auteur ; les règles
 * d'équipe, de transition et de destinataire sont décidées par les services.
 */
export interface ServicePilotage {
  // Agenda (§2.1) : intervalle de jours inclus ; collaborateur : un membre de l'équipe (PIL_TEAM_READ).
  agenda(du: string, au: string, collaborateur?: string): Promise<Evenement[]>
  creerEvenement(e: EntreeEvenement): Promise<Evenement>
  modifierEvenement(id: string, e: EntreeEvenement): Promise<Evenement>
  supprimerEvenement(id: string): Promise<void>
  repondre(id: string, reponse: Exclude<ReponseInvitation, 'EN_ATTENTE'>): Promise<Evenement>
  // Activités du jour (§2.2)
  activites(du: string, au: string, collaborateur?: string): Promise<Activite[]>
  creerActivite(a: EntreeActivite): Promise<Activite>
  modifierActivite(id: string, a: EntreeActivite): Promise<Activite>
  /** Statut ; REPORTEE avec reporteeAu crée une copie A_FAIRE au jour indiqué. */
  changerStatut(id: string, statut: StatutActivite, reporteeAu?: string): Promise<Activite>
  supprimerActivite(id: string): Promise<void>
  // Tâches (§2.3), pagination à partir de 0 comme le service.
  taches(q: { vue: VueTaches; etat?: EtatTache; page?: number; taille?: number }): Promise<PagePilotage<Tache>>
  tache(id: string): Promise<Tache>
  /** Une tâche pour soi, pour une personne ou, avec uniteCible, à prendre par une unité (D-81 §6). */
  creerTache(t: EntreeTache): Promise<Tache>
  /** POST /taches avec responsables (2 à 50) : une tâche par personne, liées par le groupe rendu (D-81 §6). */
  creerGroupe(t: EntreeTache & { responsables: string[] }): Promise<GroupeTaches>
  modifierTache(id: string, t: EntreeTache): Promise<Tache>
  transition(id: string, etat: EtatTache, motif?: string): Promise<Tache>
  // Tâches confiées (D-81 §6) : suivi groupé, tâche à prendre (prise, attribution), relance.
  groupe(groupeId: string): Promise<GroupeTaches>
  prendre(id: string): Promise<Tache>
  attribuer(id: string, responsableId: string): Promise<Tache>
  /** Notification au responsable, une par 24 h au plus (429 PIL_RELANCE_RECENTE). */
  relancer(id: string): Promise<void>
  // Rapports (§2.4)
  rapports(q: { vue: VueRapports; type?: TypeRapport; etat?: EtatRapport; page?: number; taille?: number }): Promise<PagePilotage<Rapport>>
  rapport(id: string): Promise<Rapport>
  /** Contenu proposé à partir des activités et tâches de la période ; rien n'est enregistré. */
  preremplir(type: TypeRapport, periode: string): Promise<{ contenu: string }>
  creerRapport(r: EntreeRapport): Promise<Rapport>
  modifierRapport(id: string, r: EntreeRapport): Promise<Rapport>
  /**
   * Soumission au destinataire résolu (n+1 actif, chef de l'unité, chef d'une unité parente) ou choisi dans la chaîne
   * hiérarchique (`destinataireId`, sinon 422 PIL_DESTINATAIRE_INVALIDE) ; personne au-dessus : rapport DEPOSE.
   */
  soumettre(id: string, destinataireId?: string): Promise<Rapport>
  /** Destinataire et choix possibles avant la soumission (auteur seul). */
  destinatairesRapport(id: string): Promise<DestinatairesRapport>
  viser(id: string, commentaire: string): Promise<Rapport>
  documentRapport(id: string): Promise<{ blob: Blob; empreinte: string | null }>
  /** Dépôt fragmenté du document (D-74, finalité RAPPORT_ACTIVITE, référence : l'identifiant du rapport). */
  deposerDocument(fichier: File, rapportId: string, o?: OptionsTeleversement): Promise<Televersement>
  // Équipe et synthèse (§2.5, §2.6)
  equipe(jour: string): Promise<MembreEquipe[]>
  synthese(jour: string): Promise<Synthese>
  // Organisation (MS-IAM, §1.3)
  unites(): Promise<Unite[]>
  organisation(): Promise<Rattachement>
}

/**
 * Pouvoirs hiérarchiques (D-81, docs/lot1/pouvoirs-hierarchiques.md) : mes pouvoirs selon la matrice, demandes
 * adressées à l'administration (création de compte, profils, statut) et rattachement d'un subordonné.
 * Refus : ORG_HORS_PERIMETRE, ORG_PROFIL_NON_DEMANDABLE, ORG_DEMANDE_EXISTANTE, ORG_DEMANDE_TRAITEE, ORG_DEMANDE_INTERDITE…
 */
export interface ServicePouvoirs {
  moi(): Promise<MesPouvoirs>
  demander(d: NouvelleDemande): Promise<Demande>
  demandes(q: { etat?: EtatDemande; page?: number; taille?: number }): Promise<PageDemandes>
  demande(id: string): Promise<Demande>
  annuler(id: string): Promise<Demande>
  /** RATTACHEMENT_MODIFIER : unité (parmi les unités dirigées) et n+1 (dans l'équipe) d'un subordonné. */
  /** Les deux valeurs sont toujours envoyées : une valeur nulle retire l'unité ou le n+1 (D-81). */
  rattacher(id: string, r: { unite: string | null; responsableId: string | null; motif?: string | null }): Promise<unknown>
}

/**
 * Processus d'unité (D-87, docs/lot1/processus-unite.md §3 et §5) : MS-PILOTAGE derrière /bff/des/pilotage/processus.
 * Définitions versionnées (brouillon, contrôle, publication avec levée motivée, nouvelle version, retrait) et
 * instances (lancement, livrables par le dépôt fragmenté de MS-DOCUMENT, champs, signatures FIDO2, validation,
 * réaffectation, suspension, reprise, annulation). Refus PIL_PROCESSUS_* (§5.4).
 */
export interface ServiceProcessus {
  lister(q: { unite?: string; etat?: EtatProcessus; q?: string }): Promise<ResumeProcessus[]>
  /** D-92 : portée de l'appelant sur les processus (tous domaines ou ses unités) et ce qui la fonde. */
  portee(): Promise<PorteeProcessus>
  lire(id: string): Promise<Processus>
  creer(s: SaisieProcessus): Promise<Processus>
  modifier(id: string, s: SaisieProcessus): Promise<Processus>
  supprimer(id: string): Promise<void>
  controler(id: string): Promise<ControleProcessus>
  /** 422 PIL_PROCESSUS_ANOMALIES ou PIL_PROCESSUS_AVERTISSEMENTS (details.anomalies) ; levée : motif obligatoire. */
  publier(id: string, o: { motif?: string; leverAvertissements?: boolean }): Promise<Processus>
  nouvelleVersion(id: string): Promise<Processus>
  retirer(id: string, motif: string): Promise<Processus>
  /**
   * D-103 : public d'un brouillon ou d'une version publiée, sans nouvelle version. 403 PIL_PROCESSUS_INTERDIT, 409
   * PIL_PROCESSUS_NON_MODIFIABLE (version retirée), 400 PIL_REQUETE_INVALIDE (details.champ), 422 PIL_PROCESSUS_ANOMALIES
   * (details.anomalies, version publiée).
   */
  modifierPublic(id: string, s: SaisiePublic): Promise<Processus>
  instances(q: { vue: VueInstances; etat?: EtatInstance; page?: number; taille?: number }): Promise<PageInstances>
  /** D-98 : versions publiées que je peux lancer, groupées par unité propriétaire (recherche : code, libellé, catégorie). */
  lancables(q: { q?: string }): Promise<Lancables>
  /** D-98 : « Mes demandes », instances lancées par moi (état de chaque étape, qui la tient, dernière évolution). */
  demandes(q: { etat?: EtatInstance; q?: string; page?: number; taille?: number }): Promise<PageDemandesProcessus>
  /**
   * D-98 : pièce jointe du formulaire de lancement, dépôt fragmenté (D-74), finalité PROCESSUS_UNITE, référence = la
   * définition lancée ; le ticket part ensuite dans le formulaire ({ documentId }). 422 PIL_PROCESSUS_FORMULAIRE_INVALIDE
   * (fieldErrors, details.champs) et 403 PIL_PROCESSUS_NON_INITIATEUR au lancement.
   */
  televerserPieceLancement(fichier: File, processusId: string, o?: OptionsTeleversement): Promise<Televersement>
  lancer(id: string, l: Lancement): Promise<InstanceProcessus>
  instance(id: string): Promise<InstanceProcessus>
  /** PDF de la demande initiée, de ses champs et signatures, lisible de qui lit l'instance ; langue fr, en ou es (D-139). */
  telechargerPdf(instanceId: string, langue?: string): Promise<Blob>
  /**
   * QUO-04 : pièce du formulaire de lancement ou livrable déposé d'une instance (GET …/instances/{id}/documents/{doc}),
   * lisible de qui lit l'instance ; 403 PIL_PROCESSUS_INTERDIT, 404 PIL_PROCESSUS_DOCUMENT_INCONNU.
   */
  documentInstance(instanceId: string, documentId: string): Promise<{ blob: Blob; empreinte: string | null }>
  /**
   * Dépôt fragmenté d'un livrable (D-74) : finalité PROCESSUS_UNITE, référence = identifiant de l'instance ; le ticket
   * est ensuite rattaché à l'étape (deposerLivrable).
   */
  televerserLivrable(fichier: File, instanceId: string, o?: OptionsTeleversement): Promise<Televersement>
  deposerLivrable(instanceId: string, etape: string, livrable: string, documentId: string): Promise<InstanceProcessus>
  saisirChamps(instanceId: string, etape: string, valeurs: Record<string, string | number | null>): Promise<InstanceProcessus>
  /** Acte signé : assertion FIDO2 de finalité SIGNATURE_PROCESSUS (X-Reauth-Assertion). */
  signer(instanceId: string, etape: string, signature: string, assertion: Assertion): Promise<InstanceProcessus>
  terminer(instanceId: string, etape: string): Promise<InstanceProcessus>
  valider(instanceId: string, etape: string, decision: DecisionValidation, motif?: string): Promise<InstanceProcessus>
  reaffecter(instanceId: string, etape: string, personneId: string, motif: string): Promise<InstanceProcessus>
  /**
   * Candidats d'un intervenant (modélisateur, lancement) : porteurs du profil dans l'unité, puis ailleurs dans le GIMAC
   * (désignation hors de l'unité) ; 422 PIL_PROCESSUS_UNITE_INCONNUE.
   */
  candidats(q: { unite: string; profil?: string | null }): Promise<CandidatsProcessus>
  /** Candidats à l'exécution d'une étape en cours (réaffectation) ; 403 PIL_PROCESSUS_INTERDIT sans le droit de réaffecter. */
  candidatsEtape(instanceId: string, etape: string): Promise<CandidatsProcessus>
  suspendre(instanceId: string, motif: string): Promise<InstanceProcessus>
  reprendre(instanceId: string, motif?: string): Promise<InstanceProcessus>
  annuler(instanceId: string, motif: string): Promise<InstanceProcessus>
}

/**
 * Aperçu des documents bureautiques (D-83, docs/lot1/apercu-bureautique.md) : MS-DOCUMENT convertit en PDF le
 * contenu déjà téléchargé (POST /bff/{des|console}/document/apercus). Rejet : ApiError (413, 415, 422, 429, 503, 403).
 */
export interface ServiceApercu {
  convertir(source: Blob, nom: string): Promise<Blob>
}

export interface Services {
  participant: ServiceParticipant
  parcours: ServiceParcours
  decision: ServiceDecision
  portailParcours: ServicePortailParcours
  homologation: ServiceHomologation
  certification: ServiceCertification
  portail: ServicePortail
  reauth: ServiceReauthentification
  messagerie: ServiceMessagerie
  pilotage: ServicePilotage
  pouvoirs: ServicePouvoirs
  /** Processus d'unité (D-87), couche interne. */
  processus: ServiceProcessus
  /** Aperçu bureautique (D-83), couche interne ; absent, ces formats invitent au téléchargement. */
  apercu?: ServiceApercu
}
