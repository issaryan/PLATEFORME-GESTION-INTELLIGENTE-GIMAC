import type { Fonction } from './modele/parcours'

// Matrice des rôles du parcours (D-58, D-61) : l'interface n'affiche un tableau de bord ou une action que
// si le jeton porte la permission ; la passerelle (OPA) et les services refont le contrôle à chaque appel.

export const PERM = {
  // Étapes 1 et 2 (MS-PARTICIPANT)
  institutionLire: 'PAR_INSTITUTION_READ',
  adhesionInstruire: 'PAR_ADHESION_INSTRUCT',
  transitionApprouver: 'PAR_TRANSITION_APPROVE',
  // Étape 3 et conduite de projet (MS-HOMOLOGATION)
  liaisonLister: 'HOM_LIAISON_LIST',
  liaisonLire: 'HOM_LIAISON_READ',
  liaisonOuvrir: 'HOM_LIAISON_OPEN',
  projetDesigner: 'HOM_PROJECT_DESIGNATE',
  pilotageLire: 'HOM_DASHBOARD_READ',
  operationAttribuer: 'HOM_OPERATION_ASSIGN',
  preuveDeposer: 'HOM_EVIDENCE_SUBMIT',
  operationValider: 'HOM_OPERATION_VALIDATE',
  operationRouvrir: 'HOM_OPERATION_REOPEN',
  referentielLire: 'HOM_REFERENTIEL_READ',
  /** Lecture complète du référentiel (D-89 §1) ; sans elle ni droit d'édition, seulement les opérations qui me concernent. */
  referentielLireTout: 'HOM_REFERENTIEL_READ_ALL',
  referentielModifier: 'HOM_REFERENTIEL_EDIT',
  referentielPublier: 'HOM_REFERENTIEL_PUBLISH',
  /** Nouvelle (D-56, D-59) : conduite des étapes 4 à 10 (compte rendu de lancement, ateliers, blocages, jalons). */
  etapeConduire: 'HOM_STEP_MANAGE',
  // Verrou 1 : procès-verbal de certification (D-56)
  /** Nouvelle : établir le procès-verbal (conducteur de projet, rang 18). */
  pvEtablir: 'CER_PV_ESTABLISH',
  /** Signature GIMAC du procès-verbal : le « quitus technique » du plan (D-56). */
  pvSignerGimac: 'CER_QUITUS_TECHNIQUE_SIGN',
  // Verrou 2 : approbation de la sécurité (D-56)
  ficheChangementLier: 'CER_CHANGE_RECORD_LINK',
  /** Nouvelle : vérifier et archiver les attestations du participant (RSSI, rang 23). */
  securiteVerifier: 'CER_SECURITY_VERIFY',
  visaConformite: 'CER_QUITUS_CONFORMITE_SIGN',
  visaDg: 'CER_QUITUS_DG_SIGN',
  dossierLire: 'CER_DOSSIER_READ',
  dossierVerifier: 'CER_DOSSIER_VERIFY',
  pvMiseEnServiceDeposer: 'CER_GOLIVE_REPORT_SUBMIT',
  attestationEmettre: 'CER_ATTESTATION_ISSUE',
  registreLire: 'CER_REGISTER_READ',
  registreExporter: 'CER_REGISTER_EXPORT',
  // Portail du participant
  portailLire: 'HOM_PORTAL_READ',
  portailDeposer: 'HOM_PORTAL_SUBMIT',
  portailCertification: 'CER_PORTAL_READ',
  /** Nouvelle : signature du procès-verbal par le participant (rang 19). */
  portailSigner: 'CER_PORTAL_SIGN',
  // Messagerie de travail entre acteurs internes (D-64, MS-COLLABORATION)
  messageLire: 'COL_MESSAGE_READ',
  messageEnvoyer: 'COL_MESSAGE_SEND',
  // Notifications de la plateforme (D-80, MS-NOTIFICATION) : cloche, page et préférences ; tout profil interne.
  notificationsLire: 'NOT_INBOX_READ',
  // Mon activité (D-76, MS-PILOTAGE) : agenda, activités, tâches et rapports de chacun ; équipe (lecture,
  // visa des rapports reçus) et tâches confiées aux membres de son équipe.
  activitePropre: 'PIL_ACTIVITY_OWN',
  equipeLire: 'PIL_TEAM_READ',
  tacheConfier: 'PIL_TASK_ASSIGN',
  // Administration des comptes et de la plateforme, portée par la console (D-62) : l'application du
  // département n'y renvoie que par des liens (groupe « Administration » de la navigation).
  comptesLire: 'IAM_ACCOUNT_READ',
  compteCreer: 'IAM_ACCOUNT_CREATE',
  /** Composition des profils et matrice des pouvoirs (D-81). */
  profilEditer: 'IAM_PROFILE_EDIT',
  journalAdministrationLire: 'IAM_JOURNAL_READ',
  politiqueLire: 'PLATFORM_SECURITY_POLICY_READ',
  configurationLire: 'PLATFORM_CONFIG_READ',
  supervisionLire: 'PLATFORM_SUPERVISION_READ',
  // Exploitation de l'infrastructure (D-78) : lecture des charges, journaux et historique ; gestes.
  exploitationLire: 'PLATFORM_OPS_READ',
  exploitationExecuter: 'PLATFORM_OPS_EXECUTE',
  courrielsPlateforme: 'PLATFORM_MESSAGING_EDIT',
} as const

