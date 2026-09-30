import { lirePreference, type Session } from '@gimac/socle'
import { ANNUAIRE, PROFILS } from '@gimac/lot1'

// Profils de démonstration de l'application du département (services simulés seulement), d'après les
// profils du domaine « gimac » (infra/keycloak/generer-realm-gimac.py, D-57, D-58, D-61) : les chefs du
// DES et de la DDRM ouvrent les liaisons et désignent leur conducteur (HOM_LIAISON_OPEN et
// HOM_PROJECT_DESIGNATE) ; l'ingénieur d'homologation n'ouvre plus ; le conducteur de projet lit et suit
// « Mes projets ». Profil choisi par gm.profilDemo (préférence locale) ou VITE_PROFIL_DEMO.
export const CLE_PROFIL_DEMO = 'gm.profilDemo'

export type ProfilDemo = { identifiant: string; nom: string; sujet: string; permissions: string[] }

// Un profil de démonstration par profil interne du domaine « gimac » (matrice PROFILS de @gimac/lot1,
// reflet de generer-realm-gimac.py) ; noms et identifiants de l'annuaire simulé de la messagerie (D-64).
const identifiant = (nom: string) => {
  const [prenom, ...reste] = nom.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().split(' ')
  return `${prenom[0]}.${reste[0] ?? prenom}`
}
export const PROFILS_DEMO: Record<string, ProfilDemo> = Object.fromEntries(
  Object.entries(PROFILS)
    .filter(([role]) => role !== 'ROLE_CORRESPONDANT')
    .map(([role, permissions]) => {
      const p = ANNUAIRE.find((x) => x.profils?.includes(role)) ?? ANNUAIRE[0]
      return [role, { identifiant: identifiant(p.nom), nom: p.nom, sujet: p.id, permissions }]
    }),
)

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
