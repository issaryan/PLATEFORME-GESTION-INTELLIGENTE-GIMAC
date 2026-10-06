import type { CoverageValue, Partie, Permitted, Segment, TypeProjet } from './commun'
import type { EtapeCode, Fonction, GrainPreuve, PlateformeExecution, SegmentParcours } from './parcours'

// MS-HOMOLOGATION : référentiels (HOM-01 à HOM-03), liaisons (HOM-04 à HOM-24).

export type ReferentielVersion = Permitted & {
  id: string; typeProjet: TypeProjet; numero: number | null; libelle: string; etat: 'BROUILLON' | 'PUBLIEE' | 'RETIREE'
  dateCreation: string; datePublication: string | null; versionSourceId: string | null; versionActiveId: string | null
  /** Pilote du brouillon (D-77, §5) : compte qui reçoit la tâche « Compléter et publier ». */
  piloteId?: string | null
  nombreOperations?: number
  /** Lecture filtrée (D-89) : CONCERNE pour un lecteur qui ne voit que ses opérations. */
  filtre?: FiltreReferentiel
}

/** Lecture du référentiel (D-89 §1) : complète, ou réduite aux opérations qui concernent le lecteur. */
export type FiltreReferentiel = 'CONCERNE' | 'COMPLET'
/** Couleur d'une étape (D-89 §4) : jeton du design system, jamais une valeur libre. */
export type CouleurEtape = 'NEUTRE' | 'INFO' | 'SUCCES' | 'ALERTE' | 'CRITIQUE' | 'ACTION' | 'ACCENT'
/**
 * Étape d'une version du référentiel (D-89 §2, EtapeVue) : `jeton` est la variable CSS de sa couleur ; `canonique`
 * saisie (null : rattachée à celle de l'étape précédente), `canoniqueEffective` retenue pour la progression ;
 * `protegee` : liée à une étape canonique porteuse d'un jalon ou du verrou 2 ; `supprimable` : vide, non protégée, brouillon.
 */
export type EtapeReferentiel = {
  code: string; libelle: string; ordre: number; couleur: CouleurEtape; jeton: string; canonique: EtapeCode | null; canoniqueEffective: EtapeCode | null
  protegee: boolean; nombreOperations: number; supprimable: boolean
}
/** Création (code exigé) ou modification d'une étape (code et ordre ignorés) ; ordre : position, 1 = première. */
export type SaisieEtape = { code?: string; libelle: string; couleur: CouleurEtape | null; canonique: EtapeCode | null; ordre?: number | null }
/** Étape canonique du parcours (catalogue) : rang, protection (jalon ou verrou 2). */
export type EtapeCanonique = { code: EtapeCode; libelle: string; rang: number | null; protegee: boolean }
export type CouleurCatalogue = { code: CouleurEtape; libelle: string; jeton: string }
/** Opérations d'une version (GET …/operations) et lecture appliquée (en-tête X-Gimac-Filtre). */
export type OperationsReferentiel = { operations: ReferenceOperation[]; filtre: FiltreReferentiel }

// Modélisateur de processus (D-77, docs/lot1/modelisateur-processus.md §1) : formes servies par MS-HOMOLOGATION.
export type Geste = 'DEPOSER_LIVRABLE' | 'DEPOSER_PREUVE' | 'CONSTATER_COUVERTURE' | 'AFFECTER_RESSOURCES' | 'COORDONNEES_TECHNIQUES' | 'FICHE_SUIVI' | 'EVENEMENT_EXTERNE' | 'AUCUN'
export type Jalon = 'APTE_CERTIFICATION' | 'CERTIFIEE' | 'APPROUVEE_SECURITE' | 'EN_PRODUCTION'
export type Particularite = 'ENTREE_ESSAIS' | 'FIN_ESSAIS' | 'CEREMONIE_CLES' | 'SERVICE_SUPPLEMENTAIRE'
export type NatureLivrable = 'LIVRABLE' | 'PREUVE'
export type TypeAnomalie =
  | 'CODE_DUPLIQUE' | 'PREDECESSEUR_INCONNU' | 'SEGMENT_INCOHERENT' | 'CYCLE' | 'RESPONSABLE_ABSENT' | 'VALIDATEUR_ABSENT' | 'VALIDATEUR_EGAL_RESPONSABLE'
  | 'DECLENCHEUR_INCONNU' | 'JALON_DUPLIQUE' | 'JALON_DESORDONNE' | 'SANS_ENTREE' | 'LIVRABLE_DUPLIQUE' | 'SECRET_AVEC_VALEUR'
/** Livrable ou preuve attendu d'une opération, avec la fonction qui le dépose (externe : depuis le portail). */
export type Livrable = { code: string; libelle: string; nature: NatureLivrable; deposant: Fonction | null; typesPieces: string[]; obligatoire: boolean }
export type PositionNoeud = { x: number; y: number }

