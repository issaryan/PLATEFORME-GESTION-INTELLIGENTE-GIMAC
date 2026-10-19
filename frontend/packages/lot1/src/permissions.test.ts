import { describe, expect, it } from 'vitest'
import { PERM, PERMISSIONS_NOUVELLES, PROFILS } from './permissions'
import { PROFILS_DOMAINE } from './domaine'

// Matrice des rôles du parcours (D-58, D-61) : invariants attendus du domaine Keycloak `gimac`
// (infra/keycloak/generer-realm-gimac.py) et des permissions nouvelles du modèle hybride (D-56).
const detenteurs = (permission: string) => Object.keys(PROFILS).filter((r) => PROFILS[r].includes(permission)).sort()
/** Profils de « Mon activité » sans fonction métier dans la plateforme (D-76 §4). */
const SANS_FONCTION = ['ROLE_COLLABORATEUR', 'ROLE_RESPONSABLE_SERVICE', 'ROLE_ADMIN_SYSTEME']
const LECTURE_DES = ['HOM_REFERENTIEL_READ', 'HOM_LIAISON_READ', 'HOM_LIAISON_LIST', 'HOM_DASHBOARD_READ', 'CER_CIRCUIT_READ', 'CER_DOSSIER_READ', 'CER_REGISTER_READ', 'PAR_INSTITUTION_READ']

describe('matrice des rôles (PROFILS)', () => {
  it('la DDRM seule ouvre la liaison (CR v1.7 §4.2.1) ; les deux chefs de département désignent le conducteur (D-58)', () => {
    expect(detenteurs(PERM.liaisonOuvrir)).toEqual(['ROLE_CHEF_DDRM'])
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
    // CR v1.7 §3.3 : l'agent de la sécurité des SI vérifie avec le RSSI, sans administration des comptes ni quitus.
    expect(detenteurs(PERM.securiteVerifier)).toEqual(['ROLE_AGENT_SSI', 'ROLE_RSSI'])
    expect(PROFILS.ROLE_AGENT_SSI.filter((p) => /^(IAM_|PLATFORM_|CER_QUITUS_)/.test(p))).toEqual([])
    expect(detenteurs(PERM.visaConformite)).toEqual(['ROLE_CONFORMITE'])
    expect(detenteurs(PERM.visaDg)).toEqual(['ROLE_DG'])
    // Personne ne cumule deux signatures de décision.
    const signatures = [PERM.pvSignerGimac, PERM.visaConformite, PERM.visaDg, PERM.securiteVerifier]
    Object.values(PROFILS).forEach((ps) => expect(signatures.filter((s) => ps.includes(s)).length).toBeLessThanOrEqual(1))
  })
  it('profils internes : lecture du DES commune (sauf collaborateur et responsable de service, D-76) ; correspondant : portail seul, sans lecture interne', () => {
    Object.entries(PROFILS).filter(([r]) => !PROFILS_DOMAINE.find((p) => p.code === r)?.externe && !SANS_FONCTION.includes(r)).forEach(([, ps]) => expect(ps).toEqual(expect.arrayContaining(LECTURE_DES)))
    // Le responsable de service édite le référentiel (D-89 §1) : sa seule lecture du DES est HOM_REFERENTIEL_READ.
    SANS_FONCTION.forEach((r) => expect(PROFILS[r].filter((p) => LECTURE_DES.includes(p))).toEqual(r === 'ROLE_RESPONSABLE_SERVICE' ? ['HOM_REFERENTIEL_READ'] : []))
    // Notifications du portail (D-93 §11.4) : boîte et préférences du correspondant.
    expect([...PROFILS.ROLE_CORRESPONDANT].sort()).toEqual([PERM.portailCertification, PERM.portailSigner, PERM.portailLire, PERM.portailDeposer, PERM.notificationsLire])
    expect(PROFILS.ROLE_CORRESPONDANT.some((p) => LECTURE_DES.includes(p))).toBe(false)
    // Rôles d'institution de la sandbox (D-115) : portail seul, sans aucune lecture interne ni validation de verdict.
    for (const p of PROFILS_DOMAINE.filter((x) => x.externe && x.code.startsWith('ROLE_SBX_'))) {
      expect(p.permissions.some((x) => LECTURE_DES.includes(x)), p.code).toBe(false)
      expect(p.permissions.filter((x) => !x.startsWith('SBX_')), p.code).toEqual([PERM.notificationsLire])
      expect(p.permissions.some((x) => x.endsWith('_VALIDATION')), p.code).toBe(false)
    }
  })
  it('conduite des étapes 4 à 10 : départements et conducteur, jamais les services exécutants', () => {
    expect(detenteurs(PERM.etapeConduire)).toEqual(['ROLE_CHARGE_DDRM', 'ROLE_CHEF_DDRM', 'ROLE_CHEF_DES', 'ROLE_CONDUCTEUR_PROJET'])
  })
  it('Mon activité (D-76 §4) : tout profil interne ; équipe et tâches confiées aux seuls responsables', () => {
    expect(detenteurs(PERM.activitePropre)).toEqual(Object.keys(PROFILS).filter((r) => !PROFILS_DOMAINE.find((p) => p.code === r)?.externe).sort())
    const responsables = ['ROLE_CHEF_DDRM', 'ROLE_CHEF_DES', 'ROLE_CONFORMITE', 'ROLE_DG', 'ROLE_RESPONSABLE_SERVICE', 'ROLE_RSSI']
    expect(detenteurs(PERM.equipeLire)).toEqual(responsables)
    expect(detenteurs(PERM.tacheConfier)).toEqual(responsables)
    expect([...PROFILS.ROLE_COLLABORATEUR].sort()).toEqual([PERM.messageLire, PERM.messageEnvoyer, PERM.notificationsLire, PERM.activitePropre])
  })
  it('notifications de la plateforme (D-80, D-93 §11.4) : NOT_INBOX_READ pour tout profil, correspondant compris (cloche du portail)', () => {
    expect(PERM.notificationsLire).toBe('NOT_INBOX_READ')
    expect(detenteurs(PERM.notificationsLire)).toEqual(Object.keys(PROFILS).sort())
  })
  it('exploitation de l’infrastructure (D-78) : lecture pour le super administrateur, l’administrateur système et le RSSI ; gestes sans le RSSI', () => {
    expect(detenteurs(PERM.exploitationLire)).toEqual(['ROLE_ADMIN_SYSTEME', 'ROLE_RSSI', 'ROLE_SUPER_ADMIN'])
    expect(detenteurs(PERM.exploitationExecuter)).toEqual(['ROLE_ADMIN_SYSTEME', 'ROLE_SUPER_ADMIN'])
    expect(PROFILS.ROLE_ADMIN_SYSTEME.some((p) => p.startsWith('IAM_'))).toBe(false)
  })
  it('référentiel (D-89 §1, §4) : édition, publication et lecture complète', () => {
    expect(detenteurs('HOM_REFERENTIEL_EDIT')).toEqual(['ROLE_CHEF_DDRM', 'ROLE_CHEF_DES', 'ROLE_INGENIEUR_HOMOLOGATION', 'ROLE_RESPONSABLE_SERVICE', 'ROLE_SUPER_ADMIN'])
    expect(detenteurs('HOM_REFERENTIEL_PUBLISH')).toEqual(['ROLE_CHEF_DDRM', 'ROLE_CHEF_DES', 'ROLE_INGENIEUR_HOMOLOGATION', 'ROLE_SUPER_ADMIN'])
    expect(detenteurs('HOM_REFERENTIEL_READ_ALL')).toEqual(['ROLE_CHEF_DDRM', 'ROLE_CHEF_DES', 'ROLE_CONFORMITE', 'ROLE_DG', 'ROLE_RSSI', 'ROLE_SUPER_ADMIN'])
  })
  it('permissions nouvelles du modèle hybride, toutes attribuées, sans doublon dans un profil', () => {
    expect([...PERMISSIONS_NOUVELLES]).toEqual(['HOM_STEP_MANAGE', 'CER_PV_ESTABLISH', 'CER_SECURITY_VERIFY', 'CER_PORTAL_SIGN'])
    PERMISSIONS_NOUVELLES.forEach((p) => expect(detenteurs(p).length).toBeGreaterThan(0))
    Object.values(PROFILS).forEach((ps) => expect(new Set(ps).size).toBe(ps.length))
  })
})

