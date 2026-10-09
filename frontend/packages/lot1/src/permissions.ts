// Matrice des rôles du parcours (D-58, D-61) : l'interface n'affiche un tableau de bord ou une action que
// si le jeton porte la permission ; la passerelle (OPA) et les services refont le contrôle à chaque appel.
import { PROFILS_DOMAINE } from './domaine'

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
  /** Conduite des projets d'homologation (D-94 §6.5) : un conducteur de projet est un porteur de cette permission. */
  projetConduire: 'HOM_PROJECT_CONDUCT',
  /**
   * Fonctions opérationnelles du catalogue D-91 (D-97), sur le modèle de projetConduire : un porteur est candidat d'une
   * fonction à affecter (tâche à prendre) ; aucune n'ouvre un écran.
   */
  fonctionDdrm: 'HOM_FUNCTION_DDRM',
  fonctionProduction: 'HOM_FUNCTION_PRODUCTION',
  fonctionReseau: 'HOM_FUNCTION_NETWORK',
  fonctionSysteme: 'HOM_FUNCTION_SYSTEM',
  fonctionExploitation: 'HOM_FUNCTION_OPERATIONS',
  /** Administration des référentiels métier, tous domaines (D-92). */
  referentielsMetierAdmin: 'REF_METIER_ADMIN',
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

/**
 * Profils par fonction (D-58) : compositions du domaine Keycloak `gimac`, lues dans domaine-gimac.json qu'écrit
 * infra/keycloak/generer-realm-gimac.py (recette ADM-01, ADM-09) ; aucune recopie à la main.
 */
export const PROFILS: Record<string, string[]> = Object.fromEntries(PROFILS_DOMAINE.map((p) => [p.code, [...p.permissions]]))
