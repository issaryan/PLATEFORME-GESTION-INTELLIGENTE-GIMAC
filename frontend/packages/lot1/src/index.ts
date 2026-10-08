// Lot 1 (PI-GIMAC-01) : ports, services, hooks de données, descripteurs et pages.
export type * from './modele/commun'
export type * from './modele/homologation'
export type * from './modele/certification'
export type * from './modele/portail'
export type * from './modele/participant'
export type * from './modele/pilotage'
export type * from './modele/pouvoirs'
// Noms déjà pris par les modèles de la certification, de l'homologation et du participant : renommés.
export type { EtapeCode, StatutParcours, SegmentParcours, GrainPreuve, PlateformeExecution, Fonction, Qualite, Personne as PersonneParcours, Raci, Sla, Preuve, EtatOperationParcours, OperationParcours, ElementEtape, EtatEtape, EtapeParcours, EtatServicePerimetre, ServicePerimetre, Blocage, EtatProcesVerbal, Signature, ProcesVerbalCertification, TypeAttestation, Attestation as AttestationParcours, EtatApprobation, Visa, ApprobationSecurite, FicheSuiviServices, DossierLiaison, LiaisonResume, TacheActeur, IndicateursPilotage, TableauActeur, OuvertureLiaison as OuvertureLiaisonParcours } from './modele/parcours'
export type * as Parcours from './modele/parcours'
export { ETAPES } from './modele/parcours'
export { referentielDe, type OperationReferentiel } from './modele/referentielCR'
export { PERM, PROFILS, PERMISSIONS_NOUVELLES } from './permissions'
export * from './dictionnaires'
export type { Services, ServicePilotage, ServicePouvoirs, ServiceParticipant, ServiceHomologation, ServiceCertification, ServicePortail, ServiceReauthentification, ServiceParcours, ServiceDecision, ServicePortailParcours, FiltreLiaisons, Assertion } from './services/ports'
export { ServicesProvider, useServices } from './services/ServicesProvider'
export { servicesHttp } from './services/http'
export { catalogue as catalogueMessagerie } from './i18n/ecrans/messagerie'
export { servicesSimules, type OptionsSimulation, type ServicesSimules } from './services/simule'
// Notifications de la plateforme (D-80) : aperçu lu auprès de la messagerie, MS-NOTIFICATION simulé de démonstration.
export { lecteurApercu, arriveeMessage, notificationsDemo, notificationsPortailDemo, type DemoNotifications } from './services/notifications'
export { Fournisseurs, creerClientRequetes } from './Fournisseurs'
export { ExecuteurActions, useExecuteurActions } from './ressources/actions'
export { useDescripteurDS, type Pilotes } from './ressources/descripteur'
export { useLecture, useEffet } from './hooks/base'
export { cles } from './hooks/cles'
export { useNonLus } from './hooks/messagerie'
export { useSourcesRecherche } from './recherche'
export { ExtensionsTableauDeBordProvider, type ExtensionsTableauDeBord } from './pages/des/tableau/extensions'
export { ANNUAIRE } from './services/simule/messagerie'
export { PROFILS_INTERNES_DEMO, IDENTITES_HORS_ANNUAIRE, identifiantDemo, type ProfilInterneDemo } from './profilsDemo'
// Mon activité (D-76) : organisation de démonstration (unités, rattachements), reprise par « Mon compte » et l'administration simulés.
export { ORGANISATION_DEMO, UNITES_DEMO, rattachementDemo } from './services/simule/pilotage'
export { TYPES_UNITE, LIMITES_GROUPE } from './modele/pilotage'
// Pouvoirs hiérarchiques (D-81) : matrice, portées, demandes ; MS-IAM simulé partagé avec l'administration.
export { ACTIONS_POUVOIR, PORTEES, porteesAdmisesParDefaut, AUCUN_POUVOIR, completerActions, TYPES_DEMANDE, ETATS_DEMANDE, MOTIF_DEMANDE, rangPortee, couvre, couvreUnites, couverts, estCreation, estProfils } from './modele/pouvoirs'
export { magasinPouvoirs, pouvoirsParDefaut, PROFILS_PRIVILEGIES, PROFILS_RESPONSABLES, RESPONSABLES_UNITES_DEMO, type MagasinPouvoirs } from './services/simule/pouvoirs'
// Profils et organisation (D-86) : libellé et mission des profils livrés, pour les services simulés.
export { PRESENTATIONS_PROFILS, type PresentationProfil } from './services/simule/presentations'
export { catalogues } from './i18n'
export { NAVIGATION_PORTAIL } from './navigation'
// Écrans pilotés par la configuration (D-94) : catalogue des fonctionnalités servi par MS-IAM, correspondance clé → écran.
export {
  WIDGETS, ADMINISTRATION, PLATEFORME, ECRANS_DES, ACCUEIL, urlConsole, versConsole, estOuverte, predicatDe, trier as trierFonctionnalites, accesFonctionnalite, libelleServi,
  fonctionnalitesOuvertes, widgetsOuverts, navigationDepuis, navigationRepli, type DefinitionFonctionnalite, type SaisieFonctionnalite, type EtatCatalogue, type PredicatServi,
  type DroitServi, type WidgetServi, type Application, type Ecran, type CleWidget,
} from './fonctionnalites'
export {
  CatalogueFonctionnalitesProvider, CatalogueIndisponible, GardeFonctionnalite, useCatalogueFonctionnalites, useFonctionnalitesOuvertes, useNavigationCatalogue, useTraduire,
} from './catalogue'
export { FONCTIONNALITES_INITIALES as FONCTIONNALITES_DEMO } from './services/simule/fonctionnalites'
// Rôles composés par fonctionnalité (D-90) : catalogue Module > Fonctionnalité > Droit dérivé du catalogue servi.
export {
  NATURES_DROIT, catalogueDroits, permissionsCatalogue, permissionPrivilegiee, etatFonctionnalite, basculerDroit,
  basculerFonctionnalite, comparerCompositions, type Droit, type NatureDroit, type FonctionnaliteDroits, type ModuleDroits,
} from './droits'
