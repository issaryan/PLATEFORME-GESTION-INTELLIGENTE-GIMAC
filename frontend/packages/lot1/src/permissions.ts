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
} as const

/** Permissions nouvelles attendues du backend pour le modèle hybride (consignées dans docs/lot1/correspondance.md). */
export const PERMISSIONS_NOUVELLES = [PERM.etapeConduire, PERM.pvEtablir, PERM.securiteVerifier, PERM.portailSigner] as const

const COMMUN_DES = [PERM.institutionLire, PERM.liaisonLister, PERM.liaisonLire, PERM.pilotageLire, PERM.referentielLire, 'CER_CIRCUIT_READ', PERM.dossierLire, PERM.registreLire,
  PERM.messageLire, PERM.messageEnvoyer]

/** Profils par fonction (D-58), reflet du domaine Keycloak `gimac` et des permissions nouvelles. */
export const PROFILS: Record<string, string[]> = {
  ROLE_CHARGE_DDRM: [...COMMUN_DES, 'PAR_INSTITUTION_CREATE', 'PAR_INSTITUTION_EDIT', PERM.adhesionInstruire, 'PAR_TRANSITION_REQUEST', PERM.etapeConduire],
  ROLE_CHEF_DDRM: [...COMMUN_DES, 'PAR_INSTITUTION_CREATE', 'PAR_INSTITUTION_EDIT', PERM.adhesionInstruire, 'PAR_TRANSITION_REQUEST', PERM.transitionApprouver,
    PERM.liaisonOuvrir, PERM.projetDesigner, PERM.etapeConduire],
  ROLE_CHEF_DES: [...COMMUN_DES, PERM.liaisonOuvrir, PERM.projetDesigner, PERM.pvSignerGimac, 'HOM_LIAISON_UNFREEZE', 'HOM_DEADLINE_CONFIG', 'HOM_REFERENTIEL_MIGRATE',
    PERM.operationValider, 'HOM_EVIDENCE_FILE_EXPORT', PERM.attestationEmettre, 'CER_ATTESTATION_STATUS_REQUEST', PERM.registreExporter, PERM.etapeConduire],
  ROLE_CONDUCTEUR_PROJET: [...COMMUN_DES, PERM.operationAttribuer, PERM.preuveDeposer, PERM.operationValider, 'HOM_EVIDENCE_FILE_EXPORT', PERM.etapeConduire, PERM.pvEtablir],
  ROLE_SNA: [...COMMUN_DES, PERM.preuveDeposer, PERM.operationValider, PERM.dossierVerifier],
  ROLE_PRODUCTION_MONETIQUE: [...COMMUN_DES, PERM.preuveDeposer, PERM.ficheChangementLier, PERM.pvMiseEnServiceDeposer],
  ROLE_POLE_RESEAU: [...COMMUN_DES, PERM.preuveDeposer],
  ROLE_POLE_SYSTEME: [...COMMUN_DES, PERM.preuveDeposer],
  ROLE_RSSI: [...COMMUN_DES, PERM.securiteVerifier, PERM.preuveDeposer],
  ROLE_CONFORMITE: [...COMMUN_DES, PERM.visaConformite, PERM.ficheChangementLier],
  ROLE_DG: [...COMMUN_DES, PERM.visaDg, 'CER_ATTESTATION_STATUS_APPROVE'],
  ROLE_INGENIEUR_HOMOLOGATION: [...COMMUN_DES, PERM.operationAttribuer, PERM.preuveDeposer, PERM.operationValider, PERM.operationRouvrir, 'HOM_REFERENTIEL_EDIT',
    'HOM_REFERENTIEL_PUBLISH', 'HOM_GRID_EDIT', 'HOM_SCENARIO_QUALIFY', 'HOM_EVIDENCE_FILE_EXPORT', PERM.dossierVerifier, PERM.pvMiseEnServiceDeposer, PERM.attestationEmettre],
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
