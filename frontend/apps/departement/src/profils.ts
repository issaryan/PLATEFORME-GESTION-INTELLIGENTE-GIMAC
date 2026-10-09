import { lirePreference, lireSessionDemo, type Session } from '@gimac/socle'
import { COMPTES_ORGANISATION_DEMO, PROFILS_INTERNES_DEMO, roleDeCleDemo, sessionDemoDuRole } from '@gimac/lot1'

// Profils de démonstration de l'application du département (services simulés seulement), d'après les
// profils du domaine « gimac » (infra/keycloak/generer-realm-gimac.py, D-57, D-58, D-61) : les chefs du
// DES et de la DDRM ouvrent les liaisons et désignent leur conducteur (HOM_LIAISON_OPEN et
// HOM_PROJECT_DESIGNATE) ; l'ingénieur d'homologation n'ouvre plus ; le conducteur de projet lit et suit
// « Mes projets ». Profil choisi par gm.profilDemo (préférence locale) ou VITE_PROFIL_DEMO.
export const CLE_PROFIL_DEMO = 'gm.profilDemo'

export type ProfilDemo = { identifiant: string; nom: string; sujet: string; permissions: string[] }

// Un profil de démonstration par profil interne du domaine « gimac » (comptes internes communs aux trois
// applications, @gimac/lot1 profilsDemo.ts) : noms et identifiants de l'annuaire simulé de la messagerie (D-64).
// QUO-01 : plus les comptes de l'organisation simulée hors annuaire (service RH), clé = compte, profil dans `role`.
export const PROFILS_DEMO: Record<string, ProfilDemo> = { ...PROFILS_INTERNES_DEMO, ...COMPTES_ORGANISATION_DEMO }

/**
 * Profil de la session de démonstration ouverte par la connexion simulée (D-65), sinon demandé dans
 * l'adresse (?profil=ROLE_…), puis préférence locale, puis variable de construction ; chef de la DDRM à défaut.
 */
export function roleDemo(demande = lireSessionDemo()?.role ?? profilDeLAdresse() ?? lirePreference(CLE_PROFIL_DEMO) ?? import.meta.env.VITE_PROFIL_DEMO): string {
  return demande && PROFILS_DEMO[demande] ? demande : 'ROLE_CHEF_DDRM'
}

function profilDeLAdresse(): string | null {
  const demande = typeof window === 'undefined' ? null : new URLSearchParams(window.location.search).get('profil')
  return demande && PROFILS_DEMO[demande] ? demande : null
}

/**
 * Session de démonstration : second facteur réputé fait, matrice des rôles du profil ; restreinte pendant
 * le premier accès ouvert par la connexion simulée.
 */
export function sessionDemo(role = roleDemo()): Session {
  const demo = lireSessionDemo()
  // Session ouverte avec un profil qui n'a pas sa place ici (profil externe) : gardée telle quelle pour que
  // l'aiguillage (D-79) la conduise au portail, comme en production (recette ACC-01) ; jamais de profil par défaut.
  const etrangere = demo?.role && !PROFILS_DEMO[demo.role] ? sessionDemoDuRole(demo.role, demo.identifiant) : null
  if (etrangere) return etrangere
  const p = PROFILS_DEMO[role]
  return {
    sujet: p.sujet, nom: p.nom, identifiant: demo?.identifiant || p.identifiant, roles: [roleDeCleDemo(role)], permissions: p.permissions, institutionId: '', acr: '2', doubleFacteur: true,
    ...(demo?.premierAcces ? { restreinte: true } : {}),
  }
}
