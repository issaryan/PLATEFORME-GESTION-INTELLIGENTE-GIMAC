// Administration de la plateforme (D-62, D-69) : comptes, profils, journal, politique de sécurité,
// supervision, configuration et infrastructure. Écrans communs à la console (/console) et à l'application interne
// (/des/administration, /des/plateforme).
export type * from './modele/comptes'
export type * from './modele/configuration'
export type * from './modele/plateforme'
export type * from './modele/infrastructure'
export type { ServicesConsole, ServiceComptes, ServiceConfiguration, ServicePlateforme, ServiceInfrastructure, ServiceReauthentification, Assertion } from './services/ports'
export { ServicesConsoleProvider } from './services/ServicesConsole'
export { CHEMINS_CONSOLE, type CheminsAdministration } from './services/contexte'
export { useChemins, useServicesConsole } from './services/useServicesConsole'
export { useSourceComptes } from './recherche'
export { TableauAdministrationComptes, SectionGestionUtilisateurs, SectionActes } from './composants/sectionsTableau'
// Widgets de plateforme des tableaux de bord (D-79) : résumé de la supervision et charges en erreur (D-78).
export { WidgetSupervision, WidgetChargesEnErreur } from './composants/widgetsPlateforme'
export { SectionSupervision } from './composants/supervision'
export { servicesConsoleHttp } from './services/http'
export { servicesConsoleSimules, type OptionsConsoleSimulee } from './services/simule'
export { creerClientRequetes } from './services/clientRequetes'
export { Fournisseurs } from './Fournisseurs'
export { CLE_PREMIER_ACCES_DEMO, CLE_PROFIL_DEMO, PROFILS_DEMO } from './profilsDemo'
export { catalogues as cataloguesAdministration } from './i18n'
