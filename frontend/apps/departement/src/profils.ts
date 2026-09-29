import { lirePreference, type Session } from '@gimac/socle'

// Profils de démonstration de l'application du département (services simulés seulement), d'après les
// profils du domaine « gimac » (infra/keycloak/generer-realm-gimac.py, D-57, D-58, D-61) : les chefs du
// DES et de la DDRM ouvrent les liaisons et désignent leur conducteur (HOM_LIAISON_OPEN et
// HOM_PROJECT_DESIGNATE) ; l'ingénieur d'homologation n'ouvre plus ; le conducteur de projet lit et suit
// « Mes projets ». Profil choisi par gm.profilDemo (préférence locale) ou VITE_PROFIL_DEMO.
export const CLE_PROFIL_DEMO = 'gm.profilDemo'

const LECTURE_DES = ['HOM_REFERENTIEL_READ', 'HOM_LIAISON_READ', 'HOM_LIAISON_LIST', 'HOM_DASHBOARD_READ', 'CER_CIRCUIT_READ', 'CER_DOSSIER_READ', 'CER_REGISTER_READ', 'PAR_INSTITUTION_READ']
const DDRM = ['PAR_INSTITUTION_CREATE', 'PAR_INSTITUTION_EDIT', 'PAR_ADHESION_INSTRUCT', 'PAR_TRANSITION_REQUEST']
const CADRAGE = ['HOM_LIAISON_OPEN', 'HOM_PROJECT_DESIGNATE']

export type ProfilDemo = { identifiant: string; nom: string; sujet: string; permissions: string[] }

export const PROFILS_DEMO: Record<string, ProfilDemo> = {
  ROLE_CHEF_DDRM: { identifiant: 'b.nguema', nom: 'Bertrand Nguema Obiang', sujet: 'U-020', permissions: [...LECTURE_DES, ...DDRM, 'PAR_TRANSITION_APPROVE', ...CADRAGE] },
  ROLE_CHARGE_DDRM: { identifiant: 'f.abakar', nom: 'Fatimé Abakar Hissein', sujet: 'sim-charge', permissions: [...LECTURE_DES, ...DDRM] },
  ROLE_CHEF_DES: {
    identifiant: 'p.mbarga', nom: 'Paul Mbarga Atangana', sujet: 'U-001',
    permissions: [...LECTURE_DES, 'HOM_REFERENTIEL_MIGRATE', 'HOM_LIAISON_UNFREEZE', 'HOM_OPERATION_VALIDATE', 'HOM_DEADLINE_CONFIG', 'HOM_EVIDENCE_FILE_EXPORT',
      'CER_QUITUS_TECHNIQUE_SIGN', 'CER_ATTESTATION_ISSUE', 'CER_REGISTER_EXPORT', 'CER_ATTESTATION_STATUS_REQUEST', ...CADRAGE],
  },
  ROLE_INGENIEUR_HOMOLOGATION: {
    identifiant: 'j.ekotto', nom: 'Joël Ekotto Mballa', sujet: 'U-101',
    permissions: [...LECTURE_DES, 'HOM_REFERENTIEL_EDIT', 'HOM_REFERENTIEL_PUBLISH', 'HOM_OPERATION_ASSIGN', 'HOM_EVIDENCE_SUBMIT', 'HOM_OPERATION_VALIDATE',
      'HOM_OPERATION_REOPEN', 'HOM_GRID_EDIT', 'HOM_REPLAY_ADMIN', 'HOM_SCENARIO_QUALIFY', 'HOM_EVIDENCE_FILE_EXPORT', 'CER_DOSSIER_VERIFY', 'CER_GOLIVE_REPORT_SUBMIT',
      'CER_ATTESTATION_ISSUE'],
  },
  ROLE_CONDUCTEUR_PROJET: {
    identifiant: 'a.mahamat', nom: 'Aïcha Mahamat Nour', sujet: 'U-201', permissions: [...LECTURE_DES, 'HOM_OPERATION_ASSIGN', 'HOM_EVIDENCE_SUBMIT', 'HOM_EVIDENCE_FILE_EXPORT'],
  },
  ROLE_SNA: { identifiant: 'r.biyong', nom: 'Rachel Biyong Mengue', sujet: 'u-sna', permissions: [...LECTURE_DES, 'HOM_EVIDENCE_SUBMIT', 'HOM_OPERATION_VALIDATE'] },
}

/** Profil demandé dans l'adresse (?profil=ROLE_…), sinon préférence locale, puis variable de construction ; chef de la DDRM à défaut. */
export function roleDemo(demande = profilDeLAdresse() ?? lirePreference(CLE_PROFIL_DEMO) ?? import.meta.env.VITE_PROFIL_DEMO): string {
  return demande && PROFILS_DEMO[demande] ? demande : 'ROLE_CHEF_DDRM'
}

function profilDeLAdresse(): string | null {
  const demande = typeof window === 'undefined' ? null : new URLSearchParams(window.location.search).get('profil')
  return demande && PROFILS_DEMO[demande] ? demande : null
}

/** Session de démonstration : second facteur réputé fait, matrice des rôles du profil. */
export function sessionDemo(role = roleDemo()): Session {
  const p = PROFILS_DEMO[role]
  return { sujet: p.sujet, nom: p.nom, identifiant: p.identifiant, roles: [role], permissions: p.permissions, institutionId: '', acr: '2', doubleFacteur: true }
}
