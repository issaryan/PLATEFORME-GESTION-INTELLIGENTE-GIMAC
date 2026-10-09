import { afterEach, describe, expect, it } from 'vitest'
import { PROFILS, PROFILS_INTERNES_DEMO } from '@gimac/lot1'
import { PERMISSIONS_REALM, PROFILS_REALM } from './catalogue'
import { orchestrateurDemo, servicesConsoleSimules } from './index'
import { CLE_ORCHESTRATEUR_DEMO, PROFILS_DEMO } from '../../profilsDemo'
// Domaine généré, lu tel quel (texte) : aucune dépendance aux types de Node dans les sources des applications.
import realmBrut from '../../../../../../infra/keycloak/gimac-realm.json?raw'

// Recette ADM-01, ADM-09 : le catalogue simulé des profils et les profils de démonstration sont exactement ceux du
// domaine Keycloak généré (infra/keycloak/gimac-realm.json), relu ici avec les règles de lecture de MS-IAM
// (KeycloakAnnuaire) ; toute divergence fait échouer ce test. Régénérer : python3 infra/keycloak/generer-realm-gimac.py.

type Role = { name: string; description?: string; composite?: boolean; composites?: { realm?: string[] }; attributes?: Record<string, string[]> }
const realm = JSON.parse(realmBrut) as { roles: { realm: Role[] } }
const PERMISSION = /^[A-Z][A-Z0-9]*(_[A-Z0-9]+)+$/
const estPermission = (n: string) => PERMISSION.test(n) && !n.startsWith('ROLE_')
const roles = realm.roles.realm
const permissionsDomaine = roles.map((r) => r.name).filter(estPermission).sort()
const profilsDomaine = roles.filter((r) => r.name.startsWith('ROLE_') && r.composite).map((r) => ({
  code: r.name,
  libelle: r.attributes?.gimac_libelle?.[0],
  description: r.description ?? '',
  externe: r.attributes?.gimac_externe?.[0] === 'true',
  permissions: (r.composites?.realm ?? []).filter(estPermission).sort(),
}))
const composition = (code: string) => profilsDomaine.find((p) => p.code === code)!.permissions
const ASSERTION = btoa('assertion-simulee')

describe('catalogue simulé = domaine Keycloak (ADM-01)', () => {
  it('permissions et profils livrés identiques au domaine généré (libellé, mission, externe, composition)', () => {
    expect(profilsDomaine.length).toBeGreaterThan(10)
    expect([...PERMISSIONS_REALM]).toEqual(permissionsDomaine)
    expect(PROFILS_REALM.map((p) => ({ ...p, permissions: [...p.permissions].sort() }))).toEqual(profilsDomaine)
  })

  it('MS-IAM simulé sert les profils et les permissions du domaine ; seuls les profils IAM_* ou PLATFORM_* sont privilégiés', async () => {
    const c = servicesConsoleSimules({ latenceMs: 0 }).configuration
    expect(await c.permissions()).toEqual(permissionsDomaine)
    const servis = await c.profils()
    expect(servis.map((p) => ({ code: p.code, libelle: p.libelle, description: p.description, externe: p.externe, permissions: p.permissions }))).toEqual(profilsDomaine)
    expect(servis.filter((p) => p.privilegie).map((p) => p.code)).toEqual(profilsDomaine.filter((p) => p.permissions.some((x) => /^(IAM|PLATFORM)_/.test(x))).map((p) => p.code))
    // Messagerie de travail (D-64) : tout profil interne la porte, aucun profil externe.
    expect(profilsDomaine.filter((p) => !p.externe).every((p) => p.permissions.includes('COL_MESSAGE_READ'))).toBe(true)
    expect(composition('ROLE_SUPER_ADMIN')).toContain('COL_MESSAGE_READ')
  })
})

describe('catalogue des fonctionnalités modifiable (ACC-02)', () => {
  it('toute fonctionnalité du catalogue se modifie sans refus « Permission inconnue » (liaisons, mesActionsCertification, dossierCertification, portail)', async () => {
    const c = servicesConsoleSimules({ latenceMs: 0, acteur: { id: 'u-super-admin', nom: 'Super', permissions: PROFILS_DEMO.ROLE_SUPER_ADMIN.permissions } }).configuration
    const toutes = await c.fonctionnalites()
    for (const cle of ['liaisons', 'mesActionsCertification', 'dossierCertification', 'portail']) expect(toutes.some((f) => f.cle === cle), cle).toBe(true)
    for (const f of toutes) await expect(c.modifierFonctionnalite(f.cle, { ...f, visible: !f.visible }, 'Essai de la recette', ASSERTION), f.cle).resolves.toMatchObject({ cle: f.cle })
  })
})

describe('profils de démonstration = domaine Keycloak (ADM-09)', () => {
  it('console et application interne : chaque profil de démonstration porte exactement la composition du domaine', () => {
    for (const [role, p] of Object.entries(PROFILS_DEMO)) expect([...p.permissions].sort(), role).toEqual(composition(role))
    for (const [role, p] of Object.entries(PROFILS_INTERNES_DEMO)) expect([...p.permissions].sort(), role).toEqual(composition(role))
    expect(Object.keys(PROFILS).sort()).toEqual(profilsDomaine.map((p) => p.code).sort())
    for (const [role, ps] of Object.entries(PROFILS)) expect([...ps].sort(), role).toEqual(composition(role))
  })
})

describe('compte de l’administrateur système (ADM-02)', () => {
  it('présent dans l’annuaire avec son identité de démonstration ; profil privilégié non modifiable sans l’octroi', async () => {
    const rssi = servicesConsoleSimules({ latenceMs: 0, acteur: { id: 'u-rssi', nom: 'Hervé Kamga Tchoupo', permissions: PROFILS_DEMO.ROLE_RSSI.permissions } }).comptes
    const c = await rssi.lire(PROFILS_DEMO.ROLE_ADMIN_SYSTEME.sujet)
    expect(c).toMatchObject({ identifiant: PROFILS_DEMO.ROLE_ADMIN_SYSTEME.identifiant, nomAffiche: PROFILS_DEMO.ROLE_ADMIN_SYSTEME.nom, profils: ['ROLE_ADMIN_SYSTEME'] })
    expect((await rssi.lister({ page: 1, taille: 100, q: 'abessolo' })).total).toBe(1)
    await expect(rssi.definirProfils(c.id, ['ROLE_CONFORMITE'], 'Retrait du profil', ASSERTION)).rejects.toMatchObject({ status: 403, code: 'IAM_PRIVILEGED_PROFILE' })
  })
})

describe('orchestrateur de la démonstration (gestes Docker)', () => {
  afterEach(() => localStorage.removeItem(CLE_ORCHESTRATEUR_DEMO))
  it('Kubernetes par défaut ; DOCKER ou AUCUN par la préférence gm.orchestrateurDemo', async () => {
    expect(orchestrateurDemo()).toBe('KUBERNETES')
    localStorage.setItem(CLE_ORCHESTRATEUR_DEMO, 'docker')
    const etat = await servicesConsoleSimules({ latenceMs: 0 }).infrastructure.charges()
    expect(etat.orchestrateur).toBe('DOCKER')
    expect(etat.charges.every((x) => x.orchestrateur === 'DOCKER' && !x.gestes.includes('ECHELLE'))).toBe(true)
    localStorage.setItem(CLE_ORCHESTRATEUR_DEMO, 'AUCUN')
    expect(orchestrateurDemo()).toBe('AUCUN')
  })
})
