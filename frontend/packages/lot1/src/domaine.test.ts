import { describe, expect, it } from 'vitest'
import { PROFILS_DOMAINE, compositionPrivilegiee } from './domaine'
import { PROFILS } from './permissions'
import { PROFILS_INTERNES_DEMO, sessionDemoDuRole } from './profilsDemo'
import { PRESENTATIONS_PROFILS } from './services/simule/presentations'
import { PROFILS_PRIVILEGIES } from './services/simule/pouvoirs'
import { catalogues } from './i18n'

// Domaine Keycloak généré (domaine-gimac.json) : source unique des profils de l'interface et de la démonstration
// (recettes ADM-01, ADM-09, ACC-01, ACC-04, ACC-06). La concordance avec infra/keycloak/gimac-realm.json est vérifiée
// par packages/administration/src/services/simule/catalogue.test.ts.
describe('profils du domaine', () => {
  it('matrice PROFILS, présentations et profils privilégiés lus dans le domaine, sans recopie', () => {
    expect(Object.keys(PROFILS)).toEqual(PROFILS_DOMAINE.map((p) => p.code))
    for (const p of PROFILS_DOMAINE) {
      expect(PROFILS[p.code]).toEqual(p.permissions)
      expect(PRESENTATIONS_PROFILS[p.code]).toEqual({ libelle: p.libelle, description: p.description })
    }
    expect(PROFILS_PRIVILEGIES).toEqual(PROFILS_DOMAINE.filter((p) => compositionPrivilegiee(p.permissions)).map((p) => p.code))
    expect(PROFILS.ROLE_INGENIEUR_HOMOLOGATION).toContain('HOM_REPLAY_ADMIN')
  })

  it('chaque profil du domaine a son libellé d’interface en français, anglais et espagnol (ACC-04)', () => {
    for (const l of ['fr', 'en', 'es'] as const) {
      const cles = Object.assign({}, ...catalogues.map((c) => c[l])) as Record<string, string>
      expect(PROFILS_DOMAINE.filter((p) => !cles[`profil.${p.code}`]).map((p) => p.code), l).toEqual([])
    }
  })

  it('session de démonstration d’un rôle : compte interne ou compte d’institution, jamais un profil par défaut (ACC-01)', () => {
    const externe = PROFILS_DOMAINE.find((p) => p.externe)!
    expect(sessionDemoDuRole(externe.code)).toMatchObject({ roles: [externe.code], institutionId: 'INST-0042', permissions: externe.permissions })
    const [role, interne] = Object.entries(PROFILS_INTERNES_DEMO)[0]
    expect(sessionDemoDuRole(role, 'x.y')).toMatchObject({ identifiant: 'x.y', nom: interne.nom, institutionId: '', permissions: interne.permissions })
    expect(sessionDemoDuRole('ROLE_INCONNU')).toBeNull()
  })
})