/** Permissions nouvelles attendues du backend pour le modèle hybride (consignées dans docs/lot1/correspondance.md). */
export const PERMISSIONS_NOUVELLES = [PERM.etapeConduire, PERM.pvEtablir, PERM.securiteVerifier, PERM.portailSigner] as const

/** Administration des comptes (D-62), comme le domaine Keycloak : toutes les permissions IAM sauf l'octroi privilégié. */
const IAM_SANS_PRIVILEGE = ['IAM_ACCOUNT_READ', 'IAM_ACCOUNT_CREATE', 'IAM_ACCOUNT_EDIT', 'IAM_ACCOUNT_ROLES', 'IAM_ACCOUNT_STATUS', 'IAM_ACCOUNT_DELETE',
  'IAM_ACCOUNT_SECURITY', 'IAM_JOURNAL_READ', 'IAM_PROFILE_EDIT']

/** Messagerie (D-64), « Mon activité » (D-76) et notifications de la plateforme (D-80) : tout profil interne. */
const INTERNE = [PERM.messageLire, PERM.messageEnvoyer, PERM.activitePropre, PERM.notificationsLire]
/** Équipe et tâches confiées (D-76 §4) : chefs de département et de service, DG, RSSI, conformité. */
const RESPONSABLE = [PERM.equipeLire, PERM.tacheConfier]
/** Référentiel des opérations (D-89 §1, §4) : édition et publication, lecture complète. */
const REFERENTIEL_EDITION = [PERM.referentielLire, PERM.referentielModifier]

const COMMUN_DES = [PERM.institutionLire, PERM.liaisonLister, PERM.liaisonLire, PERM.pilotageLire, PERM.referentielLire, 'CER_CIRCUIT_READ', PERM.dossierLire, PERM.registreLire,
  ...INTERNE]

