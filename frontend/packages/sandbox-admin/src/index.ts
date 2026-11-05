// Espace sandbox de l'application du département (D-118 points 2 et 3, D-121, D-122) : une entrée par écran sous
// /des/sandbox (registre ENTREES_SANDBOX), services HTTP (passerelle) ou simulés au contrat exact de chaque service.
export type * from './modele'
export { PERMISSION, PERMISSION_SUPERVISION, ETATS_ESPACE } from './modele'
export type { ServiceEspaces, ServiceSupervision, ServicesSandbox } from './services/ports'
export type { EntreeSandbox } from './entrees/types'
export { ENTREES_SANDBOX, CHEMINS_SANDBOX, RoutesSandbox } from './entrees'
export { ServicesSandboxProvider, useServicesSandbox } from './services/contexte'
export { servicesSandboxHttp, espacesHttp, supervisionHttp } from './services/http'
export { servicesSandboxSimules, espacesSimules, REGISTRE_DEMO, ESPACES_DEMO, CORRESPONDANT_99004, type OptionsEspacesSimules, type InstitutionRegistre } from './services/simule'
export { supervisionSimulee, SANTE_DEMO, ELEMENTS_DEMO, ALERTES_DEMO, DISPONIBILITES_DEMO, SERVICES_DEMO, type OptionsSupervisionSimulee } from './services/supervisionSimulee'
export { EspaceInstitution, actesPossibles } from './composants/EspaceInstitution'
export { catalogues as cataloguesSandbox } from './i18n'