/** Saisie d'une opération (corps de PUT ; POST y ajoute le code). Position et particularité absentes : conservées. */
export type SaisieOperation = {
  /** Code d'une étape de la version (D-89 §2), plus une liste figée. */
  libelle: string; description: string | null; etape: string | null; segment: SegmentParcours | null; ordre: number | null
  responsable: Fonction | null; validateur: Fonction | null; consultes: Fonction[]; informes: Fonction[]; externes: Fonction[]
  predecesseurs: string[]; delaiJours: number | null; grain: GrainPreuve | null; plateforme: PlateformeExecution | null
  cle: boolean; secret: boolean; oubliSansSymptome: boolean; pivot: boolean; valeurReference: string | null
  livrables: Livrable[]; geste: Geste | null; jalon: Jalon | null; declencheur: string | null; particularite: Particularite | null
  position?: PositionNoeud | null
  // Attributs historiques de couverture (HOM-01), renvoyés tels quels pour ne pas être effacés.
  phase?: string | null; partie?: Partie | null; serviceCode?: string | null; natureCouverture?: 'CERTIFICATION' | 'MISE_EN_SERVICE' | 'AUCUNE' | null
  preuveAttendue?: Record<string, unknown> | null; idScenario?: string | null; fonctionResponsable?: string | null; grille?: Record<string, unknown> | null
}

/** Opération du référentiel telle que servie (GET /referentiels/{id}/operations) : saisie complète et successeurs déduits. */
export type ReferenceOperation = Partial<Permitted> & Omit<SaisieOperation, 'phase' | 'partie' | 'serviceCode' | 'natureCouverture' | 'preuveAttendue' | 'idScenario' | 'fonctionResponsable'> & {
  id?: string; versionId?: string; code: string; successeurs?: string[]
  /** Fantôme (D-89 §1) : voisin direct d'une opération qui me concerne, réduit à son code et à son libellé. */
  fantome?: boolean
  phase: string | null; partie: Partie | null; serviceCode: string | null
  natureCouverture: 'CERTIFICATION' | 'MISE_EN_SERVICE' | 'AUCUNE' | null; idScenario: string | null
  fonctionResponsable: string | null; preuveAttendue: Record<string, unknown> | null
}

/** Nœud du graphe étendu : l'opération à plat, sa couche de Kahn, ses dates au plus tôt (jours), le chemin critique et ses anomalies. */
export type NoeudGraphe = ReferenceOperation & { couche: number | null; debutAuPlusTot: number | null; finAuPlusTot: number | null; critique: boolean; anomalies: TypeAnomalie[] }
export type AnomalieGraphe = { type: TypeAnomalie; codes: string[]; motif: string }
export type GrapheReferentiel = {
  versionId: string; typeProjet: TypeProjet; numero: number | null; etat: ReferentielVersion['etat']; piloteId: string | null
  noeuds: NoeudGraphe[]; arcs: { de: string; vers: string; critique: boolean }[]; anomalies: AnomalieGraphe[]
  manquants: { code: string; attributs: string[] }[]; cheminCritique: { codes: string[]; dureeJours: number }; actionsPermises: string[]
  /** Étapes de la version (D-89), dans leur ordre ; absentes d'un service antérieur. */
  etapes?: EtapeReferentiel[]
  filtre?: FiltreReferentiel
}
/** Élément d'un catalogue du modélisateur : code, libellé français, précisions facultatives. */
export type EntreeCatalogue = { code: string; libelle: string; profils?: string[]; externe?: boolean; dedie?: boolean; rang?: number }
export type CatalogueModelisateur = {
  fonctions: EntreeCatalogue[]; gestes: EntreeCatalogue[]; jalons: EntreeCatalogue[]; declencheurs: EntreeCatalogue[]; etapes: EntreeCatalogue[]
  segments: EntreeCatalogue[]; plateformes: EntreeCatalogue[]; grains: EntreeCatalogue[]; naturesLivrable: EntreeCatalogue[]; particularites: EntreeCatalogue[]
  typesPieces: string[]; pilotes: { id: string; nom: string }[]
  /** D-89 : étapes canoniques, couleurs proposées et, avec ?version=, les étapes de cette version. */
  etapesCanoniques?: EtapeCanonique[]; couleurs?: CouleurCatalogue[]; etapesVersion?: EtapeReferentiel[]
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
  /** Codes des services du périmètre (TRANSVERSE toujours compris) ; vue LiaisonVue de MS-HOMOLOGATION. */
  perimetre?: string[]
  /** Institution, banque partenaire et conducteur de projet de la liaison. */
  intervenants?: Intervenant[]
}
export type LiaisonPilotage = Liaison & { delaiConsomme: number; delaiStandard: number }
export type OuvertureLiaison = { institutionId: string; typeProjet: TypeProjet; chefDeProjetId: string; banquePartenaireId?: string | null }

/** Rôle d'un intervenant de la liaison au cadrage (étape 3 du parcours, D-57, D-58). */
export type RoleIntervenant = 'INSTITUTION' | 'BANQUE_PARTENAIRE' | 'CHEF_DE_PROJET'
export type Intervenant = { role: RoleIntervenant; id: string; nom: string }
/** Conducteur de projet habilité (titulaire de ROLE_CONDUCTEUR_PROJET), proposé à la désignation. */
export type Conducteur = { id: string; nom: string }

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
