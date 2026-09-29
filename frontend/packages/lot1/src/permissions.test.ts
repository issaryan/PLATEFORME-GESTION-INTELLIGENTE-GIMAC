import { describe, expect, it } from 'vitest'
import { FONCTION_DU_ROLE, PERM, PERMISSIONS_NOUVELLES, PROFILS, fonctionsDe } from './permissions'

// Matrice des rôles du parcours (D-58, D-61) : invariants attendus du domaine Keycloak `gimac`
// (infra/keycloak/generer-realm-gimac.py) et des permissions nouvelles du modèle hybride (D-56).
const detenteurs = (permission: string) => Object.keys(PROFILS).filter((r) => PROFILS[r].includes(permission)).sort()
const LECTURE_DES = ['HOM_REFERENTIEL_READ', 'HOM_LIAISON_READ', 'HOM_LIAISON_LIST', 'HOM_DASHBOARD_READ', 'CER_CIRCUIT_READ', 'CER_DOSSIER_READ', 'CER_REGISTER_READ', 'PAR_INSTITUTION_READ']

describe('matrice des rôles (PROFILS)', () => {
  it('seuls les chefs de département ouvrent la liaison et désignent le conducteur (D-58)', () => {
    expect(detenteurs(PERM.liaisonOuvrir)).toEqual(['ROLE_CHEF_DDRM', 'ROLE_CHEF_DES'])
    expect(detenteurs(PERM.projetDesigner)).toEqual(['ROLE_CHEF_DDRM', 'ROLE_CHEF_DES'])
    expect(PROFILS.ROLE_INGENIEUR_HOMOLOGATION).not.toContain(PERM.liaisonOuvrir)
    expect(PROFILS.ROLE_CONDUCTEUR_PROJET).not.toContain(PERM.projetDesigner)
  })
  it('maker-checker de la DDRM : le chargé propose, seul le chef prononce', () => {
    expect(PROFILS.ROLE_CHARGE_DDRM).toContain('PAR_TRANSITION_REQUEST')
    expect(detenteurs(PERM.transitionApprouver)).toEqual(['ROLE_CHEF_DDRM'])
  })
  it('verrou 1 : le conducteur établit le procès-verbal, le chef du DES le signe pour le GIMAC', () => {
    expect(detenteurs(PERM.pvEtablir)).toEqual(['ROLE_CONDUCTEUR_PROJET'])
    expect(detenteurs(PERM.pvSignerGimac)).toEqual(['ROLE_CHEF_DES'])
    expect(detenteurs(PERM.portailSigner)).toEqual(['ROLE_CORRESPONDANT'])
  })
  it('verrou 2 et visas : RSSI, conformité et direction générale séparés', () => {
    expect(detenteurs(PERM.securiteVerifier)).toEqual(['ROLE_RSSI'])
    expect(detenteurs(PERM.visaConformite)).toEqual(['ROLE_CONFORMITE'])
    expect(detenteurs(PERM.visaDg)).toEqual(['ROLE_DG'])
    // Personne ne cumule deux signatures de décision.
    const signatures = [PERM.pvSignerGimac, PERM.visaConformite, PERM.visaDg, PERM.securiteVerifier]
    Object.values(PROFILS).forEach((ps) => expect(signatures.filter((s) => ps.includes(s)).length).toBeLessThanOrEqual(1))
  })
  it('profils internes : lecture du DES commune ; correspondant : portail seul, sans lecture interne', () => {
    Object.entries(PROFILS).filter(([r]) => r !== 'ROLE_CORRESPONDANT').forEach(([, ps]) => expect(ps).toEqual(expect.arrayContaining(LECTURE_DES)))
    expect(PROFILS.ROLE_CORRESPONDANT).toEqual([PERM.portailLire, PERM.portailDeposer, PERM.portailCertification, PERM.portailSigner])
    expect(PROFILS.ROLE_CORRESPONDANT.some((p) => LECTURE_DES.includes(p))).toBe(false)
  })
  it('conduite des étapes 4 à 10 : départements et conducteur, jamais les services exécutants', () => {
    expect(detenteurs(PERM.etapeConduire)).toEqual(['ROLE_CHARGE_DDRM', 'ROLE_CHEF_DDRM', 'ROLE_CHEF_DES', 'ROLE_CONDUCTEUR_PROJET'])
  })
  it('permissions nouvelles du modèle hybride, toutes attribuées, sans doublon dans un profil', () => {
    expect([...PERMISSIONS_NOUVELLES]).toEqual(['HOM_STEP_MANAGE', 'CER_PV_ESTABLISH', 'CER_SECURITY_VERIFY', 'CER_PORTAL_SIGN'])
    PERMISSIONS_NOUVELLES.forEach((p) => expect(detenteurs(p).length).toBeGreaterThan(0))
    Object.values(PROFILS).forEach((ps) => expect(new Set(ps).size).toBe(ps.length))
  })
})

describe('fonctions du parcours', () => {
  it('chaque profil porte une fonction ; l’ingénieur d’homologation relève du SNA', () => {
    expect(Object.keys(FONCTION_DU_ROLE).sort()).toEqual(Object.keys(PROFILS).sort())
    expect(FONCTION_DU_ROLE.ROLE_INGENIEUR_HOMOLOGATION).toBe('SNA')
    expect(FONCTION_DU_ROLE.ROLE_CORRESPONDANT).toBe('PARTICIPANT')
    expect(FONCTION_DU_ROLE.ROLE_CONDUCTEUR_PROJET).toBe('CONDUCTEUR')
  })
  it('fonctionsDe : dédoublonnées, rôles techniques ignorés, ordre des rôles conservé', () => {
    expect(fonctionsDe(['ROLE_SNA', 'ROLE_INGENIEUR_HOMOLOGATION', 'ROLE_USER', 'offline_access', 'ROLE_RSSI'])).toEqual(['SNA', 'RSSI'])
    expect(fonctionsDe(['ROLE_CHEF_DES', 'ROLE_CONDUCTEUR_PROJET'])).toEqual(['CHEF_DES', 'CONDUCTEUR'])
    expect(fonctionsDe([])).toEqual([])
    expect(fonctionsDe(['ROLE_SUPER_ADMIN'])).toEqual([])
  })
})
