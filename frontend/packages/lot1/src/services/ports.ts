import type { SignPurpose } from '@gimac/socle'
import type { EvenementChronologie, ListQuery, Option, Page } from '../modele/commun'
import type {
  Attribution, CampaignResult, ClotureOperation, Conducteur, ControleResultat, DepotResultat, EtatReprise, ExportPreuves, Graphe, ImpactMigration,
  CatalogueModelisateur, GrapheReferentiel, Liaison, LiaisonPilotage, OperationInstance, OuvertureLiaison, PointGrille, PreuveVersion, ReferenceOperation, ReferentielVersion, SaisieOperation,
  ScenarioInconnu, ServiceCouvert,
} from '../modele/homologation'
import type {
  Attestation, Circuit, Condition, DecisionQuitus, Dossier, EntreeRegistre, FicheLue, Horodatage, NatureQuitus, ProcesVerbal,
  Telechargement, TypeDemande,
} from '../modele/certification'
import type { PortailCertification, PortailLiaison, PortailOperation } from '../modele/portail'
import type { EtatPresence, OptionsTeleversement, Televersement } from '@gimac/socle'
import type { Conversation, Correspondant, Envoi, FilMessages, Message, NouvelleConversation, PageMessagerie } from '../modele/messagerie'
import type {
  Activite, EntreeActivite, EntreeEvenement, EntreeRapport, EntreeTache, EtatRapport, EtatTache, Evenement, MembreEquipe, PagePilotage, Rapport, Rattachement,
  ReponseInvitation, StatutActivite, Synthese, Tache, TypeRapport, Unite, VueRapports, VueTaches,
} from '../modele/pilotage'
import type {
  Candidature, ConditionsAdhesion, DossierInstitution, FiltreInstitutions, IndicateursDdrm, Institution, PieceAdhesion, Prerequis,
  StatutInstitution, TransitionStatut,
} from '../modele/participant'
import type {
  ApprobationSecurite, Blocage, DossierLiaison, Fonction, FicheSuiviServices, IndicateursPilotage, LiaisonResume, OperationParcours,
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
  operationsReferentiel(id: string): Promise<ReferenceOperation[]>
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
  catalogueReferentiels(): Promise<CatalogueModelisateur>
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
  /** Clôt une opération à livrable (compte rendu de réunion ou d'atelier, document de paramétrage…). */
  deposerLivrable(operationId: string, fichier: File, commentaire: string): Promise<void>
  attribuer(operationId: string, qualite: 'R' | 'C' | 'V' | 'I' | 'E', personneId: string, motif: string): Promise<void>
  // Exécution des opérations (preuve au grain de l'opération en production)
  deposerPreuve(operationId: string, fichier: File, complement: { valeurReference?: string; codeControle?: string; ceremonie?: Ceremonie }): Promise<void>
  validerOperation(operationId: string, assertion?: Assertion): Promise<void>
  // Blocages datés (CR annexe B)
  declarerBlocage(liaisonId: string, b: { motif: string; prochaineAction: string; responsableId: string; echeanceLevee: string }): Promise<Blocage>
  leverBlocage(blocageId: string, commentaire: string): Promise<void>
  // Étape 7 : constat de la couverture (rang 17)
  constaterCouverture(liaisonId: string): Promise<void>
  // Étape 10 : service supplémentaire (rang 35) : nouvelle liaison, sans repasser par l'adhésion
  ouvrirServiceSupplementaire(liaisonId: string, services: string[]): Promise<LiaisonResume>
  // Tableaux de bord
  tableauActeur(fonctions: Fonction[]): Promise<TableauActeur>
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
  deposerLivrable(operationId: string, fichier: File, commentaire: string): Promise<void>
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
  creerTache(t: EntreeTache): Promise<Tache>
  modifierTache(id: string, t: EntreeTache): Promise<Tache>
  transition(id: string, etat: EtatTache, motif?: string): Promise<Tache>
  // Rapports (§2.4)
  rapports(q: { vue: VueRapports; type?: TypeRapport; etat?: EtatRapport; page?: number; taille?: number }): Promise<PagePilotage<Rapport>>
  rapport(id: string): Promise<Rapport>
  /** Contenu proposé à partir des activités et tâches de la période ; rien n'est enregistré. */
  preremplir(type: TypeRapport, periode: string): Promise<{ contenu: string }>
  creerRapport(r: EntreeRapport): Promise<Rapport>
  modifierRapport(id: string, r: EntreeRapport): Promise<Rapport>
  soumettre(id: string): Promise<Rapport>
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
}
