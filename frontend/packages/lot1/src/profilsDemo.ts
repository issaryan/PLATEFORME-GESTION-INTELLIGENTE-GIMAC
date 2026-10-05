import { PROFILS } from './permissions'
import { ANNUAIRE } from './services/simule/messagerie'

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

export const PROFILS_INTERNES_DEMO: Record<string, ProfilInterneDemo> = Object.fromEntries(
  Object.entries(PROFILS)
    .filter(([role]) => role !== 'ROLE_CORRESPONDANT')
    .map(([role, permissions]) => {
      const hors = IDENTITES_HORS_ANNUAIRE[role]
      if (hors) return [role, { ...hors, permissions }]
      const p = ANNUAIRE.find((x) => x.profils?.includes(role)) ?? ANNUAIRE[0]
      return [role, { identifiant: identifiantDemo(p.nom), nom: p.nom, sujet: p.id, permissions }]
    }),
)