describe('qualités par permission (D-94)', () => {
  it('aucune table rôle → fonction : le conducteur porte HOM_PROJECT_CONDUCT, l’administration de tous les domaines REF_METIER_ADMIN', () => {
    expect(Object.keys(PROFILS).filter((r) => PROFILS[r].includes('HOM_PROJECT_CONDUCT'))).toEqual(['ROLE_CONDUCTEUR_PROJET'])
    expect(Object.keys(PROFILS).filter((r) => PROFILS[r].includes('REF_METIER_ADMIN')).sort()).toEqual(['ROLE_INGENIEUR_HOMOLOGATION', 'ROLE_SUPER_ADMIN'])
  })
  it('fonctions opérationnelles (D-97) : une permission par fonction, aux profils que le catalogue des fonctions associe', () => {
    expect(detenteurs(PERM.fonctionDdrm)).toEqual(['ROLE_CHARGE_DDRM'])
    expect(detenteurs(PERM.fonctionProduction)).toEqual(['ROLE_PRODUCTION_MONETIQUE'])
    expect(detenteurs(PERM.fonctionReseau)).toEqual(['ROLE_POLE_RESEAU'])
    expect(detenteurs(PERM.fonctionSysteme)).toEqual(['ROLE_POLE_SYSTEME'])
    // Exploitation et Supervision : tenue par la production monétique (aucun profil propre).
    expect(detenteurs(PERM.fonctionExploitation)).toEqual(['ROLE_PRODUCTION_MONETIQUE'])
    ;[PERM.fonctionDdrm, PERM.fonctionProduction, PERM.fonctionReseau, PERM.fonctionSysteme, PERM.fonctionExploitation].forEach((p) => expect(p).toMatch(/^HOM_FUNCTION_/))
  })
})
