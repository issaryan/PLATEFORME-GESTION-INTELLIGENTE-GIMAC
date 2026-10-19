import type { Session } from '@gimac/socle'
import { PROFILS } from './permissions'
import { PROFILS_DOMAINE } from './domaine'
import { ANNUAIRE } from './services/simule/messagerie'
import { ORGANISATION_DEMO, PROFILS_ORGANISATION_DEMO } from './services/simule/pilotage'

// Comptes internes de démonstration (services simulés seulement, D-57, D-58, D-79) : un par profil interne du
// domaine « gimac » (matrice PROFILS, reflet de generer-realm-gimac.py), avec l'identité de l'annuaire simulé de
// la messagerie ; le super administrateur et l'administrateur système, hors annuaire, ont la leur. Communs aux
// trois applications : un même identifiant ouvre son compte partout, et chaque application l'aiguille.

export type ProfilInterneDemo = { identifiant: string; nom: string; sujet: string; permissions: string[] }

/** Identités hors annuaire de la messagerie (mêmes que dans la console). */
export const IDENTITES_HORS_ANNUAIRE: Record<string, Omit<ProfilInterneDemo, 'permissions'>> = {
  ROLE_SUPER_ADMIN: { identifiant: 'super.admin', nom: 'Serge Mouelle Kombi', sujet: 'u-super-admin' },
  ROLE_ADMIN_SYSTEME: { identifiant: 'admin.systeme', nom: 'Thierry Abessolo Nkoulou', sujet: 'u-admin-systeme' },
}

/** Identifiant de démonstration d'une personne : initiale du prénom, point, premier nom (sans accent). */
export function identifiantDemo(nom: string): string {
  const [prenom, ...reste] = nom.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase().split(' ')
  return `${prenom[0]}.${reste[0] ?? prenom}`
}

/** Profils externes du domaine (attribut gimac_externe, D-94) : jamais repérés par leur nom. */
const EXTERNES = new Set(PROFILS_DOMAINE.filter((p) => p.externe).map((p) => p.code))

export const PROFILS_INTERNES_DEMO: Record<string, ProfilInterneDemo> = Object.fromEntries(
  Object.entries(PROFILS)
    .filter(([role]) => !EXTERNES.has(role))
    .map(([role, permissions]) => {
      const hors = IDENTITES_HORS_ANNUAIRE[role]
      if (hors) return [role, { ...hors, permissions }]
      const p = ANNUAIRE.find((x) => x.profils?.includes(role)) ?? ANNUAIRE[0]
      return [role, { identifiant: identifiantDemo(p.nom), nom: p.nom, sujet: p.id, permissions }]
    }),
)

/** Institution de démonstration des comptes de profil externe (correspondant de l'institution INST-0042). */
export const INSTITUTION_DEMO = 'INST-0042'

/**
 * Session d'un rôle quelconque du domaine, quelle que soit l'application qui la lit (recette ACC-01) : compte interne
 * de démonstration, ou compte d'institution pour un profil externe (attribut gimac_externe du domaine). Les gardes
 * d'aiguillage (D-79) la conduisent alors vers son application, comme en production ; jamais de profil par défaut.
 * Null pour un rôle inconnu du domaine.
 */
export function sessionDemoDuRole(role: string, identifiant?: string): Session | null {
  const interne = PROFILS_INTERNES_DEMO[role]
  if (interne) return { sujet: interne.sujet, nom: interne.nom, identifiant: identifiant || interne.identifiant, roles: [role], permissions: interne.permissions, institutionId: '', acr: '2', doubleFacteur: true }
  const profil = PROFILS_DOMAINE.find((p) => p.code === role)
  if (!profil?.externe) return null
  return { sujet: 'demo-correspondant', nom: profil.libelle, identifiant: identifiant || 'correspondant-a', roles: [role], permissions: [...profil.permissions], institutionId: INSTITUTION_DEMO, acr: '2', doubleFacteur: true }
}

/**
 * QUO-01 : comptes de l'organisation simulée absents de l'annuaire de la messagerie (service des ressources humaines), un
 * compte de démonstration chacun avec le premier profil qu'il porte (données de PROFILS_ORGANISATION_DEMO, aucun nom de
 * profil ici). Clé : l'identifiant du compte, distincte des profils ; `role` porte le profil de la session.
 */
export const COMPTES_ORGANISATION_DEMO: Record<string, ProfilInterneDemo & { role: string }> = Object.fromEntries(
  ORGANISATION_DEMO
    .filter((m) => !ANNUAIRE.some((a) => a.id === m.id) && (PROFILS_ORGANISATION_DEMO[m.id]?.length ?? 0) > 0)
    .map((m) => {
      const role = PROFILS_ORGANISATION_DEMO[m.id][0]
      return [m.id, { identifiant: identifiantDemo(m.nomAffiche), nom: m.nomAffiche, sujet: m.id, permissions: PROFILS[role] ?? [], role }]
    }),
)

/** Profil d'une clé de démonstration : la clé elle-même (profil) ou le profil du compte de l'organisation (QUO-01). */
export const roleDeCleDemo = (cle: string): string => COMPTES_ORGANISATION_DEMO[cle]?.role ?? cle