/** Profils par fonction (D-58), reflet du domaine Keycloak `gimac` et des permissions nouvelles. */
export const PROFILS: Record<string, string[]> = {
  ROLE_CHARGE_DDRM: [...COMMUN_DES, 'PAR_INSTITUTION_CREATE', 'PAR_INSTITUTION_EDIT', PERM.adhesionInstruire, 'PAR_TRANSITION_REQUEST', PERM.etapeConduire],
  ROLE_CHEF_DDRM: [...COMMUN_DES, 'PAR_INSTITUTION_CREATE', 'PAR_INSTITUTION_EDIT', PERM.adhesionInstruire, 'PAR_TRANSITION_REQUEST', PERM.transitionApprouver,
    PERM.liaisonOuvrir, PERM.projetDesigner, PERM.etapeConduire, ...RESPONSABLE, PERM.referentielModifier, PERM.referentielPublier, PERM.referentielLireTout],
  ROLE_CHEF_DES: [...COMMUN_DES, PERM.liaisonOuvrir, PERM.projetDesigner, PERM.pvSignerGimac, 'HOM_LIAISON_UNFREEZE', 'HOM_DEADLINE_CONFIG', 'HOM_REFERENTIEL_MIGRATE',
    PERM.operationValider, 'HOM_EVIDENCE_FILE_EXPORT', PERM.attestationEmettre, 'CER_ATTESTATION_STATUS_REQUEST', PERM.registreExporter, PERM.etapeConduire, ...RESPONSABLE,
    PERM.referentielModifier, PERM.referentielPublier, PERM.referentielLireTout],
  ROLE_CONDUCTEUR_PROJET: [...COMMUN_DES, PERM.operationAttribuer, PERM.preuveDeposer, PERM.operationValider, 'HOM_EVIDENCE_FILE_EXPORT', PERM.etapeConduire, PERM.pvEtablir],
  ROLE_SNA: [...COMMUN_DES, PERM.preuveDeposer, PERM.operationValider, PERM.dossierVerifier],
  ROLE_PRODUCTION_MONETIQUE: [...COMMUN_DES, PERM.preuveDeposer, PERM.ficheChangementLier, PERM.pvMiseEnServiceDeposer],
  ROLE_POLE_RESEAU: [...COMMUN_DES, PERM.preuveDeposer],
  ROLE_POLE_SYSTEME: [...COMMUN_DES, PERM.preuveDeposer],
  // RSSI (generer-realm-gimac.py) : vérification de la sécurité, administration des comptes sans octroi
  // privilégié, supervision et configuration en lecture, politique de sécurité et messagerie de la plateforme.
  ROLE_RSSI: [...COMMUN_DES, 'CER_QUITUS_SECURITE_SIGN', PERM.securiteVerifier, PERM.preuveDeposer, ...IAM_SANS_PRIVILEGE,
    'PLATFORM_SUPERVISION_READ', 'PLATFORM_CONFIG_READ', 'PLATFORM_OPS_READ', 'PLATFORM_SECURITY_POLICY_READ', 'PLATFORM_SECURITY_POLICY_EDIT', 'PLATFORM_MESSAGING_EDIT', ...RESPONSABLE, PERM.referentielLireTout],
  // Super administrateur (generer-realm-gimac.py) : lecture du DES, toute l'administration des comptes dont
  // l'octroi privilégié, supervision et configuration de la plateforme (D-62, D-69).
  ROLE_SUPER_ADMIN: [...COMMUN_DES, ...IAM_SANS_PRIVILEGE, 'IAM_ROLE_PRIVILEGED_GRANT', 'PLATFORM_SUPERVISION_READ', 'PLATFORM_CONFIG_READ', 'PLATFORM_CONFIG_EDIT',
    'PLATFORM_OPS_READ', 'PLATFORM_OPS_EXECUTE',
    'PLATFORM_SECURITY_POLICY_READ', 'PLATFORM_SECURITY_POLICY_EDIT', 'PLATFORM_MESSAGING_EDIT', PERM.referentielModifier, PERM.referentielPublier],
  ROLE_CONFORMITE: [...COMMUN_DES, PERM.visaConformite, PERM.ficheChangementLier, ...RESPONSABLE, PERM.referentielLireTout],
  ROLE_DG: [...COMMUN_DES, PERM.visaDg, 'CER_ATTESTATION_STATUS_APPROVE', ...RESPONSABLE, PERM.referentielLireTout],
  ROLE_INGENIEUR_HOMOLOGATION: [...COMMUN_DES, PERM.operationAttribuer, PERM.preuveDeposer, PERM.operationValider, PERM.operationRouvrir, PERM.referentielModifier,
    PERM.referentielPublier, 'HOM_GRID_EDIT', 'HOM_SCENARIO_QUALIFY', 'HOM_EVIDENCE_FILE_EXPORT', PERM.dossierVerifier, PERM.pvMiseEnServiceDeposer, PERM.attestationEmettre],
  // D-76 §4 : employé sans fonction métier dans la plateforme (messagerie et « Mon activité », lecture de rien
  // d'autre) ; chef de service, de pôle ou de cellule (en plus : son équipe et les tâches qu'il lui confie).
  ROLE_COLLABORATEUR: [...INTERNE],
  ROLE_RESPONSABLE_SERVICE: [...INTERNE, ...RESPONSABLE, ...REFERENTIEL_EDITION],
  // Administrateur système (D-78, generer-realm-gimac.py) : supervision, configuration en lecture et exploitation
  // de l'infrastructure (charges, journaux, gestes) ; messagerie et « Mon activité » ; n'administre pas les comptes.
  ROLE_ADMIN_SYSTEME: [...INTERNE, PERM.supervisionLire, PERM.configurationLire, PERM.exploitationLire, PERM.exploitationExecuter],
  ROLE_CORRESPONDANT: [PERM.portailLire, PERM.portailDeposer, PERM.portailCertification, PERM.portailSigner],
}

/** Fonction du parcours portée par un profil : sert à composer son tableau de bord. */
export const FONCTION_DU_ROLE: Record<string, Fonction> = {
  ROLE_CHARGE_DDRM: 'DDRM',
  ROLE_CHEF_DDRM: 'CHEF_DDRM',
  ROLE_CHEF_DES: 'CHEF_DES',
  ROLE_CONDUCTEUR_PROJET: 'CONDUCTEUR',
  ROLE_SNA: 'SNA',
  ROLE_PRODUCTION_MONETIQUE: 'PRODUCTION_MONETIQUE',
  ROLE_POLE_RESEAU: 'POLE_RESEAU',
  ROLE_POLE_SYSTEME: 'POLE_SYSTEME',
  ROLE_RSSI: 'RSSI',
  ROLE_CONFORMITE: 'CONFORMITE',
  ROLE_DG: 'DG',
  ROLE_INGENIEUR_HOMOLOGATION: 'SNA',
  ROLE_CORRESPONDANT: 'PARTICIPANT',
}

/** Fonctions portées par une session (un utilisateur peut détenir plusieurs profils). */
export function fonctionsDe(roles: string[]): Fonction[] {
  return [...new Set(roles.map((r) => FONCTION_DU_ROLE[r]).filter((f): f is Fonction => Boolean(f)))]
}
